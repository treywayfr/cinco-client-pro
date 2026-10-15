package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

/** Small, performance-minded primitives shared by Orbit HUD and Nebula ClickGUI. */
public final class StarCrispUi {
    public static final int BG = 0xE9070B16;
    public static final int SURFACE = 0xEC0B1020;
    public static final int SURFACE_2 = 0xE911172A;
    public static final int SURFACE_HOVER = 0xEE151E36;
    public static final int BORDER = 0x8C405AAE;
    public static final int BORDER_SOFT = 0x60384473;
    public static final int BLUE = 0xFF3F7BFF;
    public static final int BLUE_HI = 0xFF72A0FF;
    public static final int PURPLE = 0xFF7657FF;
    public static final int PURPLE_HI = 0xFF9A82FF;
    public static final int TEXT = 0xFFF1F5FF;
    public static final int MUTED = 0xFFB2BDD8;
    public static final int DIM = 0xFF7180A6;
    public static final int TRACK = 0xFF252F4B;

    private StarCrispUi() {}

    public static void panel(DrawContext c, int x, int y, int w, int h, int radius, boolean focused) {
        // One restrained shadow only. Avoid stacking expensive glow layers on every component.
        if (focused && StarPerformance.allowMenuShadow(MinecraftClient.getInstance())) {
            CincoDraw.softShadow(c, x, y, x + w, y + h, radius + 2, BLUE, 28);
        }
        CincoDraw.bubble(c, x, y, x + w, y + h, radius, focused ? 0xC15F82EE : BORDER, BG);
        // Crisp inner keyline and top highlight create the glass effect without a real blur shader.
        CincoDraw.rounded(c, x + 1, y + 1, x + w - 1, y + 2, 1, focused ? 0xA76FA0FF : 0x5C536B9E);
    }

    public static void card(DrawContext c, int x, int y, int w, int h, boolean active, boolean hover) {
        int fill = active ? 0xE62C54C7 : hover ? SURFACE_HOVER : SURFACE;
        int border = active ? 0xCC6B9BFF : hover ? 0x8D516CB2 : BORDER_SOFT;
        CincoDraw.bubble(c, x, y, x + w, y + h, 8, border, fill);
    }

    public static void divider(DrawContext c, int x1, int y, int x2) {
        c.fill(x1, y, x2, y + 1, 0x4A43527A);
    }

    public static void accentRail(DrawContext c, int x, int y, int h) {
        CincoDraw.rounded(c, x, y, x + 3, y + h, 2, BLUE_HI);
    }

    public static void toggle(DrawContext c, int x, int y, boolean enabled, float progress) {
        float t = Math.max(0f, Math.min(1f, progress));
        int track = mix(0xFF28324D, PURPLE, t);
        CincoDraw.rounded(c, x, y, x + 28, y + 14, 7, track);
        int knobX = x + 3 + Math.round(t * 14f);
        CincoDraw.rounded(c, knobX, y + 3, knobX + 8, y + 11, 5, enabled ? 0xFFFFFFFF : 0xFFD7DDEF);
    }

    public static void slider(DrawContext c, int x, int y, int w, double fraction) {
        double f = Math.max(0.0, Math.min(1.0, fraction));
        CincoDraw.rounded(c, x, y, x + w, y + 3, 2, TRACK);
        int px = x + (int)Math.round(w * f);
        if (px > x) CincoDraw.rounded(c, x, y, px, y + 3, 2, PURPLE);
        CincoDraw.rounded(c, px - 3, y - 3, px + 4, y + 6, 4, PURPLE_HI);
    }

    public static void searchBox(DrawContext c, TextRenderer tr, int x, int y, int w, String text, boolean focused) {
        CincoDraw.bubble(c, x, y, x + w, y + 28, 8,
                focused ? 0xB06D8FFF : 0x73516A9F, 0xD80A1020);
        c.drawText(tr, StarFonts.text("⌕", true), x + 9, y + 9, focused ? BLUE_HI : DIM, false);
        c.drawText(tr, StarFonts.text(text, false), x + 25, y + 9,
                text.startsWith("Search") ? DIM : TEXT, false);
    }

    public static Text label(String s, boolean bold) {
        return StarFonts.text(s, bold);
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa=(a>>>24)&255, ar=(a>>>16)&255, ag=(a>>>8)&255, ab=a&255;
        int ba=(b>>>24)&255, br=(b>>>16)&255, bg=(b>>>8)&255, bb=b&255;
        return ((int)(aa+(ba-aa)*t)<<24)|((int)(ar+(br-ar)*t)<<16)|((int)(ag+(bg-ag)*t)<<8)|(int)(ab+(bb-ab)*t);
    }
}
