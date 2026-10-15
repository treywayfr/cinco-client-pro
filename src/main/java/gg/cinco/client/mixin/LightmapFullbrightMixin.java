package gg.cinco.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.cinco.client.CincoClient;
import gg.cinco.client.ClientModule;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** True visual fullbright: overrides the vanilla lightmap gamma stage instead of relying on the options slider. */
@Mixin(LightmapTextureManager.class)
public abstract class LightmapFullbrightMixin {
    @ModifyExpressionValue(
            method = "update",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0)
    )
    private float cinco$fullbright(float original) {
        ClientModule module = CincoClient.findModule("Fullbright");
        return module != null && module.enabled ? 1.0F : original;
    }
}
