package gg.cinco.client.mixin;

import gg.cinco.client.FreeCamController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class FreeCamMouseMixin {
    @Inject(method = "updateMouse", at = @At("TAIL"))
    private void cinco$syncFreeCamLook(double timeDelta, CallbackInfo ci) {
        FreeCamController.syncMouseLook(MinecraftClient.getInstance());
    }
}
