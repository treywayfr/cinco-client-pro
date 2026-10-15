package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Lightweight client-side controller for the newer combat/movement convenience modules. */
public final class AdvancedModulesController {
    private static int clickTicks;
    private static int silentTicks;
    private static int lungeTicks;
    private static int jumpTicks;
    private static int breachTicks;
    private static int shieldTicks;

    private AdvancedModulesController() {}

    public static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.interactionManager == null) return;
        if (clickTicks > 0) clickTicks--;
        if (silentTicks > 0) silentTicks--;
        if (lungeTicks > 0) lungeTicks--;
        if (jumpTicks > 0) jumpTicks--;
        if (breachTicks > 0) breachTicks--;
        if (shieldTicks > 0) shieldTicks--;
        if (client.currentScreen != null) return;

        tickAutoSprint(client);
        tickAimAssist(client);
        tickSilentAim(client);
        tickAutoMace(client);
        tickBreachswap(client);
        tickLunge(client);
        tickJumpReset(client);
        tickShieldDrain(client);
        tickAutoClicker(client);
    }

    private static void tickAutoSprint(MinecraftClient client) {
        ClientModule m = module("Auto Sprint");
        if (m != null && m.enabled && client.options.forwardKey.isPressed() && !client.player.isSneaking())
            client.player.setSprinting(true);
    }

    private static void tickAimAssist(MinecraftClient client) {
        ClientModule m = module("Aim Assist");
        if (m == null || !m.enabled) return;
        PlayerEntity target = target(client, m.setting, m.setting3);
        if (target == null) return;
        Rotation rot = rotationTo(client.player, target);
        float strength = (float)Math.max(0.05, Math.min(1.0, m.setting2));
        client.player.setYaw(client.player.getYaw() + MathHelper.wrapDegrees(rot.yaw - client.player.getYaw()) * strength);
        client.player.setPitch(client.player.getPitch() + MathHelper.wrapDegrees(rot.pitch - client.player.getPitch()) * strength);
    }

    /** Redirect attacks to the best target without rotating the visible camera. */
    private static void tickSilentAim(MinecraftClient client) {
        ClientModule m = module("Silent Aim");
        if (m == null || !m.enabled || silentTicks > 0) return;
        if (m.setting3 >= 0.5 && !client.options.attackKey.isPressed()) return;
        PlayerEntity target = target(client, m.setting, m.setting2);
        if (target == null) return;
        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
        silentTicks = 3;
    }

    private static void tickAutoMace(MinecraftClient client) {
        ClientModule m = module("Auto Mace");
        if (m == null || !m.enabled || client.player.getVelocity().y > -Math.max(0.05, m.setting2)) return;
        if (target(client, m.setting, 180) == null) return;
        int slot = findHotbar(client, Items.MACE);
        if (slot >= 0) client.player.getInventory().selectedSlot = slot;
    }

    private static void tickBreachswap(MinecraftClient client) {
        ClientModule m = module("Breachswap");
        if (m == null || !m.enabled || breachTicks > 0 || !client.options.attackKey.isPressed()) return;
        if (target(client, m.setting, 100) == null) return;
        int slot = findHotbar(client, Items.MACE);
        if (slot >= 0) {
            client.player.getInventory().selectedSlot = slot;
            breachTicks = Math.max(1, (int)Math.round(m.setting2));
        }
    }

    private static void tickLunge(MinecraftClient client) {
        ClientModule m = module("Lunge Macro");
        if (m == null || !m.enabled || lungeTicks > 0 || !client.options.attackKey.isPressed()) return;
        if (m.setting3 >= 0.5 && !client.player.getMainHandStack().isOf(Items.MACE)) return;
        Vec3d look = client.player.getRotationVector();
        Vec3d horizontal = new Vec3d(look.x, 0, look.z);
        if (horizontal.lengthSquared() < 0.0001) return;
        horizontal = horizontal.normalize().multiply(Math.max(0.05, m.setting));
        Vec3d old = client.player.getVelocity();
        client.player.setVelocity(horizontal.x, Math.max(old.y, m.setting2), horizontal.z);
        lungeTicks = 10;
    }

    private static void tickJumpReset(MinecraftClient client) {
        ClientModule m = module("Auto Jump Reset");
        if (m == null || !m.enabled || jumpTicks > 0 || !client.options.attackKey.isPressed()) return;
        if (m.setting2 >= 0.5 && !client.player.isOnGround()) return;
        if (m.setting3 >= 0.5 && target(client, 4.5, 100) == null) return;
        client.player.jump();
        jumpTicks = Math.max(2, (int)Math.round(m.setting));
    }

    private static void tickShieldDrain(MinecraftClient client) {
        ClientModule m = module("Shield Drain with Mace");
        if (m == null || !m.enabled || shieldTicks > 0) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof PlayerEntity target)) return;
        if (client.player.squaredDistanceTo(target) > m.setting * m.setting) return;
        if (!target.isUsingItem() || !target.getActiveItem().isOf(Items.SHIELD)) return;
        int slot = findHotbar(client, Items.MACE);
        if (slot >= 0) {
            client.player.getInventory().selectedSlot = slot;
            shieldTicks = Math.max(1, (int)Math.round(m.setting2));
        }
    }

    private static void tickAutoClicker(MinecraftClient client) {
        ClientModule m = module("Auto Clicker");
        if (m == null || !m.enabled || clickTicks > 0) return;
        int cps = Math.max(1, Math.min(20, (int)Math.round(m.setting)));
        clickTicks = Math.max(1, 20 / cps);

        if (m.setting2 >= 0.5) {
            HitResult hit = client.crosshairTarget;
            if (hit instanceof EntityHitResult entityHit) {
                client.interactionManager.attackEntity(client.player, entityHit.getEntity());
                client.player.swingHand(Hand.MAIN_HAND);
            } else if (hit instanceof BlockHitResult blockHit) {
                client.interactionManager.attackBlock(blockHit.getBlockPos(), blockHit.getSide());
                client.player.swingHand(Hand.MAIN_HAND);
            }
        }
        if (m.setting3 >= 0.5) {
            client.options.useKey.setPressed(true);
            // Release on the next tick so vanilla receives a clean pulse instead of a stuck key.
            client.execute(() -> client.options.useKey.setPressed(false));
        }
    }

    private static PlayerEntity target(MinecraftClient client, double range, double fov) {
        PlayerEntity best = null;
        double bestDist = range * range;
        for (PlayerEntity p : client.world.getPlayers()) {
            if (p == client.player || !p.isAlive() || p.isSpectator()) continue;
            double d = client.player.squaredDistanceTo(p);
            if (d > bestDist) continue;
            Rotation r = rotationTo(client.player, p);
            double yawDelta = Math.abs(MathHelper.wrapDegrees(r.yaw - client.player.getYaw()));
            if (yawDelta > Math.max(1.0, fov) * 0.5) continue;
            best = p; bestDist = d;
        }
        return best;
    }

    private static Rotation rotationTo(PlayerEntity from, PlayerEntity to) {
        Vec3d eye = from.getEyePos();
        Vec3d aim = to.getPos().add(0, to.getHeight() * 0.72, 0);
        Vec3d d = aim.subtract(eye);
        double flat = Math.hypot(d.x, d.z);
        float yaw = (float)(Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.atan2(d.y, flat)));
        return new Rotation(yaw, pitch);
    }

    private static int findHotbar(MinecraftClient client, net.minecraft.item.Item item) {
        for (int i=0;i<9;i++) if (client.player.getInventory().getStack(i).isOf(item)) return i;
        return -1;
    }

    private static ClientModule module(String name) { return CincoClient.findModule(name); }
    private record Rotation(float yaw, float pitch) {}
}
