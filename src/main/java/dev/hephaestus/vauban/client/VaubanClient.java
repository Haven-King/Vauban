package dev.hephaestus.vauban.client;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import dev.hephaestus.vauban.Vauban;
import dev.hephaestus.vauban.screen.ingame.FilterScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.model.ModelProviderContext;
import net.fabricmc.fabric.api.client.model.ModelResourceProvider;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class VaubanClient implements ClientModInitializer, ModelResourceProvider, ItemTooltipCallback {
    @SuppressWarnings("deprecation")
    private static final SpriteIdentifier[] COPPER = new SpriteIdentifier[] {
            new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/copper_block")),
            new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/exposed_copper")),
            new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/weathered_copper")),
            new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/oxidized_copper"))
    };

    private static final Multimap<Item, Text> TOOLTIPS = LinkedHashMultimap.create();

    private final Map<Identifier, UnbakedModel> models = new HashMap<>();

    @Override
    @SuppressWarnings("deprecation")
    public void onInitializeClient() {
        this.registerCopperVariants("pipe", PipeBlockModel::new);
        this.registerCopperVariants("filter", FilterBlockModel::new);
        this.registerCopperVariants("redstone_pipe", sprite -> new RedstonePipeBlockModel(sprite, new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/redstone_block"))));

        this.models.put(Vauban.id("block/buffer"), new ThickPipeBlockModel(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Vauban.id("block/buffer"))));
        this.models.put(Vauban.id("block/distributor"), new ThickPipeBlockModel(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier("block/gold_block"))));

        ModelLoadingRegistry.INSTANCE.registerResourceProvider(manager -> this);

        ScreenRegistry.register(Vauban.FILTER_BLOCK_SCREEN_HANDLER, FilterScreen::new);

        registerTooltips(Vauban.BUFFER_ITEM, 1);
        registerTooltips(Vauban.DISTRIBUTOR_ITEM, 1);
        registerTooltips(Vauban.FILTER_ITEM, 7);
        registerTooltips(Vauban.REDSTONE_PIPE_ITEM, 2);

        ItemTooltipCallback.EVENT.register(this);
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

    private static void registerTooltips(Item item, @Range(from=1, to=Integer.MAX_VALUE) int tooltips) {
        String key = Registry.ITEM.getId(item).toString().replace(":", ".");

        for (int i = 0; i < tooltips; ++i) {
            TOOLTIPS.put(item, new TranslatableText(String.format("item.%s.tooltip%d", key, i)).formatted(Formatting.GRAY));
        }
    }

    @Override
    public void getTooltip(ItemStack stack, TooltipContext context, List<Text> lines) {
        lines.addAll(TOOLTIPS.get(stack.getItem()));
    }
}
