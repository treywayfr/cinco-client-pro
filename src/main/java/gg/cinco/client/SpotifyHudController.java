package gg.cinco.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Non-blocking desktop media-session bridge used by Star's interactive Spotify HUD. */
public final class SpotifyHudController {
    public enum Control { PREVIOUS, PLAY_PAUSE, NEXT }
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"star-spotify-hud");t.setDaemon(true);return t;});
    private static final AtomicBoolean polling=new AtomicBoolean();
    private static volatile long nextPoll;
    private static volatile String title="Waiting for Spotify",artist="",lyrics="";
    private static volatile boolean active,playing;
    private static volatile double positionSeconds,durationSeconds;
    private static boolean prevDown,playDown,nextDown;
    private SpotifyHudController(){}

    public static void tick(MinecraftClient client){
        ClientModule module=CincoClient.findModule("Spotify HUD");if(module==null||!module.enabled)return;
        handleKeys(client);
        long now=System.currentTimeMillis();if(now<nextPoll||!polling.compareAndSet(false,true))return;
        long refresh=(long)(Math.max(.5,Math.min(5.0,module.setting))*1000.0);nextPoll=now+refresh;
        WORKER.execute(()->{try{SessionData d=querySession();if(d==null||d.title.isBlank()){String legacy=queryWindowTitle();if(legacy==null||legacy.isBlank()){active=false;title="Waiting for Spotify";artist="";}else{active=true;parseLegacy(legacy);}}else{active=true;title=clean(d.title);artist=clean(d.artist);playing=d.playing;positionSeconds=Math.max(0,d.position);durationSeconds=Math.max(0,d.duration);}}catch(Throwable ignored){active=false;title="Spotify unavailable";artist="";}finally{polling.set(false);}});
    }

    private static void handleKeys(MinecraftClient client){if(client==null||client.getWindow()==null||client.currentScreen!=null)return;long w=client.getWindow().getHandle();boolean a=down(w,SpotifyHudConfig.previousKey),b=down(w,SpotifyHudConfig.playPauseKey),c=down(w,SpotifyHudConfig.nextKey);if(a&&!prevDown)control(Control.PREVIOUS);if(b&&!playDown)control(Control.PLAY_PAUSE);if(c&&!nextDown)control(Control.NEXT);prevDown=a;playDown=b;nextDown=c;}
    private static boolean down(long w,int key){return key>=0&&GLFW.glfwGetKey(w,key)==GLFW.GLFW_PRESS;}

    public static boolean active(){return active;} public static boolean playing(){return playing;}
    public static String title(){return title;} public static String artist(){return artist;} public static String lyrics(){return lyrics;}
    public static double positionSeconds(){return positionSeconds;} public static double durationSeconds(){return durationSeconds;}
    public static String track(){return artist.isBlank()?title:artist+" — "+title;} public static String hudText(){return"♫  "+track();}

    public static void control(Control control){WORKER.execute(()->{try{sendControl(control);nextPoll=0L;}catch(Throwable ignored){}});}
    public static void seek(double fraction){double f=Math.max(0,Math.min(1,fraction));double duration=durationSeconds;if(duration<=0)return;WORKER.execute(()->{try{seekWindows((long)(duration*f*10_000_000L));positionSeconds=duration*f;nextPoll=0L;}catch(Throwable ignored){}});}

    private static void sendControl(Control control)throws Exception{
        String os=System.getProperty("os.name","").toLowerCase(Locale.ROOT);Process p;
        if(os.contains("win")){
            String method=switch(control){case PREVIOUS->"TrySkipPreviousAsync";case PLAY_PAUSE->"TryTogglePlayPauseAsync";case NEXT->"TrySkipNextAsync";};
            String script=winPrelude()+"$s=$m.GetCurrentSession(); if($s){$a=$s."+method+"(); [System.WindowsRuntimeSystemExtensions]::AsTask($a).Wait() }";
            p=new ProcessBuilder("powershell","-NoProfile","-ExecutionPolicy","Bypass","-Command",script).redirectErrorStream(true).start();
        }else if(os.contains("mac")){String command=switch(control){case PREVIOUS->"previous track";case PLAY_PAUSE->"playpause";case NEXT->"next track";};p=new ProcessBuilder("osascript","-e","tell application \"Spotify\" to "+command).redirectErrorStream(true).start();}
        else{String command=switch(control){case PREVIOUS->"previous";case PLAY_PAUSE->"play-pause";case NEXT->"next";};p=new ProcessBuilder("playerctl","-p","spotify",command).redirectErrorStream(true).start();}
        if(!p.waitFor(1800,TimeUnit.MILLISECONDS))p.destroyForcibly();
    }

    private static void seekWindows(long ticks)throws Exception{if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win"))return;String script=winPrelude()+"$s=$m.GetCurrentSession(); if($s){$a=$s.TryChangePlaybackPositionAsync("+ticks+"); [System.WindowsRuntimeSystemExtensions]::AsTask($a).Wait() }";Process p=new ProcessBuilder("powershell","-NoProfile","-ExecutionPolicy","Bypass","-Command",script).redirectErrorStream(true).start();if(!p.waitFor(1800,TimeUnit.MILLISECONDS))p.destroyForcibly();}

    private static SessionData querySession()throws Exception{
        String os=System.getProperty("os.name","").toLowerCase(Locale.ROOT);if(!os.contains("win"))return null;
        String script=winPrelude()+"$s=$m.GetCurrentSession(); if(!$s){exit}; $a=$s.TryGetMediaPropertiesAsync(); $p=[System.WindowsRuntimeSystemExtensions]::AsTask($a).Result; $t=$s.GetTimelineProperties(); $pb=$s.GetPlaybackInfo(); [pscustomobject]@{title=$p.Title;artist=$p.Artist;position=[math]::Round($t.Position.TotalSeconds,3);duration=[math]::Round(($t.EndTime-$t.StartTime).TotalSeconds,3);playing=($pb.PlaybackStatus.ToString() -eq 'Playing')}|ConvertTo-Json -Compress";
        Process p=new ProcessBuilder("powershell","-NoProfile","-ExecutionPolicy","Bypass","-Command",script).redirectErrorStream(true).start();if(!p.waitFor(1800,TimeUnit.MILLISECONDS)){p.destroyForcibly();return null;}StringBuilder out=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream(),StandardCharsets.UTF_8))){String l;while((l=r.readLine())!=null)out.append(l);}if(out.isEmpty()||out.charAt(0)!='{')return null;JsonObject j=JsonParser.parseString(out.toString()).getAsJsonObject();return new SessionData(str(j,"title"),str(j,"artist"),num(j,"position"),num(j,"duration"),bool(j,"playing"));
    }
    private static String winPrelude(){return"Add-Type -AssemblyName System.Runtime.WindowsRuntime; $null=[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]; $a=[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync(); $m=[System.WindowsRuntimeSystemExtensions]::AsTask($a).Result; ";}
    private static String queryWindowTitle()throws Exception{String script="$p=Get-Process Spotify -ErrorAction SilentlyContinue | Where-Object {$_.MainWindowTitle -and $_.MainWindowTitle -notmatch '^Spotify( Premium)?$'} | Select-Object -First 1; if($p){$p.MainWindowTitle}";Process p=new ProcessBuilder("powershell","-NoProfile","-Command",script).redirectErrorStream(true).start();if(!p.waitFor(1300,TimeUnit.MILLISECONDS)){p.destroyForcibly();return null;}try(BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream(),StandardCharsets.UTF_8))){return r.readLine();}}
    private static void parseLegacy(String v){String s=clean(v);int dash=s.indexOf(" - ");if(dash>0){artist=s.substring(0,dash).trim();title=s.substring(dash+3).trim();}else title=s;}
    private static String str(JsonObject j,String k){return j.has(k)&&!j.get(k).isJsonNull()?j.get(k).getAsString():"";}private static double num(JsonObject j,String k){try{return j.get(k).getAsDouble();}catch(Exception e){return 0;}}private static boolean bool(JsonObject j,String k){try{return j.get(k).getAsBoolean();}catch(Exception e){return false;}}
    private static String clean(String value){if(value==null)return"";String s=value.replace('\t',' ').replaceAll("\\s+"," ").trim();return s.length()>90?s.substring(0,87)+"…":s;}
    private record SessionData(String title,String artist,double position,double duration,boolean playing){}
}
