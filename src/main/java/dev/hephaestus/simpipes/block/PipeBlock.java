package dev.hephaestus.simpipes.block;

import dev.hephaestus.simpipes.Simpipes;
import dev.hephaestus.simpipes.block.entity.PipeBlockEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

public class PipeBlock extends BlockWithEntity {
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

    public PipeBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(Properties.FACING, UP, DOWN, NORTH, SOUTH, EAST, WEST);
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

        if (neighborState.isOf(this)) {
            Direction neighborFacing = neighborState.get(Properties.FACING);

            if (neighborFacing.equals(direction.getOpposite())) {
                connectionType = neighborState.getBlock() instanceof Inventory inventory && (!(inventory instanceof SidedInventory sided) || sided.getAvailableSlots(direction).length > 0)
                        ? ConnectionType.CONTAINER
                        : ConnectionType.PIPE;
            }
        } else if (world.getBlockEntity(neighborPos) instanceof Inventory) {
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

    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getState(this.getDefaultState().with(Properties.FACING, ctx.getSide().getOpposite()), ctx.getWorld(), ctx.getBlockPos());
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return this.update(state, direction, neighborState, world, pos, neighborPos);
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
        return new PipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, Simpipes.PIPE_BLOCK_ENTITY, PipeBlockEntity::serverTick);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
                ItemScatterer.spawn(world, pos, pipeBlockEntity.getInventory());
                world.updateComparators(pos, this);
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

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
