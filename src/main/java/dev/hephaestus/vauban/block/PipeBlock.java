package dev.hephaestus.vauban.block;

import dev.hephaestus.vauban.Vauban;
import dev.hephaestus.vauban.block.properties.OxidizationLevel;
import dev.hephaestus.vauban.block.properties.BlockProperties;
import dev.hephaestus.vauban.networking.AlternatePlacementChannel;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class PipeBlock<T extends BlockEntity> extends OxidizableBlock implements Waterloggable, BlockEntityProvider {
    public static final EnumProperty<ConnectionType> UP = EnumProperty.of("up", ConnectionType.class);
    public static final EnumProperty<ConnectionType> DOWN = EnumProperty.of("down", ConnectionType.class);
    public static final EnumProperty<ConnectionType> NORTH = EnumProperty.of("north", ConnectionType.class);
    public static final EnumProperty<ConnectionType> SOUTH = EnumProperty.of("south", ConnectionType.class);
    public static final EnumProperty<ConnectionType> EAST = EnumProperty.of("east", ConnectionType.class);
    public static final EnumProperty<ConnectionType> WEST = EnumProperty.of("west", ConnectionType.class);

    private static final VoxelShape NODE = Block.createCuboidShape(6, 6, 6, 10, 10, 10);

    private static final VoxelShape UP_PIPE = Block.createCuboidShape(6, 10, 6, 10, 16, 10);
    private static final VoxelShape DOWN_PIPE = Block.createCuboidShape(6, 0, 6, 10, 6, 10);
    private static final VoxelShape NORTH_PIPE = Block.createCuboidShape(6, 6, 0, 10, 10, 6);
    private static final VoxelShape SOUTH_PIPE = Block.createCuboidShape(6, 6, 10, 10, 10, 16);
    private static final VoxelShape EAST_PIPE = Block.createCuboidShape(10, 6, 6, 16, 10, 10);
    private static final VoxelShape WEST_PIPE = Block.createCuboidShape(0, 6, 6, 6, 10, 10);

    private static final Map<Integer, VoxelShape> SHAPES = new Int2ObjectArrayMap<>();

    private final Supplier<BlockEntityType<T>> blockEntityType;
    private final BiFunction<BlockPos, BlockState, T> blockEntitySupplier;
    private final BlockEntityTicker<T> blockEntityTicker;

    public PipeBlock(Settings settings, Supplier<BlockEntityType<T>> blockEntityType, BiFunction<BlockPos, BlockState, T> blockEntitySupplier, BlockEntityTicker<T> blockEntityTicker) {
        super(settings);
        this.blockEntityType = blockEntityType;
        this.blockEntitySupplier = blockEntitySupplier;
        this.blockEntityTicker = blockEntityTicker;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(Properties.FACING, UP, DOWN, NORTH, SOUTH, EAST, WEST, Properties.WATERLOGGED);
    }

    private static Integer key(BlockState state) {
        int key = state.get(Properties.FACING).ordinal() << Integer.bitCount(DIRECTIONS.length);

        for (Direction direction : DIRECTIONS) {
            key |= state.get(fromDirection(direction)).ordinal();
            key <<= Integer.bitCount(ConnectionType.values().length);
        }

        return key;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return context.isHolding(Vauban.PIPE_ITEM) || context.isHolding(Vauban.BUFFER_ITEM) || context.isHolding(Vauban.DISTRIBUTOR_ITEM) || context.isHolding(Vauban.FILTER_ITEM) ? VoxelShapes.fullCube() : this.getCollisionShape(state, world, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES.computeIfAbsent(key(state), key -> {
            Direction facing = state.get(Properties.FACING);

            return VoxelShapes.union(NODE,
                    state.get(UP) != ConnectionType.NONE || facing == Direction.UP ? UP_PIPE : VoxelShapes.empty(),
                    state.get(DOWN) != ConnectionType.NONE || facing == Direction.DOWN ? DOWN_PIPE : VoxelShapes.empty(),
                    state.get(NORTH) != ConnectionType.NONE || facing == Direction.NORTH ? NORTH_PIPE : VoxelShapes.empty(),
                    state.get(SOUTH) != ConnectionType.NONE || facing == Direction.SOUTH ? SOUTH_PIPE : VoxelShapes.empty(),
                    state.get(EAST) != ConnectionType.NONE || facing == Direction.EAST ? EAST_PIPE : VoxelShapes.empty(),
                    state.get(WEST) != ConnectionType.NONE || facing == Direction.WEST ? WEST_PIPE : VoxelShapes.empty()
            );
        });
    }

    private BlockState update(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        ConnectionType connectionType = state.get(fromDirection(direction));

        if (neighborState.isIn(Vauban.PIPE_BLOCKS)) {
            Direction neighborFacing = neighborState.get(Properties.FACING);

            if (neighborFacing.equals(direction.getOpposite())) {
                connectionType = ConnectionType.PIPE;
            }
        } else if (neighborState.getBlock() instanceof FilterBlock) {
            connectionType = ConnectionType.CONTAINER;
        } else if (neighborState.isIn(Vauban.PIPELIKE_BLOCKS)) {
            connectionType = ConnectionType.PIPE;
        } else if (world.getBlockEntity(neighborPos) instanceof Inventory && (direction == state.get(Properties.FACING) || (neighborState.isOf(Blocks.HOPPER) && neighborState.get(Properties.HOPPER_FACING) == direction.getOpposite()))) {
            connectionType = ConnectionType.CONTAINER;
        } else {
            connectionType = ConnectionType.NONE;
        }

        return state.with(fromDirection(direction), connectionType);
    }

    private BlockState getState(BlockState state, WorldAccess world, BlockPos pos) {
        BlockPos.Mutable mut = new BlockPos.Mutable();

        for (Direction direction : Direction.values()) {
            mut.set(pos);
            mut.move(direction);

            BlockState neighborState = world.getBlockState(mut);

            state = this.update(state, direction, neighborState, world, pos, mut);
        }

        return state;
    }

    public @NotNull BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getSide().getOpposite();
        PlayerEntity player = ctx.getPlayer();

        if (player != null && AlternatePlacementChannel.isAlternatePlacing(player)) {
            facing = player.getPitch() <= -45 ? Direction.DOWN : player.getPitch() >= 45 ? Direction.UP :ctx.getPlayerFacing().getOpposite();
        }

        return this.getState(super.getPlacementState(ctx).with(Properties.FACING, facing).with(Properties.WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER), ctx.getWorld(), ctx.getBlockPos());
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

    public static EnumProperty<ConnectionType> fromDirection(Direction direction) {
        return switch(direction) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return this.blockEntitySupplier.apply(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <B extends BlockEntity> BlockEntityTicker<B> getTicker(World world, BlockState state, BlockEntityType<B> type) {
        return world.isClient ? null : type == this.blockEntityType.get() ? (BlockEntityTicker<B>) this.blockEntityTicker : null;
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
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    public enum ConnectionType implements StringIdentifiable {
        NONE, PIPE, CONTAINER;

        @Override
        public String asString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
