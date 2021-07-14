package dev.hephaestus.vauban.client;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import dev.hephaestus.vauban.Vauban;
import dev.hephaestus.vauban.block.FilterBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class FilterBlockModel implements FabricBakedModel, BakedModel, UnbakedModel {
    private static final SpriteIdentifier WHITE = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Vauban.id("white"));
    private static final Direction[] DIRS = Direction.values();

    private final Collection<SpriteIdentifier> textureDependencies;
    private final SpriteIdentifier spriteId;
    private final Map<Direction, Mesh> pipes = new EnumMap<>(Direction.class);
    private Mesh node;
    private Sprite sprite;
    private Sprite white;
    
    public FilterBlockModel(SpriteIdentifier sprite) {
        this.spriteId = sprite;
        this.textureDependencies = Lists.newArrayList(sprite, WHITE);
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        Consumer<Mesh> consumer = context.meshConsumer();

        consumer.accept(this.node);

        for (Direction direction : DIRS) {
            if (state.get(FilterBlock.fromDirection(direction))) {
                consumer.accept(this.pipes.get(direction));
            }
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {

    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        return Collections.emptyList();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean hasDepth() {
        return false;
    }

    @Override
    public boolean isSideLit() {
        return false;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getSprite() {
        return this.sprite;
    }

    @Override
    public ModelTransformation getTransformation() {
        return ModelTransformation.NONE;
    }

    @Override
    public ModelOverrideList getOverrides() {
        return ModelOverrideList.EMPTY;
    }

    @Override
    public Collection<Identifier> getModelDependencies() {
        return Collections.emptyList();
    }

    @Override
    public Collection<SpriteIdentifier> getTextureDependencies(Function<Identifier, UnbakedModel> unbakedModelGetter, Set<Pair<String, String>> unresolvedTextureReferences) {
        return this.textureDependencies;
    }

    @Nullable
    @Override
    public BakedModel bake(ModelLoader loader, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
        Renderer renderer = RendererAccess.INSTANCE.getRenderer();

        if (renderer != null) {
            this.sprite = textureGetter.apply(this.spriteId);
            this.white = textureGetter.apply(WHITE);

            DecoratedEmitter emitter = new DecoratedEmitter(renderer.meshBuilder());

            RenderMaterial pipeMaterial = renderer.materialFinder().blendMode(0, BlendMode.SOLID).find();
            RenderMaterial bandMaterial = renderer.materialFinder().blendMode(0, BlendMode.TRANSLUCENT).find();

            // Nodes
            for (Direction direction : DIRS) {
                emitter.quad(direction, this.sprite, 3, 3, 3, 3, 3, pipeMaterial, -1);
            }

            this.node = emitter.build();

            this.pipes.compute(Direction.UP,    (dir, old) -> pipe(emitter, dir, pipeMaterial, bandMaterial, 3, 13, 3, 0));
            this.pipes.compute(Direction.DOWN,  (dir, old) -> pipe(emitter, dir, pipeMaterial, bandMaterial,3, 0, 3, 13));
            this.pipes.compute(Direction.NORTH, (dir, old) -> pipe(emitter, dir, pipeMaterial, bandMaterial,3, 13, 3, 0));
            this.pipes.compute(Direction.SOUTH, (dir, old) -> pipe(emitter, dir, pipeMaterial, bandMaterial,3, 0, 3, 13));
            this.pipes.compute(Direction.EAST,  (dir, old) -> pipe(emitter, dir, pipeMaterial, bandMaterial,0, 3, 13, 3));
            this.pipes.compute(Direction.WEST,  (dir, old) -> pipe(emitter, dir, pipeMaterial, bandMaterial,13, 3, 0, 3));
        }

        return this;
    }

    private Mesh pipe(DecoratedEmitter emitter, Direction direction, RenderMaterial pipeMaterial, RenderMaterial bandMaterial, int left, int bottom, int right, int top) {
        if (direction.getAxis().isVertical()) {
            for (Direction dir : Direction.values()) {
                if (dir.getAxis() != direction.getAxis()) {
                    emitter.quad(dir, this.sprite, left, bottom, right, top, 3, pipeMaterial, -1);
                    emitter.quad(dir, this.white, left - 0.1F, bottom + 1, right - 0.1F, top + 1, 2.9F, bandMaterial, direction == Direction.UP ? 0xFFFF8000 : 0xFFFF00FF);
                }
            }
        } else {
            switch (direction) {
                case NORTH -> {
                    emitter.quad(Direction.UP, this.sprite, 3, 0, 3, 13, 3, pipeMaterial, -1);
                    emitter.quad(Direction.DOWN, this.sprite, 3, 13, 3, 0, 3, pipeMaterial, -1);
                    emitter.quad(Direction.EAST, this.sprite, 0, 3, 13, 3, 3, pipeMaterial, -1);
                    emitter.quad(Direction.WEST, this.sprite, 13, 3, 0, 3, 3, pipeMaterial, -1);

                    emitter.quad(Direction.UP, this.white, 2.9F, 1, 2.9F, 14, 2.9F, bandMaterial, 0xFF0000FF);
                    emitter.quad(Direction.DOWN, this.white, 2.9F, 14, 2.9F, 1, 2.9F, bandMaterial, 0xFF0000FF);
                    emitter.quad(Direction.EAST, this.white, 1, 2.9F, 14, 2.9F, 2.9F, bandMaterial, 0xFF0000FF);
                    emitter.quad(Direction.WEST, this.white, 14, 2.9F, 1, 2.9F, 2.9F, bandMaterial, 0xFF0000FF);
                }
                case SOUTH -> {
                    emitter.quad(Direction.UP, this.sprite, 3, 13, 3, 0, 3, pipeMaterial, -1);
                    emitter.quad(Direction.DOWN, this.sprite, 3, 0, 3, 13, 3, pipeMaterial, -1);
                    emitter.quad(Direction.EAST, this.sprite, 13, 3, 0, 3, 3, pipeMaterial, -1);
                    emitter.quad(Direction.WEST, this.sprite, 0, 3, 13, 3, 3, pipeMaterial, -1);

                    emitter.quad(Direction.UP, this.white, 2.9F, 14, 2.9F, 1, 2.9F, bandMaterial, 0xFFFFFF00);
                    emitter.quad(Direction.DOWN, this.white, 2.9F, 1, 2.9F, 14, 2.9F, bandMaterial, 0xFFFFFF00);
                    emitter.quad(Direction.EAST, this.white, 14, 2.9F, 1, 2.9F, 2.9F, bandMaterial, 0xFFFFFF00);
                    emitter.quad(Direction.WEST, this.white, 1, 2.9F, 14, 2.9F, 2.9F, bandMaterial, 0xFFFFFF00);

                }
                case WEST -> {
                    emitter.quad(Direction.UP, this.sprite, 0, 3, 13, 3, 3, pipeMaterial, -1);
                    emitter.quad(Direction.DOWN, this.sprite, 0, 3, 13, 3, 3, pipeMaterial, -1);
                    emitter.quad(Direction.NORTH, this.sprite, 0, 3, 13, 3, 3, pipeMaterial, -1);
                    emitter.quad(Direction.SOUTH, this.sprite, 13, 3, 0, 3, 3, pipeMaterial, -1);

                    emitter.quad(Direction.UP, this.white, 1, 2.9F, 14, 2.9F, 2.9F, bandMaterial, 0xFFFF0000);
                    emitter.quad(Direction.DOWN, this.white, 1, 2.9F, 14, 2.9F, 2.9F, bandMaterial, 0xFFFF0000);
                    emitter.quad(Direction.NORTH, this.white, 1, 2.9F, 14, 2.9F, 2.9F, bandMaterial, 0xFFFF0000);
                    emitter.quad(Direction.SOUTH, this.white, 14, 2.9F, 1, 2.9F, 2.9F, bandMaterial, 0xFFFF0000);
                }
                case EAST -> {
                    emitter.quad(Direction.UP, this.sprite, 13, 3, 0, 3, 3, pipeMaterial, -1);
                    emitter.quad(Direction.DOWN, this.sprite, 13, 3, 0, 3, 3, pipeMaterial, -1);
                    emitter.quad(Direction.NORTH, this.sprite, 13, 3, 0, 3, 3, pipeMaterial, -1);
                    emitter.quad(Direction.SOUTH, this.sprite, 0, 3, 13, 3, 3, pipeMaterial, -1);

                    emitter.quad(Direction.UP, this.white, 14, 2.9F, 1, 2.9F, 2.9F, bandMaterial, 0xFF00FF00);
                    emitter.quad(Direction.DOWN, this.white, 14, 2.9F, 1, 2.9F, 2.9F, bandMaterial, 0xFF00FF00);
                    emitter.quad(Direction.NORTH, this.white, 14, 2.9F, 1, 2.9F, 2.9F, bandMaterial, 0xFF00FF00);
                    emitter.quad(Direction.SOUTH, this.white, 1, 2.9F, 14, 2.9F, 2.9F, bandMaterial, 0xFF00FF00);
                }
            }
        }

        emitter.quad(direction.getOpposite(), this.sprite, 3, 3, 3, 3, 0, pipeMaterial, -1);

        return emitter.build();
    }
}
