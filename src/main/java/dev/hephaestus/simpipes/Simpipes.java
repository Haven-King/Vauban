package dev.hephaestus.simpipes;

import dev.hephaestus.simpipes.block.PipeBlock;
import dev.hephaestus.simpipes.block.entity.PipeBlockEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;

public class Simpipes implements ModInitializer {
    public static final String MOD_ID = "simpipes";

    public static final Block PIPE_BLOCK = Registry.register(Registry.BLOCK, id("pipe"), new PipeBlock(FabricBlockSettings.of(Material.METAL, MapColor.CLEAR).breakByTool(FabricToolTags.PICKAXES, 1).requiresTool().strength(5.0F, 6.0F).sounds(BlockSoundGroup.METAL).nonOpaque()));
    public static final Item PIPE_ITEM = Registry.register(Registry.ITEM, id("pipe"), new BlockItem(PIPE_BLOCK, new FabricItemSettings()));

    public static final BlockEntityType<PipeBlockEntity> PIPE_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, id("pipe"), FabricBlockEntityTypeBuilder.create(PipeBlockEntity::new, PIPE_BLOCK).build());

    public static Identifier id(@NotNull String path, String... paths) {
        return new Identifier(MOD_ID, path + (paths.length == 0 ? "" : "." + String.join(".", paths)));
    }

    @Override
    public void onInitialize() {

    }
}
