package dev.hephaestus.vauban.block.entity;

import dev.hephaestus.vauban.Vauban;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.client.render.WorldRenderer.DIRECTIONS;

public class BufferBlockEntity extends PipelikeBlockEntity {
    private static final int[] NONE = new int[0];
    private static final int[] SLOTS = new int[] {0, 1, 2};

    private long lastTickTime;

    public BufferBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(Vauban.BUFFER_BLOCK_ENTITY, blockPos, blockState, 3);
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

    public static void serverTick(World world, BlockPos pos, BlockState state, BufferBlockEntity blockEntity) {
        --blockEntity.transferCooldown;
        blockEntity.lastTickTime = world.getTime();

        if (!blockEntity.needsCooldown()) {
            blockEntity.setCooldown(0);

            BlockPos targetPos = pos.offset(state.get(Properties.FACING));
            BlockEntity target = world.getBlockEntity(targetPos);

            if (target instanceof PipeBlockEntity || target instanceof FilterBlockEntity) {
                PipelikeBlockEntity pipeBlock = (PipelikeBlockEntity) target;
                if (pipeBlock.transferFrom(blockEntity)) {
                    int k = 0;

                    if (pipeBlock.getLastTickTime() >= blockEntity.lastTickTime) {
                        k = 1;
                    }

                    blockEntity.setCooldown(8 - k);
                    blockEntity.markDirty();
                } else {
                    BlockPos.Mutable mut = new BlockPos.Mutable();

                    for (Direction dir : DIRECTIONS) {
                        BlockEntity be = world.getBlockEntity(mut.set(pos).move(dir));

                        if (be instanceof PipeBlockEntity pipe && world.getBlockState(mut).get(Properties.FACING) != dir.getOpposite()) {
                            for (int ourSlot = 0; ourSlot < blockEntity.size(); ++ourSlot) {
                                if (pipe.insert(blockEntity.inventory.get(ourSlot))) {
                                    blockEntity.setCooldown(8);
                                    pipe.markDirty();
                                    blockEntity.markDirty();
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return side != this.getCachedState().get(Properties.FACING) ? SLOTS : NONE;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return dir != this.getCachedState().get(Properties.FACING);
    }
}
