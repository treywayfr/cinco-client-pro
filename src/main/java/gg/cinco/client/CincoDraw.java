package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;

import java.util.HashMap;
import java.util.Map;

/** Shared Star Client UI primitives with cached 8x supersampled rounded edges. */
public final class CincoDraw {
    private static final int AA_SCALE = 8;
    private static final Map<Integer, byte[]> ROUNDED_TEXTURE_CACHE = new HashMap<>();

    private CincoDraw() {}

    /**
     * Draw a genuinely anti-aliased rounded rectangle. Small controls automatically become true
     * pills/circles. The expensive 8x sampling happens only once per radius; every later draw reuses
     * the cached coverage texture.
     */
    public static void rounded(DrawContext context, int left, int top, int right, int bottom, int radius, int color) {
        int width = Math.max(0, right - left);
        int height = Math.max(0, bottom - top);
        if (width <= 0 || height <= 0) return;

        color = accentPink(color);
        int requested = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        int r = height <= 64 ? Math.max(requested, Math.min(width, height) / 2) : requested;
        r = Math.max(0, Math.min(r, Math.min(width, height) / 2));
        if (r <= 0) {
            context.fill(left, top, right, bottom, color);
            return;
        }

        // Solid center/body uses a tiny number of batch-friendly fills.
        if (right - left > r * 2) context.fill(left + r, top, right - r, bottom, color);
        if (bottom - top > r * 2) context.fill(left, top + r, right, bottom - r, color);

        byte[] mask = generateRoundedTexture(r);
        int sourceAlpha = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;

        // Draw each supersampled corner row as one opaque center span plus only its AA fringe.
        for (int py = 0; py < r; py++) {
            int firstVisible = r;
            int firstOpaque = r;
            for (int px = 0; px < r; px++) {
                int coverage = mask[py * r + px] & 0xFF;
                if (coverage > 0 && firstVisible == r) firstVisible = px;
                if (coverage >= 255) {
                    firstOpaque = px;
                    break;
                }
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

    /** Cached 8x supersampled quarter-circle coverage texture for one corner radius. */
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
        int a = Math.max(2, alpha / 4);
        rounded(context, left - 3, top - 3, right + 3, bottom + 3,
                radius + 3, (a << 24) | (rgb & 0x00FFFFFF));
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

    /** Turn legacy orange active accents into Star pink while preserving alpha. */
    private static int accentPink(int color) {
        int a = (color >>> 24) & 255, r = (color >>> 16) & 255, g = (color >>> 8) & 255, b = color & 255;
        if (r >= 210 && g >= 70 && g <= 190 && b <= 125) return (a << 24) | 0xFF55C8;
        return color;
    }
}
