package dev.hephaestus.vauban.mixin.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

// I was really hoping to get away with not having any mixins in this mod, but big tooltips need big solutions.
@Mixin(Screen.class)
@Environment(EnvType.CLIENT)
public class MixinScreen {
    @Shadow public int width;

    @Shadow public int height;
    private int vauban$i;

    @Inject(method = "renderTooltipFromComponents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void vauban$captureLocals(MatrixStack matrices, List<TooltipComponent> components, int x, int y, CallbackInfo ci, int i, int j, int l, int m) {
        this.vauban$i = i;
    }

    @ModifyVariable(method = "renderTooltipFromComponents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;getInstance()Lnet/minecraft/client/render/Tessellator;"), ordinal = 4)
    private int vauban$modifyTooltipX(int l, MatrixStack matrices, List<TooltipComponent> components, int x, int y) {
        if (x + 12 + this.vauban$i > (this.width - 4)) {
            return this.width - 4 /* Size of decorations on right side */ - this.vauban$i;
        }

        return l;
    }

    @ModifyVariable(method = "renderTooltipFromComponents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;getInstance()Lnet/minecraft/client/render/Tessellator;"), ordinal = 5)
    private int vauban$modifyTooltipY(int m, MatrixStack matrices, List<TooltipComponent> components, int x, int y) {
        int height = components.size() == 1 ? 0 : 2; // Takes into account larger spacing between the first line of a
                                                     // tooltip and subsequent lines.

        for (TooltipComponent component : components) {
            height += component.getHeight();
        }

        if (y - 12 /* Reconstruct 'm' and ignore the passed one */ + 6 + height > this.height) {
            return this.height - 4 /* Size of decorations on bottom */ - height;
        }

        return m;
    }
}
