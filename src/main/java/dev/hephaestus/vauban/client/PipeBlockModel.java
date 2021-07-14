package dev.hephaestus.vauban.client;

import com.mojang.datafixers.util.Pair;
import dev.hephaestus.vauban.Vauban;
import dev.hephaestus.vauban.block.PipeBlock;
import dev.hephaestus.vauban.mixin.SpriteAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.hud.BackgroundHelper;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class PipeBlockModel implements FabricBakedModel, BakedModel, UnbakedModel {
    private static final SpriteIdentifier WHITE = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Vauban.id("white"));

    protected static final Direction[] DIRS = Direction.values();

    protected final SpriteIdentifier spriteId;
    protected final SpriteIdentifier connectorSpriteId;
    protected final Map<Direction, Mesh> pipes = new EnumMap<>(Direction.class);
    protected final Map<Direction, Mesh> connectors = new EnumMap<>(Direction.class);
    protected int arrowFill = 0;
    protected Mesh node;
    protected Mesh arrows;
    protected Sprite sprite;
    protected Sprite connectorSprite;
    protected Sprite white;

    public PipeBlockModel(SpriteIdentifier sprite) {
        this(sprite, sprite);
    }

    public PipeBlockModel(SpriteIdentifier sprite, SpriteIdentifier connectorSprite) {
        this.spriteId = sprite;
        this.connectorSpriteId = connectorSprite;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    protected void emitConnector(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context, Direction direction) {
        context.meshConsumer().accept(this.connectors.get(direction));
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        Consumer<Mesh> consumer = context.meshConsumer();

        Direction facing = state.get(Properties.FACING);

        consumer.accept(this.node);

        boolean straight = false;
        boolean xAxis = false;
        boolean yAxis = false;
        boolean zAxis = false;

        for (Direction direction : DIRS) {
            PipeBlock.ConnectionType connectionType = state.get(PipeBlock.fromDirection(direction));

            switch (connectionType) {
                case NONE:
                    if (direction == facing) {
                        consumer.accept(this.pipes.get(direction));

                        switch (direction.getAxis()) {
                            case X -> xAxis = true;
                            case Y -> yAxis = true;
                            case Z -> zAxis = true;
                        }
                    }
                    break;
                case CONTAINER:
                    this.emitConnector(blockView, state, pos, randomSupplier, context, direction);
                case PIPE:
                    consumer.accept(this.pipes.get(direction));

                    if (direction == facing.getOpposite() && (!(blockView.getBlockState(pos.offset(direction)).getBlock() instanceof PipeBlock) || !(blockView.getBlockState(pos.offset(facing)).getBlock() instanceof PipeBlock))) {
                        straight = true;
                    }

                    switch (direction.getAxis()) {
                        case X -> xAxis = true;
                        case Y -> yAxis = true;
                        case Z -> zAxis = true;
                    }
            }
        }

        if (straight || (xAxis && yAxis) || (xAxis && zAxis) || (yAxis && zAxis)) {
            Quaternion rotate = (facing.getAxis().isHorizontal() ? Vec3f.POSITIVE_Y : Vec3f.POSITIVE_X).getDegreesQuaternion(angle(facing));
            RenderContext.QuadTransform transform = mv -> {
                Vec3f tmp = new Vec3f();

                for (int i = 0; i < 4; i++) {
                    // Transform the position (center of rotation is 0.5, 0.5, 0.5)
                    mv.copyPos(i, tmp);
                    tmp.add(-0.5f, -0.5f, -0.5f);
                    tmp.rotate(rotate);
                    tmp.add(0.5f, 0.5f, 0.5f);
                    mv.pos(i, tmp);

                    // Transform the normal
                    if (mv.hasNormal(i)) {
                        mv.copyNormal(i, tmp);
                        tmp.rotate(rotate);
                        mv.normal(i, tmp);
                    }
                }

                mv.nominalFace(facing);
                return true;
            };

            context.pushTransform(transform);
            context.meshConsumer().accept(this.arrows);
            context.popTransform();
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

    protected void initializeTextures(Function<SpriteIdentifier, Sprite> textureGetter) {
        this.sprite = textureGetter.apply(this.spriteId);
        this.connectorSprite = textureGetter.apply(this.connectorSpriteId);
        this.white = textureGetter.apply(WHITE);
    }

    protected void bakeParts(Renderer renderer, DecoratedEmitter emitter) {
        RenderMaterial pipeMaterial = renderer.materialFinder().find();

        // Nodes
        for (Direction direction : DIRS) {
            emitter.quad(direction, this.sprite, 6, 6, 6, 6, 6, pipeMaterial, -1);
        }

        this.node = emitter.build();

        this.pipes.compute(Direction.UP,    (dir, old) -> pipe(emitter, dir, pipeMaterial, 6, 10, 6, 0));
        this.pipes.compute(Direction.DOWN,  (dir, old) -> pipe(emitter, dir, pipeMaterial,  6, 0, 6, 10));
        this.pipes.compute(Direction.NORTH, (dir, old) -> pipe(emitter, dir, pipeMaterial,  6, 10, 6, 0));
        this.pipes.compute(Direction.SOUTH, (dir, old) -> pipe(emitter, dir, pipeMaterial,  6, 0, 6, 10));
        this.pipes.compute(Direction.EAST,  (dir, old) -> pipe(emitter, dir, pipeMaterial,  0, 6, 10, 6));
        this.pipes.compute(Direction.WEST,  (dir, old) -> pipe(emitter, dir, pipeMaterial,  10, 6, 0, 6));


        for (Direction direction : DIRS) {
            this.connectors.put(direction, connector(emitter, direction, pipeMaterial, this.connectorSprite));
        }

        this.arrows = arrow(emitter, pipeMaterial);
    }

    @Nullable
    @Override
    public BakedModel bake(ModelLoader loader, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
        Renderer renderer = RendererAccess.INSTANCE.getRenderer();

        if (renderer != null) {
            this.initializeTextures(textureGetter);

            if (((SpriteAccessor) this.sprite).getImages().length > 0) {
                NativeImage image = ((SpriteAccessor) this.sprite).getImages()[0];

                int lowestAverage = Integer.MAX_VALUE;

                for (int x = 0; x < image.getWidth(); ++x) {
                    for (int y = 0; y < image.getHeight(); ++y) {
                        int color = image.getPixelColor(x, y);

                        int r = color & 0x000000FF;
                        int g = (color & 0x0000FF00) >> 8;
                        int b = (color & 0x00FF0000) >> 16;

                        int avg = (r + g + b) / 3;

                        if (avg < lowestAverage) {
                            lowestAverage = avg;

                            int min = Math.min(r, Math.min(g, b));
                            int floor = MathHelper.clamp(16 - min, -min, 0);

                            this.arrowFill = BackgroundHelper.ColorMixer.getArgb(255, r + floor, g + floor, b + floor);
                        }
                    }
                }
            }

            this.bakeParts(renderer, new DecoratedEmitter(renderer.meshBuilder()));
        }

        return this;
    }

    private Mesh arrow(DecoratedEmitter decoratedEmitter, RenderMaterial arrowMaterial) {
        QuadEmitter emitter = decoratedEmitter.getEmitter();

        emitter.pos(0, 8/16F, 10.01F/16F, 7.5F/16F);
        emitter.pos(1, 8/16F, 10.01F/16F, 7.5F/16F);
        emitter.pos(2, 7.5F/16F, 10.01F/16F, 8.5F/16F);
        emitter.pos(3, 8.5F/16F, 10.01F/16F, 8.5F/16F);

        emitter.spriteColor(0, this.arrowFill, this.arrowFill, this.arrowFill, this.arrowFill);

        emitter.material(arrowMaterial);
        emitter.spriteBake(0, this.white, MutableQuadView.BAKE_LOCK_UV);
        emitter.emit();

        emitter.pos(0, 10.01F/16F, 8.5F/16F, 8.5F/16F);
        emitter.pos(1, 10.01F/16F, 7.5F/16F, 8.5F/16F);
        emitter.pos(2, 10.01F/16F, 8/16F, 7.5F/16F);
        emitter.pos(3, 10.01F/16F, 8/16F, 7.5F/16F);

        emitter.spriteColor(0, this.arrowFill, this.arrowFill, this.arrowFill, this.arrowFill);

        emitter.material(arrowMaterial);
        emitter.spriteBake(0, this.white, MutableQuadView.BAKE_LOCK_UV);
        emitter.emit();


        emitter.pos(0, 8.5F/16F, 5.99F/16F, 8.5F/16F);
        emitter.pos(1, 7.5F/16F, 5.99F/16F, 8.5F/16F);
        emitter.pos(2, 8/16F, 5.99F/16F, 7.5F/16F);
        emitter.pos(3, 8/16F, 5.99F/16F, 7.5F/16F);

        emitter.spriteColor(0, this.arrowFill, this.arrowFill, this.arrowFill, this.arrowFill);

        emitter.material(arrowMaterial);
        emitter.spriteBake(0, this.white, MutableQuadView.BAKE_LOCK_UV);
        emitter.emit();

        emitter.pos(0, 5.99F/16F, 8/16F, 7.5F/16F);
        emitter.pos(1, 5.99F/16F, 8/16F, 7.5F/16F);
        emitter.pos(2, 5.99F/16F, 7.5F/16F, 8.5F/16F);
        emitter.pos(3, 5.99F/16F, 8.5F/16F, 8.5F/16F);

        emitter.spriteColor(0, this.arrowFill, this.arrowFill, this.arrowFill, this.arrowFill);

        emitter.material(arrowMaterial);
        emitter.spriteBake(0, this.white, MutableQuadView.BAKE_LOCK_UV);
        emitter.emit();

        return decoratedEmitter.build();
    }

    private Mesh pipe(DecoratedEmitter emitter, Direction direction, RenderMaterial pipeMaterial, int left, int bottom, int right, int top) {
        if (direction.getAxis().isVertical()) {
            for (Direction dir : Direction.values()) {
                if (dir.getAxis() != direction.getAxis()) {
                    emitter.quad(dir, this.sprite, left, bottom, right, top, 6, pipeMaterial, -1);
                }
            }
        } else {
            switch (direction) {
                case NORTH -> {
                    emitter.quad(Direction.UP, this.sprite, 6, 0, 6, 10, 6, pipeMaterial, -1);
                    emitter.quad(Direction.DOWN, this.sprite, 6, 10, 6, 0, 6, pipeMaterial, -1);
                    emitter.quad(Direction.EAST, this.sprite, 0, 6, 10, 6, 6, pipeMaterial, -1);
                    emitter.quad(Direction.WEST, this.sprite, 10, 6, 0, 6, 6, pipeMaterial, -1);
                }
                case SOUTH -> {
                    emitter.quad(Direction.UP, this.sprite, 6, 10, 6, 0, 6, pipeMaterial, -1);
                    emitter.quad(Direction.DOWN, this.sprite, 6, 0, 6, 10, 6, pipeMaterial, -1);
                    emitter.quad(Direction.EAST, this.sprite, 10, 6, 0, 6, 6, pipeMaterial, -1);
                    emitter.quad(Direction.WEST, this.sprite, 0, 6, 10, 6, 6, pipeMaterial, -1);
                }
                case WEST -> {
                    emitter.quad(Direction.UP, this.sprite, 0, 6, 10, 6, 6, pipeMaterial, -1);
                    emitter.quad(Direction.DOWN, this.sprite, 0, 6, 10, 6, 6, pipeMaterial, -1);
                    emitter.quad(Direction.NORTH, this.sprite, 0, 6, 10, 6, 6, pipeMaterial, -1);
                    emitter.quad(Direction.SOUTH, this.sprite, 10, 6, 0, 6, 6, pipeMaterial, -1);
                }
                case EAST -> {
                    emitter.quad(Direction.UP, this.sprite, 10, 6, 0, 6, 6, pipeMaterial, -1);
                    emitter.quad(Direction.DOWN, this.sprite, 10, 6, 0, 6, 6, pipeMaterial, -1);
                    emitter.quad(Direction.NORTH, this.sprite, 10, 6, 0, 6, 6, pipeMaterial, -1);
                    emitter.quad(Direction.SOUTH, this.sprite, 0, 6, 10, 6, 6, pipeMaterial, -1);
                }
            }
        }

        emitter.quad(direction.getOpposite(), this.sprite, 6, 6, 6, 6, 0, pipeMaterial, -1);

        return emitter.build();
    }

    protected Mesh connector(DecoratedEmitter emitter, Direction direction, RenderMaterial pipeMaterial, Sprite sprite) {
        switch (direction) {
            case UP -> {
                emitter.quad(Direction.NORTH, sprite, 5, 15, 5, 0, 5, pipeMaterial, -1);
                emitter.quad(Direction.SOUTH, sprite, 5, 15, 5, 0, 5, pipeMaterial, -1);
                emitter.quad(Direction.EAST, sprite, 5, 15, 5, 0, 5, pipeMaterial, -1);
                emitter.quad(Direction.WEST, sprite, 5, 15, 5, 0, 5, pipeMaterial, -1);
            }
            case DOWN -> {
                emitter.quad(Direction.NORTH, sprite, 5, 0, 5, 15, 5, pipeMaterial, -1);
                emitter.quad(Direction.SOUTH, sprite, 5, 0, 5, 15, 5, pipeMaterial, -1);
                emitter.quad(Direction.EAST, sprite, 5, 0, 5, 15, 5, pipeMaterial, -1);
                emitter.quad(Direction.WEST, sprite, 5, 0, 5, 15, 5, pipeMaterial, -1);
            }
            case NORTH -> {
                emitter.quad(Direction.UP, sprite, 5, 0, 5, 15, 5, pipeMaterial, -1);
                emitter.quad(Direction.DOWN, sprite, 5, 15, 5, 0, 5, pipeMaterial, -1);
                emitter.quad(Direction.EAST, sprite, 0, 5, 15, 5, 5, pipeMaterial, -1);
                emitter.quad(Direction.WEST, sprite, 15, 5, 0, 5, 5, pipeMaterial, -1);
            }
            case SOUTH -> {
                emitter.quad(Direction.UP, sprite, 5, 15, 5, 0, 5, pipeMaterial, -1);
                emitter.quad(Direction.DOWN, sprite, 5, 0, 5, 15, 5, pipeMaterial, -1);
                emitter.quad(Direction.EAST, sprite, 15, 5, 0, 5, 5, pipeMaterial, -1);
                emitter.quad(Direction.WEST, sprite, 0, 5, 15, 5, 5, pipeMaterial, -1);
            }
            case WEST -> {
                emitter.quad(Direction.UP, sprite, 0, 5, 15, 5, 5, pipeMaterial, -1);
                emitter.quad(Direction.DOWN, sprite, 0, 5, 15, 5, 5, pipeMaterial, -1);
                emitter.quad(Direction.NORTH, sprite, 0, 5, 15, 5, 5, pipeMaterial, -1);
                emitter.quad(Direction.SOUTH, sprite, 15, 5, 0, 5, 5, pipeMaterial, -1);
            }
            case EAST -> {
                emitter.quad(Direction.UP, sprite, 15, 5, 0, 5, 5, pipeMaterial, -1);
                emitter.quad(Direction.DOWN, sprite, 15, 5, 0, 5, 5, pipeMaterial, -1);
                emitter.quad(Direction.NORTH, sprite, 15, 5, 0, 5, 5, pipeMaterial, -1);
                emitter.quad(Direction.SOUTH, sprite, 0, 5, 15, 5, 5, pipeMaterial, -1);
            }
        }

        emitter.quad(direction, sprite, 5, 5, 5, 5, 15, pipeMaterial, -1);
        emitter.quad(direction.getOpposite(), sprite, 5, 5, 5, 5, 0, pipeMaterial, -1);

        return emitter.build();
    }

    public static float angle(Direction direction) {
        return switch (direction) {
            case DOWN -> 270;
            case UP -> 90;
            case NORTH -> 0;
            case EAST -> 270;
            case SOUTH -> 180;
            case WEST -> 90;
        };
    }
}
