package dev.hephaestus.vauban.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Range;

@Environment(EnvType.CLIENT)
public class DecoratedEmitter {
    private final MeshBuilder builder;
    private final QuadEmitter emitter;

    public DecoratedEmitter(MeshBuilder builder) {
        this.builder = builder;
        this.emitter = builder.getEmitter();
    }

    public void quad(Direction direction, Sprite sprite, @Range(from=0, to=16) float left, @Range(from=0, to=16) float bottom, @Range(from=0, to=16) float right, @Range(from=0, to=16) float top, @Range(from=0, to=16) float depth, RenderMaterial material, int color) {
        emitter.square(direction.getOpposite(),
                left / 16F,
                bottom / 16F,
                1 - right / 16F,
                1 - top / 16F,
                depth / 16F
        );

        emitter.material(material);
        emitter.spriteBake(0, sprite, MutableQuadView.BAKE_LOCK_UV);
        emitter.spriteColor(0, color, color, color, color);
        emitter.emit();
    }

    public QuadEmitter getEmitter() {
        return this.emitter;
    }

    public Mesh build() {
        return this.builder.build();
    }
}
