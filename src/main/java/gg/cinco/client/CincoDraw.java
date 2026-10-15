package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared UI drawing helpers. Rounded rectangles use an 8x supersampled corner mask so the
 * silhouette has real alpha-antialiased edges instead of staircase/blocky corners.
 */
public final class CincoDraw {
    private static final int AA_SCALE = 8;
    private static final Map<Long, int[]> ROUNDED_TEXTURE_CACHE = new HashMap<>();

    private CincoDraw() {}

    public static void rounded(DrawContext context, int left, int top, int right, int bottom, int radius, int color) {
        int width = Math.max(0, right - left);
        int height = Math.max(0, bottom - top);
        if (width <= 0 || height <= 0) return;

        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r <= 0) {
            context.fill(left, top, right, bottom, color);
            return;
        }

        // Large interior surfaces stay batched as two rectangles. Only the corner pixels are
        // emitted individually, which keeps the AA quality high without turning a 600px panel
        // into hundreds of thousands of DrawContext calls.
        context.fill(left + r, top, right - r, bottom, color);
        context.fill(left, top + r, right, bottom - r, color);

        int[] roundedTexture = generateRoundedTexture(r, color);
        for (int py = 0; py < r; py++) {
            for (int px = 0; px < r; px++) {
                int sample = roundedTexture[py * r + px];
                if (((sample >>> 24) & 0xFF) == 0) continue;

                context.fill(left + px, top + py, left + px + 1, top + py + 1, sample);
                context.fill(right - 1 - px, top + py, right - px, top + py + 1, sample);
                context.fill(left + px, bottom - 1 - py, left + px + 1, bottom - py, sample);
                context.fill(right - 1 - px, bottom - 1 - py, right - px, bottom - py, sample);
            }
        }
    }

    /**
     * Generates one antialiased quarter-circle texture in CPU memory. Each final GUI pixel is
     * sampled on an 8x8 subpixel grid, then cached by radius + ARGB color.
     */
    private static int[] generateRoundedTexture(int radius, int color) {
        long key = ((long) radius << 32) ^ (color & 0xFFFFFFFFL);
        int[] cached = ROUNDED_TEXTURE_CACHE.get(key);
        if (cached != null) return cached;

        int[] texture = new int[radius * radius];
        int sourceAlpha = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        int samples = AA_SCALE * AA_SCALE;
        double rr = radius * (double) radius;

        for (int py = 0; py < radius; py++) {
            for (int px = 0; px < radius; px++) {
                int inside = 0;
                for (int sy = 0; sy < AA_SCALE; sy++) {
                    for (int sx = 0; sx < AA_SCALE; sx++) {
                        double fx = px + (sx + 0.5) / AA_SCALE;
                        double fy = py + (sy + 0.5) / AA_SCALE;
                        double dx = radius - fx;
                        double dy = radius - fy;
                        if (dx * dx + dy * dy <= rr) inside++;
                    }
                }
                int alpha = Math.max(0, Math.min(255,
                        (int) Math.round(sourceAlpha * (inside / (double) samples))));
                texture[py * radius + px] = (alpha << 24) | rgb;
            }
        }
        ROUNDED_TEXTURE_CACHE.put(key, texture);
        return texture;
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
        for (int i = 4; i >= 1; i--) {
            int a = Math.max(2, alpha / (i + 1));
            rounded(context, left - i, top - i, right + i, bottom + i,
                    radius + i, (a << 24) | (rgb & 0x00FFFFFF));
        }
    }

    public static void line(DrawContext context, int x0, int y0, int x1, int y1, int color, int thickness) {
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
}
