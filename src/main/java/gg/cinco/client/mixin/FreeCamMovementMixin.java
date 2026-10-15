package gg.cinco.client.mixin;

import gg.cinco.client.FreeCamController;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class FreeCamMovementMixin {
    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void cinco$holdPlayerDuringFreeCam(CallbackInfo ci) {
        if (!FreeCamController.isActive()) return;
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0F;
        ci.cancel();
    }
}
