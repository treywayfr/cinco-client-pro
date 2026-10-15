package gg.cinco.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
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
 * Reliability-focused targeted crystal + anchor logic.
 *
 * Important rule: client-side checks only reject conditions that definitely make the action
 * impossible. Everything else is sent through vanilla interaction and the server gets the final
 * say. This avoids false negatives from overly strict helper logic.
 */
public final class CombatReliabilityV3 {
    private static int crystalPlaceCooldown;
    private static int crystalBreakCooldown;

    private static int anchorStage; // 0 idle, 1 waiting for placement, 2 charge, 3 activate, 4 wait charge
    private static int anchorWait;
    private static int anchorPlaceTimeout;
    private static int anchorChargeRetries;
    private static int anchorPlaceAttempts;
    private static BlockPos anchorPos;
    private static BlockHitResult anchorPlacementHit;
    private static int previousSlot = -1;

    private CombatReliabilityV3() {}

    public static void tick(MinecraftClient client) {
        if (crystalPlaceCooldown > 0) crystalPlaceCooldown--;
        if (crystalBreakCooldown > 0) crystalBreakCooldown--;
        tickAnchor(client);
    }

    /**
     * Manual/targeted Auto Crystal: only while right-clicking with an end crystal in the main hand.
     * Supports both obsidian and bedrock and deliberately lets vanilla/server placement validation
     * decide edge cases instead of blocking them client-side.
     */
    public static void tickAutoCrystal(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.interactionManager == null
                || client.currentScreen != null || client.getWindow() == null) return;

        boolean rightDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                == GLFW.GLFW_PRESS;
        if (!rightDown || !client.player.getMainHandStack().isOf(Items.END_CRYSTAL)) return;

        HitResult ray = client.player.raycast(5.0, 1.0F, false);
        if (!(ray instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos base = hit.getBlockPos();
        BlockState baseState = client.world.getBlockState(base);
        if (!baseState.isOf(Blocks.OBSIDIAN) && !baseState.isOf(Blocks.BEDROCK)) return;

        // Look for a crystal already occupying the targeted base column and break only that one.
        Box column = new Box(base.getX() - 0.15, base.getY() + 0.70, base.getZ() - 0.15,
                base.getX() + 1.15, base.getY() + 3.35, base.getZ() + 1.15);
        List<EndCrystalEntity> crystals = client.world.getEntitiesByClass(
                EndCrystalEntity.class, column, Entity::isAlive);

        ClientModule module = CincoClient.findModule("Auto Crystal");
        int placeDelay = module == null ? 1 : Math.max(1, (int) Math.round(module.setting) + 1);
        int breakDelay = module == null ? 1 : Math.max(1, (int) Math.round(module.setting2));

        if (!crystals.isEmpty()) {
            if (crystalBreakCooldown <= 0) {
                client.interactionManager.attackEntity(client.player, crystals.get(0));
                client.player.swingHand(Hand.MAIN_HAND);
                crystalBreakCooldown = breakDelay;
            }
            return;
        }

        if (crystalPlaceCooldown > 0) return;

        // Do not duplicate vanilla's air/entity checks here. Those checks have changed between
        // versions and were the reason legitimate bedrock/low-clearance placements got rejected.
        // Clicking the top face is enough; the server rejects genuinely impossible positions.
        BlockHitResult top = new BlockHitResult(
                new Vec3d(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5),
                Direction.UP, base, false);
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, top);
        client.player.swingHand(Hand.MAIN_HAND);
        crystalPlaceCooldown = placeDelay;
    }

    public static void triggerAnchorMacro(MinecraftClient client) {
        ClientModule module = CincoClient.findModule("Anchor Macro");
        if (module == null || !module.enabled) {
            notify(client, "Enable Anchor Macro in the GUI first.");
            return;
        }
        if (client == null || client.player == null || client.world == null || client.interactionManager == null) return;
        if (anchorStage != 0) {
            notify(client, "Anchor sequence already running.");
            return;
        }

        HitResult ray = client.player.raycast(5.0, 1.0F, false);
        if (!(ray instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            notify(client, "Aim at the block/space where you want the anchor.");
            return;
        }

        previousSlot = client.player.getInventory().selectedSlot;
        anchorChargeRetries = 0;
        anchorPlaceAttempts = 0;
        anchorWait = 0;
        anchorPlacementHit = null;

        BlockPos hitPos = hit.getBlockPos();
        BlockState hitState = client.world.getBlockState(hitPos);

        // Existing anchors never need placement prerequisites. Start from their actual state.
        if (hitState.isOf(Blocks.RESPAWN_ANCHOR)) {
            anchorPos = hitPos.toImmutable();
            anchorStage = hitState.get(RespawnAnchorBlock.CHARGES) > 0 ? 3 : 2;
            notify(client, "Anchor sequence started.");
            return;
        }

        Placement placement = resolvePlacement(client, hit);
        if (placement == null) {
            restoreSlot(client);
            notify(client, "That exact anchor space is occupied by a non-replaceable block.");
            return;
        }

        int anchorSlot = findHotbar(client, Items.RESPAWN_ANCHOR);
        if (anchorSlot < 0) {
            restoreSlot(client);
            notify(client, "Put a respawn anchor in your hotbar to place it.");
            return;
        }

        // Glowstone/totem/dimension are intentionally NOT placement prerequisites. They matter only
        // when their later stage is reached, so they must never stop a valid anchor from being placed.
        anchorPos = placement.placePos.toImmutable();
        anchorPlacementHit = placement.hit;
        attemptAnchorPlacement(client, anchorSlot);
        anchorStage = 1;
        anchorPlaceTimeout = 50;
        anchorWait = Math.max(1, (int) Math.round(module.setting) + 1);
    }

    /**
     * Minimal placement resolution. If the looked-at block itself is replaceable, click it directly
     * and let BlockItem/vanilla choose the placement context. Otherwise use the looked-at face's
     * adjacent block. No fake "solid support", fluid, fire, shape or dimension restrictions.
     */
    private static Placement resolvePlacement(MinecraftClient client, BlockHitResult hit) {
        BlockPos hitPos = hit.getBlockPos();
        BlockState hitState = client.world.getBlockState(hitPos);

        if (canReplace(hitState)) {
            return new Placement(hitPos, hit);
        }

        BlockPos placePos = hitPos.offset(hit.getSide());
        if (!canReplace(client.world.getBlockState(placePos))) return null;
        return new Placement(placePos, hit);
    }

    private static boolean canReplace(BlockState state) {
        return state.isReplaceable() || !state.getFluidState().isEmpty()
                || state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE);
    }

    private static void attemptAnchorPlacement(MinecraftClient client, int preferredSlot) {
        if (client.player == null || client.interactionManager == null || anchorPlacementHit == null) return;
        int slot = preferredSlot;
        if (slot < 0 || slot > 8 || !client.player.getInventory().getStack(slot).isOf(Items.RESPAWN_ANCHOR)) {
            slot = findHotbar(client, Items.RESPAWN_ANCHOR);
        }
        if (slot < 0) return;
        client.player.getInventory().selectedSlot = slot;
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, anchorPlacementHit);
        client.player.swingHand(Hand.MAIN_HAND);
        anchorPlaceAttempts++;
    }

    private static void tickAnchor(MinecraftClient client) {
        if (anchorStage == 0 || client == null || client.player == null || client.world == null
                || client.interactionManager == null) return;

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
                notify(client, "Anchor placement was not confirmed by the server.");
                resetAnchor(client);
                return;
            }

            // Retry a few times to survive hotbar/packet timing without spamming every tick.
            if (anchorPlaceAttempts < 6 && anchorPlaceTimeout % 5 == 0) {
                attemptAnchorPlacement(client, findHotbar(client, Items.RESPAWN_ANCHOR));
            }
            return;
        }

        if (!state.isOf(Blocks.RESPAWN_ANCHOR)) {
            notify(client, "Anchor disappeared before the sequence finished.");
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
                notify(client, "Anchor was placed, but glowstone is not in your hotbar.");
                resetAnchor(client);
                return;
            }

            client.player.getInventory().selectedSlot = glowSlot;
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, anchorHit(anchorPos));
            client.player.swingHand(Hand.MAIN_HAND);
            anchorChargeRetries++;
            anchorStage = 4;
            anchorWait = Math.max(1, (int) Math.round(module.setting2) + 1);
            return;
        }

        if (anchorStage == 4) {
            if (state.get(RespawnAnchorBlock.CHARGES) > 0) {
                anchorStage = 3;
                anchorWait = Math.max(0, (int) Math.round(module.setting2));
            } else if (anchorChargeRetries < Math.max(1, (int) Math.round(module.setting3))) {
                anchorStage = 2;
                anchorWait = 1;
            } else {
                notify(client, "Anchor was placed, but charging was not confirmed.");
                resetAnchor(client);
            }
            return;
        }

        if (anchorStage == 3) {
            // Anchors are valid blocks in the Nether, but activating one there sets spawn rather than
            // detonating. Placement/charging is allowed; unsafe/incorrect activation is skipped.
            if (client.world.getRegistryKey().equals(World.NETHER)) {
                notify(client, "Anchor placed and charged. Activation skipped in the Nether.");
                resetAnchor(client);
                return;
            }

            int totemSlot = findHotbar(client, Items.TOTEM_OF_UNDYING);
            if (totemSlot < 0) {
                notify(client, "Anchor is ready, but a totem is not in your hotbar.");
                resetAnchor(client);
                return;
            }

            client.player.getInventory().selectedSlot = totemSlot;
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, anchorHit(anchorPos));
            client.player.swingHand(Hand.MAIN_HAND);
            notify(client, "Anchor macro complete.");
            resetAnchor(client);
        }
    }

    private static BlockHitResult anchorHit(BlockPos pos) {
        return new BlockHitResult(
                new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                Direction.UP, pos, false);
    }

    private static int findHotbar(MinecraftClient client, Item item) {
        if (client == null || client.player == null) return -1;
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
        anchorChargeRetries = 0;
        anchorPlaceAttempts = 0;
        anchorPos = null;
        anchorPlacementHit = null;
    }

    private static void restoreSlot(MinecraftClient client) {
        if (client != null && client.player != null && previousSlot >= 0 && previousSlot < 9) {
            client.player.getInventory().selectedSlot = previousSlot;
        }
        previousSlot = -1;
    }

    private static void notify(MinecraftClient client, String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§5Star §8» §f" + message), true);
        }
    }

    private record Placement(BlockPos placePos, BlockHitResult hit) {}
}
