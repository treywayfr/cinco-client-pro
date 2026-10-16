package gg.cinco.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persistent settings for the grouped HUD module. */
public final class StarHudConfig {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE= FabricLoader.getInstance().getConfigDir().resolve("star-hud.json");
    public static boolean regionMap=true,statusBar=true,potionHud=true,moduleList=true;
    public static boolean regionPlayers=true,regionStorage=true,regionLegend=true;
    public static int regionRadius=4;
    public static boolean statusClock=true,statusFps=true,statusPing=true,statusCoords=false;
    public static int potionMax=8,moduleMax=14;
    public static double opacity=.72,animationSpeed=1.0;
    private StarHudConfig(){}
    public static void load(){try{if(!Files.exists(FILE))return;Data d=GSON.fromJson(Files.readString(FILE),Data.class);if(d!=null)d.apply();}catch(Exception ignored){}}
    public static void save(){try{Files.createDirectories(FILE.getParent());Files.writeString(FILE,GSON.toJson(new Data()));}catch(Exception ignored){}}
    public static void toggle(String id){switch(id){case"region"->regionMap=!regionMap;case"status"->statusBar=!statusBar;case"potions"->potionHud=!potionHud;case"modules"->moduleList=!moduleList;case"players"->regionPlayers=!regionPlayers;case"storage"->regionStorage=!regionStorage;case"legend"->regionLegend=!regionLegend;case"clock"->statusClock=!statusClock;case"fps"->statusFps=!statusFps;case"ping"->statusPing=!statusPing;case"coords"->statusCoords=!statusCoords;}save();}
    private static final class Data{
        boolean regionMap=StarHudConfig.regionMap,statusBar=StarHudConfig.statusBar,potionHud=StarHudConfig.potionHud,moduleList=StarHudConfig.moduleList;
        boolean regionPlayers=StarHudConfig.regionPlayers,regionStorage=StarHudConfig.regionStorage,regionLegend=StarHudConfig.regionLegend;
        int regionRadius=StarHudConfig.regionRadius;boolean statusClock=StarHudConfig.statusClock,statusFps=StarHudConfig.statusFps,statusPing=StarHudConfig.statusPing,statusCoords=StarHudConfig.statusCoords;
        int potionMax=StarHudConfig.potionMax,moduleMax=StarHudConfig.moduleMax;double opacity=StarHudConfig.opacity,animationSpeed=StarHudConfig.animationSpeed;
        void apply(){StarHudConfig.regionMap=regionMap;StarHudConfig.statusBar=statusBar;StarHudConfig.potionHud=potionHud;StarHudConfig.moduleList=moduleList;StarHudConfig.regionPlayers=regionPlayers;StarHudConfig.regionStorage=regionStorage;StarHudConfig.regionLegend=regionLegend;StarHudConfig.regionRadius=Math.max(2,Math.min(8,regionRadius));StarHudConfig.statusClock=statusClock;StarHudConfig.statusFps=statusFps;StarHudConfig.statusPing=statusPing;StarHudConfig.statusCoords=statusCoords;StarHudConfig.potionMax=Math.max(1,Math.min(16,potionMax));StarHudConfig.moduleMax=Math.max(3,Math.min(30,moduleMax));StarHudConfig.opacity=Math.max(.2,Math.min(1,opacity));StarHudConfig.animationSpeed=Math.max(.2,Math.min(3,animationSpeed));}
    }
}
