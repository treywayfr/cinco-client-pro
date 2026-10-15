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
    @Shadow @Final @Mutable public String settingLabel;
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
                           String originalSettingLabel, double setting, double min, double originalMax, double step,
                           CallbackInfo ci) {
        if ("GTA Skin Changer".equals(moduleName) || "Player Changer".equals(moduleName)) {
            settingLabel = "Character";
            max = 5.0;
            setting2Label = "Outfit";
            min2 = 1.0; max2 = 6.0; step2 = 1.0;
            if (setting2 < min2 || setting2 > max2) setting2 = 1.0;
            defaultSetting2 = 1.0;
            setting3Label = "Model detail";
            min3 = 0.0; max3 = 1.0; step3 = 1.0; setting3 = 1.0; defaultSetting3 = 1.0;
        } else if ("FOV Changer".equals(moduleName)) {
            setting2Label = "Show hand";
            min2 = 0.0; max2 = 1.0; step2 = 1.0; setting2 = 1.0; defaultSetting2 = 1.0;
            setting3Label = "Hand / item rotation";
            min3 = -90.0; max3 = 90.0; step3 = 5.0; setting3 = 0.0; defaultSetting3 = 0.0;
        } else if ("Auto Totem".equals(moduleName)) {
            setting2Label = "Double hand";
            min2 = 0.0; max2 = 1.0; step2 = 1.0; setting2 = 0.0; defaultSetting2 = 0.0;
            setting3Label = "Double hand slot";
            min3 = 1.0; max3 = 9.0; step3 = 1.0; setting3 = 1.0; defaultSetting3 = 1.0;
        } else if ("Hand View".equals(moduleName)) {
            setting2Label = "Scale";
            min2 = 0.55; max2 = 1.80; step2 = 0.05; setting2 = 1.0; defaultSetting2 = 1.0;
            setting3Label = "Horizontal spacing";
            min3 = 0.0; max3 = 0.60; step3 = 0.05; setting3 = 0.20; defaultSetting3 = 0.20;
        } else if ("Sky Changer".equals(moduleName)) {
            settingLabel = "Deep space sky";
            max = 8.0;
            setting2Label = "Nebula motion";
            min2 = 0.05; max2 = 1.60; step2 = 0.05;
            if (setting2 < min2 || setting2 > max2) setting2 = 0.55;
            defaultSetting2 = 0.55;
            setting3Label = "Space intensity";
            min3 = 0.25; max3 = 1.0; step3 = 0.05;
            if (setting3 < min3 || setting3 > max3) setting3 = 0.90;
            defaultSetting3 = 0.90;
        } else if ("Animations".equals(moduleName)) {
            settingLabel = "Effect";
            max = 8.0;
            setting2Label = "Animation speed";
            min2 = 0.20; max2 = 3.0; step2 = 0.10; setting2 = 1.0; defaultSetting2 = 1.0;
            setting3Label = "Effect intensity";
            min3 = 0.20; max3 = 1.0; step3 = 0.05; setting3 = 0.80; defaultSetting3 = 0.80;
        } else if ("Custom Accessory".equals(moduleName)) {
            settingLabel = "Accessory";
            max = 8.0;
            setting2Label = "Accessory size";
            min2 = 0.60; max2 = 1.80; step2 = 0.05; setting2 = 1.0; defaultSetting2 = 1.0;
            setting3Label = "Smooth motion";
            min3 = 0.0; max3 = 2.00; step3 = 0.10; setting3 = 0.75; defaultSetting3 = 0.75;
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
