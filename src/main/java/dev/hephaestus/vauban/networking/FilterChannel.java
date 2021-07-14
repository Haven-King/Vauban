package dev.hephaestus.vauban.networking;

import dev.hephaestus.vauban.Vauban;
import dev.hephaestus.vauban.block.entity.FilterBlockEntity;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class FilterChannel implements ModInitializer {
    private static final Identifier ID = Vauban.id("channel", "c2s", "set_filter");

    @Environment(EnvType.CLIENT)
    public static void setFilter(FilterBlockEntity filterBlockEntity, int slot, @Nullable ItemStack filter) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

        buf.writeBlockPos(filterBlockEntity.getPos());
        buf.writeVarInt(slot);
        buf.writeBoolean(filter != null);

        if (filter != null) {
            buf.writeNbt(filter.writeNbt(new NbtCompound()));
        }

        ClientPlayNetworking.send(ID, buf);
    }

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.INIT.register(((handler, server) -> ServerPlayNetworking.registerReceiver(handler, ID, this::setFilter)));
    }

    private void setFilter(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity, ServerPlayNetworkHandler serverPlayNetworkHandler, PacketByteBuf buf, PacketSender packetSender) {
        BlockPos pos = buf.readBlockPos();
        int slot = buf.readVarInt();
        @Nullable ItemStack stack = buf.readBoolean() ? ItemStack.fromNbt(buf.readNbt()) : null;

        minecraftServer.execute(() -> {
            if (serverPlayerEntity.world.getBlockEntity(pos) instanceof FilterBlockEntity filter) {
                filter.setFilter(slot, stack);
            } else {
                Vauban.log("Player {} tried to set filter for filter at {}, but it does not exist.", serverPlayerEntity.getName().asString(), pos.toString());
            }
        });
    }
}
