package gg.cinco.client.mixin;

import gg.cinco.client.VisualModules;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public abstract class ClientWorldSkyMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void cinco$customSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (VisualModules.skyEnabled()) cir.setReturnValue(VisualModules.skyColor());
    }

    @Inject(method = "getCloudsColor", at = @At("RETURN"), cancellable = true)
    private void cinco$customCloudColor(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (VisualModules.skyEnabled()) cir.setReturnValue(VisualModules.cloudColor());
    }
}
