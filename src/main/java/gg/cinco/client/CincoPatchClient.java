package gg.cinco.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/** Extra lifecycle wiring for Star Client's render, movement, PvP, and quality systems. */
public final class CincoPatchClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        boolean changedModules=false;

        ClientModule legacyPlayerChanger=CincoClient.findModule("GTA Skin Changer");
        if(legacyPlayerChanger!=null)legacyPlayerChanger.name="Player Changer";
        if(CincoClient.MODULES.removeIf(m->m.name.equals("Hand View")))changedModules=true;

        if(CincoClient.findModule("Light Finder")==null){CincoClient.MODULES.add(new ClientModule("Light Finder","Highlight underground block-lit areas through walls using loaded chunk light data",ClientModule.Category.WORLD,false,"Range",48,16,96,8));changedModules=true;}
        if(CincoClient.findModule("Free Cam")==null){ClientModule freeCam=new ClientModule("Free Cam","Move only your local camera around loaded render-distance terrain",ClientModule.Category.MOVEMENT,false,"Camera speed",1.0,0.2,3.0,0.2);freeCam.keyCode=GLFW.GLFW_KEY_C;CincoClient.MODULES.add(freeCam);changedModules=true;}

        if(CincoClient.findModule("Animations")==null){CincoClient.MODULES.add(new ClientModule("Animations","Player-locked cosmetic world effects: auras, circles, trails, runes, sparks, comets, shockwaves and more",ClientModule.Category.RENDER,false,"Effect",1,1,14,1));changedModules=true;}
        if(CincoClient.findModule("Custom Accessory")==null){CincoClient.MODULES.add(new ClientModule("Custom Accessory","Solid player-locked accessories with custom colors: wings, halos, crowns, crystals, cape, blades and more",ClientModule.Category.RENDER,false,"Accessory",1,1,14,1));changedModules=true;}
        if(CincoClient.findModule("Spotify HUD")==null){CincoClient.MODULES.add(new ClientModule("Spotify HUD","Show the current Spotify desktop track as a draggable Star HUD widget",ClientModule.Category.RENDER,false,"Refresh seconds",2.5,1.0,10.0,0.5));changedModules=true;}
        if(CincoClient.findModule("Interactive Spotify")==null){CincoClient.MODULES.add(new ClientModule("Interactive Spotify","Add compact playback controls and interaction state to the Spotify HUD",ClientModule.Category.UTILITY,false,"Control size",1.0,0.7,2.0,0.1));changedModules=true;}
        if(CincoClient.findModule("Region Map")==null){CincoClient.MODULES.add(new ClientModule("Region Map","Top-down loaded-chunk region map with storage and nearby-player markers",ClientModule.Category.WORLD,false,"Chunk radius",4,2,8,1));changedModules=true;}

        // New combat / mace helpers.
        if(CincoClient.findModule("Aim Assist")==null){CincoClient.MODULES.add(new ClientModule("Aim Assist","Smoothly guide your view toward the closest player inside your configured FOV",ClientModule.Category.COMBAT,false,"Range",4.5,2.0,8.0,0.5));changedModules=true;}
        if(CincoClient.findModule("Silent Aim")==null){CincoClient.MODULES.add(new ClientModule("Silent Aim","Redirect attack interactions to the best nearby target without visibly snapping the camera",ClientModule.Category.COMBAT,false,"Range",4.5,2.0,8.0,0.5));changedModules=true;}
        if(CincoClient.findModule("Auto Sprint")==null){CincoClient.MODULES.add(new ClientModule("Auto Sprint","Automatically sprint while moving forward",ClientModule.Category.MOVEMENT,false,"Mode",1,1,1,1));changedModules=true;}
        if(CincoClient.findModule("Auto Mace")==null){CincoClient.MODULES.add(new ClientModule("Auto Mace","Select a hotbar mace while descending toward a nearby target",ClientModule.Category.COMBAT,false,"Target range",4.5,2.0,8.0,0.5));changedModules=true;}
        if(CincoClient.findModule("Breachswap")==null){CincoClient.MODULES.add(new ClientModule("Breachswap","Fast mace swap helper while attacking a nearby target",ClientModule.Category.COMBAT,false,"Range",4.5,2.0,8.0,0.5));changedModules=true;}
        if(CincoClient.findModule("Lunge Macro")==null){ClientModule m=new ClientModule("Lunge Macro","Forward lunge helper designed for mace approaches",ClientModule.Category.MOVEMENT,false,"Lunge strength",0.65,0.05,1.5,0.05);m.keyCode=GLFW.GLFW_KEY_R;CincoClient.MODULES.add(m);changedModules=true;}
        if(CincoClient.findModule("Auto Jump Reset")==null){CincoClient.MODULES.add(new ClientModule("Auto Jump Reset","Jump-reset helper while attacking at close range",ClientModule.Category.COMBAT,false,"Delay ticks",4,2,8,1));changedModules=true;}
        if(CincoClient.findModule("Shield Drain with Mace")==null){CincoClient.MODULES.add(new ClientModule("Shield Drain with Mace","Swap to a mace when the player under your crosshair is actively shielding",ClientModule.Category.COMBAT,false,"Range",4.5,2.0,6.0,0.5));changedModules=true;}

        // Folder pills: right-clicking these opens the grouped PvP folder screen through ModuleSettingsScreen.
        if(CincoClient.findModule("Crystal")==null){CincoClient.MODULES.add(new ClientModule("Crystal","Crystal PvP folder: Auto Crystal, anchors, totems, offhand and crystal practice tools",ClientModule.Category.COMBAT,false,"Folder",1,1,1,1));changedModules=true;}
        if(CincoClient.findModule("Mace")==null){CincoClient.MODULES.add(new ClientModule("Mace","Mace PvP folder: Auto Mace, Breachswap, Lunge, jump reset and shield-drain tools",ClientModule.Category.COMBAT,false,"Folder",1,1,1,1));changedModules=true;}

        if(CincoClient.findModule("ESP Style")==null){CincoClient.MODULES.add(new ClientModule("ESP Style","Choose the shared player, storage and light ESP presentation",ClientModule.Category.RENDER,true,"Style",1,1,4,1));changedModules=true;}
        if(CincoClient.findModule("Font Style")==null){CincoClient.MODULES.add(new ClientModule("Font Style","Choose Star Client's interface typeface profile",ClientModule.Category.SETTINGS,true,"Font",1,1,4,1));changedModules=true;}
        if(CincoClient.findModule("Performance Mode")==null){CincoClient.MODULES.add(new ClientModule("Performance Mode","Automatically reduce cosmetic render load when FPS falls below target",ClientModule.Category.SETTINGS,true,"FPS target",75,30,240,15));changedModules=true;}
        if(CincoClient.findModule("GUI Sounds")==null){CincoClient.MODULES.add(new ClientModule("GUI Sounds","Soft interface feedback sounds",ClientModule.Category.SETTINGS,false,"Volume",0.5,0,1,0.1));changedModules=true;}
        if(CincoClient.findModule("Client Profile")==null){CincoClient.MODULES.add(new ClientModule("Client Profile","Quick preset selector: Custom, PvP, DonutSMP, Visual, Performance",ClientModule.Category.SETTINGS,true,"Profile",1,1,5,1));changedModules=true;}

        ClientModule oldFullbright=CincoClient.findModule("Fullbright");
        if(oldFullbright!=null&&oldFullbright.max<20.0){int index=CincoClient.MODULES.indexOf(oldFullbright);ClientModule strong=new ClientModule("Fullbright","Force a bright lightmap and choose an extended client gamma level",ClientModule.Category.RENDER,oldFullbright.enabled,"Gamma",8.0,1.0,20.0,0.5);strong.keyCode=oldFullbright.keyCode;CincoClient.MODULES.set(index,strong);changedModules=true;}

        if(changedModules)ConfigStore.load();
        StarSettingsMigration.apply();applyNebulaLuxeDefaults();StorageEspConfig.load();CosmeticColorStore.load();

        ClientTickEvents.END_CLIENT_TICK.register(client->{
            EnhancedWorldScan.tick(client);FreeCamController.tick(client);FastXpController.tick(client);SpotifyHudController.tick(client);AdvancedModulesController.tick(client);
        });
        HudRenderCallback.EVENT.register((ctx,tickCounter)->{MinecraftClient client=MinecraftClient.getInstance();EnhancedEspHud.render(ctx,client);StarExtraHud.render(ctx,client);StarSplash.render(ctx,client);});

        WorldRenderEvents.BEFORE_TRANSLUCENT.register(AnimatedSkyRenderer::render);
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(StorageEspWorldRenderer::render);
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(CleanEspWorldRenderer::render);
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(StarEffectRenderer::render);
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(CustomAccessoryRenderer::render);
    }

    private static void applyNebulaLuxeDefaults(){ClientModule wheel=CincoClient.findModule("Color Wheel");if(wheel==null)return;boolean old=Math.abs(wheel.setting-28.0)<0.01&&Math.abs(wheel.setting2-0.90)<0.011&&Math.abs(wheel.setting3-1.0)<0.011;if(!old)return;wheel.setting=286.0;wheel.setting2=0.86;wheel.setting3=1.0;ConfigStore.save();}
}
