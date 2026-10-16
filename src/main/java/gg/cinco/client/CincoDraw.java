package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;

import java.util.HashMap;
import java.util.Map;

/** Shared Star Client UI primitives with cached 8x supersampled rounded profiles. */
public final class CincoDraw {
    private static final int AA_SCALE = 8;
    private static final int PROFILE_BANDS = 12;
    private static final Map<Integer, byte[]> ROUNDED_TEXTURE_CACHE = new HashMap<>();
    private static final Map<Integer, RoundedProfile> PROFILE_CACHE = new HashMap<>();

    private record RoundedProfile(int[] y0, int[] y1, int[] inset) {}

    private CincoDraw() {}

    /**
     * Smooth high-performance rounded rectangle. The exact 8x supersampled mask is generated once
     * per radius and converted to a denser cached profile. Runtime stays bounded while preserving
     * the visibly round/pill-shaped corners used by the polished Star UI.
     */
    public static void rounded(DrawContext context, int left, int top, int right, int bottom, int radius, int color) {
        int width = right - left, height = bottom - top;
        if (width <= 0 || height <= 0) return;
        color = accentPink(color);

        int requested = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        // Small controls are true capsules/circles regardless of a too-small caller radius.
        int r = height <= 64 ? Math.max(requested, Math.min(width, height) / 2) : requested;
        r = Math.max(0, Math.min(r, Math.min(width, height) / 2));
        if (r <= 2) {
            context.fill(left, top, right, bottom, color);
            return;
        }

        RoundedProfile p = roundedProfile(r);

        // Solid center/body: two large batch-friendly fills.
        if (width > r * 2) context.fill(left + r, top, right - r, bottom, color);
        if (height > r * 2) context.fill(left, top + r, right, bottom - r, color);

        // Denser AA-derived corner spans. Still fixed/bounded work instead of per-pixel rendering.
        for (int i = 0; i < p.inset.length; i++) {
            int y0 = p.y0[i], y1 = p.y1[i], inset = p.inset[i];
            if (y1 <= y0 || inset >= r) continue;
            int top0 = top + y0, top1 = top + y1;
            int bottom0 = bottom - y1, bottom1 = bottom - y0;

            context.fill(left + inset, top0, left + r, top1, color);
            context.fill(right - r, top0, right - inset, top1, color);
            if (bottom0 != top0) {
                context.fill(left + inset, bottom0, left + r, bottom1, color);
                context.fill(right - r, bottom0, right - inset, bottom1, color);
            }
        }
    }

    /** Convert the cached 8x AA coverage texture into a smooth reusable draw profile. */
    private static RoundedProfile roundedProfile(int radius) {
        RoundedProfile cached = PROFILE_CACHE.get(radius);
        if (cached != null) return cached;

        byte[] mask = generateRoundedTexture(radius);
        int bands = Math.min(PROFILE_BANDS, Math.max(3, radius));
        int[] y0 = new int[bands], y1 = new int[bands], inset = new int[bands];

        for (int b = 0; b < bands; b++) {
            // Slight easing gives more profile detail near the top/bottom where curvature is obvious.
            double t0 = b / (double) bands;
            double t1 = (b + 1) / (double) bands;
            int from = (int) Math.floor(radius * (1.0 - Math.cos(t0 * Math.PI / 2.0)));
            int to = (int) Math.ceil(radius * (1.0 - Math.cos(t1 * Math.PI / 2.0)));
            from = Math.max(0, Math.min(radius - 1, from));
            to = Math.max(from + 1, Math.min(radius, to));
            y0[b] = from;
            y1[b] = to;

            int sum = 0, rows = 0;
            for (int py = from; py < to; py++) {
                int boundary = radius;
                // Use the 50% AA boundary from the real 8x supersampled mask.
                for (int px = 0; px < radius; px++) {
                    if ((mask[py * radius + px] & 0xFF) >= 128) {
                        boundary = px;
                        break;
                    }
                }
                sum += boundary;
                rows++;
            }
            inset[b] = rows == 0 ? radius : Math.max(0, Math.min(radius, Math.round(sum / (float) rows)));
        }

        RoundedProfile profile = new RoundedProfile(y0, y1, inset);
        PROFILE_CACHE.put(radius, profile);
        return profile;
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
                texture[py * radius + px] = (byte) Math.max(0, Math.min(255,
                        (int) Math.round(255.0 * inside / samples)));
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
        int a = Math.max(2, alpha / 5);
        rounded(context, left - 2, top - 2, right + 2, bottom + 2,
                radius + 2, (a << 24) | (rgb & 0x00FFFFFF));
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
        int a = (color >>> 24) & 255, r = (color >>> 16) & 255, g = (color >>> 8) & 255, b = color & 255;
        if (r >= 210 && g >= 70 && g <= 190 && b <= 125) return (a << 24) | 0xFF55C8;
        return color;
    }
}
