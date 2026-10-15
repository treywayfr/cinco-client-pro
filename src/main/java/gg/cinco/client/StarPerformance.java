package gg.cinco.client;

import net.minecraft.client.MinecraftClient;

public final class StarPerformance {
    private StarPerformance() {}

    public static boolean active(MinecraftClient client) {
        ClientModule module = CincoClient.findModule("Performance Mode");
        return module != null && module.enabled && client != null && client.getCurrentFps() < module.setting;
    }

    /** 2 = full motion, 1 = reduced cosmetics, 0 = functional-only animation. */
    public static int animationTier(MinecraftClient client) {
        if (client == null) return 2;
        int fps = Math.max(1, client.getCurrentFps());
        ClientModule module = CincoClient.findModule("Performance Mode");
        if (module == null || !module.enabled) {
            if (fps < 35) return 0;
            if (fps < 55) return 1;
            return 2;
        }
        double target = Math.max(30.0, module.setting);
        if (fps < Math.max(30.0, target * 0.55)) return 0;
        if (fps < target) return 1;
        return 2;
    }

    public static int animatedCardCap(MinecraftClient client) {
        return switch (animationTier(client)) {
            case 0 -> 4;
            case 1 -> 10;
            default -> 18;
        };
    }

    public static boolean allowDecorativeHover(MinecraftClient client) {
        return animationTier(client) > 0;
    }

    public static boolean allowMenuShadow(MinecraftClient client) {
        return animationTier(client) > 0;
    }

    public static int guiStarCap(MinecraftClient client) {
        return switch (animationTier(client)) {
            case 0 -> 8;
            case 1 -> 20;
            default -> 42;
        };
    }

    public static int storageRenderCap(MinecraftClient client) {
        return active(client) ? 90 : 180;
    }

    public static int lightRenderCap(MinecraftClient client) {
        return active(client) ? 45 : 100;
    }
}
