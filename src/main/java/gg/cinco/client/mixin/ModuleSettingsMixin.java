package gg.cinco.client.mixin;

import gg.cinco.client.ClientModule;
import gg.cinco.client.ModuleSettingsScreen;
import gg.cinco.client.StarFonts;
import gg.cinco.client.StarProfiles;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModuleSettingsScreen.class, remap = false)
public abstract class ModuleSettingsMixin {
    @Shadow @Final private ClientModule module;

    @ModifyConstant(method = "drawSlider", constant = @Constant(stringValue = "First-person hands"), remap = false)
    private String star$renameLegacyPlayerHandSetting(String original) {
        return "Model detail";
    }

    @Inject(method = "displaySetting", at = @At("HEAD"), cancellable = true, remap = false)
    private void star$friendlySettingNames(int which, double value, CallbackInfoReturnable<String> cir) {
        int selected = (int) Math.round(value);
        boolean changer = module.name.equals("Player Changer") || module.name.equals("GTA Skin Changer");
        if (changer && which == 0) {
            cir.setReturnValue(switch (selected) {
                case 2 -> "Spiky Cartoon Kid";
                case 3 -> "Anime Heroine";
                case 4 -> "Tung Tung Sahur";
                case 5 -> "CJ / San Andreas";
                default -> "Franklin / GTA V";
            });
            return;
        }
        if (changer && which == 1) {
            int character = Math.max(1, Math.min(5, (int)Math.round(module.setting)));
            if (character == 5) {
                cir.setReturnValue(switch (selected) {
                    case 2 -> "Grove Green";
                    case 3 -> "Black Tee";
                    case 4 -> "Red Street";
                    case 5 -> "Green Jacket";
                    case 6 -> "Gray Streetwear";
                    default -> "Classic White Tank";
                });
            } else if (character == 1) {
                cir.setReturnValue(switch (selected) {
                    case 2 -> "Black Street";
                    case 3 -> "Blue Jacket";
                    case 4 -> "Burgundy";
                    case 5 -> "Night Jacket";
                    case 6 -> "Cream Streetwear";
                    default -> "Grove Green";
                });
            } else cir.setReturnValue("Palette " + selected);
            return;
        }
        if (changer && which == 2) {
            cir.setReturnValue(selected >= 1 ? "HIGH" : "STANDARD");
            return;
        }
        if (module.name.equals("FOV Changer") && which == 1) {
            cir.setReturnValue(selected >= 1 ? "ON" : "OFF");
            return;
        }
        if (module.name.equals("FOV Changer") && which == 2) {
            cir.setReturnValue(selected + "°");
            return;
        }
        if (module.name.equals("Auto Totem") && which == 1) {
            cir.setReturnValue(selected >= 1 ? "ON" : "OFF");
            return;
        }
        if (module.name.equals("Auto Totem") && which == 2) {
            cir.setReturnValue("SLOT " + Math.max(1, Math.min(9, selected)));
            return;
        }
        if (module.name.equals("Sky Changer") && which == 0) {
            cir.setReturnValue(switch (selected) {
                case 2 -> "Purple Nebula";
                case 3 -> "Electric Violet";
                case 4 -> "Aurora Flow";
                case 5 -> "Crimson Storm";
                case 6 -> "Ice Plasma";
                case 7 -> "Emerald Flux";
                case 8 -> "Rainbow Void";
                default -> "Liquid Gold";
            });
            return;
        }
        if (module.name.equals("Animations") && which == 0) {
            cir.setReturnValue(switch (selected) {
                case 2 -> "Purple Aura";
                case 3 -> "Jump Circles";
                case 4 -> "Speed Trails";
                case 5 -> "Orbit Runes";
                case 6 -> "Lightning Sparks";
                case 7 -> "Footstep Rings";
                case 8 -> "Cosmic Pulse";
                default -> "Golden Aura";
            });
            return;
        }
        if (module.name.equals("Custom Accessory") && which == 0) {
            cir.setReturnValue(switch (selected) {
                case 2 -> "Demon Wings";
                case 3 -> "Halo";
                case 4 -> "Star Crown";
                case 5 -> "Orbit Stars";
                case 6 -> "Nebula Horns";
                case 7 -> "Energy Ring";
                case 8 -> "Nebula Cape";
                default -> "Angel Wings";
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
        if (module.name.equals("ESP Style") && which == 0) {
            cir.setReturnValue(switch (selected) {
                case 2 -> "Neon";
                case 3 -> "Minimal";
                case 4 -> "Bold";
                default -> "Clean";
            });
            return;
        }
        if (module.name.equals("Font Style") && which == 0) {
            cir.setReturnValue(StarFonts.name(selected));
            return;
        }
        if (module.name.equals("Client Profile") && which == 0) cir.setReturnValue(StarProfiles.name(selected));
    }
}
