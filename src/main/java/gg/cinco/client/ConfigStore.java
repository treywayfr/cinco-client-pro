package gg.cinco.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("cinco-client.json");
    private ConfigStore() {}
    private static final class State { boolean enabled; double setting; State(boolean e,double s){enabled=e;setting=s;} }

    public static void save() {
        try {
            Map<String,State> data=new LinkedHashMap<>();
            for(ClientModule m:CincoClient.MODULES)data.put(m.name,new State(m.enabled,m.setting));
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE,GSON.toJson(data));
        } catch(IOException ignored) { }
    }
    public static void load() {
        if(!Files.exists(FILE))return;
        try {
            Type type=new TypeToken<Map<String,State>>(){}.getType();
            Map<String,State> data=GSON.fromJson(Files.readString(FILE),type);
            if(data==null)return;
            for(ClientModule m:CincoClient.MODULES){State s=data.get(m.name);if(s!=null){m.enabled=s.enabled;m.setting=Math.max(m.min,Math.min(m.max,s.setting));}}
            ClientModule sprint=CincoClient.findModule("Sprint Assist");if(sprint!=null)CincoClient.sprintAssistEnabled=sprint.enabled;
            ClientModule hud=CincoClient.findModule("Client HUD");if(hud!=null)CincoClient.hudEnabled=hud.enabled;
        }catch(Exception ignored){}
    }
}
