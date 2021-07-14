package dev.hephaestus.vauban.block.entity;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

import java.util.stream.IntStream;

public interface Pipelike extends SidedInventory {
    default boolean insert(ItemStack stack, int slot) {
        ItemStack ourStack = this.getStack(slot);

        if (ourStack.isEmpty()) {
            this.setStack(slot, stack.split(1));
            return true;
        } else if (ItemStack.canCombine(stack, ourStack) && ourStack.getCount() < ourStack.getMaxCount()) {
            ourStack.increment(1);
            stack.decrement(1);
            return true;
        }

        return false;
    }

    default boolean insert(ItemStack stack) {
        if (!stack.isEmpty()) {
            for (int theirSlot = 0; theirSlot < this.size(); ++theirSlot) {
                if (this.insert(stack, theirSlot)) {
                    return true;
                }
            }
        }

        return false;
    }

    default boolean transferFrom(Inventory from) {
        for (int theirSlot = 0; theirSlot < from.size(); ++theirSlot) {
            ItemStack theirStack = from.getStack(theirSlot);

            if (!theirStack.isEmpty()) {
                for (int ourSlot = 0; ourSlot < this.size(); ++ourSlot) {
                    if (this.insert(theirStack, ourSlot)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    default boolean insert(ItemStack stack, Inventory inventory, Direction from) {
        if (!stack.isEmpty()) {
            int[] slots = inventory instanceof SidedInventory sided
                    ? sided.getAvailableSlots(from.getOpposite())
                    : IntStream.range(0, inventory.size()).toArray();

            for (int slot : slots) {
                if (inventory instanceof SidedInventory sided && !sided.canInsert(slot, stack, from)) continue;

                ItemStack theirStack = inventory.getStack(slot);

                if (theirStack.isEmpty()) {
                    inventory.setStack(slot, stack.split(1));
                    return true;
                } else if (ItemStack.canCombine(stack, theirStack) && theirStack.getCount() < theirStack.getMaxCount()) {
                    theirStack.increment(1);
                    stack.decrement(1);
                    return true;
                }
            }
        }

        return false;
    }

    default boolean isFull() {
        for (int i = 0; i < this.size(); ++i) {
            ItemStack stack = this.getStack(i);

            if (stack.getCount() < stack.getMaxCount()) {
                return false;
            }
        }

        return true;
    }

    @Override
    default boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }
}
