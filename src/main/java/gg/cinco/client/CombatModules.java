package gg.cinco.client;

import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

public final class CombatModules {
    private static int crystalCooldown;
    private static int crystalPlaceCooldown;
    private static boolean rightMouseWasDown;
    private static int anchorPlacementTicks;
    private static int pearlRestoreTicks;
    private static int pearlPreviousSlot;
    private static int pearlThrownSlot;
    private static boolean pearlShouldRestore;
    private static int anchorCooldown;
    private static int anchorStage; // 0 idle, 1 charge once, 2 activate
    private static int anchorWait;
    private static int anchorRetries;
    private static int previousCharges;
    private static int anchorDelay = 1;
    private static BlockPos activeAnchor;
    private static boolean safeGlowstonePlaced;
    private static boolean autoEatPressing;
    private static int autoEatTicks;
    private static int autoTotemCooldown;
    private static int safeAnchorCooldown;
    private static int autoEatCooldown;

    private CombatModules() {}

    public static void tick(MinecraftClient client) {
        if (crystalCooldown > 0) crystalCooldown--;
        if (autoTotemCooldown > 0) autoTotemCooldown--;
        if (safeAnchorCooldown > 0) safeAnchorCooldown--;
        if (autoEatCooldown > 0) autoEatCooldown--;
        if (crystalPlaceCooldown > 0) crystalPlaceCooldown--;
        if (anchorCooldown > 0) anchorCooldown--;
        tickPearlRestore(client);

        if (enabled("Auto Crystal") || enabled("Crystal Breaker")) tickAutoCrystal(client, enabled("Auto Crystal"));
        tickAnchorSequence(client);
        tickAutoTotem(client);
        tickAutoEat(client);

        if (scannerEnabled()) {
            BaseScanner.tick(client);
        } else {
            BaseScanner.reset();
        }
    }

    private static boolean enabled(String name) {
        ClientModule module = CincoClient.findModule(name);
        return module != null && module.enabled;
    }

    public static void triggerAnchorMacro(MinecraftClient client) {
        if (!enabled("Anchor Macro")) {
            notify(client, "Enable Anchor Macro in the GUI first.");
            return;
        }
        if (client.player == null || client.world == null || client.interactionManager == null) return;
        if (client.world.getRegistryKey().equals(World.NETHER)) {
            notify(client, "Anchor Macro is disabled in the Nether to avoid activating a respawn anchor there.");
            return;
        }
        if (anchorStage != 0) {
            notify(client, "Anchor sequence already running.");
            return;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            notify(client, "Aim at a block to place an anchor, or aim at an existing anchor.");
            return;
        }

        anchorDelay = Math.max(0, (int) moduleSetting("Anchor Macro", 1));
        anchorRetries = 0;
        anchorWait = 0;
        safeGlowstonePlaced = false;

        BlockPos target = hit.getBlockPos();
        if (client.world.getBlockState(target).isOf(Blocks.RESPAWN_ANCHOR)) {
            activeAnchor = target.toImmutable();
            previousCharges = client.world.getBlockState(activeAnchor).get(RespawnAnchorBlock.CHARGES);
            anchorStage = previousCharges > 0 ? 2 : 1;
            anchorWait = previousCharges > 0 ? Math.max(0, (int) moduleSetting("Anchor Macro", 2)) : 0;
            notify(client, "Anchor sequence started on existing anchor.");
            return;
        }

        BlockPos placePos = target.offset(hit.getSide());
        if (!client.world.getBlockState(placePos).isAir()) {
            notify(client, "Can't place anchor there — the adjacent block space isn't empty.");
            return;
        }
        int anchorSlot = findHotbarItem(client, Items.RESPAWN_ANCHOR);
        if (anchorSlot < 0 || findHotbarItem(client, Items.GLOWSTONE) < 0 || findHotbarItem(client, Items.TOTEM_OF_UNDYING) < 0) {
            notify(client, "Put a respawn anchor, glowstone, and totem in your hotbar first.");
            return;
        }
        activeAnchor = placePos.toImmutable();
        previousCharges = 0;
        client.player.getInventory().selectedSlot = anchorSlot;
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
        client.player.swingHand(Hand.MAIN_HAND);
        anchorStage = 3; // wait for the server/client world to show the placed anchor
        anchorPlacementTicks = 8;
        anchorWait = Math.max(1, anchorDelay);
        notify(client, "Anchor placed — charging and totem activation sequence started.");
    }

    private static void tickAnchorSequence(MinecraftClient client) {
        if (anchorStage == 0 || client.player == null || client.world == null || client.interactionManager == null) return;
        if (activeAnchor == null) {
            resetAnchor();
            return;
        }
        if (anchorStage != 3 && !client.world.getBlockState(activeAnchor).isOf(Blocks.RESPAWN_ANCHOR)) {
            resetAnchor();
            return;
        }
        if (anchorStage == 3) {
            if (client.world.getBlockState(activeAnchor).isOf(Blocks.RESPAWN_ANCHOR)) {
                previousCharges = client.world.getBlockState(activeAnchor).get(RespawnAnchorBlock.CHARGES);
                anchorStage = previousCharges > 0 ? 2 : 1;
                anchorWait = Math.max(0, anchorDelay);
                return;
            }
            if (--anchorPlacementTicks <= 0) {
                notify(client, "Anchor placement didn't register. Check that the block is placeable.");
                resetAnchor();
            }
            return;
        }

        tickAutoSafeAnchor(client);
        if (anchorWait > 0) {
            anchorWait--;
            return;
        }

        if (anchorStage == 1) {
            int charges = client.world.getBlockState(activeAnchor).get(RespawnAnchorBlock.CHARGES);
            if (charges > 0) {
                previousCharges = charges;
                anchorStage = 2;
                anchorWait = anchorDelay;
                return;
            }
            int glowstoneSlot = findHotbarItem(client, Items.GLOWSTONE);
            if (glowstoneSlot < 0) {
                notify(client, "Anchor Macro stopped: put glowstone in your hotbar.");
                resetAnchor();
                return;
            }
            client.player.getInventory().selectedSlot = glowstoneSlot;
            BlockHitResult anchorHit = anchorHit(activeAnchor);
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, anchorHit);
            client.player.swingHand(Hand.MAIN_HAND);
            anchorStage = 4; // wait for server confirmation before switching to the totem
            anchorWait = Math.max(1, anchorDelay + (int) moduleSetting("Anchor Macro", 2));
            anchorRetries++;
            return;
        }

        if (anchorStage == 4) {
            int charges = client.world.getBlockState(activeAnchor).get(RespawnAnchorBlock.CHARGES);
            if (charges > 0) {
                anchorStage = 2;
                anchorWait = Math.max(0, (int) moduleSetting("Anchor Macro", 2));
            } else if (anchorRetries < moduleSettingInt("Anchor Macro", 3)) {
                anchorStage = 1;
                anchorWait = Math.max(0, anchorDelay);
            } else {
                notify(client, "Anchor charging wasn't confirmed. Macro stopped before activation.");
                resetAnchor();
            }
            return;
        }

        if (anchorStage == 2) {
            int totemSlot = findHotbarItem(client, Items.TOTEM_OF_UNDYING);
            if (totemSlot < 0) {
                notify(client, "Put a totem in your hotbar to finish the anchor sequence.");
                resetAnchor();
                return;
            }
            client.player.getInventory().selectedSlot = totemSlot;
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, anchorHit(activeAnchor));
            client.player.swingHand(Hand.MAIN_HAND);
            anchorCooldown = 8;
            notify(client, "Anchor macro sequence complete.");
            resetAnchor();
        }
    }

    private static void tickAutoSafeAnchor(MinecraftClient client) {
        if (!enabled("Auto Safe Anchor") || safeGlowstonePlaced || anchorStage == 0 || client.player == null || safeAnchorCooldown > 0) return;
        ClientModule safe = CincoClient.findModule("Auto Safe Anchor");
        float threshold = safe == null ? 70 : (float) safe.setting;
        if (client.player.getPitch() < threshold) return; // positive pitch means looking down
        if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos target = hit.getBlockPos();
        if (target.equals(activeAnchor) || client.player.squaredDistanceTo(Vec3d.ofCenter(target)) > Math.pow(safe == null ? 2.0 : safe.setting2, 2.0)) return;
        if (target.getY() < client.player.getBlockY() - 2 || target.getY() > client.player.getBlockY() + 1) return;
        int glowstoneSlot = findHotbarItem(client, Items.GLOWSTONE);
        if (glowstoneSlot < 0) return;
        client.player.getInventory().selectedSlot = glowstoneSlot;
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
        client.player.swingHand(Hand.MAIN_HAND);
        safeGlowstonePlaced = true;
        safeAnchorCooldown = safe == null ? 3 : (int) safe.setting3;
        notify(client, "Safe Anchor: placed glowstone at your look point near your feet.");
    }

    private static BlockHitResult anchorHit(BlockPos pos) {
        return new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
    }

    private static void resetAnchor() {
        anchorStage = 0;
        anchorWait = 0;
        anchorPlacementTicks = 0;
        activeAnchor = null;
        anchorRetries = 0;
        safeGlowstonePlaced = false;
    }

    private static void tickAutoTotem(MinecraftClient client) {
        if (!enabled("Auto Totem") || client.player == null || client.world == null
                || client.interactionManager == null || client.currentScreen != null || autoTotemCooldown > 0) return;
        ClientModule module = CincoClient.findModule("Auto Totem");
        float threshold = module == null ? 12.0f : (float) (module.setting + module.setting3);
        if (client.player.getHealth() > threshold || client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;

        for (int inventoryIndex = 0; inventoryIndex < 36; inventoryIndex++) {
            if (!client.player.getInventory().getStack(inventoryIndex).isOf(Items.TOTEM_OF_UNDYING)) continue;
            int handlerSlot = inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
            client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                    handlerSlot, 40, SlotActionType.SWAP, client.player);
            autoTotemCooldown = module == null ? 10 : (int) module.setting2;
            notify(client, "Auto Totem: moving a totem to your offhand.");
            return;
        }
    }

    private static void tickAutoEat(MinecraftClient client) {
        ClientModule module = CincoClient.findModule("Auto Eat");
        if (client.player == null || client.world == null || client.currentScreen != null) {
            releaseAutoEat(client);
            return;
        }
        if (!enabled("Auto Eat")) {
            releaseAutoEat(client);
            return;
        }

        int hungerThreshold = module == null ? 8 : (int) module.setting;
        int hunger = client.player.getHungerManager().getFoodLevel();

        if (autoEatPressing) {
            autoEatTicks++;
            if (hunger > hungerThreshold || autoEatTicks > 100) {
                releaseAutoEat(client);
                return;
            }
            client.options.useKey.setPressed(true);
            return;
        }
        if (hunger > hungerThreshold || client.player.isUsingItem() || autoEatCooldown > 0) return;

        for (int slot = 0; slot < 9; slot++) {
            Item item = client.player.getInventory().getStack(slot).getItem();
            if (!isUsefulFood(item)) continue;
            if (item == Items.GOLDEN_APPLE && module != null && module.setting3 < 0.5) continue;
            client.player.getInventory().selectedSlot = slot;
            client.options.useKey.setPressed(true);
            autoEatPressing = true;
            autoEatTicks = 0;
            return;
        }
    }

    private static boolean isUsefulFood(Item item) {
        return item == Items.COOKED_BEEF || item == Items.COOKED_PORKCHOP || item == Items.COOKED_MUTTON
                || item == Items.COOKED_CHICKEN || item == Items.COOKED_RABBIT || item == Items.BAKED_POTATO
                || item == Items.GOLDEN_CARROT || item == Items.BREAD || item == Items.PUMPKIN_PIE
                || item == Items.GOLDEN_APPLE || item == Items.COOKED_COD || item == Items.COOKED_SALMON;
    }

    private static void releaseAutoEat(MinecraftClient client) {
        if (autoEatPressing && client != null) client.options.useKey.setPressed(false);
        if (autoEatPressing) {
            ClientModule module = CincoClient.findModule("Auto Eat");
            autoEatCooldown = module == null ? 5 : (int) module.setting2;
        }
        autoEatPressing = false;
        autoEatTicks = 0;
    }

    private static boolean scannerEnabled() {
        return enabled("Base Finder") || enabled("Container Finder") || enabled("Sus Chunk Finder")
                || enabled("Portal Finder") || enabled("Spawner Finder") || enabled("Shulker Finder")
                || enabled("Stash Radar") || enabled("Chunk Density");
    }

    private static void tickAutoCrystal(MinecraftClient client, boolean allowPlace) {
        if (client.player == null || client.world == null || client.interactionManager == null) return;
        boolean rightDown = client.currentScreen == null && client.getWindow() != null
                && GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean rightPressedThisTick = rightDown && !rightMouseWasDown;
        rightMouseWasDown = rightDown;

        // Auto Crystal only places from the user's own right-click and only when the crystal is held.
        if (allowPlace && rightDown && client.player.getMainHandStack().isOf(Items.END_CRYSTAL) && crystalPlaceCooldown <= 0) {
            if (client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos base = hit.getBlockPos();
                boolean crystalBase = client.world.getBlockState(base).isOf(Blocks.OBSIDIAN)
                        || client.world.getBlockState(base).isOf(Blocks.BEDROCK);
                if (crystalBase && client.world.getBlockState(base.up()).isAir()
                        && client.world.getBlockState(base.up(2)).isAir()) {
                    client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
                    client.player.swingHand(Hand.MAIN_HAND);
                    ClientModule module = CincoClient.findModule("Auto Crystal");
                    crystalPlaceCooldown = module == null ? 1 : Math.max(1, (int) module.setting + 1);
                }
            }
        }

        // Break nearby crystals rapidly while Auto Crystal is being used, or continuously for Crystal Breaker.
        if (allowPlace && !rightDown) return;
        if (!allowPlace && !enabled("Crystal Breaker")) return;
        if (crystalCooldown > 0) return;
        double range = 4.5;
        EndCrystalEntity nearest = null;
        double best = range * range;
        for (EndCrystalEntity entity : client.world.getEntitiesByClass(EndCrystalEntity.class,
                client.player.getBoundingBox().expand(range), Entity::isAlive)) {
            double distance = client.player.squaredDistanceTo(entity);
            if (distance < best) {
                best = distance;
                nearest = entity;
            }
        }
        if (nearest != null) {
            client.interactionManager.attackEntity(client.player, nearest);
            client.player.swingHand(Hand.MAIN_HAND);
            ClientModule module = CincoClient.findModule(allowPlace ? "Auto Crystal" : "Crystal Breaker");
            crystalCooldown = module == null ? 1 : Math.max(1, (int) module.setting2);
        }
    }

    public static void triggerPearl(MinecraftClient client) {
        if (!enabled("Key Pearl")) {
            notify(client, "Enable Key Pearl in the GUI first.");
            return;
        }
        if (client.player == null || client.world == null || client.interactionManager == null || client.currentScreen != null) return;
        if (pearlRestoreTicks > 0) {
            notify(client, "Pearl macro is already finishing its previous throw.");
            return;
        }
        int pearlSlot = findHotbarItem(client, Items.ENDER_PEARL);
        if (pearlSlot < 0) {
            notify(client, "Put an ender pearl in your hotbar first.");
            return;
        }
        ClientModule module = CincoClient.findModule("Key Pearl");
        pearlPreviousSlot = client.player.getInventory().selectedSlot;
        pearlThrownSlot = pearlSlot;
        pearlShouldRestore = module != null && module.setting3 >= 0.5;
        client.player.getInventory().selectedSlot = pearlSlot;
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        client.player.swingHand(Hand.MAIN_HAND);
        pearlRestoreTicks = Math.max(1, module == null ? 2 : (int) module.setting2);
        notify(client, "Pearl thrown.");
    }

    private static void tickPearlRestore(MinecraftClient client) {
        if (pearlRestoreTicks <= 0 || client.player == null) return;
        pearlRestoreTicks--;
        if (pearlRestoreTicks == 0 && pearlShouldRestore
                && client.player.getInventory().selectedSlot == pearlThrownSlot) {
            client.player.getInventory().selectedSlot = pearlPreviousSlot;
        }
    }

    private static int findHotbarItem(MinecraftClient client, Item item) {
        for (int i = 0; i < 9; i++) if (client.player.getInventory().getStack(i).isOf(item)) return i;
        return -1;
    }

    private static int moduleSettingInt(String name, int fallback) {
        ClientModule module = CincoClient.findModule(name);
        if (module == null) return fallback;
        if (name.equals("Anchor Macro")) return (int) module.setting2;
        if (name.equals("Auto Totem")) return (int) module.setting2;
        return (int) module.setting;
    }

    private static double moduleSetting(String name, int slot) {
        ClientModule module = CincoClient.findModule(name);
        if (module == null) return 1.0;
        return switch (slot) {
            case 2 -> module.setting2;
            case 3 -> module.setting3;
            default -> module.setting;
        };
    }

    private static void notify(MinecraftClient client, String message) {
        if (client.player != null) client.player.sendMessage(Text.literal("§6Cinco §8» §6" + message), true);
    }
}
