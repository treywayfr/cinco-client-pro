package gg.cinco.client;

import net.minecraft.client.MinecraftClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Non-blocking desktop Spotify metadata + media-control bridge. */
public final class SpotifyHudController {
    public enum Control { PREVIOUS, PLAY_PAUSE, NEXT }

    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"star-spotify-hud");t.setDaemon(true);return t;});
    private static final AtomicBoolean polling=new AtomicBoolean();
    private static volatile long nextPoll;
    private static volatile String track="Waiting for Spotify";
    private static volatile boolean active;

    private SpotifyHudController() {}

    public static void tick(MinecraftClient client){
        ClientModule module=CincoClient.findModule("Spotify HUD");if(module==null||!module.enabled)return;
        long now=System.currentTimeMillis();if(now<nextPoll||!polling.compareAndSet(false,true))return;
        long refresh=(long)(Math.max(1.0,Math.min(10.0,module.setting))*1000.0);nextPoll=now+refresh;
        WORKER.execute(()->{try{String found=queryNowPlaying();if(found==null||found.isBlank()){active=false;track="Waiting for Spotify";}else{active=true;track=clean(found);}}catch(Throwable ignored){active=false;track="Spotify unavailable";}finally{polling.set(false);}});
    }

    public static boolean active(){return active;} public static String track(){return track;} public static String hudText(){return"♫  "+track;}

    public static void control(Control control){
        WORKER.execute(()->{try{sendControl(control);nextPoll=0L;}catch(Throwable ignored){}});
    }

    private static void sendControl(Control control)throws Exception{
        String os=System.getProperty("os.name","").toLowerCase(Locale.ROOT);Process p;
        if(os.contains("win")){
            int vk=switch(control){case PREVIOUS->0xB1;case PLAY_PAUSE->0xB3;case NEXT->0xB0;};
            String script="Add-Type -TypeDefinition 'using System; using System.Runtime.InteropServices; public class StarMediaKey { [DllImport(\"user32.dll\")] public static extern void keybd_event(byte v, byte s, uint f, UIntPtr e); }'; "
                    +"[StarMediaKey]::keybd_event("+vk+",0,0,[UIntPtr]::Zero); Start-Sleep -Milliseconds 25; [StarMediaKey]::keybd_event("+vk+",0,2,[UIntPtr]::Zero)";
            p=new ProcessBuilder("powershell","-NoProfile","-Command",script).redirectErrorStream(true).start();
        }else if(os.contains("mac")){
            String command=switch(control){case PREVIOUS->"previous track";case PLAY_PAUSE->"playpause";case NEXT->"next track";};
            p=new ProcessBuilder("osascript","-e","tell application \"Spotify\" to "+command).redirectErrorStream(true).start();
        }else{
            String command=switch(control){case PREVIOUS->"previous";case PLAY_PAUSE->"play-pause";case NEXT->"next";};
            p=new ProcessBuilder("playerctl","-p","spotify",command).redirectErrorStream(true).start();
        }
        if(!p.waitFor(1600,TimeUnit.MILLISECONDS))p.destroyForcibly();
    }

    private static String queryNowPlaying()throws Exception{
        String os=System.getProperty("os.name","").toLowerCase(Locale.ROOT);Process process;
        if(os.contains("win")){
            String script="$p=Get-Process Spotify -ErrorAction SilentlyContinue | Where-Object {$_.MainWindowTitle -and $_.MainWindowTitle -notmatch '^Spotify( Premium)?$'} | Select-Object -First 1; if($p){$p.MainWindowTitle}";
            process=new ProcessBuilder("powershell","-NoProfile","-Command",script).redirectErrorStream(true).start();
        }else if(os.contains("mac")){
            String script="tell application \"Spotify\" to if player state is playing then return (artist of current track) & \" — \" & (name of current track)";
            process=new ProcessBuilder("osascript","-e",script).redirectErrorStream(true).start();
        }else process=new ProcessBuilder("sh","-c","playerctl -p spotify metadata --format '{{artist}} — {{title}}' 2>/dev/null").redirectErrorStream(true).start();
        if(!process.waitFor(1400,TimeUnit.MILLISECONDS)){process.destroyForcibly();return null;}
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){String line=reader.readLine();return line==null?null:line.trim();}
    }

    private static String clean(String value){String s=value.replace('\t',' ').replaceAll("\\s+"," ").trim();if(s.length()>72)s=s.substring(0,69)+"…";return s;}
}
