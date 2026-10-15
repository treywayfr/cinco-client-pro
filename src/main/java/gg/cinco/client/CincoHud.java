package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Independent Star Screen GUI widgets. Open chat to drag, resize, or disable each widget. */
public final class CincoHud {
    private static final long STARTED = System.currentTimeMillis();
    private static String dragging;
    private static double dragOffsetX, dragOffsetY;
    private static boolean lastLeft;
    private static final List<Hit> hits = new ArrayList<>();

    private record Hit(String id, ClientModule module, StarHudLayoutStore.State state,
                       int x, int y, int w, int h) {}

    private CincoHud() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || !CincoClient.hudEnabled) return;
        boolean edit = client.currentScreen instanceof ChatScreen;
        hits.clear();

        int index = 0;
        index = widget(ctx, client, edit, index, "brand", "Client HUD", "★ STAR CLIENT");
        index = widget(ctx, client, edit, index, "fps", "FPS Counter", "FPS  " + client.getCurrentFps());
        index = widget(ctx, client, edit, index, "coords", "Coordinates", String.format(Locale.ROOT,
                "XYZ  %.1f  %.1f  %.1f", client.player.getX(), client.player.getY(), client.player.getZ()));
        index = widget(ctx, client, edit, index, "direction", "Direction",
                "FACING  " + client.player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT));
        index = widget(ctx, client, edit, index, "armor", "Armor HUD", "ARMOR  " + armorCount(client.player) + "/4");
        index = widget(ctx, client, edit, index, "potions", "Potion Status", "EFFECTS  " + client.player.getStatusEffects().size());
        index = widget(ctx, client, edit, index, "cps", "CPS Display", "CPS  " + ClickRateTracker.getCps());
        index = widget(ctx, client, edit, index, "session", "Session Timer", "SESSION  " + session());
        index = widget(ctx, client, edit, index, "keys", "Keystrokes", keystrokes(client));
        index = widget(ctx, client, edit, index, "ping", "Ping Display", "PING  " + ping(client) + "ms");
        index = widget(ctx, client, edit, index, "target", "Target HUD", target(client));
        index = widget(ctx, client, edit, index, "inventory", "Inventory Checklist", inventory(client));
        index = widget(ctx, client, edit, index, "radar", "Container Radar HUD",
                "STORAGE  " + EnhancedWorldScan.storage().size());
        index = widget(ctx, client, edit, index, "players", "Nearby Player List",
                "PLAYERS  " + Math.max(0, client.world.getPlayers().size() - 1));

        if (edit) handleEditor(client);
        else if (dragging != null) {
            dragging = null;
            StarHudLayoutStore.save();
        }
    }

    private static int widget(DrawContext ctx, MinecraftClient client, boolean edit, int index,
                              String id, String moduleName, String text) {
        ClientModule module = CincoClient.findModule(moduleName);
        if (module == null || !module.enabled) return index;

        int sw = client.getWindow().getScaledWidth();
        int defaultX = 12 + (index % 2) * 155;
        int defaultY = 12 + (index / 2) * 28;
        StarHudLayoutStore.State state = StarHudLayoutStore.get(id, defaultX, defaultY);
        state.scale = Math.max(0.5f, Math.min(2.5f, state.scale));

        int baseW = Math.max(68, client.textRenderer.getWidth(text) + 18);
        int baseH = 22;
        int realW = Math.max(1, Math.round(baseW * state.scale));
        int realH = Math.max(1, Math.round(baseH * state.scale));
        state.x = Math.max(0, Math.min(Math.max(0, sw - realW), state.x));
        state.y = Math.max(0, Math.min(Math.max(0, client.getWindow().getScaledHeight() - realH), state.y));

        ctx.getMatrices().push();
        ctx.getMatrices().translate(state.x, state.y, 0);
        ctx.getMatrices().scale(state.scale, state.scale, 1f);
        int border = edit ? CincoTheme.accent() : 0xA8373042;
        CincoDraw.bubble(ctx, 0, 0, baseW, baseH, 8, border, 0xD80B0910);
        CincoDraw.rounded(ctx, 5, 5, 8, baseH - 5, 2, CincoTheme.accentLight());
        ctx.drawText(client.textRenderer, StarFonts.text(text, true), 12, 7, 0xFFF8F4FF, false);
        if (edit) {
            CincoDraw.rounded(ctx, baseW - 15, 3, baseW - 3, 15, 6, 0xE0A5314D);
            ctx.drawCenteredTextWithShadow(client.textRenderer, Text.literal("×"), baseW - 9, 5, 0xFFFFFFFF);
        }
        ctx.getMatrices().pop();

        hits.add(new Hit(id, module, state, state.x, state.y, realW, realH));
        return index + 1;
    }

    private static void handleEditor(MinecraftClient client) {
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
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
            for (Hit hit : hits) {
                if (!hit.id.equals(dragging)) continue;
                hit.state.x = (int) Math.round(mx - dragOffsetX);
                hit.state.y = (int) Math.round(my - dragOffsetY);
                break;
            }
        }
        if (!left && lastLeft && dragging != null) {
            dragging = null;
            StarHudLayoutStore.save();
        }
        lastLeft = left;
    }

    /** Called by the chat-screen mixin; returns true when a widget consumed the wheel event. */
    public static boolean onScroll(MinecraftClient client, double mouseX, double mouseY, double vertical) {
        if (!(client.currentScreen instanceof ChatScreen)) return false;
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        double mx = mouseX * sw / Math.max(1.0, client.getWindow().getWidth());
        double my = mouseY * sh / Math.max(1.0, client.getWindow().getHeight());
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (!inside(mx, my, hit)) continue;
            hit.state.scale = Math.max(0.5f, Math.min(2.5f, hit.state.scale + (vertical > 0 ? 0.1f : -0.1f)));
            StarHudLayoutStore.save();
            return true;
        }
        return false;
    }

    private static boolean inside(double mx, double my, Hit h) {
        return mx >= h.x && mx <= h.x + h.w && my >= h.y && my <= h.y + h.h;
    }

    private static int armorCount(PlayerEntity player) {
        int count = 0;
        for (ItemStack stack : player.getArmorItems()) if (!stack.isEmpty()) count++;
        return count;
    }

    private static String session() {
        long sec = (System.currentTimeMillis() - STARTED) / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", sec / 60, sec % 60);
    }

    private static String keystrokes(MinecraftClient c) {
        return "KEYS  " + (c.options.forwardKey.isPressed() ? "W" : "·")
                + (c.options.leftKey.isPressed() ? " A" : " ·")
                + (c.options.backKey.isPressed() ? " S" : " ·")
                + (c.options.rightKey.isPressed() ? " D" : " ·");
    }

    private static int ping(MinecraftClient c) {
        if (c.getNetworkHandler() == null || c.player == null) return 0;
        var entry = c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    private static String target(MinecraftClient c) {
        if (c.crosshairTarget instanceof EntityHitResult hit) {
            return "TARGET  " + hit.getEntity().getName().getString();
        }
        return "TARGET  —";
    }

    private static String inventory(MinecraftClient c) {
        int crystals = count(c, Items.END_CRYSTAL);
        int totems = count(c, Items.TOTEM_OF_UNDYING);
        int glow = count(c, Items.GLOWSTONE);
        return "KIT  C:" + crystals + "  T:" + totems + "  G:" + glow;
    }

    private static int count(MinecraftClient c, Item item) {
        int total = 0;
        for (int i = 0; i < c.player.getInventory().size(); i++) {
            ItemStack stack = c.player.getInventory().getStack(i);
            if (stack.isOf(item)) total += stack.getCount();
        }
        return total;
    }
}
