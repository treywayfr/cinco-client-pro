package gg.cinco.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** Screen-space ESP markers are intentionally drawn after the world, so walls do not hide them. */
public final class EnhancedEspHud {
    private record Point(int x, int y, double depth) {}

    private EnhancedEspHud() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.currentScreen != null) return;
        drawPlayerEsp(ctx, client);
        drawStorageEsp(ctx, client);
        drawLightFinder(ctx, client);
        drawSusChunks(ctx, client);
    }

    private static void drawPlayerEsp(DrawContext ctx, MinecraftClient client) {
        ClientModule esp = CincoClient.findModule("Player ESP");
        ClientModule tracers = CincoClient.findModule("Tracers");
        boolean espOn = esp != null && esp.enabled;
        boolean tracerOn = tracers != null && tracers.enabled;
        if (!espOn && !tracerOn) return;

        double range = espOn ? esp.setting : tracers.setting;
        if (espOn && tracerOn) range = Math.max(esp.setting, tracers.setting);
        double r2 = range * range;
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || !player.isAlive() || client.player.squaredDistanceTo(player) > r2) continue;
            Point feet = project(client, new Vec3d(player.getX(), player.getY(), player.getZ()));
            Point head = project(client, new Vec3d(player.getX(), player.getY() + player.getHeight() + 0.15, player.getZ()));
            if (feet == null || head == null) continue;

            int height = Math.max(12, Math.abs(feet.y - head.y));
            int half = Math.max(5, height / 5);
            int left = head.x - half, right = head.x + half;
            int top = Math.min(head.y, feet.y), bottom = Math.max(head.y, feet.y);
            int color = 0xFFE36BFF;

            if (espOn && esp.setting3 >= 0.5) drawRectOutline(ctx, left, top, right, bottom, color);
            if (tracerOn) CincoDraw.line(ctx, sw / 2, sh - 2, feet.x, feet.y, 0xAAE36BFF, 1);
            if (espOn && esp.setting2 >= 0.5) {
                double distance = client.player.distanceTo(player);
                String label = player.getName().getString() + "  " + String.format(Locale.ROOT, "%.1fm", distance);
                int tw = client.textRenderer.getWidth(label);
                ctx.fill(head.x - tw / 2 - 3, top - 14, head.x + tw / 2 + 3, top - 3, 0xB0100D17);
                ctx.drawText(client.textRenderer, Text.literal(label), head.x - tw / 2, top - 13, 0xFFFFFFFF, false);
            }
        }
    }

    private static void drawStorageEsp(DrawContext ctx, MinecraftClient client) {
        ClientModule esp = CincoClient.findModule("Storage ESP");
        if (esp == null || !esp.enabled) return;
        double rangeSq = esp.setting * esp.setting;
        int filter = (int) Math.round(esp.setting2);
        int shown = 0;

        for (BlockPos pos : EnhancedWorldScan.storage()) {
            if (shown >= 90) break;
            double distanceSq = client.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distanceSq > rangeSq) continue;
            BlockState state = client.world.getBlockState(pos);
            if (!EnhancedWorldScan.storageMatchesFilter(state, filter)) continue;
            Point p = project(client, Vec3d.ofCenter(pos));
            if (p == null) continue;

            int color = storageColor(state);
            int size = Math.max(3, Math.min(9, (int) (85.0 / Math.max(8.0, Math.sqrt(distanceSq)))));
            drawRectOutline(ctx, p.x - size, p.y - size, p.x + size, p.y + size, color);
            ctx.fill(p.x - 1, p.y - 1, p.x + 2, p.y + 2, color);

            if (esp.setting3 >= 0.5 && shown < 35) {
                String label = EnhancedWorldScan.storageLabel(state) + "  " + Math.round(Math.sqrt(distanceSq)) + "m";
                int tw = client.textRenderer.getWidth(label);
                ctx.fill(p.x - tw / 2 - 2, p.y - size - 12, p.x + tw / 2 + 2, p.y - size - 2, 0xB0100D17);
                ctx.drawText(client.textRenderer, Text.literal(label), p.x - tw / 2, p.y - size - 11, 0xFFFFFFFF, false);
            }
            shown++;
        }
    }

    private static void drawLightFinder(DrawContext ctx, MinecraftClient client) {
        ClientModule module = CincoClient.findModule("Light Finder");
        if (module == null || !module.enabled) return;
        int shown = 0;
        for (BlockPos pos : EnhancedWorldScan.lightHotspots()) {
            if (shown++ >= 70) break;
            Point p = project(client, Vec3d.ofCenter(pos));
            if (p == null) continue;
            int color = 0xFFFFD45A;
            CincoDraw.line(ctx, p.x - 5, p.y, p.x, p.y - 5, color, 1);
            CincoDraw.line(ctx, p.x, p.y - 5, p.x + 5, p.y, color, 1);
            CincoDraw.line(ctx, p.x + 5, p.y, p.x, p.y + 5, color, 1);
            CincoDraw.line(ctx, p.x, p.y + 5, p.x - 5, p.y, color, 1);
            if (shown <= 24) {
                int level = client.world.getLightLevel(net.minecraft.world.LightType.BLOCK, pos);
                String text = "LIGHT " + level;
                ctx.drawText(client.textRenderer, Text.literal(text), p.x + 7, p.y - 4, color, false);
            }
        }
    }

    private static void drawSusChunks(DrawContext ctx, MinecraftClient client) {
        ClientModule module = CincoClient.findModule("Sus Chunk Finder");
        if (module == null || !module.enabled) return;
        int x = client.getWindow().getScaledWidth() - 176;
        int y = 58;
        int rows = Math.min(6, EnhancedWorldScan.susChunks().size());
        if (rows == 0) return;
        ctx.fill(x - 6, y - 15, x + 168, y + rows * 13 + 5, 0xC8100D17);
        ctx.fill(x - 6, y - 15, x - 3, y + rows * 13 + 5, CincoTheme.accent());
        ctx.drawText(client.textRenderer, Text.literal("SUS CHUNKS  •  LOADED"), x, y - 12, 0xFFFFC078, false);
        for (int i = 0; i < rows; i++) {
            EnhancedWorldScan.SusChunk hit = EnhancedWorldScan.susChunks().get(i);
            int bx = (hit.chunkX() << 4) + 8;
            int bz = (hit.chunkZ() << 4) + 8;
            double dx = bx - client.player.getX();
            double dz = bz - client.player.getZ();
            int distance = (int) Math.round(Math.hypot(dx, dz));
            String text = hit.chunkX() + ", " + hit.chunkZ() + "  score " + hit.score() + "  " + distance + "m";
            ctx.drawText(client.textRenderer, Text.literal(text), x, y + i * 13, 0xFFFFFFFF, false);
        }
    }

    private static Point project(MinecraftClient client, Vec3d world) {
        Camera camera = client.gameRenderer.getCamera();
        if (camera == null) return null;
        Vec3d cam = camera.getPos();
        double dx = world.x - cam.x;
        double dy = world.y - cam.y;
        double dz = world.z - cam.z;

        double yaw = Math.toRadians(camera.getYaw());
        double pitch = Math.toRadians(camera.getPitch());
        double cosY = Math.cos(yaw), sinY = Math.sin(yaw);
        double x1 = dx * cosY - dz * sinY;
        double z1 = dx * sinY + dz * cosY;
        double cosP = Math.cos(pitch), sinP = Math.sin(pitch);
        double y2 = dy * cosP + z1 * sinP;
        double z2 = -dy * sinP + z1 * cosP;
        if (z2 <= 0.08) return null;

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        double fov = Math.max(30.0, Math.min(150.0, client.options.getFov().getValue()));
        double focal = (sh * 0.5) / Math.tan(Math.toRadians(fov) * 0.5);
        int sx = (int) Math.round(sw * 0.5 + x1 * focal / z2);
        int sy = (int) Math.round(sh * 0.5 - y2 * focal / z2);
        if (sx < -80 || sx > sw + 80 || sy < -80 || sy > sh + 80) return null;
        return new Point(sx, sy, z2);
    }

    private static int storageColor(BlockState state) {
        if (state.isOf(Blocks.SPAWNER)) return 0xFFFF647A;
        if (state.getBlock() instanceof ShulkerBoxBlock) return 0xFFD68CFF;
        if (state.isOf(Blocks.ENDER_CHEST)) return 0xFF63E6E2;
        if (state.isOf(Blocks.BARREL)) return 0xFFFFBC6E;
        if (state.isOf(Blocks.HOPPER)) return 0xFFAEC1D6;
        return 0xFFFFD37A;
    }

    private static void drawRectOutline(DrawContext ctx, int left, int top, int right, int bottom, int color) {
        ctx.fill(left, top, right + 1, top + 1, color);
        ctx.fill(left, bottom, right + 1, bottom + 1, color);
        ctx.fill(left, top, left + 1, bottom + 1, color);
        ctx.fill(right, top, right + 1, bottom + 1, color);
    }
}
