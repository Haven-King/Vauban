package dev.hephaestus.vauban.block.entity;

import dev.hephaestus.vauban.Vauban;
import dev.hephaestus.vauban.inventory.FilteredInventory;
import dev.hephaestus.vauban.screen.FilterBlockScreenHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.EnumMap;
import java.util.Map;

import static dev.hephaestus.vauban.block.FilterBlock.SLOTS;

public class FilterBlockEntity extends PipelikeBlockEntity implements ExtendedScreenHandlerFactory, FilteredInventory, BlockEntityClientSerializable {
    private static final Text NAME = new TranslatableText("vauban.filter");

    private static final Direction[] DIRECTIONS = Direction.values();
    private final Map<Integer, ItemStack> filters;
    private final Map<Direction, Map<Integer, ItemStack>> sidedFilters;

    public FilterBlockEntity(BlockPos pos, BlockState state) {
        super(Vauban.FILTER_BLOCK_ENTITY, pos, state, 54);
        this.inventory = DefaultedList.ofSize(54, ItemStack.EMPTY);
        this.filters = new Int2ObjectOpenHashMap<>();
        this.sidedFilters = new EnumMap<>(Direction.class);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        this.filters.clear();

        NbtCompound filters = nbt.getCompound("Filters");

        for (String key : filters.getKeys()) {
            this.setFilter(Integer.parseInt(key), ItemStack.fromNbt(filters.getCompound(key)));
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        NbtCompound filters = new NbtCompound();

        for (Map.Entry<Integer, ItemStack> entry : this.filters.entrySet()) {
            filters.put(entry.getKey().toString(), entry.getValue().writeNbt(new NbtCompound()));
        }

        nbt.put("Filters", filters);

        return nbt;
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, FilterBlockEntity blockEntity) {
        --blockEntity.transferCooldown;

        if (!blockEntity.needsCooldown()) {
            blockEntity.setCooldown(0);

            BlockPos.Mutable mut = new BlockPos.Mutable();

            for (Direction dir : DIRECTIONS) {
                BlockEntity target = world.getBlockEntity(mut.set(pos).move(dir));

                if (target instanceof PipelikeBlockEntity pipeLike && world.getBlockState(mut).get(Properties.FACING) != dir.getOpposite()) {
                    for (int ourSlot : SLOTS[dir.getId()]) {
                        if (pipeLike.insert(blockEntity.inventory.get(ourSlot))) {
                            blockEntity.setCooldown(8);
                            pipeLike.markDirty();
                            blockEntity.markDirty();
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean insert(ItemStack stack, int slot) {
        return (!this.sideHasFilter(slot) || (this.filters.containsKey(slot) && ItemStack.canCombine(this.filters.get(slot), stack))) && super.insert(stack, slot);
    }

    @Override
    public int size() {
        return 54;
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
    public boolean canPlayerUse(PlayerEntity player) {
        if (this.world != null && this.world.getBlockEntity(this.pos) != this) {
            return false;
        } else {
            return !(player.squaredDistanceTo((double)this.pos.getX() + 0.5D, (double)this.pos.getY() + 0.5D, (double)this.pos.getZ() + 0.5D) > 64.0D);
        }
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return this.allSlots;
    }

    public int[] getVisibleSlots(Direction side) {
        return SLOTS[side.getId()];
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        ItemStack filter = this.getFilter(slot);
        boolean sideHasFilter = false;

        for (int[] side : SLOTS) {
            if (side[0] <= slot && side[8] >= slot) {
                for (int s : side) {
                    if (!this.getFilter(s).isEmpty()) sideHasFilter = true;
                }
            }
        }

        return !sideHasFilter || ItemStack.canCombine(filter, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public void setFilter(int slot, @Nullable ItemStack stack) {
        if (stack != null) {
            stack = stack.isEmpty() && this.filters.containsKey(slot) && this.filters.get(slot).isEmpty() ? new ItemStack(Items.BARRIER) : stack.copy();
            stack.setCount(1);
            this.sidedFilters.computeIfAbsent(fromSlot(slot), dir -> new Int2ObjectOpenHashMap<>()).put(slot, stack);
        } else {
            this.sidedFilters.computeIfAbsent(fromSlot(slot), dir -> new Int2ObjectOpenHashMap<>()).remove(slot);
        }

        this.filters.put(slot, stack);
    }

    @Override
    public boolean hasFilter(Direction direction) {
        return !this.sidedFilters.computeIfAbsent(direction, dir -> new Int2ObjectOpenHashMap<>()).isEmpty();
    }

    @Override
    public boolean sideHasFilter(int slot) {
        return hasFilter(fromSlot(slot));
    }

    @Override
    public Direction fromSlot(@Range(from = 0, to = 53) int slot) {
        if (slot <= 8) return Direction.UP;
        if (slot <= 17) return Direction.EAST;
        if (slot <= 26) return Direction.NORTH;
        if (slot <= 35) return Direction.WEST;
        if (slot <= 44) return Direction.SOUTH;

        return Direction.DOWN;
    }

    @Override
    public Text getDisplayName() {
        return NAME;
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new FilterBlockScreenHandler(syncId, inv, this);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.getPos());
    }

    @Override
    public @NotNull ItemStack getFilter(int slot) {
        return this.filters.getOrDefault(slot, ItemStack.EMPTY);
    }

    @Override
    public void fromClientTag(NbtCompound tag) {
        this.readNbt(tag);
    }

    @Override
    public NbtCompound toClientTag(NbtCompound tag) {
        return this.writeNbt(tag);
    }
}
