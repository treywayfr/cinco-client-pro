package gg.cinco.client.mixin;

import gg.cinco.client.CincoClickGuiScreen;
import gg.cinco.client.CincoColorWheelScreen;
import gg.cinco.client.ModuleSettingsScreen;
import gg.cinco.client.StarGuiSound;
import gg.cinco.client.StorageEspSettingsScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class StarGuiSoundMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void star$softClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (self instanceof CincoClickGuiScreen || self instanceof CincoColorWheelScreen
                || self instanceof ModuleSettingsScreen || self instanceof StorageEspSettingsScreen) {
            StarGuiSound.click(button == 1 ? 0.90f : 1.05f);
        }
    }
}
