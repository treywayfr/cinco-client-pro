package gg.cinco.client.mixin;

import gg.cinco.client.CincoClient;
import gg.cinco.client.ClientModule;
import gg.cinco.client.CombatModules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the legacy Auto Totem tick with offhand + optional double-hand hotbar behavior. */
@Mixin(value = CombatModules.class, remap = false)
public abstract class AutoTotemDoubleHandMixin {
    @Unique private static int star$totemCooldown;

    @Inject(method = "tickAutoTotem", at = @At("HEAD"), cancellable = true, remap = false)
    private static void star$autoTotemDoubleHand(MinecraftClient client, CallbackInfo ci) {
        // This is the full Auto Totem implementation for the new setting layout.
        ci.cancel();
        if (star$totemCooldown > 0) star$totemCooldown--;

        ClientModule module = CincoClient.findModule("Auto Totem");
        if (module == null || !module.enabled || client == null || client.player == null
                || client.world == null || client.interactionManager == null || client.currentScreen != null) return;

        float threshold = (float)module.setting;
        if (client.player.getHealth() > threshold) return;

        // First priority is always the offhand.
        if (!client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            if (star$totemCooldown > 0) return;
            int source = star$findTotem(client, -1);
            if (source < 0) return;
            client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                    star$handlerSlot(source), 40, SlotActionType.SWAP, client.player);
            star$totemCooldown = 2;
            return;
        }

        // Optional second totem in the user-selected hotbar slot (1-9).
        if (module.setting2 < 0.5 || star$totemCooldown > 0) return;
        int targetHotbar = Math.max(0, Math.min(8, (int)Math.round(module.setting3) - 1));
        if (client.player.getInventory().getStack(targetHotbar).isOf(Items.TOTEM_OF_UNDYING)) return;

        int source = star$findTotem(client, targetHotbar);
        if (source < 0) return;

        // SWAP's button is the target hotbar index. Whatever was in the selected double-hand slot
        // is safely moved back into the source inventory slot instead of being thrown away.
        client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                star$handlerSlot(source), targetHotbar, SlotActionType.SWAP, client.player);
        star$totemCooldown = 2;
    }

    @Unique
    private static int star$findTotem(MinecraftClient client, int excludedHotbar) {
        for (int inventoryIndex = 0; inventoryIndex < 36; inventoryIndex++) {
            if (inventoryIndex == excludedHotbar) continue;
            if (client.player.getInventory().getStack(inventoryIndex).isOf(Items.TOTEM_OF_UNDYING)) {
                return inventoryIndex;
            }
        }
        return -1;
    }

    @Unique
    private static int star$handlerSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
    }
}
