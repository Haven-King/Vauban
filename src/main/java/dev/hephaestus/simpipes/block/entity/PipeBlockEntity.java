package dev.hephaestus.simpipes.block.entity;

import dev.hephaestus.simpipes.Simpipes;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PipeBlockEntity extends BlockEntity implements BlockEntityClientSerializable, SidedInventory {
    private DefaultedList<ItemStack> inventory;

    private static final int[] NONE = new int[0];
    private static final int[] SLOTS = new int[] {0, 1, 2};

    private int transferCooldown;
    private long lastTickTime;

    public PipeBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(Simpipes.PIPE_BLOCK_ENTITY, blockPos, blockState);
        this.inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
    }

    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
        Inventories.readNbt(nbt, this.inventory);

        this.transferCooldown = nbt.getInt("TransferCooldown");
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        Inventories.writeNbt(nbt, this.inventory);
        nbt.putInt("TransferCooldown", this.transferCooldown);

        return nbt;
    }

    private boolean needsCooldown() {
        return this.transferCooldown > 0;
    }

    private void setCooldown(int cooldown) {
        this.transferCooldown = cooldown;
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, PipeBlockEntity blockEntity) {
        --blockEntity.transferCooldown;
        blockEntity.lastTickTime = world.getTime();
        if (!blockEntity.needsCooldown()) {
            blockEntity.setCooldown(0);

            BlockEntity target = world.getBlockEntity(pos.offset(state.get(Properties.FACING)));

            if (target instanceof PipeBlockEntity pipeBlock) {
                boolean movedThings = false;

                transferLoop: for (int i = 0; i< blockEntity.inventory.size(); ++i) {
                    ItemStack ourStack = blockEntity.inventory.get(i);

                    if (!ourStack.isEmpty()) {
                        for (int j = 0; j < pipeBlock.inventory.size(); ++j) {
                            ItemStack theirStack = pipeBlock.inventory.get(j);

                            if (theirStack.isEmpty()) {
                                pipeBlock.getInventory().set(j, ourStack.split(1));
                                movedThings = true;
                                break transferLoop;
                            } else if (ItemStack.canCombine(ourStack, theirStack) && theirStack.getCount() < theirStack.getMaxCount()) {
                                theirStack.increment(1);
                                ourStack.decrement(1);
                                movedThings = true;
                                break transferLoop;
                            }
                        }
                    }
                }

                if (movedThings) {
                    int k = 0;

                    if (pipeBlock.lastTickTime >= blockEntity.lastTickTime) {
                        k = 1;
                    }

                    blockEntity.setCooldown(8 - k);
                    pipeBlock.markDirty();
                    blockEntity.markDirty();
                }
            } else if (target instanceof Inventory inventory) {
                boolean movedThings = false;

                transferLoop:
                for (int i = 0; i< blockEntity.inventory.size(); ++i) {
                    ItemStack ourStack = blockEntity.inventory.get(i);

                    if (!ourStack.isEmpty()) {
                        for (int j = 0; j < inventory.size(); ++j) {
                            ItemStack theirStack = inventory.getStack(j);

                            if (theirStack.isEmpty()) {
                                inventory.setStack(j, ourStack.split(1));
                                movedThings = true;
                                break transferLoop;
                            } else if (ItemStack.canCombine(ourStack, theirStack) && theirStack.getCount() < theirStack.getMaxCount()) {
                                theirStack.increment(1);
                                ourStack.decrement(1);
                                movedThings = true;
                                break transferLoop;
                            }
                        }
                    }
                }

                if (movedThings) {
                    blockEntity.setCooldown(8);
                    inventory.markDirty();
                    blockEntity.markDirty();
                }
            }
        }
    }

    @Override
    public int size() {
        return this.inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);

        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();

        if (this.world != null && !this.world.isClient) {
            this.sync();
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return false;
    }

    @Override
    public void fromClientTag(NbtCompound tag) {
        this.readNbt(tag);
    }

    @Override
    public NbtCompound toClientTag(NbtCompound tag) {
        return this.writeNbt(tag);
    }

    public DefaultedList<ItemStack> getInventory() {
        return this.inventory;
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return side != this.getCachedState().get(Properties.FACING) ? SLOTS : NONE;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return dir != this.getCachedState().get(Properties.FACING);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }
}
