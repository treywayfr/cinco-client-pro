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
    private String star$renameLegacyPlayerHandSetting(String original) { return "Model detail"; }

    @Inject(method = "displaySetting", at = @At("HEAD"), cancellable = true, remap = false)
    private void star$friendlySettingNames(int which, double value, CallbackInfoReturnable<String> cir) {
        int selected=(int)Math.round(value);
        boolean changer=module.name.equals("Player Changer")||module.name.equals("GTA Skin Changer");
        if(changer&&which==0){cir.setReturnValue(selected==2?"Tung Tung Suhr":"CJ");return;}
        if(changer&&which==1){cir.setReturnValue(((int)Math.round(value*100))+"%");return;}
        if(changer&&which==2){cir.setReturnValue(selected>=1?"HIGH":"STANDARD");return;}
        if(module.name.equals("FOV Changer")&&which==1){cir.setReturnValue(selected>=1?"ON":"OFF");return;}
        if(module.name.equals("FOV Changer")&&which==2){cir.setReturnValue(selected+"°");return;}
        if(module.name.equals("Auto Totem")&&which==1){cir.setReturnValue(selected>=1?"ON":"OFF");return;}
        if(module.name.equals("Auto Totem")&&which==2){cir.setReturnValue("SLOT "+Math.max(1,Math.min(9,selected)));return;}
        if(module.name.equals("Sky Changer")&&which==0){cir.setReturnValue(switch(selected){case 2->"Blue Expanse";case 3->"Violet Rift";case 4->"Crimson Eclipse";case 5->"Ice Cosmos";case 6->"Emerald Void";case 7->"Event Horizon";case 8->"Galactic Spectrum";default->"Nebula Abyss";});return;}
        if(module.name.equals("Animations")&&which==0){cir.setReturnValue(switch(selected){case 2->"Purple Aura";case 3->"Jump Circles";case 4->"Speed Trails";case 5->"Orbit Runes";case 6->"Lightning Sparks";case 7->"Footstep Rings";case 8->"Cosmic Pulse";case 9->"Spiral Aura";case 10->"Star Orbit";case 11->"Comet Trail";case 12->"Shockwave";case 13->"Galaxy Cage";case 14->"Crown Aura";case 15->"Meteor Halo";case 16->"Void Helix";case 17->"Heart Burst";case 18->"Solar Rings";case 19->"Rune Storm";case 20->"Aurora Veil";default->"Aura Rings";});return;}
        if(module.name.equals("Custom Accessory")&&which==0){cir.setReturnValue(switch(selected){case 2->"Demon Wings";case 3->"Solid Halo";case 4->"Star Crown";case 5->"Orbit Crystals";case 6->"Nebula Horns";case 7->"Energy Ring";case 8->"Nebula Cape";case 9->"Dragon Wings";case 10->"Butterfly Wings";case 11->"Shoulder Orbs";case 12->"Galaxy Crown";case 13->"Back Blades";case 14->"Floating Crystals";case 15->"Phoenix Wings";case 16->"Cat Ears";case 17->"Devil Tail";case 18->"Celestial Staff";case 19->"Orbit Backpack";case 20->"Crystal Antlers";default->"Angel Wings";});return;}
        if(module.name.equals("Auto Clicker")&&(which==1||which==2)){cir.setReturnValue(selected>=1?"ON":"OFF");return;}
        if((module.name.equals("Auto Mace")||module.name.equals("Breachswap")||module.name.equals("Lunge Macro")||module.name.equals("Auto Jump Reset")||module.name.equals("Shield Drain with Mace")||module.name.equals("Silent Aim")||module.name.equals("Interactive Spotify"))&&which==2){cir.setReturnValue(selected>=1?"ON":"OFF");return;}
        if(module.name.equals("Storage ESP")&&which==1){cir.setReturnValue(switch(selected){case 2->"Chests";case 3->"Barrels";case 4->"Shulkers";case 5->"Spawners";case 6->"Ender Chests";default->"All Storage";});return;}
        if(module.name.equals("ESP Style")&&which==0){cir.setReturnValue(switch(selected){case 2->"Neon";case 3->"Minimal";case 4->"Bold";default->"Clean";});return;}
        if(module.name.equals("Font Style")&&which==0){cir.setReturnValue(StarFonts.name(selected));return;}
        if(module.name.equals("Client Profile")&&which==0)cir.setReturnValue(StarProfiles.name(selected));
    }
}
