package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Draggable/resizable Star HUD widgets that need richer rendering than a one-line label. */
public final class StarExtraHud {
    private static String dragging;
    private static double dragOffsetX, dragOffsetY;
    private static boolean lastLeft;
    private static final List<Hit> hits = new ArrayList<>();

    private record Hit(String id, ClientModule module, StarHudLayoutStore.State state,
                       int x, int y, int w, int h) {}

    private StarExtraHud() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || !CincoClient.hudEnabled) return;
        boolean edit = client.currentScreen instanceof ChatScreen;
        hits.clear();
        spotify(ctx, client, edit);
        regionMap(ctx, client, edit);
        if (edit) handleEditor(client);
        else if (dragging != null) {
            dragging = null;
            StarHudLayoutStore.save();
        }
    }

    private static void spotify(DrawContext ctx, MinecraftClient client, boolean edit) {
        ClientModule module = CincoClient.findModule("Spotify HUD");
        if (module == null || !module.enabled) return;

        int sw = client.getWindow().getScaledWidth();
        StarHudLayoutStore.State state = StarHudLayoutStore.get("spotify_hud", Math.max(8, sw - 250), 12);
        state.scale = clampScale(state.scale);
        String now = SpotifyHudController.track();
        boolean compact = module.setting2 >= 0.5;
        int baseW = compact ? 168 : 228;
        int baseH = compact ? 28 : 42;
        int realW = Math.round(baseW * state.scale), realH = Math.round(baseH * state.scale);
        clampPosition(client, state, realW, realH);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(state.x, state.y, 0);
        ctx.getMatrices().scale(state.scale, state.scale, 1f);
        CincoDraw.bubble(ctx, 0, 0, baseW, baseH, 10, edit ? CincoTheme.accentLight() : 0xB9442864, 0xE80A090E);
        CincoDraw.rounded(ctx, 7, 7, compact ? 22 : 28, compact ? 22 : 28, 8, 0xFF6E32A8);
        ctx.drawCenteredTextWithShadow(client.textRenderer, StarFonts.text("♫", true), compact ? 14 : 17, compact ? 10 : 12, 0xFFFFFFFF);
        if (!compact) {
            ctx.drawText(client.textRenderer, StarFonts.text("SPOTIFY", true), 37, 8, 0xFFDCC8FF, false);
            ctx.enableScissor(state.x + Math.round(37 * state.scale), state.y,
                    state.x + Math.round((baseW - 9) * state.scale), state.y + realH);
            ctx.drawText(client.textRenderer, StarFonts.text(trim(now, 31), false), 37, 23,
                    SpotifyHudController.active() ? 0xFFF5F2FA : 0xFF918A99, false);
            ctx.disableScissor();
        } else {
            ctx.drawText(client.textRenderer, StarFonts.text(trim(now, 19), false), 30, 10,
                    SpotifyHudController.active() ? 0xFFF5F2FA : 0xFF918A99, false);
        }
        if (edit) closeButton(ctx, client, baseW);
        ctx.getMatrices().pop();
        hits.add(new Hit("spotify_hud", module, state, state.x, state.y, realW, realH));
    }

    private static void regionMap(DrawContext ctx, MinecraftClient client, boolean edit) {
        ClientModule module = CincoClient.findModule("Region Map");
        if (module == null || !module.enabled) return;

        StarHudLayoutStore.State state = StarHudLayoutStore.get("region_map", 12, 220);
        state.scale = clampScale(state.scale);
        int baseW = 126, baseH = 145;
        int realW = Math.round(baseW * state.scale), realH = Math.round(baseH * state.scale);
        clampPosition(client, state, realW, realH);

        int radius = Math.max(2, Math.min(8, (int)Math.round(module.setting)));
        int diameter = radius * 2 + 1;
        int mapX = 8, mapY = 28, mapSize = 110;
        int cell = Math.max(3, mapSize / diameter);
        int used = cell * diameter;
        int startX = mapX + (mapSize - used) / 2;
        int startY = mapY + (mapSize - used) / 2;
        int pcx = client.player.getBlockX() >> 4;
        int pcz = client.player.getBlockZ() >> 4;
        int rx = Math.floorDiv(pcx, 32), rz = Math.floorDiv(pcz, 32);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(state.x, state.y, 0);
        ctx.getMatrices().scale(state.scale, state.scale, 1f);
        CincoDraw.bubble(ctx, 0, 0, baseW, baseH, 11, edit ? CincoTheme.accentLight() : 0xB9442864, 0xEB08080C);
        ctx.drawText(client.textRenderer, StarFonts.text("★ REGION  " + rx + ", " + rz, true), 9, 9, 0xFFDCC8FF, false);
        CincoDraw.rounded(ctx, mapX - 2, mapY - 2, mapX + mapSize + 2, mapY + mapSize + 2, 8, 0xFF17121D);

        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int cx = pcx + dx, cz = pcz + dz;
                int x = startX + (dx + radius) * cell;
                int y = startY + (dz + radius) * cell;
                boolean loaded = client.world.isChunkLoaded(cx, cz);
                int fill = loaded ? 0xFF17131E : 0xFF0C0B10;
                if (dx == 0 && dz == 0) fill = 0xFF6C35A0;
                ctx.fill(x, y, x + cell - 1, y + cell - 1, fill);
                if (Math.floorMod(cx, 32) == 0) ctx.fill(x, y, x + 1, y + cell, 0xFF9F63D9);
                if (Math.floorMod(cz, 32) == 0) ctx.fill(x, y, x + cell, y + 1, 0xFF9F63D9);
            }
        }

        if (module.setting2 >= 0.5) {
            for (BlockPos pos : EnhancedWorldScan.storage()) {
                int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
                int dx = cx - pcx, dz = cz - pcz;
                if (Math.abs(dx) > radius || Math.abs(dz) > radius) continue;
                int x = startX + (dx + radius) * cell + cell / 2;
                int y = startY + (dz + radius) * cell + cell / 2;
                ctx.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFF72D2);
            }
        }

        if (module.setting3 >= 0.5) {
            for (PlayerEntity p : client.world.getPlayers()) {
                if (p == client.player) continue;
                int cx = p.getBlockX() >> 4, cz = p.getBlockZ() >> 4;
                int dx = cx - pcx, dz = cz - pcz;
                if (Math.abs(dx) > radius || Math.abs(dz) > radius) continue;
                int x = startX + (dx + radius) * cell + cell / 2;
                int y = startY + (dz + radius) * cell + cell / 2;
                ctx.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFFFFFF);
            }
        }

        int centerX = startX + radius * cell + cell / 2;
        int centerY = startY + radius * cell + cell / 2;
        ctx.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, 0xFFC98CFF);
        ctx.drawText(client.textRenderer, StarFonts.text(String.format(Locale.ROOT, "CHUNK %d, %d", pcx, pcz), false),
                9, baseH - 12, 0xFF8E8797, false);
        if (edit) closeButton(ctx, client, baseW);
        ctx.getMatrices().pop();
        hits.add(new Hit("region_map", module, state, state.x, state.y, realW, realH));
    }

    private static void closeButton(DrawContext ctx, MinecraftClient client, int baseW) {
        CincoDraw.rounded(ctx, baseW - 15, 3, baseW - 3, 15, 6, 0xE0A5314D);
        ctx.drawCenteredTextWithShadow(client.textRenderer, net.minecraft.text.Text.literal("×"), baseW - 9, 5, 0xFFFFFFFF);
    }

    private static void handleEditor(MinecraftClient client) {
        int sw = client.getWindow().getScaledWidth(), sh = client.getWindow().getScaledHeight();
        double mx = client.mouse.getX() * sw / Math.max(1.0, client.getWindow().getWidth());
        double my = client.mouse.getY() * sh / Math.max(1.0, client.getWindow().getHeight());
        boolean left = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (left && !lastLeft) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit hit = hits.get(i);
                if (!inside(mx, my, hit)) continue;
                int closeSize = Math.max(10, Math.round(15 * hit.state.scale));
                if (mx >= hit.x + hit.w - closeSize && my <= hit.y + closeSize) {
                    hit.module.enabled = false;
                    ConfigStore.save();
                    lastLeft = left;
                    return;
                }
                dragging = hit.id;
                dragOffsetX = mx - hit.x;
                dragOffsetY = my - hit.y;
                break;
            }
        }
        if (left && dragging != null) {
            for (Hit hit : hits) if (hit.id.equals(dragging)) {
                hit.state.x = (int)Math.round(mx - dragOffsetX);
                hit.state.y = (int)Math.round(my - dragOffsetY);
                break;
            }
        }
        if (!left && lastLeft && dragging != null) {
            dragging = null;
            StarHudLayoutStore.save();
        }
        lastLeft = left;
    }

    public static boolean onScroll(MinecraftClient client, double mouseX, double mouseY, double vertical) {
        if (!(client.currentScreen instanceof ChatScreen)) return false;
        int sw = client.getWindow().getScaledWidth(), sh = client.getWindow().getScaledHeight();
        double mx = mouseX * sw / Math.max(1.0, client.getWindow().getWidth());
        double my = mouseY * sh / Math.max(1.0, client.getWindow().getHeight());
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (!inside(mx, my, hit)) continue;
            hit.state.scale = clampScale(hit.state.scale + (vertical > 0 ? 0.1f : -0.1f));
            StarHudLayoutStore.save();
            return true;
        }
        return false;
    }

    private static boolean inside(double mx, double my, Hit h) {
        return mx >= h.x && mx <= h.x + h.w && my >= h.y && my <= h.y + h.h;
    }

    private static float clampScale(float v) { return Math.max(0.5f, Math.min(2.5f, v)); }

    private static void clampPosition(MinecraftClient client, StarHudLayoutStore.State s, int w, int h) {
        s.x = Math.max(0, Math.min(Math.max(0, client.getWindow().getScaledWidth() - w), s.x));
        s.y = Math.max(0, Math.min(Math.max(0, client.getWindow().getScaledHeight() - h), s.y));
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "…";
    }
}
