package gg.cinco.client;

/**
 * Star Client's Nebula Luxe palette.
 *
 * The user's Color Wheel still controls the primary hue, while secondary hues are derived from it
 * so the interface keeps the same violet/magenta/cyan luxury look without hard-coding every panel.
 * The very dark surfaces are intentionally cheap alpha layers rather than real-time blur.
 */
public final class CincoTheme {
    private CincoTheme() {}

    private static ClientModule wheel() { return CincoClient.findModule("Color Wheel"); }

    private static float hue() {
        ClientModule m = wheel();
        return (float) (((m == null ? 286.0 : m.setting) % 360.0 + 360.0) % 360.0) / 360.0f;
    }

    private static float saturation() {
        ClientModule m = wheel();
        return (float) Math.max(0.10, Math.min(1.0, m == null ? 0.86 : m.setting2));
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

    private static float shifted(float amount) {
        return (hue() + amount + 1.0f) % 1.0f;
    }

    private static int tinted(float h, float saturationScale, float value) {
        return hsv(h, Math.max(0.0f, Math.min(1.0f, saturation() * saturationScale)), value);
    }

    private static int tinted(float saturationScale, float value) {
        return tinted(hue(), saturationScale, value);
    }

    private static float accentStrength() {
        ClientModule glow = CincoClient.findModule("Accent Glow");
        return glow == null || !glow.enabled ? 0.80f : (float) Math.max(0.30, Math.min(1.0, glow.setting));
    }

    // Nebula Luxe accent family: primary violet, magenta edge, cool cyan highlight, tiny gold detail.
    public static int accent() {
        return tinted(1.0f, Math.min(1.0f, 0.90f + 0.10f * accentStrength()));
    }

    public static int accentMuted() {
        return tinted(shifted(0.095f), 0.88f, Math.min(0.92f, 0.72f + 0.16f * accentStrength()));
    }

    public static int accentLight() {
        return tinted(shifted(0.225f), 0.46f, 1.0f);
    }

    public static int accentDark() {
        return tinted(0.96f, 0.30f + 0.08f * accentStrength());
    }

    public static int magenta() { return tinted(shifted(0.095f), 0.92f, 0.98f); }
    public static int cyan() { return tinted(shifted(0.225f), 0.70f, 1.0f); }
    public static int starGold() { return 0xFFFFD66B; }

    private static int withPanelOpacity(int color) {
        ClientModule opacity = CincoClient.findModule("Panel Opacity");
        double setting = opacity == null ? 0.96 : opacity.setting;
        int alpha = (int) Math.round(Math.max(0.74, Math.min(1.0, setting)) * 255.0);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    // Near-black translucent depth. These fake the premium glass feel without an expensive blur pass.
    public static int background() { return 0xFF05040A; }
    public static int panel() { return withPanelOpacity(tinted(0.28f, 0.055f)); }
    public static int sidebar() { return withPanelOpacity(tinted(0.38f, 0.078f)); }
    public static int card() { return withPanelOpacity(tinted(0.42f, 0.115f)); }
    public static int cardHover() { return withPanelOpacity(tinted(shifted(0.045f), 0.62f, 0.205f)); }
    public static int line() { return tinted(shifted(0.10f), 0.76f, 0.54f); }
    public static int sliderTrack() { return tinted(0.42f, 0.25f); }
    public static int enabled() { return tinted(shifted(0.07f), 0.92f, 0.96f); }
    public static int disabled() { return tinted(0.22f, 0.22f); }

    public static int primaryText() {
        return hsv(shifted(0.01f), Math.min(0.16f, saturation() * 0.20f), 1.0f);
    }

    public static int secondaryText() {
        return hsv(shifted(0.03f), Math.min(0.34f, saturation() * 0.44f), Math.max(0.78f, brightness() * 0.92f));
    }

    public static int textColor(int original) {
        int r = (original >> 16) & 255, g = (original >> 8) & 255, b = original & 255;
        int luminance = (r * 299 + g * 587 + b * 114) / 1000;
        return luminance >= 150 ? primaryText() : secondaryText();
    }
}
