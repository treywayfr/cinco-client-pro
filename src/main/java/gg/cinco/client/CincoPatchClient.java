package gg.cinco.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public final class CincoPatchClient implements ClientModInitializer {
    @Override public void onInitializeClient(){boolean changed=false;
        ClientModule legacy=CincoClient.findModule("GTA Skin Changer");if(legacy!=null){legacy.name="Player Changer";changed=true;}
        if(CincoClient.MODULES.removeIf(m->m.name.equals("Hand View")||m.name.equals("Base Finder")||m.name.equals("Sus Chunk Finder")||m.name.equals("Chunk Density")||m.name.equals("Spawner Finder")||m.name.equals("Interactive Spotify")||m.name.equals("Region Map")))changed=true;
        if(CincoClient.findModule("Sus Chunk")==null){CincoClient.MODULES.add(new ClientModule("Sus Chunk","Detect player-activity signals in already-loaded chunks",ClientModule.Category.WORLD,false,"Chunk radius",3,1,6,1));changed=true;}
        if(CincoClient.findModule("Spawner Notifier")==null){CincoClient.MODULES.add(new ClientModule("Spawner Notifier","Show a SPAWNERS! popup with coordinates",ClientModule.Category.WORLD,false,"Chunk radius",3,1,6,1));changed=true;}
        if(CincoClient.findModule("Light Finder")==null){CincoClient.MODULES.add(new ClientModule("Light Finder","Highlight underground block-lit areas through walls",ClientModule.Category.WORLD,false,"Range",48,16,96,8));changed=true;}
        if(CincoClient.findModule("Free Cam")==null){ClientModule m=new ClientModule("Free Cam","Move only your local camera around loaded terrain",ClientModule.Category.MOVEMENT,false,"Camera speed",1,.2,3,.2);m.keyCode=GLFW.GLFW_KEY_C;CincoClient.MODULES.add(m);changed=true;}
        changed|=upgradeLocker("Animations","Locker cosmetic effects","Effect");changed|=upgradeLocker("Custom Accessory","Locker accessories","Accessory");
        if(CincoClient.findModule("HUD")==null){CincoClient.MODULES.add(new ClientModule("HUD","Region Map, status bar, potion HUD and active module list",ClientModule.Category.RENDER,true,"HUD scale",1,.5,2.5,.1));changed=true;}
        if(CincoClient.findModule("Spotify HUD")==null){CincoClient.MODULES.add(new ClientModule("Spotify HUD","Interactive Spotify HUD with controls, timeline and display options",ClientModule.Category.RENDER,false,"Refresh seconds",1,.5,5,.5));changed=true;}
        if(CincoClient.findModule("Aim Assist")==null){CincoClient.MODULES.add(new ClientModule("Aim Assist","Smoothly guide your view toward the closest player",ClientModule.Category.COMBAT,false,"Range",4.5,2,8,.5));changed=true;}
        if(CincoClient.findModule("Silent Aim")==null){CincoClient.MODULES.add(new ClientModule("Silent Aim","Redirect attack interactions to the best nearby target",ClientModule.Category.COMBAT,false,"Range",4.5,2,8,.5));changed=true;}
        if(CincoClient.findModule("Auto Sprint")==null){CincoClient.MODULES.add(new ClientModule("Auto Sprint","Automatically sprint while moving forward",ClientModule.Category.MOVEMENT,false,"Mode",1,1,1,1));changed=true;}
        if(CincoClient.findModule("Auto Mace")==null){CincoClient.MODULES.add(new ClientModule("Auto Mace","Select a hotbar mace while descending toward a nearby target",ClientModule.Category.COMBAT,false,"Target range",4.5,2,8,.5));changed=true;}
        if(CincoClient.findModule("Breachswap")==null){CincoClient.MODULES.add(new ClientModule("Breachswap","Fast mace swap helper while attacking",ClientModule.Category.COMBAT,false,"Range",4.5,2,8,.5));changed=true;}
        if(CincoClient.findModule("Lunge Macro")==null){ClientModule m=new ClientModule("Lunge Macro","Forward lunge helper",ClientModule.Category.MOVEMENT,false,"Lunge strength",.65,.05,1.5,.05);m.keyCode=GLFW.GLFW_KEY_R;CincoClient.MODULES.add(m);changed=true;}
        if(CincoClient.findModule("Auto Jump Reset")==null){CincoClient.MODULES.add(new ClientModule("Auto Jump Reset","Jump-reset helper while attacking",ClientModule.Category.COMBAT,false,"Delay ticks",4,2,8,1));changed=true;}
        if(CincoClient.findModule("Shield Drain with Mace")==null){CincoClient.MODULES.add(new ClientModule("Shield Drain with Mace","Swap to mace against shields",ClientModule.Category.COMBAT,false,"Range",4.5,2,6,.5));changed=true;}
        CincoClient.MODULES.removeIf(m->m.name.equals("Crystal")||m.name.equals("Mace"));
        if(CincoClient.findModule("ESP Style")==null){CincoClient.MODULES.add(new ClientModule("ESP Style","Choose shared ESP presentation",ClientModule.Category.RENDER,true,"Style",1,1,4,1));changed=true;}
        ClientModule font=CincoClient.findModule("Font Style");if(font==null){CincoClient.MODULES.add(new ClientModule("Font Style","Minecraft, Clean Sans, Classic Minecraft, Compact UI, Minecraft TEN",ClientModule.Category.SETTINGS,true,"Font",1,1,5,1));changed=true;}else if(font.max<5){int i=CincoClient.MODULES.indexOf(font);ClientModule n=new ClientModule("Font Style","Minecraft, Clean Sans, Classic Minecraft, Compact UI, Minecraft TEN",ClientModule.Category.SETTINGS,font.enabled,"Font",Math.min(5,font.setting),1,5,1);n.keyCode=font.keyCode;CincoClient.MODULES.set(i,n);changed=true;}
        if(CincoClient.findModule("Performance Mode")==null){CincoClient.MODULES.add(new ClientModule("Performance Mode","Reduce cosmetic render load below target FPS",ClientModule.Category.SETTINGS,true,"FPS target",75,30,240,15));changed=true;}
        if(CincoClient.findModule("GUI Sounds")==null){CincoClient.MODULES.add(new ClientModule("GUI Sounds","Soft interface feedback sounds",ClientModule.Category.SETTINGS,false,"Volume",.5,0,1,.1));changed=true;}
        if(CincoClient.findModule("Client Profile")==null){CincoClient.MODULES.add(new ClientModule("Client Profile","Quick preset selector",ClientModule.Category.SETTINGS,true,"Profile",1,1,5,1));changed=true;}
        ClientModule fb=CincoClient.findModule("Fullbright");if(fb!=null&&fb.max<20){int i=CincoClient.MODULES.indexOf(fb);ClientModule n=new ClientModule("Fullbright","Force a bright client gamma level",ClientModule.Category.RENDER,fb.enabled,"Gamma",8,1,20,.5);n.keyCode=fb.keyCode;CincoClient.MODULES.set(i,n);changed=true;}
        if(changed)ConfigStore.load();StarSettingsMigration.apply();applyNebulaLuxeDefaults();StorageEspConfig.load();CosmeticColorStore.load();LockerCosmeticStore.load();SusChunkConfig.load();StarHudConfig.load();SpotifyHudConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(c->{EnhancedWorldScan.tick(c);FreeCamController.tick(c);FastXpController.tick(c);SpotifyHudController.tick(c);AdvancedModulesController.tick(c);StarWorldFeatures.tick(c);});
        HudRenderCallback.EVENT.register((ctx,t)->{MinecraftClient c=MinecraftClient.getInstance();EnhancedEspHud.render(ctx,c);StarExtraHud.render(ctx,c);StarSplash.render(ctx,c);StarWorldFeatures.renderHud(ctx,c);});
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(AnimatedSkyRenderer::render);WorldRenderEvents.BEFORE_TRANSLUCENT.register(StorageEspWorldRenderer::render);WorldRenderEvents.BEFORE_TRANSLUCENT.register(CleanEspWorldRenderer::render);WorldRenderEvents.BEFORE_TRANSLUCENT.register(StarEffectRenderer::render);WorldRenderEvents.BEFORE_TRANSLUCENT.register(CustomAccessoryRenderer::render);WorldRenderEvents.BEFORE_TRANSLUCENT.register(StarWorldFeatures::renderWorld);
    }
    private static boolean upgradeLocker(String name,String desc,String label){ClientModule old=CincoClient.findModule(name);if(old==null){CincoClient.MODULES.add(new ClientModule(name,desc,ClientModule.Category.RENDER,false,label,1,1,20,1));return true;}if(old.max>=20)return false;int i=CincoClient.MODULES.indexOf(old);ClientModule n=new ClientModule(name,desc,ClientModule.Category.RENDER,old.enabled,label,Math.min(20,old.setting),1,20,1);n.keyCode=old.keyCode;n.setting2=old.setting2;n.setting3=old.setting3;CincoClient.MODULES.set(i,n);return true;}
    private static void applyNebulaLuxeDefaults(){ClientModule wheel=CincoClient.findModule("Color Wheel");if(wheel==null)return;boolean old=Math.abs(wheel.setting-28)<.01&&Math.abs(wheel.setting2-.90)<.011&&Math.abs(wheel.setting3-1)<.011;if(!old)return;wheel.setting=286;wheel.setting2=.86;wheel.setting3=1;ConfigStore.save();}
}
