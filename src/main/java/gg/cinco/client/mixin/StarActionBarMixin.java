package gg.cinco.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/** Star Client keeps local action-bar feedback icon-only instead of filling the HUD with text. */
@Mixin(ClientPlayerEntity.class)
public abstract class StarActionBarMixin {
    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true)
    private void star$iconOnlyActionBar(Text message, boolean overlay, CallbackInfo ci) {
        if (!overlay) return;
        String lower = message.getString().toLowerCase(Locale.ROOT);
        String icon;
        int color;
        if (lower.contains("missing") || lower.contains("can't") || lower.contains("cannot")
                || lower.contains("failed") || lower.contains("error") || lower.contains("enable ")) {
            icon = "⚠"; color = 0xFFFFC85A;
        } else if (lower.contains("disabled") || lower.contains("stopped") || lower.contains("cancel")) {
            icon = "✕"; color = 0xFFFF6C87;
        } else if (lower.contains("enabled") || lower.contains("success") || lower.contains("placed")
                || lower.contains("ready")) {
            icon = "★"; color = 0xFFFFF08A;
        } else {
            icon = "✦"; color = 0xFFDFA7FF;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null) client.inGameHud.setOverlayMessage(Text.literal(icon).styled(s -> s.withColor(color).withBold(true)), false);
        ci.cancel();
    }
}
