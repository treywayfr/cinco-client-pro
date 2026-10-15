package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;

/** Tiny rounded-rectangle helpers built only from DrawContext fills (no shaders or blur passes). */
public final class CincoDraw {
    private CincoDraw() {}

    public static void rounded(DrawContext context, int left, int top, int right, int bottom, int radius, int color) {
        int width = Math.max(0, right - left);
        int height = Math.max(0, bottom - top);
        if (width == 0 || height == 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r == 0) {
            context.fill(left, top, right, bottom, color);
            return;
        }
        int rr = r * r;
        for (int row = 0; row < height; row++) {
            int edgeY = Math.min(row, height - 1 - row);
            int inset = 0;
            if (edgeY < r) {
                double dy = r - edgeY - 0.5;
                inset = Math.max(0, r - (int) Math.floor(Math.sqrt(Math.max(0.0, rr - dy * dy))));
            }
            int x1 = left + inset;
            int x2 = right - inset;
            if (x2 > x1) context.fill(x1, top + row, x2, top + row + 1, color);
        }
    }

    public static void bubble(DrawContext context, int left, int top, int right, int bottom, int radius,
                              int borderColor, int fillColor) {
        rounded(context, left, top, right, bottom, radius, borderColor);
        rounded(context, left + 1, top + 1, right - 1, bottom - 1, Math.max(1, radius - 1), fillColor);
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
