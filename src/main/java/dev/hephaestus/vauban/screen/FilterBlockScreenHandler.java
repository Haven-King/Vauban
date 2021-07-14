package dev.hephaestus.vauban.screen;

import dev.hephaestus.vauban.Vauban;
import dev.hephaestus.vauban.block.FilterBlock;
import dev.hephaestus.vauban.block.entity.FilterBlockEntity;
import dev.hephaestus.vauban.networking.FilterChannel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

public class FilterBlockScreenHandler extends ScreenHandler {
    private final FilterBlockEntity filter;

    public FilterBlockScreenHandler(int syncId, PlayerInventory playerInventory, FilterBlockEntity filter) {
        super(Vauban.FILTER_BLOCK_SCREEN_HANDLER, syncId);
        this.filter = filter;
        this.filter.onOpen(playerInventory.player);

        this.addSlots(Direction.UP, 69, 17);
        this.addSlots(Direction.EAST, 8, 78);
        this.addSlots(Direction.NORTH, 69, 78);
        this.addSlots(Direction.WEST, 130, 78);
        this.addSlots(Direction.SOUTH, 191, 78);
        this.addSlots(Direction.DOWN, 69, 139);

        for(int y = 0; y < 3; ++y) {
            for(int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 130 + x * 18, 103 + y * 18 + 45));
            }
        }

        for(int slot = 0; slot < 9; ++slot) {
            this.addSlot(new Slot(playerInventory, slot, 130 + slot * 18, 206));
        }
    }

    public static FilterBlockScreenHandler create(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        return playerInventory.player.world.getBlockEntity(buf.readBlockPos()) instanceof FilterBlockEntity filter
                ? new FilterBlockScreenHandler(syncId, playerInventory, filter)
                : null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.filter.canPlayerUse(player);
    }

    public ItemStack transferSlot(PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasStack()) {
            ItemStack itemStack2 = slot.getStack();
            itemStack = itemStack2.copy();
            if (index < 54) {
                if (!this.insertItem(itemStack2, 54, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(itemStack2, 0, 54, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return itemStack;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < 54 && actionType == SlotActionType.QUICK_MOVE && this.filter.getStack(slotIndex).isEmpty()) {
            this.filter.setFilter(slotIndex, this.getCursorStack());
            FilterChannel.setFilter(this.filter, slotIndex, this.getCursorStack());
        } else {
            if (slotIndex >= 0 && slotIndex < 54 && !this.getCursorStack().isEmpty()) {
                ItemStack filter = this.getFilter(slotIndex);

                if (!filter.isEmpty() && !ItemStack.canCombine(filter, this.getCursorStack())) return;
            }

            super.onSlotClick(slotIndex, button, actionType, player);
        }
    }

    public void close(PlayerEntity playerEntity) {
        super.close(playerEntity);
        this.filter.onClose(playerEntity);
    }

    private void addSlots(Direction direction, int x, int y) {
        int i = 0;

        for (int slot : FilterBlock.SLOTS[direction.getId()]) {
            this.addSlot(new FilteredSlot(this.filter, slot, x + (i % 3) * 18, y + (i / 3) * 18));
            ++i;
        }
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        ItemStack filter = this.getFilter(slot.id);

        return filter.isEmpty() || ItemStack.canCombine(filter, stack);
    }

    public @NotNull ItemStack getFilter(int slot) {
        return this.filter.getFilter(slot);
    }

    private class FilteredSlot extends Slot {
        public FilteredSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            ItemStack filter = FilterBlockScreenHandler.this.getFilter(this.getIndex());

            boolean sideHasFilter = false;

            for (int i = 0; i < 9; ++i) {
                ItemStack slotFilter = FilterBlockScreenHandler.this.getFilter(this.id - this.id % 9 + i);

                if (!slotFilter.isEmpty()) sideHasFilter = true;
            }

            return (sideHasFilter && ItemStack.canCombine(filter, stack)) || (!sideHasFilter && filter.isEmpty());
        }
    }
}
