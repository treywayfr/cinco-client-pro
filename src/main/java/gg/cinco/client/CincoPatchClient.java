package gg.cinco.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/** Extra lifecycle wiring for Star Client's render, movement, and quality systems. */
public final class CincoPatchClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        boolean changedModules = false;

        ClientModule legacyPlayerChanger = CincoClient.findModule("GTA Skin Changer");
        if (legacyPlayerChanger != null) legacyPlayerChanger.name = "Player Changer";
        if (CincoClient.MODULES.removeIf(m -> m.name.equals("Hand View"))) changedModules = true;

        if (CincoClient.findModule("Light Finder") == null) {
            CincoClient.MODULES.add(new ClientModule("Light Finder","Highlight underground block-lit areas through walls using loaded chunk light data",ClientModule.Category.WORLD,false,"Range",48,16,96,8));
            changedModules = true;
        }
        if (CincoClient.findModule("Free Cam") == null) {
            ClientModule freeCam = new ClientModule("Free Cam","Move only your local camera around loaded render-distance terrain",ClientModule.Category.MOVEMENT,false,"Camera speed",1.0,0.2,3.0,0.2);
            freeCam.keyCode = GLFW.GLFW_KEY_C; CincoClient.MODULES.add(freeCam); changedModules = true;
        }
        if (CincoClient.findModule("Animations") == null) {
            CincoClient.MODULES.add(new ClientModule("Animations","Cosmetic world effects inspired by the showcase: auras, jump circles, trails, runes and sparks",ClientModule.Category.RENDER,false,"Effect",1,1,8,1)); changedModules=true;
        }
        if (CincoClient.findModule("Custom Accessory") == null) {
            CincoClient.MODULES.add(new ClientModule("Custom Accessory","Local-only animated accessories: wings, halo, crown, orbit stars, horns, rings and cape",ClientModule.Category.RENDER,false,"Accessory",1,1,8,1)); changedModules=true;
        }
        if (CincoClient.findModule("Spotify HUD") == null) {
            CincoClient.MODULES.add(new ClientModule("Spotify HUD","Show the current Spotify desktop track as a draggable Star HUD widget",ClientModule.Category.RENDER,false,"Refresh seconds",2.5,1.0,10.0,0.5)); changedModules=true;
        }
        if (CincoClient.findModule("Region Map") == null) {
            CincoClient.MODULES.add(new ClientModule("Region Map","Top-down loaded-chunk region map with storage and nearby-player markers",ClientModule.Category.WORLD,false,"Chunk radius",4,2,8,1)); changedModules=true;
        }
        if (CincoClient.findModule("ESP Style") == null) { CincoClient.MODULES.add(new ClientModule("ESP Style","Choose the shared player, storage and light ESP presentation",ClientModule.Category.RENDER,true,"Style",1,1,4,1)); changedModules=true; }
        if (CincoClient.findModule("Font Style") == null) { CincoClient.MODULES.add(new ClientModule("Font Style","Choose Star Client's interface typeface profile",ClientModule.Category.SETTINGS,true,"Font",1,1,4,1)); changedModules=true; }
        if (CincoClient.findModule("Performance Mode") == null) { CincoClient.MODULES.add(new ClientModule("Performance Mode","Automatically reduce cosmetic render load when FPS falls below target",ClientModule.Category.SETTINGS,true,"FPS target",75,30,240,15)); changedModules=true; }
        if (CincoClient.findModule("GUI Sounds") == null) { CincoClient.MODULES.add(new ClientModule("GUI Sounds","Soft interface feedback sounds",ClientModule.Category.SETTINGS,false,"Volume",0.5,0.0,1.0,0.1)); changedModules=true; }
        if (CincoClient.findModule("Client Profile") == null) { CincoClient.MODULES.add(new ClientModule("Client Profile","Quick preset selector: Custom, PvP, DonutSMP, Visual, Performance",ClientModule.Category.SETTINGS,true,"Profile",1,1,5,1)); changedModules=true; }

        ClientModule oldFullbright=CincoClient.findModule("Fullbright");
        if(oldFullbright!=null&&oldFullbright.max<20.0){int index=CincoClient.MODULES.indexOf(oldFullbright);ClientModule strong=new ClientModule("Fullbright","Force a bright lightmap and choose an extended client gamma level",ClientModule.Category.RENDER,oldFullbright.enabled,"Gamma",8.0,1.0,20.0,0.5);strong.keyCode=oldFullbright.keyCode;CincoClient.MODULES.set(index,strong);changedModules=true;}

        if(changedModules)ConfigStore.load();
        StarSettingsMigration.apply(); applyNebulaLuxeDefaults(); StorageEspConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(client->{EnhancedWorldScan.tick(client);FreeCamController.tick(client);FastXpController.tick(client);SpotifyHudController.tick(client);});
        HudRenderCallback.EVENT.register((ctx,tickCounter)->{MinecraftClient client=MinecraftClient.getInstance();EnhancedEspHud.render(ctx,client);StarExtraHud.render(ctx,client);StarSplash.render(ctx,client);});

        // 1.21.11 rendering is split into extraction/drawing stages. BEFORE_TRANSLUCENT provides the
        // shared world consumer used by all Star world-space visuals without legacy direct GL calls.
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(AnimatedSkyRenderer::render);
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(StorageEspWorldRenderer::render);
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(CleanEspWorldRenderer::render);
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(StarEffectRenderer::render);
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(CustomAccessoryRenderer::render);
    }

    private static void applyNebulaLuxeDefaults(){ClientModule wheel=CincoClient.findModule("Color Wheel");if(wheel==null)return;boolean old=Math.abs(wheel.setting-28.0)<0.01&&Math.abs(wheel.setting2-0.90)<0.011&&Math.abs(wheel.setting3-1.0)<0.011;if(!old)return;wheel.setting=286.0;wheel.setting2=0.86;wheel.setting3=1.0;ConfigStore.save();}
}
