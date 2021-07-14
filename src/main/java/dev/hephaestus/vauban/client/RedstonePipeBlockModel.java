package dev.hephaestus.vauban.client;

import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

public class RedstonePipeBlockModel extends PipeBlockModel{
    private final Map<Direction, Mesh> activeConnectors = new EnumMap<>(Direction.class);

    public RedstonePipeBlockModel(SpriteIdentifier sprite, SpriteIdentifier connectorSpriteInactive) {
        super(sprite, connectorSpriteInactive);
    }

    @Override
    protected void emitConnector(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context, Direction direction) {
        if (state.get(Properties.POWERED) && direction == state.get(Properties.FACING)) {
            context.meshConsumer().accept(this.activeConnectors.get(direction));
        } else {
            context.pushTransform(qt -> {
                qt.spriteColor(0, 0xFF404040, 0xFF404040, 0xFF404040, 0xFF404040);

                return true;
            });

            super.emitConnector(blockView, state, pos, randomSupplier, context, direction);
            context.popTransform();
        }
    }

    @Override
    protected void initializeTextures(Function<SpriteIdentifier, Sprite> textureGetter) {
        super.initializeTextures(textureGetter);
    }

    @Override
    protected void bakeParts(Renderer renderer, DecoratedEmitter emitter) {
        super.bakeParts(renderer, emitter);

        RenderMaterial connectorMaterial = renderer.materialFinder().emissive(0, true).find();

        for (Direction direction : DIRS) {
            this.activeConnectors.put(direction, connector(emitter, direction, connectorMaterial, this.connectorSprite));
        }
    }
}
