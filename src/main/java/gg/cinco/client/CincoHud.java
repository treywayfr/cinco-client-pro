package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;

public final class CincoHud {
    private static long startedAt = System.currentTimeMillis();
    private record Stat(String label, String value, int valueColor) {}
    private record HudLine(String text, int color) {}
    private CincoHud() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        ClientModule master = module("Client HUD");
        if (master == null || !master.enabled || client.player == null || client.world == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        List<Stat> stats = buildStats(client);
        int brandWidth = 124;
        int availableWidth = Math.max(120, screenWidth - 16);
        while (stats.size() > 1 && getBarWidth(client, stats, brandWidth) > availableWidth) {
            stats.remove(stats.size() - 1);
        }
        int barWidth = Math.min(availableWidth, getBarWidth(client, stats, brandWidth));
        int x = Math.max(8, (screenWidth - barWidth) / 2);
        int y = 8;
        int h = 42;

        // Solid, high-contrast panels keep the HUD crisp at different Minecraft GUI scales.
        ctx.fill(x - 1, y - 1, x + barWidth + 1, y + h + 1, CincoTheme.accent());
        ctx.fill(x, y, x + barWidth, y + h, CincoTheme.panel());
        ctx.fill(x, y, x + brandWidth, y + h, CincoTheme.sidebar());
        ctx.fill(x, y, x + barWidth, y + 2, CincoTheme.accent());
        ctx.fill(x, y + h - 2, x + barWidth, y + h, CincoTheme.line());

        ctx.fill(x + 9, y + 8, x + 12, y + 33, CincoTheme.accent());
        ctx.drawText(client.textRenderer, hudText("CINCO CLIENT"), x + 19, y + 8, textColor(0xFFFFC078), false);
        ctx.drawText(client.textRenderer, hudText("PURPLE NEBULA  /  v2.3"), x + 19, y + 23, textColor(0xFFFFB86B), false);

        int cursor = x + brandWidth;
        for (Stat stat : stats) {
            int cellWidth = cellWidth(client, stat);
            ctx.fill(cursor, y + 8, cursor + 1, y + h - 8, CincoTheme.line());
            int tx = cursor + 10;
            ctx.drawText(client.textRenderer, hudText(stat.label), tx, y + 7, textColor(0xFFE88736), false);
            ctx.drawText(client.textRenderer, hudText(stat.value), tx, y + 22, textColor(stat.valueColor), false);
            cursor += cellWidth;
        }

        List<HudLine> details = buildDetails(client);
        if (!details.isEmpty()) {
            int left = 10, top = y + h + 9, lineHeight = 12;
            int maxTextWidth = 0;
            for (HudLine line : details) maxTextWidth = Math.max(maxTextWidth, client.textRenderer.getWidth(line.text));
            int panelWidth = Math.min(Math.max(210, maxTextWidth + 20), Math.max(210, screenWidth - 20));
            int panelHeight = 9 + details.size() * lineHeight;
            ctx.fill(left - 1, top - 1, left + panelWidth + 1, top + panelHeight + 1, CincoTheme.accentDark());
            ctx.fill(left, top, left + panelWidth, top + panelHeight, CincoTheme.panel());
            ctx.fill(left, top, left + 3, top + panelHeight, CincoTheme.accent());
            int textY = top + 6;
            for (HudLine line : details) {
                ctx.drawText(client.textRenderer, hudText(line.text), left + 10, textY, textColor(line.color), false);
                textY += lineHeight;
            }
        }

        if (on("Keystrokes")) drawKeystrokes(ctx, client);
        if (on("Crosshair Guide")) drawCrosshair(ctx, client, (int) module("Crosshair Guide").setting);
    }

    private static List<Stat> buildStats(MinecraftClient client) {
        List<Stat> stats = new ArrayList<>();
        if (on("FPS Counter")) stats.add(new Stat("FPS", Integer.toString(client.getCurrentFps()), 0xFFFFFFFF));
        if (on("Coordinates")) {
            ClientModule coords = module("Coordinates");
            String format = coords.setting <= 0 ? "%.0f / %.0f / %.0f"
                    : coords.setting >= 2 ? "%.2f / %.2f / %.2f" : "%.1f / %.1f / %.1f";
            String value = String.format(Locale.ROOT, format, client.player.getX(), client.player.getY(), client.player.getZ());
            stats.add(new Stat("XYZ", value, 0xFFFFC078));
        }
        if (on("Direction")) stats.add(new Stat("FACING", client.player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT), 0xFFFFFFFF));
        if (on("Armor HUD")) {
            int total = 0, count = 0;
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack stack = client.player.getEquippedStack(slot);
                if (!stack.isEmpty() && stack.isDamageable()) {
                    total += (int) (100.0 * (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage());
                    count++;
                }
            }
            stats.add(new Stat("ARMOR", count == 0 ? "NONE" : (total / count) + "%", count > 0 && total / count < 25 ? 0xFFFFB16D : 0xFFFFC078));
        }
        if (on("CPS Display")) stats.add(new Stat("CPS", Integer.toString(ClickRateTracker.getCps()), 0xFFFFFFFF));
        if (on("Potion Status")) stats.add(new Stat("EFFECTS", Integer.toString(client.player.getStatusEffects().size()), 0xFFDCCBFF));
        if (on("Session Timer")) {
            long seconds = Math.max(0, (System.currentTimeMillis() - startedAt) / 1000);
            stats.add(new Stat("SESSION", String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60), 0xFFFFFFFF));
        }
        if (on("Cooldown Coach")) {
            int cooldown = Math.round(client.player.getAttackCooldownProgress(0.0F) * 100);
            stats.add(new Stat("ATTACK", cooldown + "%", cooldown >= 95 ? 0xFFC7FFD8 : 0xFFDCCBFF));
        }
        return stats;
    }

    private static int getBarWidth(MinecraftClient client, List<Stat> stats, int brandWidth) {
        int width = brandWidth;
        for (Stat stat : stats) width += cellWidth(client, stat);
        return width;
    }

    private static int cellWidth(MinecraftClient client, Stat stat) {
        return Math.max(client.textRenderer.getWidth(stat.label), client.textRenderer.getWidth(stat.value)) + 20;
    }

    private static List<HudLine> buildDetails(MinecraftClient client) {
        List<HudLine> lines = new ArrayList<>();
        if (on("Totem Reminder") && !client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            lines.add(new HudLine("! OFFHAND TOTEM MISSING", 0xFFFFB16D));
        }
        if (on("Gapple Reminder") && countItem(client, Items.GOLDEN_APPLE) == 0) {
            lines.add(new HudLine("! NO GOLDEN APPLES IN INVENTORY", 0xFFFFB16D));
        }
        if (on("Elytra Reminder") && !client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            lines.add(new HudLine("! ELYTRA NOT EQUIPPED", 0xFFFFB16D));
        }
        if (on("Inventory Checklist")) {
            lines.add(new HudLine("CRYSTALS " + countItem(client, Items.END_CRYSTAL)
                    + "   GLOWSTONE " + countItem(client, Items.GLOWSTONE)
                    + "   TOTEMS " + countItem(client, Items.TOTEM_OF_UNDYING), 0xFFDCCBFF));
        }
        if (on("Block Notes") && client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            String block = client.world.getBlockState(pos).getBlock().getName().getString();
            lines.add(new HudLine("TARGET  " + block + " @ " + pos.getX() + " " + pos.getY() + " " + pos.getZ(), 0xFFDCCBFF));
        }
        if (on("Nearby Player List")) {
            double range = module("Nearby Player List").setting;
            List<PlayerEntity> players = new ArrayList<>();
            for (PlayerEntity player : client.world.getPlayers()) {
                if (player != client.player && player.isAlive() && client.player.distanceTo(player) <= range) players.add(player);
            }
            players.sort(Comparator.comparingDouble(client.player::distanceTo));
            lines.add(new HudLine("NEARBY PLAYERS  " + players.size(), 0xFFDCCBFF));
            for (int i = 0; i < Math.min(3, players.size()); i++) {
                PlayerEntity player = players.get(i);
                lines.add(new HudLine("  " + player.getName().getString() + "  "
                        + Math.round(client.player.distanceTo(player)) + "m", 0xFFE89745));
            }
        }
        if ((on("Base Finder") || on("Container Finder")) && on("Container Radar HUD")) {
            List<BlockPos> hits = BaseScanner.getFindings();
            lines.add(new HudLine("NEARBY BLOCK RADAR  " + hits.size() + " HITS  /  " + BaseScanner.getProgressPercent() + "% SCANNED", 0xFFDCCBFF));
            int maxRows = (int) module("Container Radar HUD").setting;
            for (int i = 0; i < Math.min(maxRows, hits.size()); i++) {
                BlockPos pos = hits.get(i);
                lines.add(new HudLine("  " + pos.getX() + "  " + pos.getY() + "  " + pos.getZ(), 0xFFCEB8ED));
            }
        }
        if ((on("Sus Chunk Finder") || on("Stash Radar") || on("Chunk Density")) && on("Container Radar HUD")) {
            Map<Long, Integer> chunkCounts = new HashMap<>();
            for (BlockPos pos : BaseScanner.getFindings()) {
                long key = ((long) (pos.getX() >> 4) << 32) | ((pos.getZ() >> 4) & 0xFFFFFFFFL);
                chunkCounts.merge(key, 1, Integer::sum);
            }
            int minimum = on("Stash Radar") ? (int) module("Stash Radar").setting
                    : (int) module(on("Sus Chunk Finder") ? "Sus Chunk Finder" : "Chunk Density").setting3;
            List<Map.Entry<Long, Integer>> ranked = new ArrayList<>();
            for (Map.Entry<Long, Integer> entry : chunkCounts.entrySet()) {
                if (entry.getValue() >= minimum) ranked.add(entry);
            }
            ranked.sort(Map.Entry.<Long, Integer>comparingByValue().reversed());
            lines.add(new HudLine("SUS CHUNK RADAR  " + ranked.size() + " CANDIDATES", 0xFFFFC078));
            int rows = Math.min((int) module("Container Radar HUD").setting, ranked.size());
            for (int i = 0; i < rows; i++) {
                Map.Entry<Long, Integer> entry = ranked.get(i);
                int chunkX = (int) (entry.getKey() >> 32);
                int chunkZ = (int) (long) entry.getKey();
                lines.add(new HudLine("  CHUNK " + chunkX + ", " + chunkZ + "  •  " + entry.getValue() + " blocks", 0xFFE89745));
            }
        }

        if (on("Reach Trainer") && client.crosshairTarget instanceof EntityHitResult hit) {
            double distance = client.player.distanceTo(hit.getEntity());
            double desired = module("Reach Trainer").setting;
            lines.add(new HudLine(String.format(Locale.ROOT, "TARGET DISTANCE  %.2fm  /  %.1fm", distance, desired),
                    distance <= desired ? 0xFFC7FFD8 : 0xFFFFB16D));
        }
        if (on("Safe Logout Reminder")) lines.add(new HudLine("REMINDER  Save your location before disconnecting", 0xFFDCCBFF));
        return lines;
    }

    private static int countItem(MinecraftClient client, Item item) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isOf(item)) count += stack.getCount();
        }
        if (client.player.getOffHandStack().isOf(item)) count += client.player.getOffHandStack().getCount();
        return count;
    }

    private static void drawKeystrokes(DrawContext ctx, MinecraftClient client) {
        int x = 10, y = client.getWindow().getScaledHeight() - 72;
        ctx.fill(x - 4, y - 4, x + 78, y + 55, CincoTheme.panel());
        ctx.fill(x - 4, y - 4, x + 78, y - 2, CincoTheme.accent());
        key(ctx, client.options.forwardKey.isPressed(), x + 27, y, "W");
        key(ctx, client.options.leftKey.isPressed(), x, y + 18, "A");
        key(ctx, client.options.backKey.isPressed(), x + 27, y + 18, "S");
        key(ctx, client.options.rightKey.isPressed(), x + 54, y + 18, "D");
        ctx.fill(x, y + 37, x + 76, y + 48, client.options.jumpKey.isPressed() ? CincoTheme.enabled() : CincoTheme.disabled());
        Text jumpText = hudText("JUMP");
        ctx.drawText(client.textRenderer, jumpText, x + 38 - client.textRenderer.getWidth(jumpText) / 2, y + 39, textColor(0xFFFFFFFF), false);
    }

    private static void key(DrawContext ctx, boolean pressed, int x, int y, String label) {
        ctx.fill(x, y, x + 22, y + 16, pressed ? CincoTheme.enabled() : CincoTheme.disabled());
        Text keyText = hudText(label);
        var renderer = MinecraftClient.getInstance().textRenderer;
        ctx.drawText(renderer, keyText, x + 11 - renderer.getWidth(keyText) / 2, y + 4, textColor(0xFFFFFFFF), false);
    }

    private static void drawCrosshair(DrawContext ctx, MinecraftClient client, int size) {
        int cx = client.getWindow().getScaledWidth() / 2, cy = client.getWindow().getScaledHeight() / 2;
        ctx.fill(cx - size, cy, cx - 2, cy + 1, CincoTheme.accent());
        ctx.fill(cx + 2, cy, cx + size, cy + 1, CincoTheme.accent());
        ctx.fill(cx, cy - size, cx + 1, cy - 2, CincoTheme.accent());
        ctx.fill(cx, cy + 2, cx + 1, cy + size, CincoTheme.accent());
    }

    private static Text hudText(String value) {
        ClientModule typography = module("Bold Typeface");
        if (typography != null && typography.enabled) {
            return Text.literal(value).setStyle(Style.EMPTY.withBold(true).withFont(Identifier.of("minecraft", "uniform")));
        }
        return Text.literal(value).setStyle(Style.EMPTY.withFont(Identifier.of("minecraft", "default")));
    }

    private static int textColor(int original) {
        return CincoTheme.textColor(original);
    }

    private static boolean on(String name) {
        ClientModule found = module(name);
        return found != null && found.enabled;
    }

    private static ClientModule module(String name) {
        return CincoClient.findModule(name);
    }
}
