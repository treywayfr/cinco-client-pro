package gg.cinco.client.mixin;

import gg.cinco.client.ClientModule;
import gg.cinco.client.ModuleSettingsScreen;
import gg.cinco.client.StarFonts;
import gg.cinco.client.StarProfiles;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModuleSettingsScreen.class, remap = false)
public abstract class ModuleSettingsMixin {
    @Shadow @Final private ClientModule module;

    @Inject(method = "displaySetting", at = @At("HEAD"), cancellable = true, remap = false)
    private void star$friendlySettingNames(int which, double value, CallbackInfoReturnable<String> cir) {
        int selected = (int) Math.round(value);
        if ((module.name.equals("Player Changer") || module.name.equals("GTA Skin Changer")) && which == 0) {
            cir.setReturnValue(switch (selected) {
                case 2 -> "Spiky Cartoon Kid";
                case 3 -> "Anime Heroine";
                case 4 -> "Tung Tung Sahur";
                default -> "Franklin / GTA V";
            });
            return;
        }
        if (module.name.equals("Storage ESP") && which == 1) {
            cir.setReturnValue(switch (selected) {
                case 2 -> "Chests";
                case 3 -> "Barrels";
                case 4 -> "Shulkers";
                case 5 -> "Spawners";
                case 6 -> "Ender Chests";
                default -> "All Storage";
            });
            return;
        }
        if (module.name.equals("Font Style") && which == 0) {
            cir.setReturnValue(StarFonts.name(selected));
            return;
        }
        if (module.name.equals("Client Profile") && which == 0) {
            cir.setReturnValue(StarProfiles.name(selected));
        }
    }
}
