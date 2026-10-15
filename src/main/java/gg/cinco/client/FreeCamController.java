package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Local-only detached camera. The real player stays in place while an invisible client-side
 * armor stand is used as the camera entity. This does not try to bypass server movement checks.
 */
public final class FreeCamController {
    private static ArmorStandEntity camera;
    private static boolean active;
    private static float savedYaw;
    private static float savedPitch;

    private FreeCamController() {}

    public static boolean isActive() { return active; }

    public static void tick(MinecraftClient client) {
        ClientModule module = CincoClient.findModule("Free Cam");
        boolean wanted = module != null && module.enabled;
        if (client == null || client.player == null || client.world == null || !wanted) {
            disable(client);
            return;
        }

        if (!active || camera == null || camera.getWorld() != client.world) enable(client);
        if (!active || camera == null) return;

        // Mouse look still changes the local player's yaw/pitch; mirror that rotation to the detached camera.
        camera.setYaw(client.player.getYaw());
        camera.setPitch(client.player.getPitch());
        client.player.setVelocity(Vec3d.ZERO);
        client.player.fallDistance = 0.0F;

        if (client.currentScreen != null) return;

        double base = Math.max(0.05, module.setting) * 0.22;
        double speed = client.options.sprintKey.isPressed() ? base * 2.75 : base;
        double forward = (client.options.forwardKey.isPressed() ? 1.0 : 0.0)
                - (client.options.backKey.isPressed() ? 1.0 : 0.0);
        double strafe = (client.options.leftKey.isPressed() ? 1.0 : 0.0)
                - (client.options.rightKey.isPressed() ? 1.0 : 0.0);
        double vertical = (client.options.jumpKey.isPressed() ? 1.0 : 0.0)
                - (client.options.sneakKey.isPressed() ? 1.0 : 0.0);

        double yaw = Math.toRadians(camera.getYaw());
        double dx = -Math.sin(yaw) * forward - Math.cos(yaw) * strafe;
        double dz = Math.cos(yaw) * forward - Math.sin(yaw) * strafe;
        double len = Math.hypot(dx, dz);
        if (len > 0.0001) { dx /= len; dz /= len; }

        camera.setPosition(camera.getX() + dx * speed,
                camera.getY() + vertical * speed,
                camera.getZ() + dz * speed);
    }

    private static void enable(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        savedYaw = client.player.getYaw();
        savedPitch = client.player.getPitch();
        camera = new ArmorStandEntity(client.world,
                client.player.getX(), client.player.getY(), client.player.getZ());
        camera.setInvisible(true);
        camera.setNoGravity(true);
        camera.setYaw(savedYaw);
        camera.setPitch(savedPitch);
        client.setCameraEntity(camera);
        active = true;
    }

    private static void disable(MinecraftClient client) {
        if (!active) return;
        if (client != null && client.player != null) {
            client.setCameraEntity(client.player);
            client.player.setYaw(savedYaw);
            client.player.setPitch(savedPitch);
            client.player.setVelocity(Vec3d.ZERO);
        }
        camera = null;
        active = false;
    }
}
