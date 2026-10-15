package gg.cinco.client.mixin;

import gg.cinco.client.CincoClient;
import gg.cinco.client.FortniteSkinTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void cinco$localFortniteSkin(AbstractClientPlayerEntity player, CallbackInfoReturnable<Identifier> cir) {
        var module = CincoClient.findModule("Fortnite Style");
        if (module != null && module.enabled && player == MinecraftClient.getInstance().player) {
            cir.setReturnValue(FortniteSkinTexture.getTexture());
        }
    }
}
