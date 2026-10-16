package gg.cinco.client.mixin;

import gg.cinco.client.StarHudSilencer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the temporary held-item name while Star macros swap hotbar slots. */
@Mixin(InGameHud.class)
public abstract class HeldItemTooltipMixin {
    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true, require = 0)
    private void star$hideMacroHeldItemTooltip(DrawContext context, CallbackInfo ci) {
        if (StarHudSilencer.active()) ci.cancel();
    }
}
