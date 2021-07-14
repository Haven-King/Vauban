package dev.hephaestus.vauban.block.entity;

import dev.hephaestus.vauban.Vauban;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class DistributorBlockEntity extends PipelikeBlockEntity {
    private int lastDistributedSide = 0;

    public DistributorBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(Vauban.DISTRIBUTOR_BLOCK_ENTITY, blockPos, blockState, 3);
        this.inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, DistributorBlockEntity blockEntity) {
        --blockEntity.transferCooldown;
        blockEntity.lastTickTime = world.getTime();

        if (!blockEntity.needsCooldown()) {
            blockEntity.setCooldown(0);

            BlockPos.Mutable mut = new BlockPos.Mutable();

            for (int i = 1; i < 7; ++i) {
                Direction dir = Direction.byId(i + blockEntity.lastDistributedSide % 6);
                BlockEntity be = world.getBlockEntity(mut.set(pos).move(dir));

                if ((be instanceof PipeBlockEntity && world.getBlockState(mut).get(Properties.FACING) != dir.getOpposite()) || be instanceof FilterBlockEntity) {
                    PipelikeBlockEntity pipe = (PipelikeBlockEntity) be;

                    if (pipe.transferFrom(blockEntity)) {
                        blockEntity.setCooldown(8);
                        blockEntity.lastDistributedSide = dir.getId();
                        pipe.markDirty();
                        blockEntity.markDirty();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return this.allSlots;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return true;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }
}
