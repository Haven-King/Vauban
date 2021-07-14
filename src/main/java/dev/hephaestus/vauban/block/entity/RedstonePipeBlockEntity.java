package dev.hephaestus.vauban.block.entity;

import dev.hephaestus.vauban.Vauban;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class RedstonePipeBlockEntity extends PipeBlockEntity {
    public RedstonePipeBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(Vauban.REDSTONE_PIPE_BLOCK_ENTITY, blockPos, blockState);
    }

    @Override
    public boolean insert(ItemStack stack, Inventory into, Direction from) {
        boolean result = super.insert(stack, into, from);

        if (result && this.world != null) {
            this.world.setBlockState(this.pos, this.getCachedState().with(Properties.POWERED, true));
            this.world.getBlockTickScheduler().schedule(this.pos, this.getCachedState().getBlock(), 1);
        }

        return result;
    }
}
