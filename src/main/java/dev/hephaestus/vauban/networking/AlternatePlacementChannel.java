package dev.hephaestus.vauban.networking;

import dev.hephaestus.vauban.Vauban;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.WeakHashMap;

public class AlternatePlacementChannel implements ModInitializer, ClientModInitializer {
    private static final Identifier ID = Vauban.id("channel", "c2s", "alternate_placement");

    @Environment(EnvType.CLIENT)
    private static final KeyBinding KEY_BINDING = new KeyBinding("key.vauban.alt", GLFW.GLFW_KEY_LEFT_ALT, "Vauban");

    private static final Map<ServerPlayerEntity, Boolean> ALTERNATE_PLACEMENT = new WeakHashMap<>();

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.INIT.register((handler, server) -> ServerPlayNetworking.registerReceiver(handler, ID, (server1, player, handler1, buf, responseSender) -> {
            boolean bl = buf.readBoolean();

            server1.execute(() -> ALTERNATE_PLACEMENT.put(player, bl));
        }));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(KEY_BINDING);
        ClientTickEvents.START_CLIENT_TICK.register(this::updateAlternatePlacement);
    }

    @Environment(EnvType.CLIENT)
    private void updateAlternatePlacement(MinecraftClient client) {
        if (client.world != null) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeBoolean(KEY_BINDING.isPressed());
            ClientPlayNetworking.send(ID, buf);
        }
    }

    public static boolean isAlternatePlacing(PlayerEntity playerEntity) {
        if (playerEntity instanceof ServerPlayerEntity serverPlayerEntity) return ALTERNATE_PLACEMENT.get(serverPlayerEntity);
        if (playerEntity.world.isClient) return KEY_BINDING.isPressed();

        throw new IllegalArgumentException();
    }
}
