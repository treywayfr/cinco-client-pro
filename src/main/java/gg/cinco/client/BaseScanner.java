package gg.cinco.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/** Scans only the client world state around the player; it does not request or download unloaded chunks. */
public final class BaseScanner {
    private static final List<BlockPos> findings = new ArrayList<>();
    private static final Set<BlockPos> seen = new HashSet<>();
    private static int cursor;
    private static int centerX, centerY, centerZ, radius;
    private static long lastWorldIdentity;
    private static boolean started;

    private BaseScanner() {}

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) { reset(); return; }
        ClientModule module = activeRadiusModule();
        int wantedRadius;
        if (module != null && module.name.equals("Sus Chunk Finder")) wantedRadius = (int) module.setting * 16;
        else if (module != null && module.name.equals("Chunk Density")) wantedRadius = (int) module.setting * 16;
        else wantedRadius = module == null ? 32 : (int) module.setting;
        int px = client.player.getBlockX(), py = client.player.getBlockY(), pz = client.player.getBlockZ();
        long worldIdentity = System.identityHashCode(client.world);
        if (!started || worldIdentity != lastWorldIdentity || Math.abs(px - centerX) > 12 || Math.abs(pz - centerZ) > 12 || wantedRadius != radius) {
            centerX = px; centerY = py; centerZ = pz; radius = wantedRadius;
            cursor = 0; findings.clear(); seen.clear(); lastWorldIdentity = worldIdentity; started = true;
        }

        int width = radius * 2 + 1;
        int height = 49;
        int volume = width * width * height;
        int budget = 1200;
        for (int i = 0; i < budget && cursor < volume; i++, cursor++) {
            int layer = cursor / (width * width);
            int within = cursor % (width * width);
            int dx = within % width - radius;
            int dz = within / width - radius;
            int dy = layer - 24;
            BlockPos pos = new BlockPos(centerX + dx, centerY + dy, centerZ + dz);
            if (!client.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            BlockState state = client.world.getBlockState(pos);
            if (isInteresting(state) && findings.size() < 800) { BlockPos stable = pos.toImmutable(); if (seen.add(stable)) findings.add(stable); }
        }
        if (cursor >= volume) {
            findings.sort(Comparator.comparingDouble(p -> {
                double dx = p.getX() - client.player.getX();
                double dy = p.getY() - client.player.getY();
                double dz = p.getZ() - client.player.getZ();
                return dx * dx + dy * dy + dz * dz;
            }));
            cursor = 0;
            centerX = px; centerY = py; centerZ = pz;
        }
    }

    private static ClientModule activeRadiusModule() {
        String[] candidates = {"Container Finder", "Base Finder", "Portal Finder", "Spawner Finder", "Shulker Finder", "Sus Chunk Finder", "Chunk Density"};
        for (String name : candidates) {
            ClientModule module = CincoClient.findModule(name);
            if (module != null && module.enabled) return module;
        }
        return null;
    }

    private static boolean isInteresting(BlockState state) {
        return state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST) || state.isOf(Blocks.BARREL)
                || state.isOf(Blocks.ENDER_CHEST) || state.isOf(Blocks.SPAWNER)
                || state.isOf(Blocks.FURNACE) || state.isOf(Blocks.BLAST_FURNACE)
                || state.isOf(Blocks.SMOKER) || state.getBlock() instanceof ShulkerBoxBlock
                || state.isOf(Blocks.NETHER_PORTAL) || state.isOf(Blocks.RESPAWN_ANCHOR)
                || state.isOf(Blocks.CRAFTING_TABLE) || state.isOf(Blocks.BREWING_STAND)
                || state.isOf(Blocks.ENCHANTING_TABLE) || state.isOf(Blocks.ANVIL)
                || state.isOf(Blocks.CHIPPED_ANVIL) || state.isOf(Blocks.DAMAGED_ANVIL);
    }

    public static List<BlockPos> getFindings() { return List.copyOf(findings); }
    public static int getRadius() { return radius; }
    public static int getProgressPercent() {
        int width = radius * 2 + 1;
        int volume = width * width * 49;
        return volume == 0 ? 0 : Math.min(100, (int)(cursor * 100.0 / volume));
    }
    public static void reset() { findings.clear(); seen.clear(); cursor = 0; started = false; }
}
