package gg.cinco.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Perspective-only detached camera. The real network player is never teleported by Free Cam. */
public final class FreeCamController {
    private static OtherClientPlayerEntity camera;
    private static ClientWorld activeWorld;
    private static boolean active;
    private static float savedYaw;
    private static float savedPitch;
    private static float freeYaw;
    private static float freePitch;

    private FreeCamController() {}

    public static boolean isActive() { return active; }

    public static void tick(MinecraftClient client) {
        ClientModule module = CincoClient.findModule("Free Cam");
        boolean wanted = module != null && module.enabled;
        if (client == null || client.player == null || client.world == null || !wanted) {
            disable(client);
            return;
        }
        if (!active || camera == null || activeWorld != client.world) enable(client);
        if (!active || camera == null) return;

        // Keep the actual player anchored to the exact orientation it had when Free Cam started.
        client.player.setYaw(savedYaw);
        client.player.setPitch(savedPitch);
        client.player.setHeadYaw(savedYaw);
        client.player.setVelocity(0.0, 0.0, 0.0);

        camera.noClip = true;
        camera.setNoGravity(true);
        camera.setVelocity(0.0, 0.0, 0.0);
        camera.setYaw(freeYaw);
        camera.setPitch(freePitch);
        camera.setHeadYaw(freeYaw);
        if (client.getCameraEntity() != camera) client.setCameraEntity(camera);
        if (client.currentScreen != null || client.getWindow() == null) return;

        // Read physical keys directly. This keeps Free Cam controls independent from the frozen
        // real-player movement path and fixes the old case where movement input could be swallowed.
        long window = client.getWindow().getHandle();
        double base = Math.max(0.05, module.setting) * 0.24;
        boolean sprint = down(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || down(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        double speed = sprint ? base * 3.0 : base;
        double forward = (down(window, GLFW.GLFW_KEY_W) ? 1.0 : 0.0)
                - (down(window, GLFW.GLFW_KEY_S) ? 1.0 : 0.0);
        double strafe = (down(window, GLFW.GLFW_KEY_D) ? 1.0 : 0.0)
                - (down(window, GLFW.GLFW_KEY_A) ? 1.0 : 0.0);
        double vertical = (down(window, GLFW.GLFW_KEY_SPACE) ? 1.0 : 0.0)
                - ((down(window, GLFW.GLFW_KEY_LEFT_SHIFT) || down(window, GLFW.GLFW_KEY_RIGHT_SHIFT)) ? 1.0 : 0.0);

        double yaw = Math.toRadians(freeYaw);
        double dx = -Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
        double dz = Math.cos(yaw) * forward + Math.sin(yaw) * strafe;
        double horizontal = Math.hypot(dx, dz);
        if (horizontal > 0.0001) { dx /= horizontal; dz /= horizontal; }
        double length3d = Math.sqrt(dx * dx + dz * dz + vertical * vertical);
        if (length3d > 1.0) { dx /= length3d; dz /= length3d; vertical /= length3d; }

        camera.prevX = camera.getX();
        camera.prevY = camera.getY();
        camera.prevZ = camera.getZ();
        camera.prevYaw = camera.getYaw();
        camera.prevPitch = camera.getPitch();
        camera.setPosition(camera.getX() + dx * speed,
                camera.getY() + vertical * speed,
                camera.getZ() + dz * speed);
    }

    private static boolean down(long window, int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    /**
     * Capture the camera entity's vanilla mouse rotation without ever allowing that rotation to
     * become the real player's server-facing look direction.
     */
    public static void syncMouseLook(MinecraftClient client) {
        if (!active || camera == null || client == null || client.player == null) return;

        // In modern Minecraft Mouse rotates the active camera entity. The previous implementation
        // read the real player's yaw instead, so freeYaw stayed stale and tick() snapped the camera
        // back every frame. That is the shake/seizure bug this path fixes.
        float cameraYaw = camera.getYaw();
        float cameraPitch = camera.getPitch();
        float cameraYawDelta = MathHelper.wrapDegrees(cameraYaw - freeYaw);
        float cameraPitchDelta = cameraPitch - freePitch;

        if (Math.abs(cameraYawDelta) > 0.00001F || Math.abs(cameraPitchDelta) > 0.00001F) {
            freeYaw = MathHelper.wrapDegrees(cameraYaw);
            freePitch = MathHelper.clamp(cameraPitch, -90.0F, 90.0F);
        } else {
            // Fallback for mappings/versions where Mouse still rotates the local player directly.
            float playerYawDelta = MathHelper.wrapDegrees(client.player.getYaw() - savedYaw);
            float playerPitchDelta = client.player.getPitch() - savedPitch;
            if (Math.abs(playerYawDelta) > 0.00001F || Math.abs(playerPitchDelta) > 0.00001F) {
                freeYaw = MathHelper.wrapDegrees(freeYaw + playerYawDelta);
                freePitch = MathHelper.clamp(freePitch + playerPitchDelta, -90.0F, 90.0F);
                camera.setYaw(freeYaw);
                camera.setPitch(freePitch);
                camera.setHeadYaw(freeYaw);
            }
        }

        client.player.setYaw(savedYaw);
        client.player.setPitch(savedPitch);
        client.player.setHeadYaw(savedYaw);
    }

    private static void enable(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (active) disable(client);
        activeWorld = client.world;
        savedYaw = client.player.getYaw();
        savedPitch = client.player.getPitch();
        freeYaw = savedYaw;
        freePitch = savedPitch;

        UUID id = UUID.nameUUIDFromBytes("star-free-camera".getBytes(StandardCharsets.UTF_8));
        camera = new OtherClientPlayerEntity(client.world, new GameProfile(id, "StarFreeCam"));
        // Camera entity starts at the player's feet; vanilla Camera adds eye height itself.
        camera.setPosition(client.player.getX(), client.player.getY(), client.player.getZ());
        camera.prevX = camera.getX();
        camera.prevY = camera.getY();
        camera.prevZ = camera.getZ();
        camera.setInvisible(true);
        camera.setNoGravity(true);
        camera.noClip = true;
        camera.prevYaw = freeYaw;
        camera.prevPitch = freePitch;
        camera.setYaw(freeYaw);
        camera.setPitch(freePitch);
        camera.setHeadYaw(freeYaw);
        client.setCameraEntity(camera);
        active = true;
    }

    public static void disable(MinecraftClient client) {
        if (!active) return;
        if (client != null && client.player != null) {
            client.setCameraEntity(client.player);
            client.player.setYaw(savedYaw);
            client.player.setPitch(savedPitch);
            client.player.setHeadYaw(savedYaw);
            client.player.setVelocity(0.0, 0.0, 0.0);
        }
        camera = null;
        activeWorld = null;
        active = false;
    }
}
