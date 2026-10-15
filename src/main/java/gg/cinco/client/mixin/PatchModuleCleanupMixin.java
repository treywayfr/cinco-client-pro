package gg.cinco.client.mixin;

import gg.cinco.client.CincoClient;
import gg.cinco.client.CincoPatchClient;
import gg.cinco.client.ConfigStore;
import gg.cinco.client.StarSettingsMigration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps old configs compatible while removing the standalone Hand View module from the GUI. */
@Mixin(value = CincoPatchClient.class, remap = false)
public abstract class PatchModuleCleanupMixin {
    @Inject(method = "onInitializeClient", at = @At("TAIL"), remap = false)
    private void star$cleanupMovedSettings(CallbackInfo ci) {
        boolean removed = CincoClient.MODULES.removeIf(module -> module.name.equals("Hand View"));
        StarSettingsMigration.apply();
        if (removed) ConfigStore.save();
    }
}
