package gg.cinco.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** Bright screen-space ESP drawn after the world so walls never hide the marker or tag. */
public final class EnhancedEspHud {
    private record Point(int x, int y, double depth) {}

    private EnhancedEspHud() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.currentScreen != null) return;
        drawPlayerEsp(ctx, client);
        drawStorageEsp(ctx, client);
        drawLightFinder(ctx, client);
        drawSusChunks(ctx, client);
        drawFreeCamStatus(ctx, client);
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
            int color = 0xFFFF74FF;

            if (espOn && esp.setting3 >= 0.5) glowRect(ctx, left, top, right, bottom, color);
            if (tracerOn) CincoDraw.line(ctx, sw / 2, sh - 2, feet.x, feet.y, 0xCCFF74FF, 1);
            if (espOn && esp.setting2 >= 0.5) {
                double distance = client.player.distanceTo(player);
                String label = player.getName().getString() + "  •  " + String.format(Locale.ROOT, "%.1fm", distance);
                smallTag(ctx, client, label, head.x, top - 11, color, 0.78f);
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
            if (shown >= 110) break;
            double distanceSq = client.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distanceSq > rangeSq) continue;
            BlockState state = client.world.getBlockState(pos);
            if (!EnhancedWorldScan.storageMatchesFilter(state, filter)) continue;
            Point p = project(client, Vec3d.ofCenter(pos));
            if (p == null) continue;

            int color = storageColor(state);
            int size = Math.max(4, Math.min(10, (int) (92.0 / Math.max(8.0, Math.sqrt(distanceSq)))));
            glowRect(ctx, p.x - size, p.y - size, p.x + size, p.y + size, color);
            ctx.fill(p.x - 2, p.y - 2, p.x + 3, p.y + 3, 0x55000000 | (color & 0x00FFFFFF));
            ctx.fill(p.x - 1, p.y - 1, p.x + 2, p.y + 2, color);

            if (shown < 46) {
                String label = EnhancedWorldScan.storageLabel(state) + "  •  " + Math.round(Math.sqrt(distanceSq)) + "m";
                if (esp.setting3 >= 0.5) label += "  •  " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
                smallTag(ctx, client, label, p.x, p.y - size - 9, color, 0.72f);
            }
            shown++;
        }
    }

    private static void drawLightFinder(DrawContext ctx, MinecraftClient client) {
        ClientModule module = CincoClient.findModule("Light Finder");
        if (module == null || !module.enabled) return;
        int shown = 0;
        for (BlockPos pos : EnhancedWorldScan.lightHotspots()) {
            if (shown++ >= 80) break;
            Point p = project(client, Vec3d.ofCenter(pos));
            if (p == null) continue;
            int color = 0xFFFFE05F;
            CincoDraw.line(ctx, p.x - 6, p.y, p.x, p.y - 6, color, 2);
            CincoDraw.line(ctx, p.x, p.y - 6, p.x + 6, p.y, color, 2);
            CincoDraw.line(ctx, p.x + 6, p.y, p.x, p.y + 6, color, 2);
            CincoDraw.line(ctx, p.x, p.y + 6, p.x - 6, p.y, color, 2);
            if (shown <= 28) {
                int level = client.world.getLightLevel(net.minecraft.world.LightType.BLOCK, pos);
                String text = "LIGHT " + level + "  •  " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
                smallTag(ctx, client, text, p.x, p.y - 11, color, 0.70f);
            }
        }
    }

    private static void drawSusChunks(DrawContext ctx, MinecraftClient client) {
        ClientModule module = CincoClient.findModule("Sus Chunk Finder");
        if (module == null || !module.enabled) return;
        int x = client.getWindow().getScaledWidth() - 184;
        int y = 58;
        int rows = Math.min(7, EnhancedWorldScan.susChunks().size());
        if (rows == 0) return;
        ctx.fill(x - 7, y - 16, x + 176, y + rows * 13 + 6, 0xD40B0812);
        ctx.fill(x - 7, y - 16, x - 3, y + rows * 13 + 6, CincoTheme.accent());
        ctx.drawText(client.textRenderer, Text.literal("SUS CHUNKS  •  LOADED"), x, y - 12, CincoTheme.accentLight(), true);
        for (int i = 0; i < rows; i++) {
            EnhancedWorldScan.SusChunk hit = EnhancedWorldScan.susChunks().get(i);
            int bx = (hit.chunkX() << 4) + 8;
            int bz = (hit.chunkZ() << 4) + 8;
            int distance = (int) Math.round(Math.hypot(bx - client.player.getX(), bz - client.player.getZ()));
            String text = hit.chunkX() + ", " + hit.chunkZ() + "  score " + hit.score() + "  " + distance + "m";
            drawSmallLeft(ctx, client, text, x, y + i * 13, 0xFFF7F0FF, 0.82f);
        }
    }

    private static void drawFreeCamStatus(DrawContext ctx, MinecraftClient client) {
        if (!FreeCamController.isActive()) return;
        String text = "FREE CAM  •  WASD / SPACE / SHIFT  •  SPRINT = BOOST";
        smallTag(ctx, client, text, client.getWindow().getScaledWidth() / 2, 58, 0xFF7FF7FF, 0.78f);
    }

    private static void smallTag(DrawContext ctx, MinecraftClient client, String text,
                                 int centerX, int y, int color, float scale) {
        MatrixStack matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(centerX, y, 0);
        matrices.scale(scale, scale, 1.0f);
        int tw = client.textRenderer.getWidth(text);
        ctx.fill(-tw / 2 - 4, -2, tw / 2 + 4, 11, 0xD8080710);
        ctx.fill(-tw / 2 - 4, -2, -tw / 2 - 2, 11, color);
        ctx.drawText(client.textRenderer, Text.literal(text), -tw / 2, 0, color, true);
        matrices.pop();
    }

    private static void drawSmallLeft(DrawContext ctx, MinecraftClient client, String text,
                                      int x, int y, int color, float scale) {
        MatrixStack matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0);
        matrices.scale(scale, scale, 1.0f);
        ctx.drawText(client.textRenderer, Text.literal(text), 0, 0, color, true);
        matrices.pop();
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
        if (sx < -90 || sx > sw + 90 || sy < -90 || sy > sh + 90) return null;
        return new Point(sx, sy, z2);
    }

    private static int storageColor(BlockState state) {
        if (state.isOf(Blocks.SPAWNER)) return 0xFFFF5570;
        if (state.getBlock() instanceof ShulkerBoxBlock) return 0xFFE28CFF;
        if (state.isOf(Blocks.ENDER_CHEST)) return 0xFF5DFFF5;
        if (state.isOf(Blocks.BARREL)) return 0xFFFFC05C;
        if (state.isOf(Blocks.HOPPER)) return 0xFFBBD9FF;
        if (state.isOf(Blocks.DISPENSER) || state.isOf(Blocks.DROPPER)) return 0xFFFF8FD8;
        if (state.isOf(Blocks.FURNACE) || state.isOf(Blocks.BLAST_FURNACE) || state.isOf(Blocks.SMOKER)) return 0xFFFF9D57;
        return 0xFFFFE26F;
    }

    private static void glowRect(DrawContext ctx, int left, int top, int right, int bottom, int color) {
        int glow = 0x55000000 | (color & 0x00FFFFFF);
        drawRectOutline(ctx, left - 1, top - 1, right + 1, bottom + 1, glow);
        drawRectOutline(ctx, left, top, right, bottom, color);
    }

    private static void drawRectOutline(DrawContext ctx, int left, int top, int right, int bottom, int color) {
        ctx.fill(left, top, right + 1, top + 1, color);
        ctx.fill(left, bottom, right + 1, bottom + 1, color);
        ctx.fill(left, top, left + 1, bottom + 1, color);
        ctx.fill(right, top, right + 1, bottom + 1, color);
    }
}
