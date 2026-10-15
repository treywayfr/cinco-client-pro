package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class CincoHud {
    private static long startedAt = System.currentTimeMillis();
    private record HudLine(String text, int color) {}
    private CincoHud() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        ClientModule master = CincoClient.findModule("Client HUD");
        if (master == null || !master.enabled || client.player == null || client.world == null) return;
        List<HudLine> lines = new ArrayList<>();
        lines.add(new HudLine("✦ CINCO CLIENT", 0xFFFFFFFF));
        addIfEnabled(lines, "FPS Counter", "FPS  " + client.getCurrentFps(), 0xFFD7B8FF);

        ClientModule coords = module("Coordinates");
        if (on("Coordinates")) {
            String fmt = coords.setting <= 0 ? "XYZ  %.0f / %.0f / %.0f" : coords.setting >= 2 ? "XYZ  %.2f / %.2f / %.2f" : "XYZ  %.1f / %.1f / %.1f";
            lines.add(new HudLine(String.format(Locale.ROOT, fmt, client.player.getX(), client.player.getY(), client.player.getZ()), 0xFFC9B5E9));
        }
        if (on("Direction")) lines.add(new HudLine("FACING  " + client.player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT), 0xFFC9B5E9));

        if (on("Armor HUD")) {
            int total = 0, count = 0;
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack stack = client.player.getEquippedStack(slot);
                if (!stack.isEmpty() && stack.isDamageable()) {
                    total += (int) (100.0 * (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage());
                    count++;
                }
            }
            lines.add(new HudLine("ARMOR  " + (count == 0 ? "none" : (total / count) + "% avg"), 0xFFC9B5E9));
        }
        if (on("Potion Status")) lines.add(new HudLine("EFFECTS  " + client.player.getStatusEffects().size(), 0xFFC9B5E9));
        if (on("CPS Display")) lines.add(new HudLine("CPS  " + ClickRateTracker.getCps(), 0xFFD7B8FF));
        if (on("Session Timer")) {
            long seconds = Math.max(0, (System.currentTimeMillis() - startedAt) / 1000);
            lines.add(new HudLine(String.format(Locale.ROOT, "SESSION  %02d:%02d", seconds / 60, seconds % 60), 0xFFC9B5E9));
        }
        if (on("Totem Reminder") && !client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING))
            lines.add(new HudLine("! OFFHAND TOTEM MISSING", 0xFFFFB36B));
        if (on("Gapple Reminder") && countItem(client, Items.GOLDEN_APPLE) == 0)
            lines.add(new HudLine("! NO GOLDEN APPLES IN INVENTORY", 0xFFFFB36B));
        if (on("Elytra Reminder") && !client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA))
            lines.add(new HudLine("! ELYTRA NOT EQUIPPED", 0xFFFFB36B));
        if (on("Reach Trainer") && client.crosshairTarget instanceof EntityHitResult hitEntity) {
            double reach = client.player.distanceTo(hitEntity.getEntity());
            double maxReach = module("Reach Trainer").setting;
            lines.add(new HudLine(String.format(Locale.ROOT, "TARGET DISTANCE  %.2fm / %.1fm", reach, maxReach),
                    reach <= maxReach ? 0xFF92FFC5 : 0xFFFFB36B));
        }
        if (on("Cooldown Coach")) {
            int progress = Math.round(client.player.getAttackCooldownProgress(0.0F) * 100);
            lines.add(new HudLine("ATTACK COOLDOWN  " + progress + "%", 0xFFD9C6FF));
        }
        if (on("Inventory Checklist")) {
            lines.add(new HudLine("CRYSTALS " + countItem(client, Items.END_CRYSTAL)
                    + "   GLOWSTONE " + countItem(client, Items.GLOWSTONE)
                    + "   TOTEMS " + countItem(client, Items.TOTEM_OF_UNDYING), 0xFFCEADFF));
        }
        if (on("Block Notes") && client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            String block = client.world.getBlockState(pos).getBlock().getName().getString();
            lines.add(new HudLine("TARGET  " + block + " @ " + pos.getX() + " " + pos.getY() + " " + pos.getZ(), 0xFFC9B5E9));
        }
        if (on("Nearby Player List")) {
            ClientModule playerList = module("Nearby Player List");
            List<PlayerEntity> players = new ArrayList<>();
            for (PlayerEntity player : client.world.getPlayers()) {
                if (player != client.player && player.isAlive() && client.player.distanceTo(player) <= playerList.setting) players.add(player);
            }
            players.sort(Comparator.comparingDouble(client.player::distanceTo));
            lines.add(new HudLine("NEARBY PLAYERS  " + players.size(), 0xFFD9C6FF));
            for (int i = 0; i < Math.min(3, players.size()); i++) {
                PlayerEntity p = players.get(i);
                lines.add(new HudLine("  " + p.getName().getString() + "  " + Math.round(client.player.distanceTo(p)) + "m", 0xFFB9A4D9));
            }
        }
        if (on("Container Radar HUD") && on("Base Finder")) {
            List<BlockPos> hits = BaseScanner.getFindings();
            lines.add(new HudLine("CONTAINER RADAR  " + hits.size() + " hits • " + BaseScanner.getProgressPercent() + "% scan", 0xFFCEADFF));
            int count = 0;
            for (BlockPos pos : hits) {
                if (count++ >= (int) module("Container Radar HUD").setting) break;
                lines.add(new HudLine("  " + pos.getX() + " " + pos.getY() + " " + pos.getZ(), 0xFFCEADFF));
            }
        }

        int x = 10, y = 10;
        int alpha = (int) (Math.max(0.35, Math.min(1.0, master.setting)) * 232);
        ClientModule compact = module("Compact HUD");
        int lineHeight = on("Compact HUD") ? Math.max(9, 14 - (int) compact.setting / 2) : 12;
        int panelHeight = 10 + lines.size() * lineHeight;
        ctx.fill(x - 5, y - 5, x + 270, y + panelHeight, (alpha << 24) | 0x000C0915);
        ctx.fill(x - 5, y - 5, x + 270, y - 3, accentColor());
        for (int i = 0; i < lines.size(); i++) {
            HudLine line = lines.get(i);
            ctx.drawTextWithShadow(client.textRenderer, Text.literal(line.text), x, y + i * lineHeight, line.color);
        }
        if (on("Keystrokes")) drawKeystrokes(ctx, client);
        if (on("Crosshair Guide")) drawCrosshair(ctx, client, (int) module("Crosshair Guide").setting);
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
        ctx.fill(x - 4, y - 4, x + 74, y + 55, 0xB90C0915);
        ctx.fill(x - 4, y - 4, x + 74, y - 2, accentColor());
        key(ctx, client.options.forwardKey.isPressed(), x + 25, y, "W");
        key(ctx, client.options.leftKey.isPressed(), x, y + 18, "A");
        key(ctx, client.options.backKey.isPressed(), x + 25, y + 18, "S");
        key(ctx, client.options.rightKey.isPressed(), x + 50, y + 18, "D");
        ctx.fill(x, y + 37, x + 74, y + 47, client.options.jumpKey.isPressed() ? 0xFF8F54E9 : 0xFF392B48);
        ctx.drawCenteredTextWithShadow(client.textRenderer, Text.literal("JUMP"), x + 37, y + 39, 0xFFFFFFFF);
    }

    private static void key(DrawContext ctx, boolean pressed, int x, int y, String label) {
        ctx.fill(x, y, x + 22, y + 16, pressed ? 0xFF8F54E9 : 0xFF392B48);
        ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(label), x + 11, y + 4, 0xFFFFFFFF);
    }

    private static void drawCrosshair(DrawContext ctx, MinecraftClient client, int size) {
        int cx = client.getWindow().getScaledWidth() / 2, cy = client.getWindow().getScaledHeight() / 2;
        ctx.fill(cx - size, cy, cx - 2, cy + 1, 0xCCB58CFF);
        ctx.fill(cx + 2, cy, cx + size, cy + 1, 0xCCB58CFF);
        ctx.fill(cx, cy - size, cx + 1, cy - 2, 0xCCB58CFF);
        ctx.fill(cx, cy + 2, cx + 1, cy + size, 0xCCB58CFF);
    }

    private static int accentColor() {
        ClientModule glow = module("Accent Glow");
        double power = glow == null || !glow.enabled ? 0.35 : glow.setting;
        int alpha = (int)(100 + power * 155);
        return (alpha << 24) | 0x009B61FF;
    }

    private static void addIfEnabled(List<HudLine> lines, String moduleName, String text, int color) {
        if (on(moduleName)) lines.add(new HudLine(text, color));
    }
    private static boolean on(String name) { ClientModule m = module(name); return m != null && m.enabled; }
    private static ClientModule module(String name) { return CincoClient.findModule(name); }
}
