package gg.cinco.client.mixin;

import gg.cinco.client.ClientModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Small post-constructor tuning layer for newer Star modules without breaking old config files. */
@Mixin(value = ClientModule.class, remap = false)
public abstract class ClientModuleTuningMixin {
    @Shadow public String name;
    @Shadow @Final @Mutable public double max;
    @Shadow @Final @Mutable public String setting2Label;
    @Shadow @Final @Mutable public double min2;
    @Shadow @Final @Mutable public double max2;
    @Shadow @Final @Mutable public double step2;
    @Shadow public double setting2;
    @Shadow @Final @Mutable public double defaultSetting2;
    @Shadow @Final @Mutable public String setting3Label;
    @Shadow @Final @Mutable public double min3;
    @Shadow @Final @Mutable public double max3;
    @Shadow @Final @Mutable public double step3;
    @Shadow public double setting3;
    @Shadow @Final @Mutable public double defaultSetting3;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void star$tune(String moduleName, String description, ClientModule.Category category, boolean enabled,
                           String settingLabel, double setting, double min, double originalMax, double step,
                           CallbackInfo ci) {
        if ("GTA Skin Changer".equals(moduleName) || "Player Changer".equals(moduleName)) {
            max = 5.0;
            setting2Label = "Outfit";
            min2 = 1.0; max2 = 6.0; step2 = 1.0;
            if (setting2 < min2 || setting2 > max2) setting2 = 1.0;
            defaultSetting2 = 1.0;
        } else if ("Hand View".equals(moduleName)) {
            setting2Label = "Scale";
            min2 = 0.55; max2 = 1.80; step2 = 0.05; setting2 = 1.0; defaultSetting2 = 1.0;
            setting3Label = "Horizontal spacing";
            min3 = 0.0; max3 = 0.60; step3 = 0.05; setting3 = 0.20; defaultSetting3 = 0.20;
        } else if ("Spotify HUD".equals(moduleName)) {
            setting2Label = "Compact mode";
            min2 = 0; max2 = 1; step2 = 1; setting2 = 0; defaultSetting2 = 0;
            setting3Label = "Show status";
            min3 = 0; max3 = 1; step3 = 1; setting3 = 1; defaultSetting3 = 1;
        } else if ("Region Map".equals(moduleName)) {
            setting2Label = "Show storage";
            min2 = 0; max2 = 1; step2 = 1; setting2 = 1; defaultSetting2 = 1;
            setting3Label = "Show players";
            min3 = 0; max3 = 1; step3 = 1; setting3 = 1; defaultSetting3 = 1;
        }
    }
}
