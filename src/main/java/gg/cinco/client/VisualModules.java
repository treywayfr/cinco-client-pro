package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.util.math.Vec3d;

/** Client-only render options which can be safely toggled from the GUI. */
public final class VisualModules {
    private static boolean gammaCaptured;
    private static double originalGamma = 0.5;
    private static double appliedGamma = Double.NaN;
    private static boolean fovCaptured;
    private static int originalFov = 70;
    private static double currentFov = 70.0;
    private static boolean cloudCaptured;
    private static CloudRenderMode originalCloudMode;

    private VisualModules() {}

    public static void tick(MinecraftClient client) {
        if (client == null || client.options == null) return;

        ClientModule bright = CincoClient.findModule("Fullbright");
        if (bright != null && bright.enabled) {
            if (!gammaCaptured) {
                originalGamma = client.options.getGamma().getValue();
                gammaCaptured = true;
                appliedGamma = Double.NaN;
            }
            double gamma = Math.max(0.0, Math.min(1.0, bright.setting));
            double current = client.options.getGamma().getValue();
            if (Double.isNaN(appliedGamma) || Math.abs(appliedGamma - gamma) > 0.0001 || Math.abs(current - gamma) > 0.0001) {
                client.options.getGamma().setValue(gamma);
                appliedGamma = gamma;
            }
        } else if (gammaCaptured) {
            double restore = Math.max(0.0, Math.min(1.0, originalGamma));
            if (Math.abs(client.options.getGamma().getValue() - restore) > 0.0001) client.options.getGamma().setValue(restore);
            gammaCaptured = false;
            appliedGamma = Double.NaN;
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

        // Deep-space presets intentionally hide vanilla clouds. Restore the player's exact option
        // as soon as Sky Changer is disabled.
        if (skyEnabled()) {
            if (!cloudCaptured) {
                originalCloudMode = client.options.getCloudRenderMode().getValue();
                cloudCaptured = true;
            }
            if (client.options.getCloudRenderMode().getValue() != CloudRenderMode.OFF) {
                client.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
            }
        } else if (cloudCaptured) {
            if (originalCloudMode != null) client.options.getCloudRenderMode().setValue(originalCloudMode);
            cloudCaptured = false;
            originalCloudMode = null;
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
        return sky == null ? 0.55 : Math.max(0.05, Math.min(1.60, sky.setting2));
    }

    public static double skyIntensity() {
        ClientModule sky = CincoClient.findModule("Sky Changer");
        return sky == null ? 0.90 : Math.max(0.25, Math.min(1.0, sky.setting3));
    }

    public static String skyStyleName() {
        return switch (skyStyle()) {
            case 2 -> "Blue Expanse";
            case 3 -> "Violet Rift";
            case 4 -> "Crimson Eclipse";
            case 5 -> "Ice Cosmos";
            case 6 -> "Emerald Void";
            case 7 -> "Event Horizon";
            case 8 -> "Galactic Spectrum";
            default -> "Nebula Abyss";
        };
    }

    public static Vec3d skyColor() {
        if (!skyEnabled()) return new Vec3d(0.35, 0.20, 0.55);
        int style = skyStyle();
        double intensity = skyIntensity();
        double phase = System.currentTimeMillis() / 1000.0 * skySpeed();
        double breathe = 0.90 + 0.10 * (0.5 + 0.5 * Math.sin(phase * 0.16));
        double r, g, b;
        switch (style) {
            case 2 -> { r = 0.008; g = 0.013; b = 0.045; }
            case 3 -> { r = 0.020; g = 0.006; b = 0.050; }
            case 4 -> { r = 0.030; g = 0.005; b = 0.030; }
            case 5 -> { r = 0.004; g = 0.020; b = 0.040; }
            case 6 -> { r = 0.004; g = 0.026; b = 0.021; }
            case 7 -> { r = 0.004; g = 0.003; b = 0.013; }
            case 8 -> { r = 0.018; g = 0.008; b = 0.034; }
            default -> { r = 0.010; g = 0.005; b = 0.030; }
        }
        double lift = (0.62 + 0.38 * intensity) * breathe;
        return new Vec3d(clamp(r * lift), clamp(g * lift), clamp(b * lift));
    }

    public static Vec3d cloudColor() {
        Vec3d sky = skyColor();
        return new Vec3d(clamp(sky.x * 0.30), clamp(sky.y * 0.30), clamp(sky.z * 0.36));
    }

    public static int skyFilamentColor() {
        return switch (skyStyle()) {
            case 2 -> 0xFF709CFF;
            case 3 -> 0xFFB05CFF;
            case 4 -> 0xFFFF61B5;
            case 5 -> 0xFF61D8FF;
            case 6 -> 0xFF52FFB8;
            case 7 -> 0xFF8D78FF;
            case 8 -> 0xFFFF72DE;
            default -> 0xFF8F67FF;
        };
    }

    private static double clamp(double value) { return Math.max(0.0, Math.min(1.0, value)); }
}
