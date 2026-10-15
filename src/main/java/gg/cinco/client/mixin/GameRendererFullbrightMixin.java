package gg.cinco.client.mixin;

import gg.cinco.client.CincoClient;
import gg.cinco.client.ClientModule;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds full night-vision contribution on top of the lightmap override for an extra-bright result. */
@Mixin(GameRenderer.class)
public abstract class GameRendererFullbrightMixin {
    @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
    private static void cinco$superFullbright(LivingEntity entity, float tickDelta,
                                              CallbackInfoReturnable<Float> cir) {
        ClientModule module = CincoClient.findModule("Fullbright");
        if (module != null && module.enabled) cir.setReturnValue(1.0F);
    }
}
