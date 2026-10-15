package gg.cinco.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;

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

        // Never touch the real player's position. The movement mixin only suppresses local input
        // while this detached camera is active, so the server-side player remains exactly where it is.
        client.player.setYaw(savedYaw);
        client.player.setPitch(savedPitch);
        client.player.setHeadYaw(savedYaw);

        camera.noClip = true;
        camera.setNoGravity(true);
        camera.setVelocity(0.0, 0.0, 0.0);
        camera.setYaw(freeYaw);
        camera.setPitch(freePitch);
        camera.setHeadYaw(freeYaw);
        if (client.getCameraEntity() != camera) client.setCameraEntity(camera);
        if (client.currentScreen != null) return;

        double base = Math.max(0.05, module.setting) * 0.24;
        double speed = client.options.sprintKey.isPressed() ? base * 3.0 : base;
        double forward = (client.options.forwardKey.isPressed() ? 1.0 : 0.0)
                - (client.options.backKey.isPressed() ? 1.0 : 0.0);
        double strafe = (client.options.rightKey.isPressed() ? 1.0 : 0.0)
                - (client.options.leftKey.isPressed() ? 1.0 : 0.0);
        double vertical = (client.options.jumpKey.isPressed() ? 1.0 : 0.0)
                - (client.options.sneakKey.isPressed() ? 1.0 : 0.0);

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
        camera.prevYaw = freeYaw;
        camera.prevPitch = freePitch;
        camera.setPosition(camera.getX() + dx * speed,
                camera.getY() + vertical * speed,
                camera.getZ() + dz * speed);
    }

    /** Capture vanilla mouse deltas without allowing them to rotate the real player. */
    public static void syncMouseLook(MinecraftClient client) {
        if (!active || camera == null || client == null || client.player == null) return;
        float yawDelta = MathHelper.wrapDegrees(client.player.getYaw() - savedYaw);
        float pitchDelta = client.player.getPitch() - savedPitch;
        if (Math.abs(yawDelta) > 0.00001F || Math.abs(pitchDelta) > 0.00001F) {
            freeYaw = MathHelper.wrapDegrees(freeYaw + yawDelta);
            freePitch = MathHelper.clamp(freePitch + pitchDelta, -90.0F, 90.0F);
            camera.prevYaw = freeYaw;
            camera.prevPitch = freePitch;
            camera.setYaw(freeYaw);
            camera.setPitch(freePitch);
            camera.setHeadYaw(freeYaw);
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
        // The render Camera adds this entity's normal eye height itself, so seed the detached
        // entity at the player's feet rather than at eyeY. Starting at eyeY would double-add the
        // eye height and make toggling Free Cam jump upward by roughly a block and a half.
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
        }
        camera = null;
        activeWorld = null;
        active = false;
    }
}
