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

/** Precision combat helpers: targeted crystals and an anchor macro that also places into water/fire. */
public final class CombatV2 {
    private static int crystalPlaceCooldown;
    private static int crystalBreakCooldown;

    private static int anchorStage;
    private static int anchorWait;
    private static int anchorPlaceTimeout;
    private static int anchorRetries;
    private static BlockPos anchorPos;
    private static int previousSlot = -1;

    private CombatV2() {}

    public static void tick(MinecraftClient client) {
        if (crystalPlaceCooldown > 0) crystalPlaceCooldown--;
        if (crystalBreakCooldown > 0) crystalBreakCooldown--;
        tickAnchor(client);
    }

    /**
     * Auto Crystal is deliberately NOT an aura. It only acts while right click is held,
     * an end crystal is in the main hand, and the player's block-only ray is on obsidian.
     */
    public static void tickAutoCrystal(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null
                || client.currentScreen != null || client.getWindow() == null) return;
        boolean rightDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                == GLFW.GLFW_PRESS;
        if (!rightDown || !client.player.getMainHandStack().isOf(Items.END_CRYSTAL)) return;

        HitResult ray = client.player.raycast(5.0, 1.0F, false);
        if (!(ray instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos base = hit.getBlockPos();
        if (!client.world.getBlockState(base).isOf(Blocks.OBSIDIAN)) return;

        Box column = new Box(base.getX() - 0.10, base.getY() + 0.75, base.getZ() - 0.10,
                base.getX() + 1.10, base.getY() + 3.25, base.getZ() + 1.10);
        List<EndCrystalEntity> crystals = client.world.getEntitiesByClass(EndCrystalEntity.class, column, Entity::isAlive);
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
        if (!client.world.getBlockState(base.up()).isAir() || !client.world.getBlockState(base.up(2)).isAir()) return;

        BlockHitResult top = new BlockHitResult(new Vec3d(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5),
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
        if (client.player == null || client.world == null || client.interactionManager == null) return;
        if (client.world.getRegistryKey().equals(World.NETHER)) {
            notify(client, "Anchor Macro is disabled in the Nether.");
            return;
        }
        if (anchorStage != 0) {
            notify(client, "Anchor sequence already running.");
            return;
        }

        HitResult ray = client.player.raycast(5.0, 1.0F, false);
        if (!(ray instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            notify(client, "Aim at a block, water/fire space, or an existing anchor.");
            return;
        }

        previousSlot = client.player.getInventory().selectedSlot;
        anchorRetries = 0;
        anchorWait = 0;

        int hotbarTotem = findHotbar(client, Items.TOTEM_OF_UNDYING);
        if (hotbarTotem < 0) {
            restoreSlot(client);
            notify(client, "Put a totem in your hotbar first. Offhand totems are intentionally ignored.");
            return;
        }

        BlockPos hitPos = hit.getBlockPos();
        BlockState hitState = client.world.getBlockState(hitPos);
        if (hitState.isOf(Blocks.RESPAWN_ANCHOR)) {
            anchorPos = hitPos.toImmutable();
            anchorStage = hitState.get(RespawnAnchorBlock.CHARGES) > 0 ? 3 : 2;
            notify(client, "Anchor sequence started.");
            return;
        }

        Placement placement = resolvePlacement(client, hit);
        if (placement == null) {
            restoreSlot(client);
            notify(client, "Couldn't find a replaceable anchor space with solid support.");
            return;
        }

        int anchorSlot = findHotbar(client, Items.RESPAWN_ANCHOR);
        int glowSlot = findHotbar(client, Items.GLOWSTONE);
        if (anchorSlot < 0 || glowSlot < 0) {
            restoreSlot(client);
            notify(client, "Need an anchor, glowstone, and totem in the hotbar.");
            return;
        }

        anchorPos = placement.placePos.toImmutable();
        client.player.getInventory().selectedSlot = anchorSlot;
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, placement.hit);
        client.player.swingHand(Hand.MAIN_HAND);
        anchorStage = 1;
        anchorPlaceTimeout = 45;
        anchorWait = Math.max(1, (int) Math.round(module.setting) + 1);
    }

    private static Placement resolvePlacement(MinecraftClient client, BlockHitResult hit) {
        BlockPos hitPos = hit.getBlockPos();
        BlockState hitState = client.world.getBlockState(hitPos);
        if (canReplace(hitState)) {
            BlockPos cursor = hitPos;
            for (int depth = 0; depth < 8; depth++) {
                BlockPos below = cursor.down();
                BlockState belowState = client.world.getBlockState(below);
                if (!canReplace(belowState)) {
                    BlockHitResult supportHit = new BlockHitResult(
                            new Vec3d(below.getX() + 0.5, below.getY() + 1.0, below.getZ() + 0.5),
                            Direction.UP, below, false);
                    return new Placement(cursor, supportHit);
                }
                cursor = below;
            }
            return null;
        }

        BlockPos placePos = hitPos.offset(hit.getSide());
        BlockState placeState = client.world.getBlockState(placePos);
        if (!canReplace(placeState)) return null;
        if (hit.getSide() == Direction.UP && !placeState.getFluidState().isEmpty()) {
            BlockHitResult supportHit = new BlockHitResult(
                    new Vec3d(hitPos.getX() + 0.5, hitPos.getY() + 1.0, hitPos.getZ() + 0.5),
                    Direction.UP, hitPos, false);
            return new Placement(placePos, supportHit);
        }
        return new Placement(placePos, hit);
    }

    private static boolean canReplace(BlockState state) {
        return state.isReplaceable() || !state.getFluidState().isEmpty()
                || state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE);
    }

    private static void tickAnchor(MinecraftClient client) {
        if (anchorStage == 0 || client.player == null || client.world == null || client.interactionManager == null) return;
        ClientModule module = CincoClient.findModule("Anchor Macro");
        if (module == null || !module.enabled || anchorPos == null) {
            resetAnchor(client);
            return;
        }
        if (anchorWait > 0) { anchorWait--; return; }

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
            int totemSlot = findHotbar(client, Items.TOTEM_OF_UNDYING);
            if (totemSlot < 0) {
                notify(client, "Totem left the hotbar; macro stopped before detonation.");
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
        return new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                Direction.UP, pos, false);
    }

    private static int findHotbar(MinecraftClient client, Item item) {
        for (int i = 0; i < 9; i++) if (client.player.getInventory().getStack(i).isOf(item)) return i;
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
        if (client != null && client.player != null && previousSlot >= 0 && previousSlot < 9)
            client.player.getInventory().selectedSlot = previousSlot;
        previousSlot = -1;
    }

    private static void notify(MinecraftClient client, String message) {
        if (client != null && client.player != null)
            client.player.sendMessage(Text.literal("§6Cinco §8» §f" + message), true);
    }

    private record Placement(BlockPos placePos, BlockHitResult hit) {}
}
