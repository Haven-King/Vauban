package dev.hephaestus.simpipes.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.hephaestus.simpipes.Simpipes;
import dev.hephaestus.simpipes.block.entity.PipeBlockEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.model.ModelProviderContext;
import net.fabricmc.fabric.api.client.model.ModelResourceProvider;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@Environment(net.fabricmc.api.EnvType.CLIENT)
public class SimpipesClient implements ClientModInitializer, ModelResourceProvider, HudRenderCallback {
    private static boolean IS_HOVERING_PIPE = false;
    private final Map<Identifier, UnbakedModel> models = new HashMap<>();

    @Override
    public void onInitializeClient() {
        this.models.put(Simpipes.id("block/copper_pipe"), new PipeModel(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/copper_block"))));
//        this.models.put(Simpipes.id("block/exposed_copper_pipe"), new PipeModel(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/exposed_copper"))));
//        this.models.put(Simpipes.id("block/weathered_copper_pipe"), new PipeModel(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/weathered_copper"))));
//        this.models.put(Simpipes.id("block/oxidized_copper_pipe"), new PipeModel(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/oxidized_copper"))));

        ModelLoadingRegistry.INSTANCE.registerResourceProvider(manager -> this);

        HudRenderCallback.EVENT.register(this);
    }

    @Override
    public @Nullable UnbakedModel loadModelResource(Identifier resourceId, ModelProviderContext context) {
        return this.models.get(resourceId);
    }

    @Override
    public void onHudRender(MatrixStack matrixStack, float tickDelta) {
        IS_HOVERING_PIPE = false;

        if (MinecraftClient.getInstance().crosshairTarget instanceof BlockHitResult blockHitResult) {
            World world = MinecraftClient.getInstance().world;
            BlockPos pos = blockHitResult.getBlockPos();

            if (world != null
                    && world.getBlockState(pos).isOf(Simpipes.PIPE_BLOCK)
                    && world.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity
            ) {
                DefaultedList<ItemStack> inventory = pipeBlockEntity.getInventory();
                int slots = (int) inventory.stream().filter(Predicate.not(ItemStack::isEmpty)).count();

                if (slots > 0) {
                    IS_HOVERING_PIPE = true;
                    int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
                    int height = MinecraftClient.getInstance().getWindow().getScaledHeight();

                    ItemRenderer renderer = MinecraftClient.getInstance().getItemRenderer();
                    TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

                    int x = width / 2 + 9;

                    for (ItemStack stack : inventory) {
                        if (!stack.isEmpty()) {
                            renderer.renderInGui(stack, x, height / 2 - 9);
                            renderer.renderGuiItemOverlay(textRenderer, stack, x, height / 2 - 9);
                            x += 18;
                        }
                    }
                }
            }
        }
    }

    public static boolean isInspectingPipe() {
        return IS_HOVERING_PIPE;
    }
}
