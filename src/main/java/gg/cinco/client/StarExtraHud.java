package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Polished draggable glass HUD suite. Open chat to edit. */
public final class StarExtraHud {
    private static String dragging;
    private static double dragOffsetX,dragOffsetY;
    private static boolean lastLeft;
    private static final List<Hit> hits=new ArrayList<>();
    private record Hit(String id,ClientModule module,StarHudLayoutStore.State state,int x,int y,int w,int h){}
    private StarExtraHud(){}

    public static void render(DrawContext ctx,MinecraftClient client){
        if(client==null||client.player==null||client.world==null||!CincoClient.hudEnabled)return;
        boolean edit=client.currentScreen instanceof ChatScreen;hits.clear();
        ClientModule hud=CincoClient.findModule("HUD");
        if(hud!=null&&hud.enabled){
            if(StarHudConfig.statusBar)statusBar(ctx,client,edit);
            if(StarHudConfig.regionMap)regionMap(ctx,client,edit);
            if(StarHudConfig.potionHud)potionHud(ctx,client,edit);
            if(StarHudConfig.moduleList)moduleList(ctx,client,edit);
        }
        spotify(ctx,client,edit);
        if(edit)handleEditor(client);else if(dragging!=null){dragging=null;StarHudLayoutStore.save();}
    }

    private static void panel(DrawContext c,int w,int h,boolean edit){
        int alpha=(int)(StarHudConfig.opacity*205);int bg=(alpha<<24)|0x080711;
        CincoDraw.rounded(c,-2,-2,w+2,h+2,10,0x221F073B);
        CincoDraw.bubble(c,0,0,w,h,9,edit?0xD0B44DFF:0x5F6F3E9B,bg);
        c.fill(8,3,w-8,4,0x669D42FF);
        c.fill(8,h-4,w-8,h-3,0x443A1F64);
    }
    private static void transform(DrawContext c,StarHudLayoutStore.State s){c.getMatrices().pushMatrix();c.getMatrices().translate(s.x,s.y);c.getMatrices().scale(s.scale,s.scale);}

    private static void statusBar(DrawContext c,MinecraftClient client,boolean edit){
        StarHudLayoutStore.State s=StarHudLayoutStore.get("status_bar",Math.max(8,client.getWindow().getScaledWidth()/2-120),8);s.scale=clampScale(s.scale);
        int bw=242,bh=24,rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);transform(c,s);panel(c,bw,bh,edit);
        c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("✦",true),14,7,0xFFFFA54F);
        int x=30;
        if(StarHudConfig.statusClock){String t=LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));tag(c,client,x,"TIME",t);x+=69;}
        if(StarHudConfig.statusFps){tag(c,client,x,"FPS",Integer.toString(client.getCurrentFps()));x+=59;}
        if(StarHudConfig.statusPing){tag(c,client,x,"PING",ping(client)+"ms");x+=66;}
        if(StarHudConfig.statusCoords)c.drawText(client.textRenderer,StarFonts.text((int)client.player.getX()+", "+(int)client.player.getZ(),true),x,7,0xFFF3ECF8,false);
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("status_bar",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }
    private static void tag(DrawContext c,MinecraftClient mc,int x,String k,String v){c.drawText(mc.textRenderer,StarFonts.text(k,false),x,7,0xFF9587A6,false);c.drawText(mc.textRenderer,StarFonts.text(v,true),x+mc.textRenderer.getWidth(k)+5,7,0xFFF4EFF8,false);}

    private static void regionMap(DrawContext c,MinecraftClient client,boolean edit){
        StarHudLayoutStore.State s=StarHudLayoutStore.get("region_map",10,58);s.scale=clampScale(s.scale);
        int bw=128,bh=112,rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);
        int pcx=client.player.getBlockX()>>4,pcz=client.player.getBlockZ()>>4,rx=Math.floorDiv(pcx,32),rz=Math.floorDiv(pcz,32);
        transform(c,s);panel(c,bw,bh,edit);
        c.drawText(client.textRenderer,StarFonts.text("REGION",true),8,8,0xFFF6EFFF,false);
        c.drawText(client.textRenderer,StarFonts.text("R "+rx+", "+rz+"  "+quadrant(client.player.getBlockX(),client.player.getBlockZ()),false),8,21,0xFF9F91AC,false);
        int cols=7,rows=4,cell=15,mx=8,my=38;
        for(int z=0;z<rows;z++)for(int x=0;x<cols;x++){
            int rrX=rx+x-3,rrZ=rz+z-2,xx=mx+x*cell,yy=my+z*cell,col=regionColor(rrX,rrZ);
            CincoDraw.rounded(c,xx,yy,xx+12,yy+12,3,col);
            if(rrX==rx&&rrZ==rz){c.fill(xx,yy,xx+12,yy+2,0xFFFFFFFF);c.fill(xx,yy+10,xx+12,yy+12,0xFFFFFFFF);c.fill(xx,yy,xx+2,yy+12,0xFFFFFFFF);c.fill(xx+10,yy,xx+12,yy+12,0xFFFFFFFF);}
        }
        c.drawText(client.textRenderer,StarFonts.text("Chunk "+pcx+", "+pcz,false),8,99,0xFFC4B7CF,false);
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("region_map",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }
    private static int regionColor(int x,int z){int n=Math.floorMod(x*31+z*17,6);return switch(n){case 0->0xFF6A4FDB;case 1->0xFF3BB7A0;case 2->0xFF6DC66C;case 3->0xFFD7B34E;case 4->0xFFE27838;default->0xFF3F80BD;};}
    private static String quadrant(int x,int z){if(x>=0&&z<0)return"NE";if(x<0&&z<0)return"NW";if(x>=0)return"SE";return"SW";}

    private static void potionHud(DrawContext c,MinecraftClient client,boolean edit){
        List<StatusEffectInstance> list=new ArrayList<>(client.player.getStatusEffects());list.sort(Comparator.comparingInt(StatusEffectInstance::getDuration));
        int count=Math.min(StarHudConfig.potionMax,list.size()),bh=Math.max(28,22+count*16),bw=142;
        StarHudLayoutStore.State s=StarHudLayoutStore.get("potion_hud",10,184);s.scale=clampScale(s.scale);int rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);
        transform(c,s);panel(c,bw,bh,edit);c.drawText(client.textRenderer,StarFonts.text("EFFECTS",true),8,7,0xFFF2EAF8,false);
        for(int i=0;i<count;i++){StatusEffectInstance e=list.get(i);int yy=22+i*16;String name=e.getEffectType().value().getName().getString(),dur=time(e.getDuration()/20);c.drawText(client.textRenderer,StarFonts.text(trim(name,16),false),8,yy,0xFFE9E2ED,false);c.drawText(client.textRenderer,StarFonts.text(dur,true),bw-8-client.textRenderer.getWidth(dur),yy,0xFFC793FF,false);}
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("potion_hud",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }

    private static void moduleList(DrawContext c,MinecraftClient client,boolean edit){
        List<ClientModule> list=new ArrayList<>();for(ClientModule m:CincoClient.MODULES)if(m.enabled&&!hiddenHudModule(m))list.add(m);
        list.sort(Comparator.comparingInt((ClientModule m)->client.textRenderer.getWidth(m.name)).reversed());int count=Math.min(StarHudConfig.moduleMax,list.size());
        int bw=172,bh=Math.max(28,18+count*14);StarHudLayoutStore.State s=StarHudLayoutStore.get("module_list",Math.max(4,client.getWindow().getScaledWidth()-184),62);s.scale=clampScale(s.scale);int rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);
        transform(c,s);
        if(edit)panel(c,bw,bh,true);
        for(int i=0;i<count;i++){
            ClientModule m=list.get(i);int yy=4+i*14,col=gradient(i,count);int tw=client.textRenderer.getWidth(m.name),x=bw-8-tw;
            CincoDraw.rounded(c,x-8,yy-1,bw-4,yy+12,5,0x69080711);
            c.fill(bw-4,yy,bw-2,yy+10,col);
            c.drawText(client.textRenderer,StarFonts.text(m.name,true),x,yy,col,false);
        }
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("module_list",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }
    private static boolean hiddenHudModule(ClientModule m){return m.name.equals("HUD")||m.name.equals("Font Style")||m.name.equals("Client Profile")||m.name.equals("Animations")||m.name.equals("Custom Accessory")||m.name.equals("Player Changer")||m.name.equals("GTA Skin Changer");}
    private static int gradient(int i,int n){double t=n<=1?0:i/(double)(n-1);int r=(int)(255-35*t),g=(int)(171-80*t),b=255;return 0xFF000000|(r<<16)|(g<<8)|b;}

    private static void spotify(DrawContext c,MinecraftClient client,boolean edit){
        ClientModule m=CincoClient.findModule("Spotify HUD");if(m==null||!m.enabled)return;
        StarHudLayoutStore.State s=StarHudLayoutStore.get("spotify_hud",Math.max(8,client.getWindow().getScaledWidth()-286),10);s.scale=clampScale(s.scale);
        int bw=276,bh=94+Math.max(0,SpotifyHudConfig.lyrics?SpotifyHudConfig.lyricLines:0)*11,rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);transform(c,s);panel(c,bw,bh,edit);
        int tx=12;
        if(SpotifyHudConfig.albumCover){CincoDraw.rounded(c,9,9,69,69,8,0xFF35134D);for(int i=0;i<7;i++)CincoDraw.rounded(c,16+i*7,18+(i%3)*8,20+i*7,60-(i%2)*7,2,0x884C8DFF);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("✦",true),39,35,0xFFFFA84F);tx=81;}
        if(SpotifyHudConfig.song)c.drawText(client.textRenderer,StarFonts.text(trim(SpotifyHudController.title(),25),true),tx,13,0xFFF7F0FA,false);
        if(SpotifyHudConfig.artist)c.drawText(client.textRenderer,StarFonts.text(trim(SpotifyHudController.artist(),27),false),tx,29,0xFFA99DAF,false);
        if(SpotifyHudConfig.controls){String pp=SpotifyHudController.playing()?"Ⅱ":"▶";c.drawText(client.textRenderer,StarFonts.text("◀",true),tx,49,0xFFD6CBE0,false);c.drawText(client.textRenderer,StarFonts.text(pp,true),tx+29,49,0xFFFFFFFF,false);c.drawText(client.textRenderer,StarFonts.text("▶",true),tx+58,49,0xFFD6CBE0,false);}
        if(SpotifyHudConfig.progress){double dur=SpotifyHudController.durationSeconds(),pos=SpotifyHudController.positionSeconds(),f=dur<=0?0:Math.max(0,Math.min(1,pos/dur));int py=75;c.fill(10,py,bw-10,py+3,0xFF30263A);c.fill(10,py,10+(int)((bw-20)*f),py+3,0xFFC13DFF);}
        if(SpotifyHudConfig.lyrics){String lyr=SpotifyHudController.lyrics();if(lyr==null||lyr.isBlank())lyr="Lyrics unavailable";int yy=84;for(int i=0;i<SpotifyHudConfig.lyricLines;i++)c.drawText(client.textRenderer,StarFonts.text(i==0?trim(lyr,42):"",false),10,yy+i*11,i==0?0xFFCDA1FF:0xFF9D91A5,false);}
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("spotify_hud",m,s,s.x,s.y,rw,rh));
    }

    private static void closeButton(DrawContext c,MinecraftClient client,int bw){CincoDraw.rounded(c,bw-15,3,bw-3,15,6,0xD0A33B57);c.drawCenteredTextWithShadow(client.textRenderer,Text.literal("×"),bw-9,5,0xFFFFFFFF);}

    private static void handleEditor(MinecraftClient client){
        int sw=client.getWindow().getScaledWidth(),sh=client.getWindow().getScaledHeight();double mx=client.mouse.getX()*sw/Math.max(1.0,client.getWindow().getWidth()),my=client.mouse.getY()*sh/Math.max(1.0,client.getWindow().getHeight());boolean left=GLFW.glfwGetMouseButton(client.getWindow().getHandle(),GLFW.GLFW_MOUSE_BUTTON_LEFT)==GLFW.GLFW_PRESS;
        if(left&&!lastLeft){for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(!inside(mx,my,h))continue;double lx=(mx-h.x)/h.state.scale,ly=(my-h.y)/h.state.scale;if(lx>=h.w/h.state.scale-18&&ly<=18){if(h.id.equals("spotify_hud"))h.module.enabled=false;else if(h.id.equals("region_map"))StarHudConfig.regionMap=false;else if(h.id.equals("status_bar"))StarHudConfig.statusBar=false;else if(h.id.equals("potion_hud"))StarHudConfig.potionHud=false;else if(h.id.equals("module_list"))StarHudConfig.moduleList=false;StarHudConfig.save();ConfigStore.save();lastLeft=left;return;}dragging=h.id;dragOffsetX=mx-h.x;dragOffsetY=my-h.y;break;}}
        if(left&&dragging!=null)for(Hit h:hits)if(h.id.equals(dragging)){h.state.x=(int)Math.round(mx-dragOffsetX);h.state.y=(int)Math.round(my-dragOffsetY);break;}
        if(!left&&lastLeft&&dragging!=null){dragging=null;StarHudLayoutStore.save();}lastLeft=left;
    }

    public static boolean onScroll(MinecraftClient client,double mouseX,double mouseY,double vertical){if(!(client.currentScreen instanceof ChatScreen))return false;int sw=client.getWindow().getScaledWidth(),sh=client.getWindow().getScaledHeight();double mx=mouseX*sw/Math.max(1.0,client.getWindow().getWidth()),my=mouseY*sh/Math.max(1.0,client.getWindow().getHeight());for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(!inside(mx,my,h))continue;h.state.scale=Math.max(.5f,Math.min(2.5f,h.state.scale+(vertical>0?.1f:-.1f)));StarHudLayoutStore.save();return true;}return false;}
    private static boolean inside(double mx,double my,Hit h){return mx>=h.x&&mx<=h.x+h.w&&my>=h.y&&my<=h.y+h.h;}
    private static float clampScale(float s){return Math.max(.5f,Math.min(2.5f,s));}
    private static void clampPosition(MinecraftClient c,StarHudLayoutStore.State s,int w,int h){int sw=c.getWindow().getScaledWidth(),sh=c.getWindow().getScaledHeight();s.x=Math.max(0,Math.min(Math.max(0,sw-w),s.x));s.y=Math.max(0,Math.min(Math.max(0,sh-h),s.y));}
    private static int ping(MinecraftClient c){if(c.getNetworkHandler()==null||c.player==null)return 0;var entry=c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());return entry==null?0:entry.getLatency();}
    private static String time(int sec){return String.format("%d:%02d",Math.max(0,sec)/60,Math.max(0,sec)%60);}
    private static String trim(String s,int max){if(s==null)return"";return s.length()<=max?s:s.substring(0,max-1)+"…";}
}
