package gg.cinco.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

/** Registers Star's replacement player-model and premium Locker detail passes. */
public final class StarModelClient implements ClientModInitializer {
    @Override public void onInitializeClient(){
        ClientModule skin=CincoClient.findModule("Player Changer");
        if(skin==null)skin=CincoClient.findModule("GTA Skin Changer");
        if(skin!=null){
            boolean changed=false;
            if(skin.setting<1||skin.setting>2){skin.setting=1;changed=true;}
            if(skin.setting2<.80||skin.setting2>1.25){skin.setting2=1.0;changed=true;}
            if(skin.setting3<0||skin.setting3>1){skin.setting3=1.0;changed=true;}
            if(changed)ConfigStore.save();
        }
        // Start the real GTASA-derived CJ mesh/atlas loader before the player opens Locker or F5.
        GtaCjImportedModel.bootstrap();
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(StarPlayerModelWorldRenderer::render);
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(PremiumCosmeticDetailRenderer::render);
    }
}
