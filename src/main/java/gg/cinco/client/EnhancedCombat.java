package gg.cinco.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Reliability patch for the two combat helpers that need precise block targeting.
 * Auto Crystal is intentionally NOT an aura: it only touches the crystal above the
 * obsidian block currently under the block-only crosshair ray.
 */
public final class EnhancedCombat {
    private static int crystalPlaceCooldown;
    private static int crystalBreakCooldown;

    private static int anchorStage;
    private static int anchorWait;
    private static int anchorPlaceTimeout;
    private static int anchorRetries;
    private static BlockPos anchorPos;
    private static int previousSlot = -1;

    private EnhancedCombat() {}

    public static void tick(MinecraftClient client) {
        if (crystalPlaceCooldown > 0) crystalPlaceCooldown--;
        if (crystalBreakCooldown > 0) crystalBreakCooldown--;
        tickAnchor(client);
    }

    /** Called in place of CombatModules' old Auto Crystal body. */
    public static void tickAutoCrystal(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null
                || client.currentScreen != null || client.getWindow() == null) return;

        boolean rightDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                == GLFW.GLFW_PRESS;
        if (!rightDown || !client.player.getMainHandStack().isOf(Items.END_CRYSTAL)) return;

        // Block-only raycast means an already-spawned crystal or a dropped item cannot steal the target.
        HitResult ray = client.player.raycast(5.0, 1.0F, false);
        if (!(ray instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos base = hit.getBlockPos();
        if (!client.world.getBlockState(base).isOf(Blocks.OBSIDIAN)) return;

        // Only interact with a crystal occupying THIS obsidian's crystal column.
        Box crystalColumn = new Box(base.getX() - 0.15, base.getY() + 0.8, base.getZ() - 0.15,
                base.getX() + 1.15, base.getY() + 3.2, base.getZ() + 1.15);
        List<EndCrystalEntity> crystals = client.world.getEntitiesByClass(EndCrystalEntity.class,
                crystalColumn, Entity::isAlive);

        ClientModule module = CincoClient.findModule("Auto Crystal");
        int placeDelay = module == null ? 1 : Math.max(1, (int) Math.round(module.setting) + 1);
        int breakDelay = module == null ? 1 : Math.max(1, (int) Math.round(module.setting2));

        if (!crystals.isEmpty()) {
            if (crystalBreakCooldown <= 0) {
                EndCrystalEntity crystal = crystals.get(0);
                client.interactionManager.attackEntity(client.player, crystal);
                client.player.swingHand(Hand.MAIN_HAND);
                crystalBreakCooldown = breakDelay;
            }
            return;
        }

        if (crystalPlaceCooldown > 0) return;
        // Vanilla crystal placement still validates entity collisions server-side. These air checks
        // prevent useless packets and match normal crystal placement behavior.
        if (!client.world.getBlockState(base.up()).isAir() || !client.world.getBlockState(base.up(2)).isAir()) return;

        BlockHitResult top = new BlockHitResult(
                new Vec3d(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5),
                Direction.UP, base, false);
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, top);
        client.player.swingHand(Hand.MAIN_HAND);
        crystalPlaceCooldown = placeDelay;
    }

    /** Called instead of the old Anchor Macro trigger. Uses a block-only ray to ignore dropped items. */
    public static void triggerAnchorMacro(MinecraftClient client) {
        ClientModule module = CincoClient.findModule("Anchor Macro");
        if (module == null || !module.enabled) {
            notify(client, "Enable Anchor Macro in the GUI first.");
            return;
        }
        if (client.player == null || client.world == null || client.interactionManager == null) return;
        if (client.world.getRegistryKey().equals(World.NETHER)) {
            notify(client, "Anchor Macro is disabled in the Nether.");
            return;
        }
        if (anchorStage != 0) {
            notify(client, "Anchor sequence already running.");
            return;
        }

        // Entity hits (including dropped items/xp) are deliberately ignored.
        HitResult ray = client.player.raycast(5.0, 1.0F, false);
        if (!(ray instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            notify(client, "Aim at a block, fire/replaceable block, or an existing anchor.");
            return;
        }

        BlockPos hitPos = hit.getBlockPos();
        BlockState hitState = client.world.getBlockState(hitPos);
        previousSlot = client.player.getInventory().selectedSlot;
        anchorRetries = 0;
        anchorWait = 0;

        if (hitState.isOf(Blocks.RESPAWN_ANCHOR)) {
            anchorPos = hitPos.toImmutable();
            int charges = hitState.get(RespawnAnchorBlock.CHARGES);
            anchorStage = charges > 0 ? 3 : 2;
            notify(client, "Anchor sequence started.");
            return;
        }

        BlockPos placePos;
        BlockHitResult placementHit;

        if (hitState.isReplaceable()) {
            // Fire, grass, vines, snow layers, etc: replace the looked-at block itself.
            placePos = hitPos;
            BlockPos support = hitPos.down();
            if (client.world.getBlockState(support).isReplaceable()) {
                restoreSlot(client);
                notify(client, "No solid support under that replaceable block.");
                return;
            }
            placementHit = new BlockHitResult(
                    new Vec3d(support.getX() + 0.5, support.getY() + 1.0, support.getZ() + 0.5),
                    Direction.UP, support, false);
        } else {
            placePos = hitPos.offset(hit.getSide());
            if (!client.world.getBlockState(placePos).isReplaceable()) {
                restoreSlot(client);
                notify(client, "That anchor space is occupied.");
                return;
            }
            placementHit = hit;
        }

        int anchorSlot = findHotbar(client, Items.RESPAWN_ANCHOR);
        int glowSlot = findHotbar(client, Items.GLOWSTONE);
        boolean hasTotem = client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)
                || findHotbar(client, Items.TOTEM_OF_UNDYING) >= 0;
        if (anchorSlot < 0 || glowSlot < 0 || !hasTotem) {
            restoreSlot(client);
            notify(client, "Need an anchor, glowstone, and a totem in hotbar/offhand.");
            return;
        }

        anchorPos = placePos.toImmutable();
        client.player.getInventory().selectedSlot = anchorSlot;
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, placementHit);
        client.player.swingHand(Hand.MAIN_HAND);
        anchorStage = 1; // wait for placement confirmation
        anchorPlaceTimeout = 30;
        anchorWait = Math.max(1, (int) Math.round(module.setting) + 1);
    }

    private static void tickAnchor(MinecraftClient client) {
        if (anchorStage == 0 || client.player == null || client.world == null || client.interactionManager == null) return;
        ClientModule module = CincoClient.findModule("Anchor Macro");
        if (module == null || !module.enabled || anchorPos == null) {
            resetAnchor(client);
            return;
        }
        if (anchorWait > 0) {
            anchorWait--;
            return;
        }

        BlockState state = client.world.getBlockState(anchorPos);
        if (anchorStage == 1) {
            if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
                anchorStage = state.get(RespawnAnchorBlock.CHARGES) > 0 ? 3 : 2;
                anchorWait = Math.max(0, (int) Math.round(module.setting2));
                return;
            }
            if (--anchorPlaceTimeout <= 0) {
                notify(client, "Anchor placement was not confirmed.");
                resetAnchor(client);
            }
            return;
        }

        if (!state.isOf(Blocks.RESPAWN_ANCHOR)) {
            resetAnchor(client);
            return;
        }

        if (anchorStage == 2) {
            if (state.get(RespawnAnchorBlock.CHARGES) > 0) {
                anchorStage = 3;
                anchorWait = Math.max(0, (int) Math.round(module.setting2));
                return;
            }
            int glowSlot = findHotbar(client, Items.GLOWSTONE);
            if (glowSlot < 0) {
                notify(client, "Glowstone is no longer in the hotbar.");
                resetAnchor(client);
                return;
            }
            client.player.getInventory().selectedSlot = glowSlot;
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, anchorHit(anchorPos));
            client.player.swingHand(Hand.MAIN_HAND);
            anchorRetries++;
            anchorStage = 4;
            anchorWait = Math.max(1, (int) Math.round(module.setting2) + 1);
            return;
        }

        if (anchorStage == 4) {
            if (state.get(RespawnAnchorBlock.CHARGES) > 0) {
                anchorStage = 3;
                anchorWait = Math.max(0, (int) Math.round(module.setting2));
            } else if (anchorRetries < Math.max(1, (int) Math.round(module.setting3))) {
                anchorStage = 2;
                anchorWait = 1;
            } else {
                notify(client, "Anchor charge was not confirmed.");
                resetAnchor(client);
            }
            return;
        }

        if (anchorStage == 3) {
            Hand hand;
            if (client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                hand = Hand.OFF_HAND;
            } else {
                int totemSlot = findHotbar(client, Items.TOTEM_OF_UNDYING);
                if (totemSlot < 0) {
                    notify(client, "Totem is no longer available.");
                    resetAnchor(client);
                    return;
                }
                client.player.getInventory().selectedSlot = totemSlot;
                hand = Hand.MAIN_HAND;
            }
            client.interactionManager.interactBlock(client.player, hand, anchorHit(anchorPos));
            client.player.swingHand(hand);
            notify(client, "Anchor macro complete.");
            resetAnchor(client);
        }
    }

    private static BlockHitResult anchorHit(BlockPos pos) {
        return new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                Direction.UP, pos, false);
    }

    private static int findHotbar(MinecraftClient client, net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private static void resetAnchor(MinecraftClient client) {
        restoreSlot(client);
        anchorStage = 0;
        anchorWait = 0;
        anchorPlaceTimeout = 0;
        anchorRetries = 0;
        anchorPos = null;
    }

    private static void restoreSlot(MinecraftClient client) {
        if (client != null && client.player != null && previousSlot >= 0 && previousSlot < 9) {
            client.player.getInventory().selectedSlot = previousSlot;
        }
        previousSlot = -1;
    }

    private static void notify(MinecraftClient client, String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§6Cinco §8» §f" + message), true);
        }
    }
}
