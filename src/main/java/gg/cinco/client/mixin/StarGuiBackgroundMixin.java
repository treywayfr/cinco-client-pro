package gg.cinco.client.mixin;

import gg.cinco.client.CincoClickGuiScreen;
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
 * Draws the same persistent starfield immediately after each Star screen paints its black backdrop.
 * This prevents stars from resetting/disappearing when switching into settings/color pages.
 */
@Mixin({CincoClickGuiScreen.class, ModuleSettingsScreen.class, CincoColorWheelScreen.class, StorageEspSettingsScreen.class})
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
