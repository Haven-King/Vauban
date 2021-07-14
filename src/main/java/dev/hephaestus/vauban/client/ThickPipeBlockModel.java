package dev.hephaestus.vauban.client;

import com.mojang.datafixers.util.Pair;
import dev.hephaestus.vauban.block.ThickPipeBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
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
public class ThickPipeBlockModel implements FabricBakedModel, BakedModel, UnbakedModel {
    private static final Direction[] DIRS = Direction.values();

    private final SpriteIdentifier spriteId;
    private final Map<Direction, Mesh> pipes = new EnumMap<>(Direction.class);
    private Mesh node;
    private Sprite sprite;

    public ThickPipeBlockModel(SpriteIdentifier sprite) {
        this.spriteId = sprite;
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
            if (state.get(ThickPipeBlock.fromDirection(direction))) {
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
        return Collections.singleton(this.spriteId);
    }

    @Nullable
    @Override
    public BakedModel bake(ModelLoader loader, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
        Renderer renderer = RendererAccess.INSTANCE.getRenderer();

        if (renderer != null) {
            this.sprite = textureGetter.apply(this.spriteId);
            DecoratedEmitter emitter = new DecoratedEmitter(renderer.meshBuilder());

            RenderMaterial pipeMaterial = renderer.materialFinder().find();

            // Nodes
            for (Direction direction : DIRS) {
                emitter.quad(direction, this.sprite, 5, 5, 5, 5, 5, pipeMaterial, -1);
            }

            this.node = emitter.build();

            this.pipes.compute(Direction.UP,    (dir, old) -> pipe(emitter, dir, pipeMaterial, 5, 11, 5, 0));
            this.pipes.compute(Direction.DOWN,  (dir, old) -> pipe(emitter, dir, pipeMaterial,  5, 0, 5, 11));
            this.pipes.compute(Direction.NORTH, (dir, old) -> pipe(emitter, dir, pipeMaterial,  5, 11, 5, 0));
            this.pipes.compute(Direction.SOUTH, (dir, old) -> pipe(emitter, dir, pipeMaterial,  5, 0, 5, 11));
            this.pipes.compute(Direction.EAST,  (dir, old) -> pipe(emitter, dir, pipeMaterial,  0, 5, 11, 5));
            this.pipes.compute(Direction.WEST,  (dir, old) -> pipe(emitter, dir, pipeMaterial,  11, 5, 0, 5));
        }

        return this;
    }

    private Mesh pipe(DecoratedEmitter emitter, Direction direction, RenderMaterial pipeMaterial, int left, int bottom, int right, int top) {
        if (direction.getAxis().isVertical()) {
            for (Direction dir : Direction.values()) {
                if (dir.getAxis() != direction.getAxis()) {
                    emitter.quad(dir, this.sprite, left, bottom, right, top, 5, pipeMaterial, -1);
                }
            }
        } else {
            switch (direction) {
                case NORTH -> {
                    emitter.quad(Direction.UP, this.sprite, 5, 0, 5, 11, 5, pipeMaterial, -1);
                    emitter.quad(Direction.DOWN, this.sprite, 5, 11, 5, 0, 5, pipeMaterial, -1);
                    emitter.quad(Direction.EAST, this.sprite, 0, 5, 11, 5, 5, pipeMaterial, -1);
                    emitter.quad(Direction.WEST, this.sprite, 11, 5, 0, 5, 5, pipeMaterial, -1);
                }
                case SOUTH -> {
                    emitter.quad(Direction.UP, this.sprite, 5, 11, 5, 0, 5, pipeMaterial, -1);
                    emitter.quad(Direction.DOWN, this.sprite, 5, 0, 5, 11, 5, pipeMaterial, -1);
                    emitter.quad(Direction.EAST, this.sprite, 11, 5, 0, 5, 5, pipeMaterial, -1);
                    emitter.quad(Direction.WEST, this.sprite, 0, 5, 11, 5, 5, pipeMaterial, -1);
                }
                case WEST -> {
                    emitter.quad(Direction.UP, this.sprite, 0, 5, 11, 5, 5, pipeMaterial, -1);
                    emitter.quad(Direction.DOWN, this.sprite, 0, 5, 11, 5, 5, pipeMaterial, -1);
                    emitter.quad(Direction.NORTH, this.sprite, 0, 5, 11, 5, 5, pipeMaterial, -1);
                    emitter.quad(Direction.SOUTH, this.sprite, 11, 5, 0, 5, 5, pipeMaterial, -1);
                }
                case EAST -> {
                    emitter.quad(Direction.UP, this.sprite, 11, 5, 0, 5, 5, pipeMaterial, -1);
                    emitter.quad(Direction.DOWN, this.sprite, 11, 5, 0, 5, 5, pipeMaterial, -1);
                    emitter.quad(Direction.NORTH, this.sprite, 11, 5, 0, 5, 5, pipeMaterial, -1);
                    emitter.quad(Direction.SOUTH, this.sprite, 0, 5, 11, 5, 5, pipeMaterial, -1);
                }
            }
        }

        emitter.quad(direction.getOpposite(), this.sprite, 5, 5, 5, 5, 0, pipeMaterial, -1);

        return emitter.build();
    }

}
