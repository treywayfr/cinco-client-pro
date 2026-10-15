package gg.cinco.client.mixin;

import gg.cinco.client.FreeCamController;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class FreeCamPlayerMixin {
    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void cinco$freezePlayerDuringFreeCam(CallbackInfo ci) {
        if (FreeCamController.isActive()) ci.cancel();
    }
}
