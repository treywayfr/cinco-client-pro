package gg.cinco.client;

import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class CombatModules {
    private static int crystalCooldown;
    private static int anchorCooldown;

    private CombatModules() {}

    public static void tick(MinecraftClient client) {
        if (crystalCooldown > 0) crystalCooldown--;
        if (anchorCooldown > 0) anchorCooldown--;
        if (enabled("Auto Crystal") || enabled("Crystal Breaker")) tickAutoCrystal(client, enabled("Auto Crystal"));
        if (enabled("Anchor Macro")) tickAnchorMacro(client);
        if (enabled("Base Finder") || enabled("Container Finder")) BaseScanner.tick(client);
    }

    private static boolean enabled(String name) {
        ClientModule module = CincoClient.findModule(name);
        return module != null && module.enabled;
    }

    private static void tickAutoCrystal(MinecraftClient client, boolean allowPlace) {
        if (client.player == null || client.world == null || client.interactionManager == null || crystalCooldown > 0) return;
        ClientModule module = CincoClient.findModule(allowPlace ? "Auto Crystal" : "Crystal Breaker");
        double range = module == null ? 4.5 : module.setting;
        EndCrystalEntity crystal = null;
        double best = range * range;
        for (EndCrystalEntity endCrystal : client.world.getEntitiesByClass(EndCrystalEntity.class, client.player.getBoundingBox().expand(range), Entity::isAlive)) {
            double dist = client.player.squaredDistanceTo(endCrystal);
            if (dist < best) { best = dist; crystal = endCrystal; }
        }
        if (crystal != null) {
            client.interactionManager.attackEntity(client.player, crystal);
            client.player.swingHand(Hand.MAIN_HAND);
            crystalCooldown = 2;
            return;
        }

        if (!allowPlace) return;
        PlayerEntity target = null;
        double targetDistance = range * range;
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || !player.isAlive() || player.isSpectator()) continue;
            double dist = client.player.squaredDistanceTo(player);
            if (dist < targetDistance) { targetDistance = dist; target = player; }
        }
        if (target == null) return;
        int crystalSlot = findHotbarItem(client, Items.END_CRYSTAL);
        if (crystalSlot < 0) return;
        client.player.getInventory().selectedSlot = crystalSlot;
        BlockPos targetPos = target.getBlockPos();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos base = targetPos.add(dx, dy, dz);
                    if (!client.world.getBlockState(base).isOf(Blocks.OBSIDIAN)
                            && !client.world.getBlockState(base).isOf(Blocks.BEDROCK)) continue;
                    if (!client.world.getBlockState(base.up()).isAir() || !client.world.getBlockState(base.up(2)).isAir()) continue;
                    if (client.player.squaredDistanceTo(Vec3d.ofCenter(base)) > range * range) continue;
                    Vec3d hitPos = new Vec3d(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
                    BlockHitResult hit = new BlockHitResult(hitPos, Direction.UP, base, false);
                    client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
                    client.player.swingHand(Hand.MAIN_HAND);
                    crystalCooldown = 5;
                    return;
                }
            }
        }
    }

    private static int findHotbarItem(MinecraftClient client, net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) if (client.player.getInventory().getStack(i).isOf(item)) return i;
        return -1;
    }

    private static void tickAnchorMacro(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null || anchorCooldown > 0) return;
        boolean inNether = client.world.getRegistryKey().equals(World.NETHER);
        if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        if (!client.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)) return;
        int charges = client.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES);
        if (charges < 4) {
            int glowstoneSlot = findHotbarItem(client, Items.GLOWSTONE);
            if (glowstoneSlot < 0) return;
            client.player.getInventory().selectedSlot = glowstoneSlot;
        } else {
            if (inNether) return;
            int safeSlot = findNonGlowstoneSlot(client);
            if (safeSlot >= 0) client.player.getInventory().selectedSlot = safeSlot;
        }
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
        client.player.swingHand(Hand.MAIN_HAND);
        anchorCooldown = Math.max(3, moduleSettingInt("Anchor Macro", 4));
    }

    private static int findNonGlowstoneSlot(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            if (!client.player.getInventory().getStack(i).isOf(Items.GLOWSTONE)) return i;
        }
        return -1;
    }

    private static int moduleSettingInt(String name, int fallback) {
        ClientModule m = CincoClient.findModule(name);
        return m == null ? fallback : (int)m.setting;
    }
}
