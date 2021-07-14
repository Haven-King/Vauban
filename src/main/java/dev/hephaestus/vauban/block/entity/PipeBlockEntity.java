package dev.hephaestus.vauban.block.entity;

import dev.hephaestus.vauban.Vauban;
import dev.hephaestus.vauban.inventory.FilteredInventory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PipeBlockEntity extends PipelikeBlockEntity {
    public PipeBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(Vauban.PIPE_BLOCK_ENTITY, blockPos, blockState, 3);
        this.inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
    }

    protected PipeBlockEntity(BlockEntityType<?> type, BlockPos blockPos, BlockState blockState) {
        super(type, blockPos, blockState, 3);
        this.inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
    }

    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
        Inventories.readNbt(nbt, this.inventory);
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        Inventories.writeNbt(nbt, this.inventory);

        return nbt;
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
        --blockEntity.transferCooldown;
        blockEntity.lastTickTime = world.getTime();

        if (!blockEntity.needsCooldown()) {
            blockEntity.setCooldown(0);

            Direction dir = state.get(Properties.FACING);
            BlockEntity target = world.getBlockEntity(pos.offset(dir));

            if (target instanceof PipelikeBlockEntity pipeLike) {
                boolean movedThings = false;

                transferLoop:
                for (int ourSlot = 0; ourSlot < blockEntity.inventory.size(); ++ourSlot) {
                    ItemStack ourStack = blockEntity.inventory.get(ourSlot);

                    if (!ourStack.isEmpty()) {
                        for (int theirSlot = 0; theirSlot < pipeLike.size(); ++theirSlot) {
                            if (pipeLike.insert(ourStack, theirSlot)) {
                                movedThings = true;
                                break transferLoop;
                            }
                        }
                    }
                }

                if (movedThings) {
                    int k = 0;

                    if (pipeLike.getLastTickTime() >= blockEntity.lastTickTime) {
                        k = 1;
                    }

                    blockEntity.setCooldown(8 - k);
                    pipeLike.markDirty();
                    blockEntity.markDirty();
                }
            } else if (target instanceof Inventory inventory) {
                for (int ourSlot = 0; ourSlot < blockEntity.size(); ++ourSlot) {
                    if (blockEntity.insert(blockEntity.getStack(ourSlot), inventory, dir)) {
                        blockEntity.setCooldown(8);
                        inventory.markDirty();
                        blockEntity.markDirty();
                        return;
                    }
                }
            }
        }
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return side != this.getCachedState().get(Properties.FACING) ? this.allSlots : NONE;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return dir != this.getCachedState().get(Properties.FACING);
    }
}
