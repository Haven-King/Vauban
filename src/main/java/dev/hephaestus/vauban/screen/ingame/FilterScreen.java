package dev.hephaestus.vauban.screen.ingame;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.hephaestus.vauban.Vauban;
import dev.hephaestus.vauban.screen.FilterBlockScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class FilterScreen extends HandledScreen<FilterBlockScreenHandler> {
    private static final Identifier TEXTURE = Vauban.id("textures/gui/container/filter.png");
    private static final Text NAME = new TranslatableText("vauban.filter");

    public FilterScreen(FilterBlockScreenHandler handler, PlayerInventory inventory, Text title /* This is silly, but Fabric API wants it */) {
        super(handler, inventory, NAME);
        this.backgroundWidth = 298;
        this.backgroundHeight = 230;
        this.playerInventoryTitleX = 130;
        this.playerInventoryTitleY = 136;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        this.fillGradient(matrices, 0, 0, this.width, this.height, -1072689136, -804253680);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        drawTexture(matrices, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight, 512, 512);

        matrices.push();
        matrices.translate(0, 0, 50);
        drawTexture(matrices, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight, 512, 512);
        matrices.pop();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void drawMouseoverTooltip(MatrixStack matrices, int x, int y) {
        ItemStack filter;

        if (this.handler.getCursorStack().isEmpty() && this.focusedSlot != null && this.focusedSlot.hasStack()) {
            this.renderTooltip(matrices, this.focusedSlot.getStack(), x, y);
        } else if (this.handler.getCursorStack().isEmpty() && this.focusedSlot != null && !(filter = this.handler.getFilter(this.focusedSlot.id)).isEmpty()) {
            this.renderTooltip(matrices, filter, x, y);
        }
    }

    @Override
    public void drawSlot(MatrixStack matrices, Slot slot) {
        ItemStack filter = this.handler.getFilter(slot.id);

        if (!filter.isEmpty()) {
            int x = slot.x;
            int y = slot.y;

            ItemRenderer renderer = MinecraftClient.getInstance().getItemRenderer();

            RenderSystem.enableDepthTest();

            matrices.push();
            matrices.translate(0, 0, -75);
            renderer.renderInGuiWithOverrides(filter, x, y);
            renderer.renderGuiItemOverlay(this.textRenderer, filter, x, y);
            matrices.pop();
        }

        super.drawSlot(matrices, slot);
    }
}
