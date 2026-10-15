package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/** Client-only render options which can be safely toggled from the GUI. */
public final class VisualModules {
    private static boolean fovCaptured;
    private static int originalFov = 70;
    private static double currentFov = 70.0;

    private VisualModules() {}

    public static void tick(MinecraftClient client) {
        if (client == null || client.options == null) return;
        ClientModule fov = CincoClient.findModule("FOV Changer");
        boolean enabled = fov != null && fov.enabled;

        if (enabled) {
            if (!fovCaptured) {
                originalFov = client.options.getFov().getValue();
                currentFov = originalFov;
                fovCaptured = true;
            }
            double desired = fov.setting;
            if (fov.setting3 >= 0.5) {
                double easeTicks = Math.max(1.0, fov.setting2);
                currentFov += (desired - currentFov) / easeTicks;
            } else {
                currentFov = desired;
            }
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

    /**
     * Presets: 1 Sunset, 2 Aurora, 3 Cyberpunk, 4 Cosmic, 5 Ember.
     * The moving presets use the selected animation speed and intensity.
     */
    public static Vec3d skyColor() {
        ClientModule module = CincoClient.findModule("Sky Changer");
        if (module == null || !module.enabled) return new Vec3d(0.35, 0.20, 0.55);
        int style = (int) Math.round(module.setting);
        double speed = Math.max(0.1, module.setting2);
        double intensity = Math.max(0.2, Math.min(1.0, module.setting3));
        double phase = System.currentTimeMillis() / 1000.0 * speed;
        double wave = (Math.sin(phase) + 1.0) * 0.5;
        double wave2 = (Math.sin(phase * 0.63 + 2.4) + 1.0) * 0.5;
        double r, g, b;

        switch (style) {
            case 1 -> { // Sunset gold / rose
                r = 0.92;
                g = 0.24 + wave * 0.22;
                b = 0.18 + wave2 * 0.18;
            }
            case 2 -> { // Aurora teal / emerald
                r = 0.08 + wave2 * 0.12;
                g = 0.38 + wave * 0.40;
                b = 0.48 + wave2 * 0.38;
            }
            case 3 -> { // Cyberpunk violet / neon blue
                r = 0.32 + wave * 0.38;
                g = 0.08 + wave2 * 0.10;
                b = 0.58 + wave * 0.36;
            }
            case 4 -> { // Cosmic hue cycling
                double hue = (phase * 0.08) % 1.0;
                double[] rgb = hsv(hue, 0.78, 0.95);
                r = rgb[0]; g = rgb[1]; b = rgb[2];
            }
            default -> { // Ember
                r = 0.70 + wave * 0.30;
                g = 0.13 + wave2 * 0.25;
                b = 0.025 + wave * 0.055;
            }
        }

        return new Vec3d(clamp(r * intensity), clamp(g * intensity), clamp(b * intensity));
    }

    public static Vec3d cloudColor() {
        Vec3d sky = skyColor();
        return new Vec3d(clamp(sky.x * 0.75 + 0.10), clamp(sky.y * 0.75 + 0.08), clamp(sky.z * 0.75 + 0.12));
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

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
