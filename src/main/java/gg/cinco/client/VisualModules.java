package gg.cinco.client;

import gg.cinco.client.mixin.SimpleOptionAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/** Client-only render options which can be safely toggled from the GUI. */
public final class VisualModules {
    private static boolean gammaCaptured;
    private static double originalGamma = 0.5;
    private static boolean fovCaptured;
    private static int originalFov = 70;
    private static double currentFov = 70.0;

    private VisualModules() {}

    public static void tick(MinecraftClient client) {
        if (client == null || client.options == null) return;

        ClientModule bright = CincoClient.findModule("Fullbright");
        if (bright != null && bright.enabled) {
            if (!gammaCaptured) {
                originalGamma = client.options.getGamma().getValue();
                gammaCaptured = true;
            }
            double gamma = Math.max(1.0, Math.min(20.0, bright.setting));
            ((SimpleOptionAccessor) (Object) client.options.getGamma()).cinco$setRawValue(gamma);
        } else if (gammaCaptured) {
            ((SimpleOptionAccessor) (Object) client.options.getGamma()).cinco$setRawValue(originalGamma);
            gammaCaptured = false;
        }

        ClientModule fov = CincoClient.findModule("FOV Changer");
        boolean enabled = fov != null && fov.enabled;
        if (enabled) {
            if (!fovCaptured) {
                originalFov = client.options.getFov().getValue();
                currentFov = originalFov;
                fovCaptured = true;
            }
            currentFov = Math.max(fov.min, Math.min(fov.max, fov.setting));
            int adjusted = (int) Math.round(currentFov);
            if (!client.options.getFov().getValue().equals(adjusted)) client.options.getFov().setValue(adjusted);
        } else if (fovCaptured) {
            client.options.getFov().setValue(originalFov);
            currentFov = originalFov;
            fovCaptured = false;
        }
    }

    public static boolean skyEnabled() {
        ClientModule sky = CincoClient.findModule("Sky Changer");
        return sky != null && sky.enabled;
    }

    public static int skyStyle() {
        ClientModule sky = CincoClient.findModule("Sky Changer");
        return sky == null ? 1 : Math.max(1, Math.min(8, (int)Math.round(sky.setting)));
    }

    public static double skySpeed() {
        ClientModule sky = CincoClient.findModule("Sky Changer");
        return sky == null ? 0.8 : Math.max(0.15, Math.min(2.5, sky.setting2));
    }

    public static double skyIntensity() {
        ClientModule sky = CincoClient.findModule("Sky Changer");
        return sky == null ? 0.85 : Math.max(0.2, Math.min(1.0, sky.setting3));
    }

    /**
     * Dark animated bases are intentional. The procedural sky renderer paints the brighter
     * flowing filaments on top, producing the high-contrast liquid-light look from the reference.
     */
    public static Vec3d skyColor() {
        if (!skyEnabled()) return new Vec3d(0.35, 0.20, 0.55);
        int style = skyStyle();
        double speed = skySpeed();
        double intensity = skyIntensity();
        double phase = System.currentTimeMillis() / 1000.0 * speed;
        double w = (Math.sin(phase * 0.55) + 1.0) * 0.5;
        double w2 = (Math.sin(phase * 0.31 + 1.7) + 1.0) * 0.5;
        double r, g, b;
        switch (style) {
            case 1 -> { r = 0.018 + w * 0.020; g = 0.012 + w2 * 0.012; b = 0.006; } // liquid gold
            case 2 -> { r = 0.025 + w * 0.035; g = 0.006; b = 0.075 + w2 * 0.080; }
            case 3 -> { r = 0.045 + w * 0.060; g = 0.005; b = 0.115 + w2 * 0.080; }
            case 4 -> { r = 0.008; g = 0.040 + w * 0.055; b = 0.060 + w2 * 0.055; }
            case 5 -> { r = 0.090 + w * 0.065; g = 0.004; b = 0.012 + w2 * 0.020; }
            case 6 -> { r = 0.010 + w * 0.018; g = 0.055 + w2 * 0.055; b = 0.100 + w * 0.085; }
            case 7 -> { r = 0.005; g = 0.070 + w * 0.070; b = 0.025 + w2 * 0.040; }
            case 8 -> {
                double[] rgb = hsv((phase * 0.025) % 1.0, 0.82, 0.16 + 0.05 * w);
                r = rgb[0]; g = rgb[1]; b = rgb[2];
            }
            default -> { r = 0.018; g = 0.012; b = 0.006; }
        }
        double lift = 0.55 + intensity * 0.45;
        return new Vec3d(clamp(r * lift), clamp(g * lift), clamp(b * lift));
    }

    public static Vec3d cloudColor() {
        Vec3d sky = skyColor();
        return new Vec3d(clamp(sky.x * 0.65 + 0.025), clamp(sky.y * 0.65 + 0.025), clamp(sky.z * 0.65 + 0.035));
    }

    /** Main filament color for the active procedural sky. */
    public static int skyFilamentColor() {
        int style = skyStyle();
        double phase = System.currentTimeMillis() / 1000.0 * skySpeed();
        return switch (style) {
            case 2 -> 0xFF9A55FF;
            case 3 -> 0xFFCC5CFF;
            case 4 -> 0xFF59FFD1;
            case 5 -> 0xFFFF4F68;
            case 6 -> 0xFF73D9FF;
            case 7 -> 0xFF63FF89;
            case 8 -> rgb(hsv((phase * 0.04) % 1.0, 0.78, 1.0));
            default -> 0xFFFFD65A;
        };
    }

    private static int rgb(double[] c) {
        int r = (int)Math.round(clamp(c[0]) * 255.0);
        int g = (int)Math.round(clamp(c[1]) * 255.0);
        int b = (int)Math.round(clamp(c[2]) * 255.0);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static double clamp(double value) { return Math.max(0.0, Math.min(1.0, value)); }

    private static double[] hsv(double hue, double saturation, double value) {
        double h = ((hue % 1.0) + 1.0) % 1.0;
        double chroma = value * saturation;
        double section = h * 6.0;
        double x = chroma * (1.0 - Math.abs(section % 2.0 - 1.0));
        double r = 0, g = 0, b = 0;
        if (section < 1) { r = chroma; g = x; }
        else if (section < 2) { r = x; g = chroma; }
        else if (section < 3) { g = chroma; b = x; }
        else if (section < 4) { g = x; b = chroma; }
        else if (section < 5) { r = x; b = chroma; }
        else { r = chroma; b = x; }
        double m = value - chroma;
        return new double[]{r + m, g + m, b + m};
    }
}
