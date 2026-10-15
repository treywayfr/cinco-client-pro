package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared UI drawing helpers. Rounded rectangles keep the requested 8x supersampled edge quality,
 * but cache only radius coverage and render each rounded row as spans instead of hundreds of
 * individual pixels. This keeps the smooth silhouette without tanking frame time.
 */
public final class CincoDraw {
    private static final int AA_SCALE = 8;
    private static final Map<Integer, byte[]> ROUNDED_TEXTURE_CACHE = new HashMap<>();

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

        // Middle body is one batch-friendly rectangle.
        if (bottom - top > r * 2) context.fill(left, top + r, right, bottom - r, color);

        byte[] mask = generateRoundedTexture(r);
        int sourceAlpha = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;

        // A rounded row normally has only one partially covered boundary pixel per side.
        // Draw that tiny AA fringe, then draw the fully covered center as one span.
        for (int py = 0; py < r; py++) {
            int firstVisible = r;
            int firstOpaque = r;
            for (int px = 0; px < r; px++) {
                int coverage = mask[py * r + px] & 0xFF;
                if (coverage > 0 && firstVisible == r) firstVisible = px;
                if (coverage >= 255) { firstOpaque = px; break; }
            }

            int yTop = top + py;
            int yBottom = bottom - 1 - py;
            int solidLeft = left + firstOpaque;
            int solidRight = right - firstOpaque;
            if (solidRight > solidLeft) {
                context.fill(solidLeft, yTop, solidRight, yTop + 1, color);
                if (yBottom != yTop) context.fill(solidLeft, yBottom, solidRight, yBottom + 1, color);
            }

            for (int px = firstVisible; px < firstOpaque; px++) {
                int coverage = mask[py * r + px] & 0xFF;
                if (coverage <= 0) continue;
                int alpha = (sourceAlpha * coverage + 127) / 255;
                int aaColor = (alpha << 24) | rgb;
                int lx = left + px;
                int rx = right - 1 - px;
                context.fill(lx, yTop, lx + 1, yTop + 1, aaColor);
                if (rx != lx) context.fill(rx, yTop, rx + 1, yTop + 1, aaColor);
                if (yBottom != yTop) {
                    context.fill(lx, yBottom, lx + 1, yBottom + 1, aaColor);
                    if (rx != lx) context.fill(rx, yBottom, rx + 1, yBottom + 1, aaColor);
                }
            }
        }
    }

    /**
     * Generates one 8x supersampled quarter-circle coverage texture. Coverage depends only on
     * radius, so animated colors/glows no longer create a new cache entry every frame.
     */
    private static byte[] generateRoundedTexture(int radius) {
        byte[] cached = ROUNDED_TEXTURE_CACHE.get(radius);
        if (cached != null) return cached;

        byte[] texture = new byte[radius * radius];
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
                int coverage = Math.max(0, Math.min(255,
                        (int) Math.round(255.0 * inside / samples)));
                texture[py * radius + px] = (byte) coverage;
            }
        }
        ROUNDED_TEXTURE_CACHE.put(radius, texture);
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
        // Two AA layers are enough visually and are much cheaper than the previous four-pass shadow.
        for (int i = 2; i >= 1; i--) {
            int a = Math.max(2, alpha / (i + 2));
            rounded(context, left - i * 2, top - i * 2, right + i * 2, bottom + i * 2,
                    radius + i * 2, (a << 24) | (rgb & 0x00FFFFFF));
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
