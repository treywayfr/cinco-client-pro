package gg.cinco.client.mixin;

import gg.cinco.client.CincoHud;
import gg.cinco.client.StarExtraHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatHudEditorMixin {
    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void star$resizeHud(double mouseX, double mouseY, double horizontalAmount, double verticalAmount,
                                CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (StarExtraHud.onScroll(client, mouseX, mouseY, verticalAmount)
                || CincoHud.onScroll(client, mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }
}
