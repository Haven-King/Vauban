package dev.hephaestus.vauban.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public interface FilteredInventory {
    void setFilter(int slot, @Nullable ItemStack stack);

    boolean hasFilter(Direction direction);

    boolean sideHasFilter(int slot);

    Direction fromSlot(@Range(from = 0, to = 53) int slot);

    @NotNull ItemStack getFilter(int slot);
}
