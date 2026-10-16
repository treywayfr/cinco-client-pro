package gg.cinco.client.mixin;

import gg.cinco.client.CincoColorWheelScreen;
import gg.cinco.client.ModuleSettingsScreen;
import gg.cinco.client.StarBackground;
import gg.cinco.client.StorageEspSettingsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the persistent starfield on secondary Star settings screens.
 * CincoClickGuiScreen already renders StarBackground directly, so deliberately
 * do not mix into the Right Shift screen. Keeping that class mixin-free avoids
 * runtime transformation failures when the GUI is opened.
 */
@Mixin({ModuleSettingsScreen.class, CincoColorWheelScreen.class, StorageEspSettingsScreen.class})
public abstract class StarGuiBackgroundMixin {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            ),
            require = 0
    )
    private void star$persistentPurpleStars(DrawContext context, int mouseX, int mouseY, float delta,
                                            CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        StarBackground.render(context,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight(),
                delta);
    }
}
