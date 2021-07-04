package dev.hephaestus.simpipes.client;

import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Range;

public class DecoratedEmitter {
    private final MeshBuilder builder;
    private final QuadEmitter emitter;
    private final Sprite sprite;

    public DecoratedEmitter(MeshBuilder builder, Sprite sprite) {
        this.builder = builder;
        this.emitter = builder.getEmitter();
        this.sprite = sprite;
    }

    public void quad(Direction direction, @Range(from=0, to=16) int left, @Range(from=0, to=16) int bottom, @Range(from=0, to=16) int right, @Range(from=0, to=16) int top, @Range(from=0, to=16) int depth) {
        emitter.square(direction.getOpposite(),
                left / 16F,
                bottom / 16F,
                1 - right / 16F,
                1 - top / 16F,
                depth / 16F
        );

        emitter.spriteBake(0, this.sprite, MutableQuadView.BAKE_LOCK_UV);
        emitter.spriteColor(0, -1, -1, -1, -1);
        emitter.emit();
    }

    public Mesh build() {
        return this.builder.build();
    }
}
