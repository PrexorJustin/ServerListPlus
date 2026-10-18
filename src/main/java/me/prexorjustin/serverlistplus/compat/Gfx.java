package me.prexorjustin.serverlistplus.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.Identifier;

import java.util.List;
//? if <1.21.6 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
*///?}
//? if >=1.21.6 {
import net.minecraft.client.renderer.RenderPipelines;
//?} elif >=1.21.2 {
/*import net.minecraft.client.renderer.RenderType;
*///?} else
/*import com.mojang.blaze3d.systems.RenderSystem;*/

/**
 * The drawing calls ServerListPlus needs, on top of whatever the current version calls its GUI graphics.
 */
public final class Gfx {
    private final GuiGraphicsExtractor graphics;

    public Gfx(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    /** For vanilla drawing code that takes the graphics itself. */
    public GuiGraphicsExtractor raw() {
        return graphics;
    }

    /** Moves everything drawn until {@link #pop()} by {@code dx}, {@code dy}. */
    public void push(int dx, int dy) {
        //? if >=1.21.6 {
        graphics.pose().pushMatrix();
        graphics.pose().translate(dx, dy);
        //?} else {
        /*graphics.pose().pushPose();
        graphics.pose().translate(dx, dy, 0);
        *///?}
    }

    public void pop() {
        //? if >=1.21.6 {
        graphics.pose().popMatrix();
        //?} else
        /*graphics.pose().popPose();*/
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y2, color);
    }

    /** One pixel border inside the given rectangle. */
    public void outline(int x1, int y1, int x2, int y2, int color) {
        fill(x1, y1, x2, y1 + 1, color);
        fill(x1, y2 - 1, x2, y2, color);
        fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }

    /** Rectangle with the corner pixels left out, which reads as slightly rounded at GUI scale. */
    public void roundedRect(int x1, int y1, int x2, int y2, int color) {
        fill(x1 + 1, y1, x2 - 1, y2, color);
        fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }

    /** Rectangle with corners rounded by {@code radius} pixels. */
    public void roundedRect(int x1, int y1, int x2, int y2, int radius, int color) {
        int r = Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        if (r <= 1) {
            roundedRect(x1, y1, x2, y2, color);
            return;
        }
        fill(x1, y1 + r, x2, y2 - r, color);
        for (int row = 0; row < r; row++) {
            double dy = r - row - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            fill(x1 + inset, y1 + row, x2 - inset, y1 + row + 1, color);
            fill(x1 + inset, y2 - row - 1, x2 - inset, y2 - row, color);
        }
    }

    public void gradient(int x1, int y1, int x2, int y2, int topColor, int bottomColor) {
        graphics.fillGradient(x1, y1, x2, y2, topColor, bottomColor);
    }

    /**
     * Draws part of a texture stretched to a rectangle. u, v, partWidth and partHeight are in
     * texture pixels. tint is ARGB and gets multiplied in, 0xFFFFFFFF for none.
     */
    public void image(Identifier texture, int x, int y, int width, int height,
                      int u, int v, int partWidth, int partHeight, int textureWidth, int textureHeight, int tint) {
        //? if >=1.21.6 {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, partWidth, partHeight, textureWidth, textureHeight, tint);
        //?} elif >=1.21.2 {
        /*graphics.blit(RenderType::guiTextured, texture, x, y, u, v, width, height, partWidth, partHeight, textureWidth, textureHeight, tint);
        *///?} else {
        /*RenderSystem.enableBlend();
        RenderSystem.setShaderColor((tint >> 16 & 0xFF) / 255f, (tint >> 8 & 0xFF) / 255f, (tint & 0xFF) / 255f, (tint >>> 24) / 255f);
        graphics.blit(texture, x, y, width, height, u, v, partWidth, partHeight, textureWidth, textureHeight);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        *///?}
    }

    public void text(Font font, Component text, int x, int y, int color) {
        //? if >=26.1 {
        graphics.text(font, text, x, y, color);
        //?} else
        /*graphics.drawString(font, text, x, y, color);*/
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color) {
        //? if >=26.1 {
        graphics.text(font, text, x, y, color);
        //?} else
        /*graphics.drawString(font, text, x, y, color);*/
    }

    /** Shows a tooltip at the given position after everything else is drawn. */
    public void tooltip(Font font, Component text, int x, int y) {
        //? if >=1.21.6 {
        graphics.setTooltipForNextFrame(font, text, x, y);
        //?} else {
        /*Screen screen = Minecraft.getInstance().screen;
        if (screen != null) screen.setTooltipForNextRenderPass(text);
        *///?}
    }

    /** A tooltip of several lines, e.g. from {@link Font#split}. */
    public void tooltip(Font font, List<FormattedCharSequence> lines, int x, int y) {
        //? if >=1.21.6 {
        graphics.setTooltipForNextFrame(font, lines, x, y);
        //?} else {
        /*Screen screen = Minecraft.getInstance().screen;
        if (screen != null) screen.setTooltipForNextRenderPass(lines);
        *///?}
    }

    /**
     * Starts a new layer, so everything drawn afterwards covers text drawn before
     * (since 1.21.6 text is batched after all shapes of the same layer).
     */
    public void overlay() {
        //? if >=1.21.6
        graphics.nextStratum();
    }
}
