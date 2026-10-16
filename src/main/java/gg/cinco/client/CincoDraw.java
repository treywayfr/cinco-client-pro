package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Shared Star Client UI primitives.
 *
 * Rounded rectangles use a cached 8x-supersampled alpha mask uploaded once to the GPU. Runtime
 * rendering is a nine-slice (4 corner texture draws + 3 disjoint fills), so translucent glass has
 * perfectly even alpha with no horizontal banding and the UI stays fast.
 */
public final class CincoDraw {
    private static final int AA_SCALE = 8;
    private static final int MASK_SIZE = 64;
    private static final int MASK_HALF = MASK_SIZE / 2;
    private static Identifier roundedMask;

    private CincoDraw() {}

    public static void rounded(DrawContext context, int left, int top, int right, int bottom,
                               int radius, int color) {
        int width = right - left;
        int height = bottom - top;
        if (width <= 0 || height <= 0) return;
        color = accentPink(color);

        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r <= 2) {
            context.fill(left, top, right, bottom, color);
            return;
        }

        ensureRoundedTexture();
        if (roundedMask == null) {
            // Extremely defensive fallback if a texture manager is not ready yet.
            context.fill(left + r, top, right - r, bottom, color);
            context.fill(left, top + r, left + r, bottom - r, color);
            context.fill(right - r, top + r, right, bottom - r, color);
            return;
        }

        // Three DISJOINT fills. No alpha stacking = no darker horizontal stripes.
        if (right - r > left + r) context.fill(left + r, top, right - r, bottom, color);
        if (bottom - r > top + r) {
            context.fill(left, top + r, left + r, bottom - r, color);
            context.fill(right - r, top + r, right, bottom - r, color);
        }

        // Four AA corners, stretched to the requested radius. The source is a 64x64 white circle
        // with 8x supersampled alpha coverage, so tinting preserves smooth translucent edges.
        drawCorner(context, left, top, 0, 0, r, color);                         // TL
        drawCorner(context, right - r, top, MASK_HALF, 0, r, color);             // TR
        drawCorner(context, left, bottom - r, 0, MASK_HALF, r, color);            // BL
        drawCorner(context, right - r, bottom - r, MASK_HALF, MASK_HALF, r, color);// BR
    }

    private static void drawCorner(DrawContext context, int x, int y, int u, int v, int size, int color) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, roundedMask,
                x, y, (float) u, (float) v,
                size, size, MASK_HALF, MASK_HALF, MASK_SIZE, MASK_SIZE, color);
    }

    private static void ensureRoundedTexture() {
        if (roundedMask != null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getTextureManager() == null) return;

        NativeImage image = generateRoundedTexture();
        Identifier id = Identifier.of("cinco_client", "dynamic/ui_rounded_mask_v2");
        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "Star UI Rounded Mask", image);
        client.getTextureManager().registerTexture(id, texture);
        texture.upload();
        roundedMask = id;
    }

    /** 8x-supersampled white circle used as the four rounded-corner quadrants. */
    private static NativeImage generateRoundedTexture() {
        NativeImage image = new NativeImage(MASK_SIZE, MASK_SIZE, false);
        int samples = AA_SCALE * AA_SCALE;
        double cx = MASK_SIZE / 2.0;
        double cy = MASK_SIZE / 2.0;
        double radius = MASK_SIZE / 2.0;
        double rr = radius * radius;

        for (int py = 0; py < MASK_SIZE; py++) {
            for (int px = 0; px < MASK_SIZE; px++) {
                int inside = 0;
                for (int sy = 0; sy < AA_SCALE; sy++) {
                    for (int sx = 0; sx < AA_SCALE; sx++) {
                        double fx = px + (sx + 0.5) / AA_SCALE;
                        double fy = py + (sy + 0.5) / AA_SCALE;
                        double dx = fx - cx;
                        double dy = fy - cy;
                        if (dx * dx + dy * dy <= rr) inside++;
                    }
                }
                int alpha = Math.max(0, Math.min(255, (int) Math.round(255.0 * inside / samples)));
                image.setColorArgb(px, py, (alpha << 24) | 0x00FFFFFF);
            }
        }
        return image;
    }

    public static void bubble(DrawContext context, int left, int top, int right, int bottom, int radius,
                              int borderColor, int fillColor) {
        rounded(context, left, top, right, bottom, radius, borderColor);
        if (right - left > 2 && bottom - top > 2) {
            rounded(context, left + 1, top + 1, right - 1, bottom - 1,
                    Math.max(1, radius - 1), fillColor);
        }
    }

    public static void softShadow(DrawContext context, int left, int top, int right, int bottom,
                                  int radius, int rgb, int alpha) {
        int a1 = Math.max(1, alpha / 7);
        int a2 = Math.max(1, alpha / 11);
        rounded(context, left - 3, top - 3, right + 3, bottom + 3,
                radius + 3, (a1 << 24) | (rgb & 0x00FFFFFF));
        rounded(context, left - 6, top - 6, right + 6, bottom + 6,
                radius + 6, (a2 << 24) | (rgb & 0x00FFFFFF));
    }

    public static void line(DrawContext context, int x0, int y0, int x1, int y1, int color, int thickness) {
        color = accentPink(color);
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            context.fill(x0 - thickness / 2, y0 - thickness / 2,
                    x0 + thickness / 2 + 1, y0 + thickness / 2 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int twice = 2 * error;
            if (twice >= dy) { error += dy; x0 += sx; }
            if (twice <= dx) { error += dx; y0 += sy; }
        }
    }

    private static int accentPink(int color) {
        int a = (color >>> 24) & 255;
        int r = (color >>> 16) & 255;
        int g = (color >>> 8) & 255;
        int b = color & 255;
        if (r >= 210 && g >= 70 && g <= 190 && b <= 125) return (a << 24) | 0xFF55C8;
        return color;
    }
}
