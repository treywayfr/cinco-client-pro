package gg.cinco.client;

import net.minecraft.client.MinecraftClient;

public final class StarPerformance {
    private StarPerformance() {}

    public static boolean active(MinecraftClient client) {
        ClientModule module = CincoClient.findModule("Performance Mode");
        return module != null && module.enabled && client != null && client.getCurrentFps() < module.setting;
    }

    public static int guiStarCap(MinecraftClient client) {
        return active(client) ? 16 : 42;
    }

    public static int storageRenderCap(MinecraftClient client) {
        return active(client) ? 90 : 180;
    }

    public static int lightRenderCap(MinecraftClient client) {
        return active(client) ? 45 : 100;
    }
}
