package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Shared crisp black + purple primitives for both Star Client GUI modes. */
public final class StarCrispUi {
    public static final int BG = 0xF4020205;
    public static final int SURFACE = 0xF008080D;
    public static final int SURFACE_2 = 0xF00D0A13;
    public static final int SURFACE_HOVER = 0xF014101B;
    public static final int BORDER = 0x925F3A8E;
    public static final int BORDER_SOFT = 0x66452A66;

    public static final int PURPLE = 0xFF8B5CFF;
    public static final int PURPLE_HI = 0xFFB58AFF;
    public static final int PURPLE_DARK = 0xFF32184F;

    // Compatibility aliases. The whole interface is intentionally purple now.
    public static final int BLUE = PURPLE;
    public static final int BLUE_HI = PURPLE_HI;

    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFFE6E0EC;
    public static final int DIM = 0xFFA39AAE;
    public static final int TRACK = 0xFF29242F;

    private StarCrispUi() {}

    public static void panel(DrawContext c, int x, int y, int w, int h, int radius, boolean focused) {
        if (focused && StarPerformance.allowMenuShadow(MinecraftClient.getInstance())) {
            CincoDraw.softShadow(c, x, y, x + w, y + h, radius + 2, PURPLE, 25);
        }
        CincoDraw.bubble(c, x, y, x + w, y + h, radius,
                focused ? 0xC08359DD : BORDER, BG);
        c.fill(x + radius, y + 1, x + w - radius, y + 2,
                focused ? 0x7AB58AFF : 0x47513A68);
    }

    public static void card(DrawContext c, int x, int y, int w, int h, boolean active, boolean hover) {
        int fill = active ? 0xE31A1026 : hover ? SURFACE_HOVER : SURFACE;
        int border = active ? 0xDA9A68FF : hover ? 0x8C6D507E : BORDER_SOFT;
        CincoDraw.bubble(c, x, y, x + w, y + h, 7, border, fill);
    }

    public static void divider(DrawContext c, int x1, int y, int x2) {
        c.fill(x1, y, x2, y + 1, 0x534D395D);
    }

    public static void accentRail(DrawContext c, int x, int y, int h) {
        CincoDraw.rounded(c, x, y, x + 2, y + h, 1, PURPLE_HI);
    }

    public static void toggle(DrawContext c, int x, int y, boolean enabled, float progress) {
        float t = Math.max(0f, Math.min(1f, progress));
        int track = mix(0xFF3B3641, PURPLE, t);
        CincoDraw.rounded(c, x, y, x + 24, y + 12, 6, track);
        int knobX = x + 2 + Math.round(t * 12f);
        CincoDraw.rounded(c, knobX, y + 2, knobX + 8, y + 10, 5, 0xFFFFFFFF);
    }

    public static void miniToggle(DrawContext c, int x, int y, boolean enabled, float progress) {
        float t = Math.max(0f, Math.min(1f, progress));
        int track = mix(0xFF4A444F, PURPLE, t);
        CincoDraw.rounded(c, x, y, x + 20, y + 10, 5, track);
        int knobX = x + 2 + Math.round(t * 10f);
        CincoDraw.rounded(c, knobX, y + 2, knobX + 6, y + 8, 4, 0xFFFFFFFF);
    }

    public static void slider(DrawContext c, int x, int y, int w, double fraction) {
        double f = Math.max(0.0, Math.min(1.0, fraction));
        CincoDraw.rounded(c, x, y, x + w, y + 3, 2, 0xFF4A444F);
        int px = x + (int)Math.round(w * f);
        if (px > x) CincoDraw.rounded(c, x, y, px, y + 3, 2, PURPLE);
        CincoDraw.rounded(c, px - 3, y - 3, px + 4, y + 6, 4, 0xFFFFFFFF);
    }

    public static void searchBox(DrawContext c, TextRenderer tr, int x, int y, int w, String text, boolean focused) {
        int border = focused ? 0xD49E72FF : 0xB8FFFFFF;
        CincoDraw.bubble(c, x, y, x + w, y + 26, 7, border, 0xE308080D);
        c.drawText(tr, StarFonts.text("⌕", true), x + 8, y + 8, focused ? PURPLE_HI : TEXT, false);
        c.drawText(tr, StarFonts.text(text, false), x + 23, y + 8,
                text.startsWith("Search") ? 0xFFCFC8D5 : TEXT, false);
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
