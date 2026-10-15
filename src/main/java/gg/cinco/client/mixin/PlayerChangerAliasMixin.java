package gg.cinco.client.mixin;

import gg.cinco.client.CincoClient;
import gg.cinco.client.ClientModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets old renderer code keep resolving the renamed Player Changer module. */
@Mixin(value = CincoClient.class, remap = false)
public abstract class PlayerChangerAliasMixin {
    @Inject(method = "findModule", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cinco$playerChangerAlias(String name, CallbackInfoReturnable<ClientModule> cir) {
        if (!"GTA Skin Changer".equals(name)) return;
        for (ClientModule module : CincoClient.MODULES) {
            if ("Player Changer".equals(module.name)) {
                cir.setReturnValue(module);
                return;
            }
        }
    }
}
