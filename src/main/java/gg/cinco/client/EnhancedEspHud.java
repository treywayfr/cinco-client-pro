package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** Minimal HUD extras. The actual ESP geometry is rendered in world space. */
public final class EnhancedEspHud {
    private record Point(int x, int y, double depth) {}

    private EnhancedEspHud() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.currentScreen != null) return;
        drawPlayerNames(ctx, client);
        drawSusChunks(ctx, client);
        drawFreeCamStatus(ctx, client);
    }

    /** Optional small player label; boxes, glow and tracers are handled by world renderers. */
    private static void drawPlayerNames(DrawContext ctx, MinecraftClient client) {
        ClientModule esp = CincoClient.findModule("Player ESP");
        if (esp == null || !esp.enabled || esp.setting2 < 0.5) return;
        double r2 = esp.setting * esp.setting;

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || !player.isAlive() || client.player.squaredDistanceTo(player) > r2) continue;
            Point head = project(client, new Vec3d(player.getX(), player.getY() + player.getHeight() + 0.14, player.getZ()));
            if (head == null) continue;
            String label = player.getName().getString() + "  "
                    + String.format(Locale.ROOT, "%.1fm", client.player.distanceTo(player));
            smallTag(ctx, client, label, head.x, head.y - 7, CincoTheme.accentLight(), 0.58f);
        }
    }

    private static void drawSusChunks(DrawContext ctx, MinecraftClient client) {
        ClientModule module = CincoClient.findModule("Sus Chunk Finder");
        if (module == null || !module.enabled) return;
        int rows = Math.min(7, EnhancedWorldScan.susChunks().size());
        if (rows == 0) return;

        int x = client.getWindow().getScaledWidth() - 181;
        int y = 58;
        CincoDraw.bubble(ctx, x - 7, y - 16, x + 171, y + rows * 13 + 7, 10,
                CincoTheme.accentMuted(), 0xD40B0812);
        ctx.drawText(client.textRenderer, Text.literal("SUS CHUNKS  •  LOADED"), x, y - 11,
                CincoTheme.accentLight(), true);
        for (int i = 0; i < rows; i++) {
            EnhancedWorldScan.SusChunk hit = EnhancedWorldScan.susChunks().get(i);
            int bx = (hit.chunkX() << 4) + 8;
            int bz = (hit.chunkZ() << 4) + 8;
            int distance = (int) Math.round(Math.hypot(bx - client.player.getX(), bz - client.player.getZ()));
            drawSmallLeft(ctx, client,
                    hit.chunkX() + ", " + hit.chunkZ() + "  score " + hit.score() + "  " + distance + "m",
                    x, y + i * 13, 0xFFF7F0FF, 0.76f);
        }
    }

    private static void drawFreeCamStatus(DrawContext ctx, MinecraftClient client) {
        if (!FreeCamController.isActive()) return;
        smallTag(ctx, client, "FREE CAM  •  SPRINT = BOOST",
                client.getWindow().getScaledWidth() / 2, 58, 0xFF77F8FF, 0.64f);
    }

    private static void smallTag(DrawContext ctx, MinecraftClient client, String text,
                                 int centerX, int y, int color, float scale) {
        MatrixStack matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(centerX, y, 0);
        matrices.scale(scale, scale, 1.0f);
        int tw = client.textRenderer.getWidth(text);
        CincoDraw.bubble(ctx, -tw / 2 - 5, -3, tw / 2 + 5, 12, 7,
                (0xFF000000 | (color & 0x00FFFFFF)), 0xD80A0810);
        ctx.drawText(client.textRenderer, Text.literal(text), -tw / 2, 1, color, true);
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
}
