package dev.hephaestus.vauban.client;

import dev.hephaestus.vauban.Vauban;
import dev.hephaestus.vauban.block.entity.FilterBlockEntity;
import dev.hephaestus.vauban.block.entity.PipeBlockEntity;
import dev.hephaestus.vauban.screen.ingame.FilterScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.model.ModelProviderContext;
import net.fabricmc.fabric.api.client.model.ModelResourceProvider;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class VaubanClient implements ClientModInitializer, ModelResourceProvider, HudRenderCallback {
    private static final SpriteIdentifier[] COPPER = new SpriteIdentifier[] {
            new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/copper_block")),
            new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/exposed_copper")),
            new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/weathered_copper")),
            new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/oxidized_copper"))
    };

    private final Map<Identifier, UnbakedModel> models = new HashMap<>();

    @Override
    public void onInitializeClient() {
        this.registerCopperVariants("pipe", PipeBlockModel::new);
        this.registerCopperVariants("filter", FilterBlockModel::new);
        this.registerCopperVariants("redstone_pipe", sprite -> new RedstonePipeBlockModel(sprite, new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/redstone_block"))));

        this.models.put(Vauban.id("block/buffer"), new ThickPipeBlockModel(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Vauban.id("block/buffer"))));
        this.models.put(Vauban.id("block/distributor"), new ThickPipeBlockModel(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/gold_block"))));

        ModelLoadingRegistry.INSTANCE.registerResourceProvider(manager -> this);

        ScreenRegistry.register(Vauban.FILTER_BLOCK_SCREEN_HANDLER, FilterScreen::new);
    }

    private void registerCopperVariants(String name, Function<SpriteIdentifier, UnbakedModel> function) {
        this.models.put(Vauban.id("block/copper_" + name), function.apply(COPPER[0]));
        this.models.put(Vauban.id("block/exposed_copper_" + name), function.apply(COPPER[1]));
        this.models.put(Vauban.id("block/weathered_copper_" + name), function.apply(COPPER[2]));
        this.models.put(Vauban.id("block/oxidized_copper_" + name), function.apply(COPPER[3]));
    }

    @Override
    public @Nullable UnbakedModel loadModelResource(Identifier resourceId, ModelProviderContext context) {
        return this.models.get(resourceId);
    }

    @Override
    public void onHudRender(MatrixStack matrixStack, float tickDelta) {

        if (MinecraftClient.getInstance().crosshairTarget instanceof BlockHitResult blockHitResult) {
            World world = MinecraftClient.getInstance().world;
            BlockPos pos = blockHitResult.getBlockPos();

            if (world != null) {
                Inventory inventory =
                        world.getBlockState(pos).isOf(Vauban.PIPE_BLOCK) && world.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity ? pipeBlockEntity
                        : world.getBlockState(pos).isOf(Vauban.FILTER_BLOCK) && world.getBlockEntity(pos) instanceof FilterBlockEntity filterBlockEntity ? filterBlockEntity
                        : null;

                int[] slots = inventory instanceof FilterBlockEntity filter ? filter.getVisibleSlots(blockHitResult.getSide()) : new int[] {0, 1, 2};

                if (inventory != null) {

                    int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
                    int height = MinecraftClient.getInstance().getWindow().getScaledHeight();

                    ItemRenderer renderer = MinecraftClient.getInstance().getItemRenderer();
                    TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

                    for (int slot : slots) {
                        int x = width / 2 + 9 + 18 * (slot % 3);
                        int y = height / 2 - 9 + 18 * (slot / 3);
                        ItemStack stack = inventory.getStack(slot);

                        if (!stack.isEmpty()) {
                            renderer.renderInGuiWithOverrides(stack, x, y);
                            renderer.renderGuiItemOverlay(textRenderer, stack, x, y);
                        }
                    }
                }
            }
        }
    }

}
