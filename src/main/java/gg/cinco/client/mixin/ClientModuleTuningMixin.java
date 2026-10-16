package gg.cinco.client.mixin;

import gg.cinco.client.ClientModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Post-constructor tuning for newer Star modules without breaking old config files. */
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
            settingLabel = "Character"; max = 2.0;
            setting2Label = "Model scale"; min2 = 0.80; max2 = 1.25; step2 = 0.05;
            if (setting2 < min2 || setting2 > max2) setting2 = 1.0; defaultSetting2 = 1.0;
            setting3Label = "Model detail"; min3 = 0.0; max3 = 1.0; step3 = 1.0;
            if (setting3 < 0 || setting3 > 1) setting3 = 1.0; defaultSetting3 = 1.0;
        } else if ("FOV Changer".equals(moduleName)) {
            setting2Label = "Show hand"; min2 = 0.0; max2 = 1.0; step2 = 1.0; setting2 = 1.0; defaultSetting2 = 1.0;
            setting3Label = "Hand / item rotation"; min3 = -90.0; max3 = 90.0; step3 = 5.0; setting3 = 0.0; defaultSetting3 = 0.0;
        } else if ("Auto Totem".equals(moduleName)) {
            setting2Label = "Double hand"; min2 = 0.0; max2 = 1.0; step2 = 1.0; setting2 = 0.0; defaultSetting2 = 0.0;
            setting3Label = "Double hand slot"; min3 = 1.0; max3 = 9.0; step3 = 1.0; setting3 = 1.0; defaultSetting3 = 1.0;
        } else if ("Hand View".equals(moduleName)) {
            setting2Label = "Scale"; min2 = 0.55; max2 = 1.80; step2 = 0.05; setting2 = 1.0; defaultSetting2 = 1.0;
            setting3Label = "Horizontal spacing"; min3 = 0.0; max3 = 0.60; step3 = 0.05; setting3 = 0.20; defaultSetting3 = 0.20;
        } else if ("Sky Changer".equals(moduleName)) {
            settingLabel = "Deep space sky"; max = 8.0;
            setting2Label = "Nebula motion"; min2 = 0.05; max2 = 1.60; step2 = 0.05;
            if (setting2 < min2 || setting2 > max2) setting2 = 0.55; defaultSetting2 = 0.55;
            setting3Label = "Space intensity"; min3 = 0.25; max3 = 1.0; step3 = 0.05;
            if (setting3 < min3 || setting3 > max3) setting3 = 0.90; defaultSetting3 = 0.90;
        } else if ("Animations".equals(moduleName)) {
            settingLabel = "Effect"; max = 20.0;
            setting2Label = "Animation speed"; min2 = 0.15; max2 = 3.0; step2 = 0.10;
            if (setting2 < min2 || setting2 > max2) setting2 = 1.0; defaultSetting2 = 1.0;
            setting3Label = "Effect intensity"; min3 = 0.15; max3 = 1.0; step3 = 0.05;
            if (setting3 < min3 || setting3 > max3) setting3 = 0.80; defaultSetting3 = 0.80;
        } else if ("Custom Accessory".equals(moduleName)) {
            settingLabel = "Accessory"; max = 20.0;
            setting2Label = "Accessory size"; min2 = 0.60; max2 = 1.80; step2 = 0.05;
            if (setting2 < min2 || setting2 > max2) setting2 = 1.0; defaultSetting2 = 1.0;
            setting3Label = "Smooth motion"; min3 = 0.0; max3 = 2.50; step3 = 0.10;
            if (setting3 < min3 || setting3 > max3) setting3 = 0.80; defaultSetting3 = 0.80;
        } else if ("Auto Clicker".equals(moduleName)) {
            settingLabel = "Clicks / second"; max = 20.0;
            setting2Label = "Left click"; min2 = 0; max2 = 1; step2 = 1; setting2 = 1; defaultSetting2 = 1;
            setting3Label = "Right click"; min3 = 0; max3 = 1; step3 = 1; setting3 = 0; defaultSetting3 = 0;
        } else if ("Aim Assist".equals(moduleName)) {
            settingLabel = "Range"; max = 8.0;
            setting2Label = "Strength"; min2 = 0.05; max2 = 1.0; step2 = 0.05; setting2 = 0.28; defaultSetting2 = 0.28;
            setting3Label = "FOV"; min3 = 10; max3 = 180; step3 = 5; setting3 = 70; defaultSetting3 = 70;
        } else if ("Silent Aim".equals(moduleName)) {
            settingLabel = "Range"; max = 8.0;
            setting2Label = "FOV"; min2 = 10; max2 = 180; step2 = 5; setting2 = 60; defaultSetting2 = 60;
            setting3Label = "Only while attacking"; min3 = 0; max3 = 1; step3 = 1; setting3 = 1; defaultSetting3 = 1;
        } else if ("Auto Mace".equals(moduleName)) {
            settingLabel = "Target range"; max = 8.0;
            setting2Label = "Min fall speed"; min2 = 0.05; max2 = 1.5; step2 = 0.05; setting2 = 0.20; defaultSetting2 = 0.20;
            setting3Label = "Restore slot"; min3 = 0; max3 = 1; step3 = 1; setting3 = 1; defaultSetting3 = 1;
        } else if ("Breachswap".equals(moduleName)) {
            settingLabel = "Range"; max = 8.0;
            setting2Label = "Switch delay"; min2 = 0; max2 = 10; step2 = 1; setting2 = 1; defaultSetting2 = 1;
            setting3Label = "Restore slot"; min3 = 0; max3 = 1; step3 = 1; setting3 = 1; defaultSetting3 = 1;
        } else if ("Lunge Macro".equals(moduleName)) {
            settingLabel = "Lunge strength"; max = 1.5;
            setting2Label = "Vertical boost"; min2 = 0; max2 = 1.0; step2 = 0.05; setting2 = 0.15; defaultSetting2 = 0.15;
            setting3Label = "Only with mace"; min3 = 0; max3 = 1; step3 = 1; setting3 = 1; defaultSetting3 = 1;
        } else if ("Auto Jump Reset".equals(moduleName)) {
            settingLabel = "Delay ticks"; max = 8.0;
            setting2Label = "Only grounded"; min2 = 0; max2 = 1; step2 = 1; setting2 = 1; defaultSetting2 = 1;
            setting3Label = "Require target"; min3 = 0; max3 = 1; step3 = 1; setting3 = 0; defaultSetting3 = 0;
        } else if ("Shield Drain with Mace".equals(moduleName)) {
            settingLabel = "Range"; max = 6.0;
            setting2Label = "Switch delay"; min2 = 0; max2 = 8; step2 = 1; setting2 = 1; defaultSetting2 = 1;
            setting3Label = "Restore slot"; min3 = 0; max3 = 1; step3 = 1; setting3 = 1; defaultSetting3 = 1;
        } else if ("Spotify HUD".equals(moduleName)) {
            setting2Label = "Compact mode"; min2 = 0; max2 = 1; step2 = 1; setting2 = 0; defaultSetting2 = 0;
            setting3Label = "Show status"; min3 = 0; max3 = 1; step3 = 1; setting3 = 1; defaultSetting3 = 1;
        } else if ("Interactive Spotify".equals(moduleName)) {
            settingLabel = "Control size"; max = 2.0;
            setting2Label = "Show controls"; min2 = 0; max2 = 1; step2 = 1; setting2 = 1; defaultSetting2 = 1;
            setting3Label = "Auto hide"; min3 = 0; max3 = 1; step3 = 1; setting3 = 1; defaultSetting3 = 1;
        } else if ("Region Map".equals(moduleName)) {
            setting2Label = "Show storage"; min2 = 0; max2 = 1; step2 = 1; setting2 = 1; defaultSetting2 = 1;
            setting3Label = "Show players"; min3 = 0; max3 = 1; step3 = 1; setting3 = 1; defaultSetting3 = 1;
        }
    }
}
