package gg.cinco.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SpotifyHudConfig {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("star-spotify-hud.json");
    public static boolean albumCover=true,song=true,artist=true,lyrics=true,controls=true,progress=true;
    public static int lyricLines=2;
    public static int previousKey=GLFW.GLFW_KEY_LEFT_BRACKET,playPauseKey=GLFW.GLFW_KEY_BACKSLASH,nextKey=GLFW.GLFW_KEY_RIGHT_BRACKET;
    public static double opacity=.74;
    private SpotifyHudConfig(){}
    public static void load(){try{if(!Files.exists(FILE))return;Data d=GSON.fromJson(Files.readString(FILE),Data.class);if(d!=null)d.apply();}catch(Exception ignored){}}
    public static void save(){try{Files.createDirectories(FILE.getParent());Files.writeString(FILE,GSON.toJson(new Data()));}catch(Exception ignored){}}
    public static void toggle(String id){switch(id){case"cover"->albumCover=!albumCover;case"song"->song=!song;case"artist"->artist=!artist;case"lyrics"->lyrics=!lyrics;case"controls"->controls=!controls;case"progress"->progress=!progress;}save();}
    private static final class Data{
        boolean albumCover=SpotifyHudConfig.albumCover,song=SpotifyHudConfig.song,artist=SpotifyHudConfig.artist,lyrics=SpotifyHudConfig.lyrics,controls=SpotifyHudConfig.controls,progress=SpotifyHudConfig.progress;
        int lyricLines=SpotifyHudConfig.lyricLines,previousKey=SpotifyHudConfig.previousKey,playPauseKey=SpotifyHudConfig.playPauseKey,nextKey=SpotifyHudConfig.nextKey;double opacity=SpotifyHudConfig.opacity;
        void apply(){SpotifyHudConfig.albumCover=albumCover;SpotifyHudConfig.song=song;SpotifyHudConfig.artist=artist;SpotifyHudConfig.lyrics=lyrics;SpotifyHudConfig.controls=controls;SpotifyHudConfig.progress=progress;SpotifyHudConfig.lyricLines=Math.max(0,Math.min(6,lyricLines));SpotifyHudConfig.previousKey=previousKey;SpotifyHudConfig.playPauseKey=playPauseKey;SpotifyHudConfig.nextKey=nextKey;SpotifyHudConfig.opacity=Math.max(.2,Math.min(1,opacity));}
    }
}
