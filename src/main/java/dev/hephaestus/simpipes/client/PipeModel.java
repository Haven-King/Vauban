package dev.hephaestus.simpipes.client;

import com.mojang.datafixers.util.Pair;
import dev.hephaestus.simpipes.block.PipeBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
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
import net.minecraft.state.property.Properties;
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
public class PipeModel implements FabricBakedModel, BakedModel, UnbakedModel {
    private static final Direction[] DIRS = Direction.values();

    private final SpriteIdentifier spriteId;
    private final Map<Direction, Mesh> pipes = new EnumMap<>(Direction.class);
    private final Map<Direction, Mesh> connectors = new EnumMap<>(Direction.class);
    private Mesh node;
    private Sprite sprite;


    public PipeModel(SpriteIdentifier sprite) {
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
            PipeBlock.ConnectionType connectionType = state.get(PipeBlock.fromDirection(direction));

            switch (connectionType) {
                case NONE:
                    if (direction == state.get(Properties.FACING)) {
                        consumer.accept(this.pipes.get(direction));
                    }
                    break;
                case CONTAINER:
                    consumer.accept(this.connectors.get(direction));
                case PIPE:
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
            DecoratedEmitter emitter = new DecoratedEmitter(renderer.meshBuilder(), this.sprite);

            // Nodes
            for (Direction direction : DIRS) {
                emitter.quad(direction, 6, 6, 6, 6, 6);
            }

            this.node = emitter.build();

            this.pipes.compute(Direction.UP,    (dir, old) -> pipe(emitter, dir, 6, 10, 6, 0));
            this.pipes.compute(Direction.DOWN,  (dir, old) -> pipe(emitter, dir, 6, 0, 6, 10));
            this.pipes.compute(Direction.NORTH, (dir, old) -> pipe(emitter, dir, 6, 10, 6, 0));
            this.pipes.compute(Direction.SOUTH, (dir, old) -> pipe(emitter, dir, 6, 0, 6, 10));
            this.pipes.compute(Direction.EAST,  (dir, old) -> pipe(emitter, dir, 0, 6, 10, 6));
            this.pipes.compute(Direction.WEST,  (dir, old) -> pipe(emitter, dir, 10, 6, 0, 6));


            this.connectors.compute(Direction.UP, (dir, old) -> connector(emitter, dir));
            this.connectors.compute(Direction.DOWN, (dir, old) -> connector(emitter, dir));
            this.connectors.compute(Direction.NORTH, (dir, old) -> connector(emitter, dir));
            this.connectors.compute(Direction.SOUTH, (dir, old) -> connector(emitter, dir));
            this.connectors.compute(Direction.EAST, (dir, old) -> connector(emitter, dir));
            this.connectors.compute(Direction.WEST, (dir, old) -> connector(emitter, dir));
        }

        return this;
    }

    private Mesh pipe(DecoratedEmitter emitter, Direction direction, int left, int bottom, int right, int top) {
        if (direction.getAxis().isVertical()) {
            for (Direction dir : Direction.values()) {
                if (dir.getAxis() != direction.getAxis()) {
                    emitter.quad(dir, left, bottom, right, top, 6);
                }
            }
        } else {
            switch (direction) {
                case NORTH -> {
                    emitter.quad(Direction.UP, 6, 0, 6, 10, 6);
                    emitter.quad(Direction.DOWN, 6, 10, 6, 0, 6);
                    emitter.quad(Direction.EAST, 0, 6, 10, 6, 6);
                    emitter.quad(Direction.WEST, 10, 6, 0, 6, 6);
                }
                case SOUTH -> {
                    emitter.quad(Direction.UP, 6, 10, 6, 0, 6);
                    emitter.quad(Direction.DOWN, 6, 0, 6, 10, 6);
                    emitter.quad(Direction.EAST, 10, 6, 0, 6, 6);
                    emitter.quad(Direction.WEST, 0, 6, 10, 6, 6);
                }
                case WEST -> {
                    emitter.quad(Direction.UP, 0, 6, 10, 6, 6);
                    emitter.quad(Direction.DOWN, 0, 6, 10, 6, 6);
                    emitter.quad(Direction.NORTH, 0, 6, 10, 6, 6);
                    emitter.quad(Direction.SOUTH, 10, 6, 0, 6, 6);
                }
                case EAST -> {
                    emitter.quad(Direction.UP, 10, 6, 0, 6, 6);
                    emitter.quad(Direction.DOWN, 10, 6, 0, 6, 6);
                    emitter.quad(Direction.NORTH, 10, 6, 0, 6, 6);
                    emitter.quad(Direction.SOUTH, 0, 6, 10, 6, 6);
                }
            }
        }

        emitter.quad(direction.getOpposite(), 6, 6, 6, 6, 0);

        return emitter.build();
    }

    private Mesh connector(DecoratedEmitter emitter, Direction direction) {
        switch (direction) {
            case UP -> {
                emitter.quad(Direction.NORTH, 5, 15, 5, 0, 5);
                emitter.quad(Direction.SOUTH, 5, 15, 5, 0, 5);
                emitter.quad(Direction.EAST, 5, 15, 5, 0, 5);
                emitter.quad(Direction.WEST, 5, 15, 5, 0, 5);
            }
            case DOWN -> {
                emitter.quad(Direction.NORTH, 5, 0, 5, 15, 5);
                emitter.quad(Direction.SOUTH, 5, 0, 5, 15, 5);
                emitter.quad(Direction.EAST, 5, 0, 5, 15, 5);
                emitter.quad(Direction.WEST, 5, 0, 5, 15, 5);
            }
            case NORTH -> {
                emitter.quad(Direction.UP, 5, 0, 5, 15, 5);
                emitter.quad(Direction.DOWN, 5, 15, 5, 0, 5);
                emitter.quad(Direction.EAST, 0, 5, 15, 5, 5);
                emitter.quad(Direction.WEST, 15, 5, 0, 5, 5);
            }
            case SOUTH -> {
                emitter.quad(Direction.UP, 5, 15, 5, 0, 5);
                emitter.quad(Direction.DOWN, 5, 0, 5, 15, 5);
                emitter.quad(Direction.EAST, 15, 5, 0, 5, 5);
                emitter.quad(Direction.WEST, 0, 5, 15, 5, 5);
            }
            case WEST -> {
                emitter.quad(Direction.UP, 0, 5, 15, 5, 5);
                emitter.quad(Direction.DOWN, 0, 5, 15, 5, 5);
                emitter.quad(Direction.NORTH, 0, 5, 15, 5, 5);
                emitter.quad(Direction.SOUTH, 15, 5, 0, 5, 5);
            }
            case EAST -> {
                emitter.quad(Direction.UP, 15, 5, 0, 5, 5);
                emitter.quad(Direction.DOWN, 15, 5, 0, 5, 5);
                emitter.quad(Direction.NORTH, 15, 5, 0, 5, 5);
                emitter.quad(Direction.SOUTH, 0, 5, 15, 5, 5);
            }
        }

        emitter.quad(direction, 5, 5, 5, 5, 15);
        emitter.quad(direction.getOpposite(), 5, 5, 5, 5, 0);

        return emitter.build();
    }
}
