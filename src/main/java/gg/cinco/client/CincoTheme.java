package gg.cinco.client;

/**
 * Shared dynamic palette. The SETTINGS > Color Wheel module controls hue, saturation,
 * and brightness for the GUI, module editors, and in-game HUD.
 */
public final class CincoTheme {
    private CincoTheme() {}

    private static ClientModule wheel() { return CincoClient.findModule("Color Wheel"); }

    private static float hue() {
        ClientModule m = wheel();
        return (float) (((m == null ? 28.0 : m.setting) % 360.0 + 360.0) % 360.0) / 360.0f;
    }

    private static float saturation() {
        ClientModule m = wheel();
        return (float) Math.max(0.10, Math.min(1.0, m == null ? 0.90 : m.setting2));
    }

    private static float brightness() {
        ClientModule m = wheel();
        return (float) Math.max(0.15, Math.min(1.0, m == null ? 1.0 : m.setting3));
    }

    private static int hsv(float h, float s, float v) {
        h = (h % 1.0f + 1.0f) % 1.0f;
        s = Math.max(0.0f, Math.min(1.0f, s));
        v = Math.max(0.0f, Math.min(1.0f, v));
        float chroma = v * s;
        float sector = h * 6.0f;
        float x = chroma * (1.0f - Math.abs(sector % 2.0f - 1.0f));
        float r = 0, g = 0, b = 0;
        if (sector < 1) { r = chroma; g = x; }
        else if (sector < 2) { r = x; g = chroma; }
        else if (sector < 3) { g = chroma; b = x; }
        else if (sector < 4) { g = x; b = chroma; }
        else if (sector < 5) { r = x; b = chroma; }
        else { r = chroma; b = x; }
        float m = v - chroma;
        int ri = Math.round((r + m) * 255), gi = Math.round((g + m) * 255), bi = Math.round((b + m) * 255);
        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    private static int tinted(float saturationScale, float value) {
        float sat = Math.max(0.0f, Math.min(1.0f, saturation() * saturationScale));
        return hsv(hue(), sat, value);
    }

    private static float accentStrength() {
        ClientModule glow = CincoClient.findModule("Accent Glow");
        float strength = glow == null || !glow.enabled ? 0.60f : (float) Math.max(0.2, Math.min(1.0, glow.setting));
        ClientModule animation = CincoClient.findModule("GUI Animation");
        if (animation != null && animation.enabled) strength *= (float) Math.max(0.25, Math.min(1.0, animation.setting2));
        return strength;
    }

    public static int accent() { return tinted(0.95f, Math.min(1.0f, brightness() * (0.65f + 0.35f * accentStrength()))); }
    public static int accentLight() { return tinted(0.32f, Math.min(1.0f, brightness())); }
    public static int accentMuted() { return tinted(0.75f, Math.min(0.80f, brightness() * (0.55f + 0.30f * accentStrength()))); }
    public static int accentDark() { return tinted(0.85f, 0.30f); }

    private static int withPanelOpacity(int color) {
        ClientModule opacity = CincoClient.findModule("Panel Opacity");
        double setting = opacity == null ? 1.0 : opacity.setting;
        int alpha = (int) Math.round(Math.max(0.75, Math.min(1.0, setting)) * 255.0);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int background() { return tinted(0.22f, 0.025f); }
    public static int panel() { return withPanelOpacity(tinted(0.26f, 0.055f)); }
    public static int sidebar() { return withPanelOpacity(tinted(0.30f, 0.075f)); }
    public static int card() { return withPanelOpacity(tinted(0.34f, 0.105f)); }
    public static int cardHover() { return withPanelOpacity(tinted(0.52f, 0.19f)); }
    public static int line() { return tinted(0.66f, 0.35f); }
    public static int sliderTrack() { return tinted(0.60f, 0.24f); }
    public static int enabled() { return tinted(0.85f, 0.68f); }
    public static int disabled() { return tinted(0.28f, 0.23f); }

    public static int primaryText() {
        // Near-white tint keeps labels readable while following the selected hue.
        return hsv(hue(), Math.min(0.48f, saturation() * 0.58f), Math.max(0.78f, brightness()));
    }

    public static int secondaryText() {
        return hsv(hue(), Math.min(0.72f, saturation() * 0.82f), Math.max(0.58f, brightness() * 0.88f));
    }

    /** Preserve warning/success readability while applying the selected hue to ordinary UI copy. */
    public static int textColor(int original) {
        int r = (original >> 16) & 255, g = (original >> 8) & 255, b = original & 255;
        int luminance = (r * 299 + g * 587 + b * 114) / 1000;
        return luminance >= 160 ? primaryText() : secondaryText();
    }
}
