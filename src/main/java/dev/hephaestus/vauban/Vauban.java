package dev.hephaestus.vauban;

import dev.hephaestus.vauban.block.RedstonePipeBlock;
import dev.hephaestus.vauban.block.ThickPipeBlock;
import dev.hephaestus.vauban.block.FilterBlock;
import dev.hephaestus.vauban.block.PipeBlock;
import dev.hephaestus.vauban.block.entity.*;
import dev.hephaestus.vauban.block.properties.OxidizationLevel;
import dev.hephaestus.vauban.block.properties.BlockProperties;
import dev.hephaestus.vauban.item.OxidizableBlockItem;
import dev.hephaestus.vauban.screen.FilterBlockScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.Tag;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class Vauban implements ModInitializer {
    public static final String MOD_ID = "vauban";
    private static final Logger LOGGER = LogManager.getLogger("Vauban");

    public static final Tag<Block> PIPE_BLOCKS = TagRegistry.block(id("pipes"));
    public static final Tag<Block> PIPELIKE_BLOCKS = TagRegistry.block(id("pipelike"));

    public static final Block PIPE_BLOCK = register("pipe", new PipeBlock<>(FabricBlockSettings.of(Material.METAL, MapColor.ORANGE).breakByTool(FabricToolTags.PICKAXES, 1).requiresTool().strength(5.0F, 6.0F).sounds(BlockSoundGroup.COPPER).nonOpaque(), () -> Vauban.PIPE_BLOCK_ENTITY, PipeBlockEntity::new, PipeBlockEntity::serverTick));
    public static final Block FILTER_BLOCK = register("filter", new FilterBlock(FabricBlockSettings.of(Material.METAL, MapColor.ORANGE).breakByTool(FabricToolTags.PICKAXES, 1).requiresTool().strength(5F, 6F).sounds(BlockSoundGroup.COPPER).nonOpaque()));
    public static final Block BUFFER_BLOCK = register("buffer", new ThickPipeBlock<>(FabricBlockSettings.of(Material.METAL, MapColor.BLACK).breakByTool(FabricToolTags.PICKAXES, 1).requiresTool().strength(5F, 6F).sounds(BlockSoundGroup.METAL).nonOpaque(), () -> Vauban.BUFFER_BLOCK_ENTITY, BufferBlockEntity::new, BufferBlockEntity::serverTick));
    public static final Block DISTRIBUTOR_BLOCK = register("distributor", new ThickPipeBlock<>(FabricBlockSettings.of(Material.METAL, MapColor.GOLD).breakByTool(FabricToolTags.PICKAXES, 1).requiresTool().strength(5F, 6F).sounds(BlockSoundGroup.METAL).nonOpaque(), () -> Vauban.DISTRIBUTOR_BLOCK_ENTITY, DistributorBlockEntity::new, DistributorBlockEntity::serverTick));
    public static final Block REDSTONE_PIPE_BLOCK = register("redstone_pipe", new RedstonePipeBlock(FabricBlockSettings.of(Material.METAL, MapColor.ORANGE).breakByTool(FabricToolTags.PICKAXES, 1).requiresTool().strength(5.0F, 6.0F).sounds(BlockSoundGroup.COPPER).nonOpaque()));

    public static final ItemGroup ITEM_GROUP = FabricItemGroupBuilder.create(id("items")).icon(() -> new ItemStack(Vauban.PIPE_ITEM)).build();

    public static final Tag<Item> PIPELIKE_ITEMS = TagRegistry.item(id("pipelike"));

    public static final Item PIPE_ITEM = register("pipe", new OxidizableBlockItem(PIPE_BLOCK, new FabricItemSettings().group(ITEM_GROUP)));
    public static final Item FILTER_ITEM = register("filter", new OxidizableBlockItem(FILTER_BLOCK, new FabricItemSettings().group(ITEM_GROUP)));
    public static final Item BUFFER_ITEM = register("buffer", new BlockItem(BUFFER_BLOCK, new FabricItemSettings().group(ITEM_GROUP)));
    public static final Item DISTRIBUTOR_ITEM = register("distributor", new BlockItem(DISTRIBUTOR_BLOCK, new FabricItemSettings().group(ITEM_GROUP)));
    public static final Item REDSTONE_PIPE_ITEM = register("redstone_pipe", new BlockItem(REDSTONE_PIPE_BLOCK, new FabricItemSettings().group(ITEM_GROUP)));

    public static final BlockEntityType<PipeBlockEntity> PIPE_BLOCK_ENTITY = register("pipe", FabricBlockEntityTypeBuilder.create(PipeBlockEntity::new, PIPE_BLOCK).build());
    public static final BlockEntityType<FilterBlockEntity> FILTER_BLOCK_ENTITY = register("filter", FabricBlockEntityTypeBuilder.create(FilterBlockEntity::new, FILTER_BLOCK).build());
    public static final BlockEntityType<BufferBlockEntity> BUFFER_BLOCK_ENTITY = register("buffer", FabricBlockEntityTypeBuilder.create(BufferBlockEntity::new, BUFFER_BLOCK).build());
    public static final BlockEntityType<DistributorBlockEntity> DISTRIBUTOR_BLOCK_ENTITY = register("distributor", FabricBlockEntityTypeBuilder.create(DistributorBlockEntity::new, DISTRIBUTOR_BLOCK).build());
    public static final BlockEntityType<RedstonePipeBlockEntity> REDSTONE_PIPE_BLOCK_ENTITY = register("redstone_pipe", FabricBlockEntityTypeBuilder.create(RedstonePipeBlockEntity::new, REDSTONE_PIPE_BLOCK).build());

    public static final ScreenHandlerType<FilterBlockScreenHandler> FILTER_BLOCK_SCREEN_HANDLER = ScreenHandlerRegistry.registerExtended(id("filter"), FilterBlockScreenHandler::create);

    public static Identifier id(@NotNull String path, String... paths) {
        return new Identifier(MOD_ID, path + (paths.length == 0 ? "" : "." + String.join(".", paths)));
    }

    public static void log(String message, String... args) {
        LOGGER.info(message, (Object[]) args);
    }

    private static <T extends Block> T register(String id, T block) {
        return Registry.register(Registry.BLOCK, id(id), block);
    }

    private static <T extends Item> T register(String id, T item) {
        return Registry.register(Registry.ITEM, id(id), item);
    }

    private static <B extends BlockEntity, T extends BlockEntityType<B>> T register(String id, T blockEntityType) {
        return Registry.register(Registry.BLOCK_ENTITY_TYPE, id(id), blockEntityType);
    }

    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register(Vauban::scrapeBlock);
        UseBlockCallback.EVENT.register(Vauban::waxBlock);
    }

    private static ActionResult scrapeBlock(PlayerEntity playerEntity, World world, Hand hand, BlockHitResult blockHitResult) {
        ItemStack stack = playerEntity.getMainHandStack();

        if (stack.isIn(FabricToolTags.AXES)) {
            BlockState state = world.getBlockState(blockHitResult.getBlockPos());

            BlockState newState = state;

            if (state.getBlock().getStateManager().getProperties().contains(OxidizationLevel.PROPERTY) && state.get(OxidizationLevel.PROPERTY).ordinal() > 0) {
                newState = state.with(OxidizationLevel.PROPERTY, OxidizationLevel.VALUES[state.get(OxidizationLevel.PROPERTY).ordinal() - 1]);
                world.playSound(playerEntity, blockHitResult.getBlockPos(), SoundEvents.ITEM_AXE_SCRAPE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.syncWorldEvent(playerEntity, WorldEvents.BLOCK_SCRAPED, blockHitResult.getBlockPos(), 0);
            } else if (state.getBlock().getStateManager().getProperties().contains(BlockProperties.WAXED) && state.get(BlockProperties.WAXED)) {
                newState = state.with(BlockProperties.WAXED, false);
                world.playSound(playerEntity, blockHitResult.getBlockPos(), SoundEvents.ITEM_AXE_WAX_OFF, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.syncWorldEvent(playerEntity, WorldEvents.WAX_REMOVED, blockHitResult.getBlockPos(), 0);

            }

            if (newState != state) {
                if (playerEntity instanceof ServerPlayerEntity) {
                    Criteria.ITEM_USED_ON_BLOCK.test((ServerPlayerEntity)playerEntity, blockHitResult.getBlockPos(), stack);
                }

                world.setBlockState(blockHitResult.getBlockPos(), newState);

                stack.damage(1, playerEntity, p -> p.sendToolBreakStatus(hand));

                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.PASS;
    }

    private static ActionResult waxBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getStackInHand(hand);
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (stack.isOf(Items.HONEYCOMB) && state.getProperties().contains(BlockProperties.WAXED) && !state.get(BlockProperties.WAXED)) {
            world.setBlockState(pos, state.with(BlockProperties.WAXED, true), Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);
            world.syncWorldEvent(player, WorldEvents.BLOCK_WAXED, pos, 0);

            if (player instanceof ServerPlayerEntity serverPlayerEntity) {
                Criteria.ITEM_USED_ON_BLOCK.test(serverPlayerEntity, pos, stack);
            }

            if (!player.isCreative()) {
                stack.decrement(1);
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}
