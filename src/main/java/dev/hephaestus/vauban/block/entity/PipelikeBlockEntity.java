package dev.hephaestus.vauban.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.util.stream.IntStream;

public abstract class PipelikeBlockEntity extends BlockEntity implements Inventory, Pipelike, CoolsDown {
    protected static final int[] NONE = new int[0];

    private final int inventorySize;

    protected final int[] allSlots;

    protected DefaultedList<ItemStack> inventory;
    protected int transferCooldown;
    protected long lastTickTime;

    protected PipelikeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize) {
        super(type, pos, state);
        this.inventorySize = inventorySize;
        this.allSlots = IntStream.range(0, inventorySize).toArray();
    }

    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        this.inventory = DefaultedList.ofSize(this.inventorySize, ItemStack.EMPTY);
        Inventories.readNbt(nbt, this.inventory);

        this.transferCooldown = nbt.getInt("TransferCooldown");
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        Inventories.writeNbt(nbt, this.inventory);

        nbt.putInt("TransferCooldown", this.transferCooldown);

        return nbt;
    }

    @Override
    public final long getLastTickTime() {
        return this.lastTickTime;
    }

    @Override
    public final boolean needsCooldown() {
        return this.transferCooldown > 0;
    }

    @Override
    public final void setCooldown(int cooldown) {
        this.transferCooldown = cooldown;
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return false;
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
}
