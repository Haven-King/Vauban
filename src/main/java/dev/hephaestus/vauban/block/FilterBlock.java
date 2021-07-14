package dev.hephaestus.vauban.block;

import dev.hephaestus.vauban.Vauban;
import dev.hephaestus.vauban.block.entity.FilterBlockEntity;
import dev.hephaestus.vauban.block.properties.BlockProperties;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class FilterBlock extends OxidizableBlock implements Waterloggable, BlockEntityProvider {
    public static final int[][] SLOTS = new int[][] {
            {45, 46, 47, 48, 49, 50, 51, 52, 53},
            {0, 1, 2, 3, 4, 5, 6, 7, 8},
            {18, 19, 20, 21, 22, 23, 24, 25, 26},
            {36, 37, 38, 39, 40, 41, 42, 43, 44},
            {27, 28, 29, 30, 31, 32, 33, 34, 35},
            {9, 10, 11, 12, 13, 14, 15, 16, 17}
    };

    public static final BooleanProperty UP = BooleanProperty.of("up");
    public static final BooleanProperty DOWN = BooleanProperty.of("down");
    public static final BooleanProperty NORTH = BooleanProperty.of("north");
    public static final BooleanProperty SOUTH = BooleanProperty.of("south");
    public static final BooleanProperty EAST = BooleanProperty.of("east");
    public static final BooleanProperty WEST = BooleanProperty.of("west");

    private static final VoxelShape NODE = Block.createCuboidShape(3, 3, 3, 13, 13, 13);

    private static final VoxelShape UP_PIPE = Block.createCuboidShape(3, 13, 3, 13, 16, 13);
    private static final VoxelShape DOWN_PIPE = Block.createCuboidShape(3, 0, 3, 13, 3, 13);
    private static final VoxelShape NORTH_PIPE = Block.createCuboidShape(3, 3, 0, 13, 13, 3);
    private static final VoxelShape SOUTH_PIPE = Block.createCuboidShape(3, 3, 13, 13, 13, 16);
    private static final VoxelShape EAST_PIPE = Block.createCuboidShape(13, 3, 3, 16, 13, 13);
    private static final VoxelShape WEST_PIPE = Block.createCuboidShape(0, 3, 3, 3, 13, 13);

    private static final Map<Integer, VoxelShape> SHAPES = new Int2ObjectArrayMap<>();

    public FilterBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(UP, DOWN, NORTH, SOUTH, EAST, WEST, Properties.WATERLOGGED);
    }

    private static Integer key(BlockState state) {
        int key = 0;

        for (Direction direction : DIRECTIONS) {
            key |= state.get(fromDirection(direction)) ? 1 : 0;
            key <<= 1;
        }

        return key;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES.computeIfAbsent(key(state), key -> VoxelShapes.union(NODE,
                state.get(UP) ? UP_PIPE : VoxelShapes.empty(),
                state.get(DOWN) ? DOWN_PIPE : VoxelShapes.empty(),
                state.get(NORTH) ? NORTH_PIPE : VoxelShapes.empty(),
                state.get(SOUTH) ? SOUTH_PIPE : VoxelShapes.empty(),
                state.get(EAST) ? EAST_PIPE : VoxelShapes.empty(),
                state.get(WEST) ? WEST_PIPE : VoxelShapes.empty()
        ));
    }

    private BlockState getState(BlockState state, WorldView world, BlockPos pos) {
        BlockPos.Mutable mut = new BlockPos.Mutable();

        for (Direction direction : DIRECTIONS) {
            BlockState neighborState = world.getBlockState(mut.set(pos).move(direction));

            state = state.with(fromDirection(direction), neighborState.isIn(Vauban.PIPELIKE_BLOCKS) || (neighborState.isOf(Blocks.HOPPER) && neighborState.get(Properties.HOPPER_FACING) == direction.getOpposite()));
        }

        return state;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getState(this.getDefaultState().with(Properties.WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER), ctx.getWorld(), ctx.getBlockPos()).with(BlockProperties.WAXED, false);
    }

    public FluidState getFluidState(BlockState state) {
        return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(Properties.WATERLOGGED)) {
            world.getFluidTickScheduler().schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        return getState(state, world, pos);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FilterBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof FilterBlockEntity filter ? new ScreenHandlerFactory(filter) : null;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof Inventory inventory) {
                ItemScatterer.spawn(world, pos, inventory);
                world.updateComparators(pos, this);
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        } else {
            if (world.getBlockEntity(pos) instanceof FilterBlockEntity filter) {
                player.openHandledScreen(filter);
            }

            return ActionResult.CONSUME;
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, Vauban.FILTER_BLOCK_ENTITY, FilterBlockEntity::serverTick);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> checkType(BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
    }

    public static BooleanProperty fromDirection(Direction direction) {
        return switch(direction) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    private static record ScreenHandlerFactory(FilterBlockEntity filter) implements NamedScreenHandlerFactory {
        private static final Text NAME = new TranslatableText("vauban.filter");

        @Override
        public Text getDisplayName() {
            return NAME;
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
            return GenericContainerScreenHandler.createGeneric9x6(syncId, inv, filter);
        }
    }
}
