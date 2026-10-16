package gg.cinco.client.mixin;

import gg.cinco.client.StarHudSilencer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/** Removes Star/Cinco macro action-bar chatter while leaving unrelated server action bars alone. */
@Mixin(ClientPlayerEntity.class)
public abstract class StarActionBarMixin {
    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void star$silenceMacroActionBar(Text message, boolean overlay, CallbackInfo ci) {
        if (!overlay || message == null) return;
        String lower = message.getString().toLowerCase(Locale.ROOT);
        boolean starMessage = lower.contains("cinco") || lower.contains("star")
                || lower.contains("pearl thrown") || lower.contains("pearl macro")
                || lower.contains("anchor macro") || lower.contains("anchor sequence")
                || lower.contains("anchor placed") || lower.contains("safe anchor")
                || lower.contains("auto totem");
        if (!starMessage) return;
        StarHudSilencer.silenceFor(60);
        ci.cancel();
    }
}
