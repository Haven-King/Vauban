package dev.hephaestus.vauban.block;

import dev.hephaestus.vauban.block.properties.BlockProperties;
import dev.hephaestus.vauban.block.properties.OxidizationLevel;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Random;

public abstract class OxidizableBlock extends Block {
    protected OxidizableBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(OxidizationLevel.PROPERTY, BlockProperties.WAXED);
    }

    @NotNull
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(OxidizationLevel.PROPERTY, OxidizationLevel.UNAFFECTED).with(BlockProperties.WAXED, false);
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return !state.get(BlockProperties.WAXED) && state.get(OxidizationLevel.PROPERTY) != OxidizationLevel.OXIDIZED;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (random.nextFloat() < 0.05688889F) {
            this.tryDegrade(state, world, pos, random);
        }
    }

    private Optional<BlockState> getDegradationResult(BlockState state) {
        return !state.get(BlockProperties.WAXED) && state.get(OxidizationLevel.PROPERTY) != OxidizationLevel.OXIDIZED
                ? Optional.of(state.with(OxidizationLevel.PROPERTY, OxidizationLevel.VALUES[state.get(OxidizationLevel.PROPERTY).ordinal() + 1]))
                : Optional.empty();
    }

    private void tryDegrade(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        OxidizationLevel level = state.get(OxidizationLevel.PROPERTY);
        int i = level.ordinal();
        int j = 0;
        int k = 0;

        for (BlockPos blockPos : BlockPos.iterateOutwards(pos, 4, 4, 4)) {
            int l = blockPos.getManhattanDistance(pos);
            if (l > 4) {
                break;
            }

            if (!blockPos.equals(pos)) {
                BlockState blockState = world.getBlockState(blockPos);
                Block block = blockState.getBlock();

                int m = -1;

                if (block.getStateManager().getProperties().contains(OxidizationLevel.PROPERTY)) {
                    m = blockState.get(OxidizationLevel.PROPERTY).ordinal();
                } else if (block instanceof net.minecraft.block.OxidizableBlock oxidizable) {
                    m = oxidizable.getDegradationLevel().ordinal();
                }

                if (m >= 0) {
                    if (m < i) {
                        return;
                    }

                    if (m > i) {
                        ++k;
                    } else {
                        ++j;
                    }
                }
            }
        }

        float f = (float)(k + 1) / (float)(k + j + 1);
        float g = f * f * level.degradationChanceMultiplier;
        if (random.nextFloat() < g) {
            this.getDegradationResult(state).ifPresent((statex) -> world.setBlockState(pos, statex));
        }
    }
}
