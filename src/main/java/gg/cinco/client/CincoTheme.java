package gg.cinco.client;

/** Shared high-contrast neon palette driven by SETTINGS > Color Wheel. */
public final class CincoTheme {
    private CincoTheme() {}

    private static ClientModule wheel() { return CincoClient.findModule("Color Wheel"); }

    private static float hue() {
        ClientModule m = wheel();
        return (float) (((m == null ? 28.0 : m.setting) % 360.0 + 360.0) % 360.0) / 360.0f;
    }

    private static float saturation() {
        ClientModule m = wheel();
        return (float) Math.max(0.10, Math.min(1.0, m == null ? 0.94 : m.setting2));
    }

    private static float brightness() {
        ClientModule m = wheel();
        return (float) Math.max(0.20, Math.min(1.0, m == null ? 1.0 : m.setting3));
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
        return hsv(hue(), Math.max(0.0f, Math.min(1.0f, saturation() * saturationScale)), value);
    }

    private static float accentStrength() {
        ClientModule glow = CincoClient.findModule("Accent Glow");
        return glow == null || !glow.enabled ? 0.82f : (float) Math.max(0.35, Math.min(1.0, glow.setting));
    }

    // Brighter neon accent while keeping the panels dark enough to make it pop.
    public static int accent() { return tinted(1.0f, Math.min(1.0f, 0.90f + 0.10f * accentStrength())); }
    public static int accentLight() { return tinted(0.42f, 1.0f); }
    public static int accentMuted() { return tinted(0.88f, Math.min(0.88f, 0.70f + 0.12f * accentStrength())); }
    public static int accentDark() { return tinted(0.95f, 0.38f); }

    private static int withPanelOpacity(int color) {
        ClientModule opacity = CincoClient.findModule("Panel Opacity");
        double setting = opacity == null ? 1.0 : opacity.setting;
        int alpha = (int) Math.round(Math.max(0.78, Math.min(1.0, setting)) * 255.0);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int background() { return tinted(0.30f, 0.030f); }
    public static int panel() { return withPanelOpacity(tinted(0.34f, 0.070f)); }
    public static int sidebar() { return withPanelOpacity(tinted(0.40f, 0.095f)); }
    public static int card() { return withPanelOpacity(tinted(0.46f, 0.135f)); }
    public static int cardHover() { return withPanelOpacity(tinted(0.70f, 0.255f)); }
    public static int line() { return tinted(0.82f, 0.52f); }
    public static int sliderTrack() { return tinted(0.70f, 0.32f); }
    public static int enabled() { return tinted(1.0f, 0.86f); }
    public static int disabled() { return tinted(0.34f, 0.26f); }

    public static int primaryText() {
        return hsv(hue(), Math.min(0.22f, saturation() * 0.26f), 1.0f);
    }

    public static int secondaryText() {
        return hsv(hue(), Math.min(0.58f, saturation() * 0.64f), Math.max(0.82f, brightness() * 0.96f));
    }

    public static int textColor(int original) {
        int r = (original >> 16) & 255, g = (original >> 8) & 255, b = original & 255;
        int luminance = (r * 299 + g * 587 + b * 114) / 1000;
        return luminance >= 150 ? primaryText() : secondaryText();
    }
}
