package gg.cinco.client.mixin;

import gg.cinco.client.CincoClient;
import gg.cinco.client.EnhancedMovement;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CincoClient.class, remap = false)
public abstract class CincoClientMixin {
    @Inject(method = "tickFlight", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinco$stableFlight(MinecraftClient client, CallbackInfo ci) {
        EnhancedMovement.tickFlight(client);
        ci.cancel();
    }
}
