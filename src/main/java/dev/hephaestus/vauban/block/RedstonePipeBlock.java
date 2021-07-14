package dev.hephaestus.vauban.block;

import dev.hephaestus.vauban.Vauban;
import dev.hephaestus.vauban.block.entity.PipeBlockEntity;
import dev.hephaestus.vauban.block.entity.RedstonePipeBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class RedstonePipeBlock extends PipeBlock<RedstonePipeBlockEntity> {
    public RedstonePipeBlock(Settings settings) {
        super(settings, () -> Vauban.REDSTONE_PIPE_BLOCK_ENTITY, RedstonePipeBlockEntity::new, PipeBlockEntity::serverTick);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(Properties.POWERED);
    }

    @Override
    public @NotNull BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(Properties.POWERED, false);
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(Properties.POWERED) && direction == state.get(Properties.FACING).getOpposite() ? 15 : 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return this.getWeakRedstonePower(state, world, pos, direction);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        world.setBlockState(pos, state.with(Properties.POWERED, false));
    }
}
