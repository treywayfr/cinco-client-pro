package gg.cinco.client;

/**
 * Star Client's black + purple interface palette.
 *
 * The GUI intentionally stays near-black with one purple accent family so every screen reads as
 * one product instead of shifting between magenta/cyan/gold. Existing method names such as
 * magenta(), cyan(), and starGold() remain as compatibility aliases for older UI code, but they
 * now resolve to coordinated purple shades.
 */
public final class CincoTheme {
    private CincoTheme() {}

    private static float accentStrength() {
        ClientModule glow = CincoClient.findModule("Accent Glow");
        return glow == null || !glow.enabled ? 0.80f : (float) Math.max(0.30, Math.min(1.0, glow.setting));
    }

    private static int withPanelOpacity(int color) {
        ClientModule opacity = CincoClient.findModule("Panel Opacity");
        double setting = opacity == null ? 0.96 : opacity.setting;
        int alpha = (int) Math.round(Math.max(0.74, Math.min(1.0, setting)) * 255.0);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int scaleRgb(int color, float scale) {
        int r = Math.min(255, Math.round(((color >> 16) & 255) * scale));
        int g = Math.min(255, Math.round(((color >> 8) & 255) * scale));
        int b = Math.min(255, Math.round((color & 255) * scale));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int accent() {
        return scaleRgb(0xFF965CFF, 0.90f + 0.10f * accentStrength());
    }

    public static int accentMuted() { return 0xFF7540D8; }
    public static int accentLight() { return 0xFFD1B3FF; }
    public static int accentDark() { return 0xFF351A5F; }

    // Compatibility aliases: all decorative accents are now purple, never cyan/gold.
    public static int magenta() { return 0xFFB16CFF; }
    public static int cyan() { return 0xFF8F72FF; }
    public static int starGold() { return 0xFFC99AFF; }

    public static int background() { return 0xFF030305; }
    public static int panel() { return withPanelOpacity(0xFF08080C); }
    public static int sidebar() { return withPanelOpacity(0xFF0B0A10); }
    public static int card() { return withPanelOpacity(0xFF101015); }
    public static int cardHover() { return withPanelOpacity(0xFF171321); }
    public static int line() { return 0xFF5D3A8E; }
    public static int sliderTrack() { return 0xFF25222B; }
    public static int enabled() { return 0xFF965CFF; }
    public static int disabled() { return 0xFF29262F; }

    public static int primaryText() { return 0xFFF8F5FF; }
    public static int secondaryText() { return 0xFFAAA1B8; }

    public static int textColor(int original) {
        int r = (original >> 16) & 255, g = (original >> 8) & 255, b = original & 255;
        int luminance = (r * 299 + g * 587 + b * 114) / 1000;
        return luminance >= 150 ? primaryText() : secondaryText();
    }
}
