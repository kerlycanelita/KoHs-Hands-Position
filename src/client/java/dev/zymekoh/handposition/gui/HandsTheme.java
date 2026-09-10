package dev.zymekoh.handposition.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Shared contrast and geometry for the configuration's interactive controls.
 *
 * <p>Frames, borders and buttons follow the Crystal Tweaks KoHs look: rounded
 * violet panels over a near-black gradient, a pulsing magenta button outline and
 * an accent ring for the selected or primary action. The hand accents stay
 * distinct because they identify which hand a control belongs to.
 */
final class HandsTheme {
    static final int TEXT = 0xFFF7EDFF;
    static final int MUTED = 0xFFC6B5CC;
    static final int MAIN = 0xFF64D9F3;
    static final int OFF = 0xFFFFC17D;
    static final int GLOBAL = 0xFFE2A2FF;
    static final int GREEN = 0xFF86E5B1;
    /** Panel body, matching the Crystal Tweaks frame fill. */
    static final int PANEL = 0xB71B0928;
    /** Panel outline, matching the Crystal Tweaks frame border. */
    static final int PANEL_EDGE = 0xC6B85BE8;
    static final int DIM_TEXT = 0xFFA091AC;

    /** A Crystal Tweaks frame: soft shadow, rounded violet body and rounded border. */
    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int edge) {
        g.fill(x + 4, y + h, x + w - 2, y + h + 2, 0x58000000);
        fillRounded(g, x, y, w, h, fill, shade(fill, 0.45F));
        roundedOutline(g, x, y, w, h, edge);
    }

    /** Vertical gradient with the 4 px corner cut used by every Crystal Tweaks frame. */
    static void fillRounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        int radius = Math.min(4, Math.min(w / 2, h / 2));
        g.fillGradient(x + radius, y, x + w - radius, y + h, top, bottom);
        g.fillGradient(x, y + radius, x + w, y + h - radius, top, bottom);
        if (radius == 4) {
            g.fill(x + 2, y + 1, x + w - 2, y + 2, top);
            g.fill(x + 1, y + 2, x + w - 1, y + 4, top);
            g.fill(x + 1, y + h - 4, x + w - 1, y + h - 2, bottom);
            g.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, bottom);
        }
    }

    /** Border with the same 4 px corner cut; falls back to a square border when too small. */
    static void roundedOutline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (w < 9 || h < 9) {
            outline(g, x, y, w, h, color);
            return;
        }
        int right = x + w;
        int bottom = y + h;
        g.fill(x + 4, y, right - 4, y + 1, color);
        g.fill(x + 2, y + 1, x + 4, y + 2, color);
        g.fill(x + 1, y + 2, x + 2, y + 4, color);
        g.fill(x, y + 4, x + 1, bottom - 4, color);
        g.fill(right - 4, y + 1, right - 2, y + 2, color);
        g.fill(right - 2, y + 2, right - 1, y + 4, color);
        g.fill(right - 1, y + 4, right, bottom - 4, color);
        g.fill(x + 1, bottom - 4, x + 2, bottom - 2, color);
        g.fill(x + 2, bottom - 2, x + 4, bottom - 1, color);
        g.fill(x + 4, bottom - 1, right - 4, bottom, color);
        g.fill(right - 2, bottom - 4, right - 1, bottom - 2, color);
        g.fill(right - 4, bottom - 2, right - 2, bottom - 1, color);
    }

    static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Darkens a colour for the bottom half of a frame gradient, keeping its alpha. */
    static int shade(int color, float factor) {
        return (color & 0xFF000000)
                | Math.round(((color >> 16) & 0xFF) * factor) << 16
                | Math.round(((color >> 8) & 0xFF) * factor) << 8
                | Math.round((color & 0xFF) * factor);
    }

    private static int lerp(int start, int end, float progress) {
        return start + Math.round((end - start) * progress);
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    static final class ActionButton extends Button {
        private final int accent;
        private final boolean primary;
        private boolean selected;
        private float hoverProgress;
        private long lastFrameTime = System.currentTimeMillis();

        ActionButton(int x, int y, int width, int height, Component label, int accent, boolean primary, Runnable action) {
            super(x, y, width, height, label, b -> action.run(), DEFAULT_NARRATION);
            this.accent = accent;
            this.primary = primary;
        }

        void selected(boolean selected) { this.selected = selected; }

        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            long now = System.currentTimeMillis();
            long elapsed = Mth.clamp(now - lastFrameTime, 0L, 50L);
            lastFrameTime = now;
            float target = active && isHoveredOrFocused() ? 1.0F : 0.0F;
            hoverProgress += (target - hoverProgress) * (1.0F - (float) Math.exp(-elapsed / 65.0F));

            boolean solid = active && (primary || selected);
            float lit = Math.max(hoverProgress, solid ? 0.45F : 0.0F);
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            int red = lerp(active ? 66 : 40, 126, lit);
            int green = lerp(active ? 18 : 14, 40, lit);
            int blue = lerp(active ? 96 : 58, 176, lit);
            g.fillGradient(x + 2, y, x + w - 2, y + h,
                    argb(220, red, green, blue), argb(230, red / 2, green / 2, blue / 2));
            g.fill(x, y + 2, x + w, y + h - 2, argb(95, red, green, blue));

            int pulse = 205 + Math.round(45.0F * (0.5F + 0.5F * (float) Math.sin(now / 190.0F)));
            outline(g, x + 1, y + 1, w - 2, h - 2,
                    active ? argb(220, 210, 112, pulse) : argb(150, 108, 78, 132));
            if (solid) outline(g, x, y, w, h, accent);
            if (isFocused()) g.fill(x + 4, y + h - 3, x + w - 4, y + h - 2, accent);

            var font = Minecraft.getInstance().font;
            String label = font.plainSubstrByWidth(getMessage().getString(), w - 12);
            g.centeredText(font, label, x + w / 2, y + (h - 8) / 2, active ? TEXT : 0xFFAA98B2);
        }
    }
}
