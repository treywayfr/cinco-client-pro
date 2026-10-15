package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerAbilities;

/** Smooth local flight controller. It does not attempt to bypass server movement checks. */
public final class EnhancedMovement {
    private static boolean captured;
    private static boolean oldAllowFlying;
    private static boolean oldFlying;
    private static float oldFlySpeed;

    private EnhancedMovement() {}

    public static void tickFlight(MinecraftClient client) {
        ClientModule flight = CincoClient.findModule("Flight");
        if (client == null || client.player == null || flight == null || !flight.enabled || client.player.isSpectator()) {
            restore(client);
            return;
        }

        PlayerAbilities abilities = client.player.getAbilities();
        if (!captured) {
            oldAllowFlying = abilities.allowFlying;
            oldFlying = abilities.flying;
            oldFlySpeed = abilities.getFlySpeed();
            captured = true;
        }

        abilities.allowFlying = true;
        abilities.flying = true;
        abilities.setFlySpeed((float) Math.max(0.02, Math.min(0.25, flight.setting2 * 0.08)));

        boolean controls = client.currentScreen == null;
        double forward = controls ? (client.options.forwardKey.isPressed() ? 1.0 : 0.0)
                - (client.options.backKey.isPressed() ? 1.0 : 0.0) : 0.0;
        double strafe = controls ? (client.options.leftKey.isPressed() ? 1.0 : 0.0)
                - (client.options.rightKey.isPressed() ? 1.0 : 0.0) : 0.0;
        double vertical = controls ? (client.options.jumpKey.isPressed() ? flight.setting3 : 0.0)
                - (client.options.sneakKey.isPressed() ? flight.setting3 : 0.0) : 0.0;

        double yaw = Math.toRadians(client.player.getYaw());
        double dx = -Math.sin(yaw) * forward - Math.cos(yaw) * strafe;
        double dz = Math.cos(yaw) * forward - Math.sin(yaw) * strafe;
        double length = Math.hypot(dx, dz);
        if (length > 0.0001) {
            dx /= length;
            dz /= length;
        }

        double horizontalSpeed = Math.max(0.05, flight.setting2);
        client.player.setVelocity(dx * horizontalSpeed, vertical, dz * horizontalSpeed);
        client.player.fallDistance = 0.0F;
    }

    private static void restore(MinecraftClient client) {
        if (!captured) return;
        if (client != null && client.player != null) {
            PlayerAbilities abilities = client.player.getAbilities();
            abilities.allowFlying = oldAllowFlying;
            abilities.flying = oldFlying;
            abilities.setFlySpeed(oldFlySpeed);
        }
        captured = false;
    }
}
