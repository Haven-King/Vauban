package dev.hephaestus.vauban.block;

import dev.hephaestus.vauban.Vauban;
import dev.hephaestus.vauban.networking.AlternatePlacementChannel;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ThickPipeBlock<T extends BlockEntity> extends BlockWithEntity implements Waterloggable {
    public static final BooleanProperty UP = BooleanProperty.of("up");
    public static final BooleanProperty DOWN = BooleanProperty.of("down");
    public static final BooleanProperty NORTH = BooleanProperty.of("north");
    public static final BooleanProperty SOUTH = BooleanProperty.of("south");
    public static final BooleanProperty EAST = BooleanProperty.of("east");
    public static final BooleanProperty WEST = BooleanProperty.of("west");

    private static final VoxelShape NODE = Block.createCuboidShape(5, 5, 5, 11, 11, 11);

    private static final VoxelShape UP_PIPE = Block.createCuboidShape(5, 11, 5, 11, 16, 11);
    private static final VoxelShape DOWN_PIPE = Block.createCuboidShape(5, 0, 5, 11, 5, 11);
    private static final VoxelShape NORTH_PIPE = Block.createCuboidShape(5, 5, 0, 11, 11, 5);
    private static final VoxelShape SOUTH_PIPE = Block.createCuboidShape(5, 5, 11, 11, 11, 16);
    private static final VoxelShape EAST_PIPE = Block.createCuboidShape(11, 5, 5, 16, 11, 11);
    private static final VoxelShape WEST_PIPE = Block.createCuboidShape(0, 5, 5, 5, 11, 11);

    private static final Map<Integer, VoxelShape> SHAPES = new Int2ObjectArrayMap<>();

    private final Supplier<BlockEntityType<T>> blockEntityType;
    private final BiFunction<BlockPos, BlockState, T> blockEntitySupplier;
    private final BlockEntityTicker<T> blockEntityTicker;

    public ThickPipeBlock(Settings settings, Supplier<BlockEntityType<T>> blockEntityType, BiFunction<BlockPos, BlockState, T> blockEntitySupplier, BlockEntityTicker<T> blockEntityTicker) {
        super(settings);
        this.blockEntityType = blockEntityType;
        this.blockEntitySupplier = blockEntitySupplier;
        this.blockEntityTicker = blockEntityTicker;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.FACING, UP, DOWN, NORTH, SOUTH, EAST, WEST, Properties.WATERLOGGED);
    }

    private static Integer key(BlockState state) {
        int key = state.get(Properties.FACING).ordinal() << Integer.bitCount(DIRECTIONS.length);

        for (Direction direction : DIRECTIONS) {
            key |= state.get(fromDirection(direction)) ? 1 : 0;
            key <<= 1;
        }

        return key;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (context instanceof EntityShapeContext ctx) {
            Optional<Entity> entity = ctx.getEntity();

            if (entity.isPresent() && entity.get() instanceof PlayerEntity player && player.getMainHandStack().isIn(Vauban.PIPELIKE_ITEMS)) {
                return VoxelShapes.fullCube();
            }
        }

        return this.getCollisionShape(state, world, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES.computeIfAbsent(key(state), key -> VoxelShapes.union(NODE,
                state.get(UP) ? UP_PIPE : VoxelShapes.empty(),
                state.get(DOWN) ? DOWN_PIPE : VoxelShapes.empty(),
                state.get(NORTH) ? NORTH_PIPE : VoxelShapes.empty(),
                state.get(SOUTH) ? SOUTH_PIPE : VoxelShapes.empty(),
                state.get(EAST) ? EAST_PIPE : VoxelShapes.empty(),
                state.get(WEST) ? WEST_PIPE : VoxelShapes.empty()
        ));
    }

    private BlockState update(BlockState state, Direction direction, BlockState neighborState) {
        return state.with(fromDirection(direction), neighborState.isIn(Vauban.PIPE_BLOCKS) || neighborState.isOf(Vauban.FILTER_BLOCK) || (neighborState.isOf(Blocks.HOPPER) && neighborState.get(Properties.HOPPER_FACING) == direction.getOpposite()));
    }

    private BlockState getState(BlockState state, WorldAccess world, BlockPos pos) {
        BlockPos.Mutable mut = new BlockPos.Mutable();

        for (Direction direction : Direction.values()) {
            mut.set(pos);
            mut.move(direction);

            BlockState neighborState = world.getBlockState(mut);

            state = this.update(state, direction, neighborState);
        }

        return state;
    }

    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getSide().getOpposite();
        PlayerEntity player = ctx.getPlayer();

        if (player != null && AlternatePlacementChannel.isAlternatePlacing(player)) {
            facing = player.getHorizontalFacing().getOpposite();
        }

        return this.getState(this.getDefaultState().with(Properties.FACING, facing).with(Properties.WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER), ctx.getWorld(), ctx.getBlockPos());
    }

    public FluidState getFluidState(BlockState state) {
        return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(Properties.WATERLOGGED)) {
            world.getFluidTickScheduler().schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        return this.getState(state, world, pos);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return this.blockEntitySupplier.apply(pos, state);
    }

    @Nullable
    @Override
    public <B extends BlockEntity> BlockEntityTicker<B> getTicker(World world, BlockState state, BlockEntityType<B> type) {
        return world.isClient ? null : checkType(type, this.blockEntityType.get(), this.blockEntityTicker);
    }

    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
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
}
