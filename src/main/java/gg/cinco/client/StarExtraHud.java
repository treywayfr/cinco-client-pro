package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reference-matched draggable Star HUD. Open chat to edit. */
public final class StarExtraHud {
    private static final int PINK=0xFFFF58D8;
    private static final int PINK_SOFT=0xFFFF9AE8;
    private static final int PURPLE=0xFFB873FF;
    private static final int TEXT=0xFFF8F4FF;
    private static final int MUTED=0xFFB9ADBF;
    private static String dragging;
    private static double dragOffsetX,dragOffsetY;
    private static boolean lastLeft;
    private static final List<Hit> hits=new ArrayList<>();
    private record Hit(String id,ClientModule module,StarHudLayoutStore.State state,int x,int y,int w,int h){}
    private StarExtraHud(){}

    public static void render(DrawContext ctx,MinecraftClient client){
        if(client==null||client.player==null||client.world==null||!CincoClient.hudEnabled)return;
        int sw=client.getWindow().getScaledWidth(),sh=client.getWindow().getScaledHeight();
        StarHudLayoutStore.applyPolishedDefaults(sw,sh);
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
        int alpha=(int)Math.max(82,Math.min(145,StarHudConfig.opacity*132));
        int bg=(alpha<<24)|0x080612;
        int radius=Math.min(18,Math.max(12,h/5));
        CincoDraw.softShadow(c,0,0,w,h,radius,0xFF5ADF,edit?52:26);
        CincoDraw.bubble(c,0,0,w,h,radius,edit?0xDBFF65DC:0xA8CB8DF2,bg);
        // Single inner glass highlight only. No stacked scanline/banding layers.
        CincoDraw.rounded(c,2,2,w-2,h-2,Math.max(2,radius-2),0x17140A20);
    }
    private static void transform(DrawContext c,StarHudLayoutStore.State s){c.getMatrices().pushMatrix();c.getMatrices().translate(s.x,s.y);c.getMatrices().scale(s.scale,s.scale);}

    private static void statusBar(DrawContext c,MinecraftClient client,boolean edit){
        StarHudLayoutStore.State s=StarHudLayoutStore.get("status_bar",Math.max(12,client.getWindow().getScaledWidth()/2-146),12);s.scale=clampScale(s.scale);
        int bw=292,bh=30,rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);transform(c,s);panel(c,bw,bh,edit);
        CincoDraw.rounded(c,8,5,28,25,10,0x45150B29);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("◷",true),18,10,PINK);
        int x=39;
        if(StarHudConfig.statusClock){String t=LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));valueTag(c,client,x,"",t);x+=78;separator(c,x-8);}
        if(StarHudConfig.statusFps){valueTag(c,client,x,"FPS",Integer.toString(client.getCurrentFps()));x+=73;separator(c,x-8);}
        if(StarHudConfig.statusPing){valueTag(c,client,x,"PING",ping(client)+"ms");x+=78;}
        if(StarHudConfig.statusCoords)c.drawText(client.textRenderer,StarFonts.text((int)client.player.getX()+", "+(int)client.player.getZ(),true),x,11,TEXT,false);
        c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("✦",true),bw-17,10,0xFFEFB9FF);
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("status_bar",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }
    private static void separator(DrawContext c,int x){c.fill(x,7,x+1,23,0x4ABFA5CE);}
    private static void valueTag(DrawContext c,MinecraftClient mc,int x,String k,String v){if(!k.isBlank()){c.drawText(mc.textRenderer,StarFonts.text(k,true),x,11,PINK,false);x+=mc.textRenderer.getWidth(k)+5;}c.drawText(mc.textRenderer,StarFonts.text(v,true),x,11,TEXT,false);}

    private static void regionMap(DrawContext c,MinecraftClient client,boolean edit){
        StarHudLayoutStore.State s=StarHudLayoutStore.get("region_map",14,88);s.scale=clampScale(s.scale);
        int bw=124,bh=120,rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);
        int pcx=client.player.getBlockX()>>4,pcz=client.player.getBlockZ()>>4,rx=Math.floorDiv(pcx,32),rz=Math.floorDiv(pcz,32);
        transform(c,s);panel(c,bw,bh,edit);
        CincoDraw.rounded(c,9,8,25,24,8,0x40150B29);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("●",true),17,12,PINK);
        c.drawText(client.textRenderer,StarFonts.text("REGION",true),31,11,TEXT,false);
        c.drawText(client.textRenderer,StarFonts.text("R  "+rx+", "+rz+"   "+quadrant(client.player.getBlockX(),client.player.getBlockZ()),false),11,28,MUTED,false);
        int cols=7,rows=4,cell=15,mx=9,my=45;
        for(int z=0;z<rows;z++)for(int x=0;x<cols;x++){
            int rrX=rx+x-3,rrZ=rz+z-2,xx=mx+x*cell,yy=my+z*cell,col=regionColor(rrX,rrZ);
            CincoDraw.rounded(c,xx,yy,xx+12,yy+12,6,col);
            if(rrX==rx&&rrZ==rz){CincoDraw.rounded(c,xx+1,yy+1,xx+11,yy+11,5,0xFFFFFFFF);CincoDraw.rounded(c,xx+4,yy+4,xx+8,yy+8,2,PINK);}
        }
        c.drawText(client.textRenderer,StarFonts.text("Chunk "+pcx+", "+pcz,false),10,106,0xFFD2C7D8,false);
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("region_map",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }
    private static int regionColor(int x,int z){int n=Math.floorMod(x*31+z*17,6);return switch(n){case 0->0xEE6A4FDB;case 1->0xEE45C0A8;case 2->0xEE73CF79;case 3->0xEEF05CB7;case 4->0xEE7180DF;default->0xEEA56BEE;};}
    private static String quadrant(int x,int z){if(x>=0&&z<0)return"NE";if(x<0&&z<0)return"NW";if(x>=0)return"SE";return"SW";}

    private static void potionHud(DrawContext c,MinecraftClient client,boolean edit){
        List<StatusEffectInstance> list=new ArrayList<>(client.player.getStatusEffects());list.sort(Comparator.comparingInt(StatusEffectInstance::getDuration));
        int count=Math.min(StarHudConfig.potionMax,list.size()),bw=124,bh=Math.max(33,31+count*18);
        StarHudLayoutStore.State s=StarHudLayoutStore.get("potion_hud",14,218);s.scale=clampScale(s.scale);int rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);
        transform(c,s);panel(c,bw,bh,edit);
        CincoDraw.rounded(c,8,7,25,24,9,0x40150B29);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("⚗",true),16,11,PINK);c.drawText(client.textRenderer,StarFonts.text("EFFECTS",true),31,11,TEXT,false);c.drawText(client.textRenderer,StarFonts.text("›",true),bw-16,11,0xFFE4CEF1,false);
        for(int i=0;i<count;i++){StatusEffectInstance e=list.get(i);int yy=32+i*18;String name=e.getEffectType().value().getName().getString(),dur=time(e.getDuration()/20);CincoDraw.rounded(c,6,yy-1,bw-6,yy+15,8,0x24110A18);c.drawText(client.textRenderer,StarFonts.text(trim(name,13),false),11,yy+3,0xFFEDE7F0,false);c.drawText(client.textRenderer,StarFonts.text(dur,true),bw-10-client.textRenderer.getWidth(dur),yy+3,PURPLE,false);}
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("potion_hud",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }

    private static void moduleList(DrawContext c,MinecraftClient client,boolean edit){
        List<ClientModule> list=new ArrayList<>();for(ClientModule m:CincoClient.MODULES)if(m.enabled&&!hiddenHudModule(m))list.add(m);
        list.sort(Comparator.comparingInt((ClientModule m)->client.textRenderer.getWidth(m.name)).reversed());int count=Math.min(StarHudConfig.moduleMax,list.size());
        int bw=148,bh=Math.max(35,31+count*19);StarHudLayoutStore.State s=StarHudLayoutStore.get("module_list",Math.max(12,client.getWindow().getScaledWidth()-160),116);s.scale=clampScale(s.scale);int rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);
        transform(c,s);panel(c,bw,bh,edit);
        CincoDraw.rounded(c,8,7,25,24,9,0x40150B29);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("✦",true),16,11,PINK);c.drawText(client.textRenderer,StarFonts.text("Modules",true),31,11,TEXT,false);
        for(int i=0;i<count;i++){
            ClientModule m=list.get(i);int yy=31+i*19;
            CincoDraw.rounded(c,6,yy-1,bw-6,yy+16,8,0x28110A18);
            c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text(moduleGlyph(m.name),true),15,yy+3,0xFFFF78DC);
            c.drawText(client.textRenderer,StarFonts.text(trim(m.name,15),false),27,yy+3,0xFFF2EAF6,false);
            CincoDraw.rounded(c,bw-28,yy+2,bw-8,yy+14,6,0xC8FF55D1);
            CincoDraw.rounded(c,bw-18,yy+4,bw-10,yy+12,4,0xFFFFFFFF);
        }
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("module_list",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }
    private static boolean hiddenHudModule(ClientModule m){return m.name.equals("HUD")||m.name.equals("Font Style")||m.name.equals("Client Profile")||m.name.equals("Animations")||m.name.equals("Custom Accessory")||m.name.equals("Player Changer")||m.name.equals("GTA Skin Changer");}
    private static String moduleGlyph(String n){String s=n.toLowerCase();if(s.contains("container")||s.contains("radar"))return"◆";if(s.contains("performance"))return"ϟ";if(s.contains("save"))return"⚙";if(s.contains("font"))return"Aa";if(s.contains("opacity"))return"◐";if(s.contains("star"))return"✦";if(s.contains("fov"))return"◉";if(s.contains("animation"))return"▣";if(s.contains("crystal"))return"✧";if(s.contains("sky"))return"☁";if(s.contains("coordinate"))return"●";return"✦";}

    private static void spotify(DrawContext c,MinecraftClient client,boolean edit){
        ClientModule m=CincoClient.findModule("Spotify HUD");if(m==null||!m.enabled)return;
        StarHudLayoutStore.State s=StarHudLayoutStore.get("spotify_hud",Math.max(12,client.getWindow().getScaledWidth()-236),18);s.scale=clampScale(s.scale);
        int bw=224,bh=88,rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);transform(c,s);panel(c,bw,bh,edit);

        // Spotify disc from the approved reference.
        CincoDraw.rounded(c,10,10,58,58,24,0xA20C0A16);
        CincoDraw.rounded(c,16,16,52,52,18,0xFF1ED760);
        CincoDraw.rounded(c,30,29,35,34,3,0xFF07130B);

        String title=SpotifyHudController.title();
        if(title==null||title.isBlank()||title.equalsIgnoreCase("Spotify"))title="Listening on Spotify";
        c.drawText(client.textRenderer,StarFonts.text(trim(title,24),true),68,13,TEXT,false);
        if(SpotifyHudConfig.artist){String ar=SpotifyHudController.artist();if(ar!=null&&!ar.isBlank())c.drawText(client.textRenderer,StarFonts.text(trim(ar,25),false),68,27,MUTED,false);}

        if(SpotifyHudConfig.controls){String pp=SpotifyHudController.playing()?"Ⅱ":"▶";control(c,client,68,39,"◀");control(c,client,98,39,pp);control(c,client,128,39,"▶");}
        // Waveform bars at right like the mockup.
        for(int i=0;i<8;i++){int hh=8+(i%4)*4;CincoDraw.rounded(c,168+i*5,39-hh/2,171+i*5,39+hh/2,2,0xC8E75AFF);}

        if(SpotifyHudConfig.progress){double dur=SpotifyHudController.durationSeconds(),pos=SpotifyHudController.positionSeconds(),f=dur<=0?0:Math.max(0,Math.min(1,pos/dur));int py=63;CincoDraw.rounded(c,12,py,bw-12,py+3,2,0x55463A52);CincoDraw.rounded(c,12,py,12+(int)((bw-24)*f),py+3,2,PINK);}
        if(SpotifyHudConfig.lyrics){String lyr=SpotifyHudController.lyrics();if(lyr==null||lyr.isBlank())lyr="Lyrics unavailable";c.drawText(client.textRenderer,StarFonts.text(trim(lyr,35),false),12,74,0xFFD1C4D8,false);}
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("spotify_hud",m,s,s.x,s.y,rw,rh));
    }
    private static void control(DrawContext c,MinecraftClient mc,int x,int y,String text){CincoDraw.rounded(c,x,y,x+24,y+24,12,0x5A1A1129);CincoDraw.rounded(c,x+1,y+1,x+23,y+23,11,0x8A0C0814);c.drawCenteredTextWithShadow(mc.textRenderer,StarFonts.text(text,true),x+12,y+8,0xFFF8F0FA);}

    private static void closeButton(DrawContext c,MinecraftClient mc,int w){CincoDraw.rounded(c,w-16,3,w-3,16,7,0xB8FF4B8D);c.drawCenteredTextWithShadow(mc.textRenderer,net.minecraft.text.Text.literal("×"),w-9,5,0xFFFFFFFF);}

    private static void handleEditor(MinecraftClient client){
        int sw=client.getWindow().getScaledWidth(),sh=client.getWindow().getScaledHeight();double mx=client.mouse.getX()*sw/Math.max(1.0,client.getWindow().getWidth()),my=client.mouse.getY()*sh/Math.max(1.0,client.getWindow().getHeight());boolean left=GLFW.glfwGetMouseButton(client.getWindow().getHandle(),GLFW.GLFW_MOUSE_BUTTON_LEFT)==GLFW.GLFW_PRESS;
        if(left&&!lastLeft)for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(!inside(mx,my,h))continue;double lx=(mx-h.x)/h.state.scale,ly=(my-h.y)/h.state.scale;if(h.id.equals("spotify_hud")&&spotifyClick(lx,ly)){lastLeft=left;return;}int close=Math.max(10,Math.round(17*h.state.scale));if(mx>=h.x+h.w-close&&my<=h.y+close){if(h.module!=null){h.module.enabled=false;ConfigStore.save();}lastLeft=left;return;}dragging=h.id;dragOffsetX=mx-h.x;dragOffsetY=my-h.y;break;}
        if(left&&dragging!=null)for(Hit h:hits)if(h.id.equals(dragging)){h.state.x=(int)Math.round(mx-dragOffsetX);h.state.y=(int)Math.round(my-dragOffsetY);break;}
        if(!left&&lastLeft&&dragging!=null){dragging=null;StarHudLayoutStore.save();}lastLeft=left;
    }
    private static boolean spotifyClick(double lx,double ly){if(SpotifyHudConfig.controls&&ly>=38&&ly<=64){if(lx>=68&&lx<96){SpotifyHudController.control(SpotifyHudController.Control.PREVIOUS);return true;}if(lx>=98&&lx<126){SpotifyHudController.control(SpotifyHudController.Control.PLAY_PAUSE);return true;}if(lx>=128&&lx<156){SpotifyHudController.control(SpotifyHudController.Control.NEXT);return true;}}if(SpotifyHudConfig.progress&&ly>=59&&ly<=70){SpotifyHudController.seek(Math.max(0,Math.min(1,(lx-12)/200.0)));return true;}return false;}

    public static boolean onScroll(MinecraftClient client,double mouseX,double mouseY,double vertical){if(!(client.currentScreen instanceof ChatScreen))return false;int sw=client.getWindow().getScaledWidth(),sh=client.getWindow().getScaledHeight();double mx=mouseX*sw/Math.max(1.0,client.getWindow().getWidth()),my=mouseY*sh/Math.max(1.0,client.getWindow().getHeight());for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(!inside(mx,my,h))continue;h.state.scale=Math.max(.5f,Math.min(2.5f,h.state.scale+(vertical>0?.1f:-.1f)));StarHudLayoutStore.save();return true;}return false;}
    private static boolean inside(double mx,double my,Hit h){return mx>=h.x&&mx<=h.x+h.w&&my>=h.y&&my<=h.y+h.h;}
    private static float clampScale(float s){return Math.max(.5f,Math.min(2.5f,s));}
    private static void clampPosition(MinecraftClient c,StarHudLayoutStore.State s,int w,int h){int sw=c.getWindow().getScaledWidth(),sh=c.getWindow().getScaledHeight();s.x=Math.max(0,Math.min(Math.max(0,sw-w),s.x));s.y=Math.max(0,Math.min(Math.max(0,sh-h),s.y));}
    private static int ping(MinecraftClient c){if(c.getNetworkHandler()==null||c.player==null)return 0;var entry=c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());return entry==null?0:entry.getLatency();}
    private static String time(int sec){return String.format("%d:%02d",Math.max(0,sec)/60,Math.max(0,sec)%60);}
    private static String trim(String s,int max){if(s==null)return"";return s.length()<=max?s:s.substring(0,max-1)+"…";}
}
