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

/** Reference-matched draggable Star HUD. Open chat to edit. */
public final class StarExtraHud {
    private static final int PINK=0xFFFF4FD1;
    private static final int PURPLE=0xFFB86CFF;
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
        int alpha=(int)Math.max(34,Math.min(92,StarHudConfig.opacity*92));
        int bg=(alpha<<24)|0x0A0713;
        int radius=Math.min(20,Math.max(12,h/5));
        CincoDraw.rounded(c,-4,-4,w+4,h+4,radius+4,0x17350064);
        CincoDraw.rounded(c,-2,-2,w+2,h+2,radius+2,edit?0xBFFF55D5:0x80C97CFF);
        CincoDraw.rounded(c,-1,-1,w+1,h+1,radius+1,0x77FF55D5);
        CincoDraw.rounded(c,0,0,w,h,radius,bg);
        CincoDraw.rounded(c,8,2,w-8,4,2,0x74FF67DB);
    }
    private static void transform(DrawContext c,StarHudLayoutStore.State s){c.getMatrices().pushMatrix();c.getMatrices().translate(s.x,s.y);c.getMatrices().scale(s.scale,s.scale);}

    private static void statusBar(DrawContext c,MinecraftClient client,boolean edit){
        StarHudLayoutStore.State s=StarHudLayoutStore.get("status_bar",Math.max(12,client.getWindow().getScaledWidth()/2-148),12);s.scale=clampScale(s.scale);
        int bw=296,bh=34,rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);transform(c,s);panel(c,bw,bh,edit);
        CincoDraw.rounded(c,9,7,29,27,10,0x46FF55D1);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("◷",true),19,13,PINK);
        int x=40;
        if(StarHudConfig.statusClock){String t=LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));tag(c,client,x,"",t);x+=82;separator(c,x-9);}
        if(StarHudConfig.statusFps){tag(c,client,x,"FPS",Integer.toString(client.getCurrentFps()));x+=75;separator(c,x-9);}
        if(StarHudConfig.statusPing){tag(c,client,x,"PING",ping(client)+"ms");x+=82;}
        if(StarHudConfig.statusCoords)c.drawText(client.textRenderer,StarFonts.text((int)client.player.getX()+", "+(int)client.player.getZ(),true),x,12,0xFFF3ECF8,false);
        c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("✦",true),bw-18,13,0xFFFFA4F1);
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("status_bar",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }
    private static void separator(DrawContext c,int x){c.fill(x,8,x+1,26,0x55D9B8F0);}
    private static void tag(DrawContext c,MinecraftClient mc,int x,String k,String v){if(!k.isBlank()){c.drawText(mc.textRenderer,StarFonts.text(k,true),x,12,PINK,false);x+=mc.textRenderer.getWidth(k)+6;}c.drawText(mc.textRenderer,StarFonts.text(v,true),x,12,0xFFF8F4FA,false);}

    private static void regionMap(DrawContext c,MinecraftClient client,boolean edit){
        StarHudLayoutStore.State s=StarHudLayoutStore.get("region_map",12,91);s.scale=clampScale(s.scale);
        int bw=139,bh=129,rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);
        int pcx=client.player.getBlockX()>>4,pcz=client.player.getBlockZ()>>4,rx=Math.floorDiv(pcx,32),rz=Math.floorDiv(pcz,32);
        transform(c,s);panel(c,bw,bh,edit);
        CincoDraw.rounded(c,9,9,25,25,8,0x45FF55D1);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("●",true),17,13,PINK);
        c.drawText(client.textRenderer,StarFonts.text("REGION",true),32,12,0xFFF8F4FA,false);
        c.drawText(client.textRenderer,StarFonts.text("R  "+rx+", "+rz+"   "+quadrant(client.player.getBlockX(),client.player.getBlockZ()),false),12,31,0xFFC5B8CE,false);
        int cols=7,rows=4,cell=16,mx=11,my=49;
        for(int z=0;z<rows;z++)for(int x=0;x<cols;x++){
            int rrX=rx+x-3,rrZ=rz+z-2,xx=mx+x*cell,yy=my+z*cell,col=regionColor(rrX,rrZ);
            CincoDraw.rounded(c,xx,yy,xx+13,yy+13,7,col);
            if(rrX==rx&&rrZ==rz){CincoDraw.rounded(c,xx+1,yy+1,xx+12,yy+12,6,0xFFFFFFFF);CincoDraw.rounded(c,xx+4,yy+4,xx+9,yy+9,3,PINK);}
        }
        c.drawText(client.textRenderer,StarFonts.text("Chunk "+pcx+", "+pcz,false),11,115,0xFFD5CADB,false);
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("region_map",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }
    private static int regionColor(int x,int z){int n=Math.floorMod(x*31+z*17,6);return switch(n){case 0->0xE86A4FDB;case 1->0xE83BB7A0;case 2->0xE86DC66C;case 3->0xE8D7B34E;case 4->0xE8F15DB7;default->0xE85D74CF;};}
    private static String quadrant(int x,int z){if(x>=0&&z<0)return"NE";if(x<0&&z<0)return"NW";if(x>=0)return"SE";return"SW";}

    private static void potionHud(DrawContext c,MinecraftClient client,boolean edit){
        List<StatusEffectInstance> list=new ArrayList<>(client.player.getStatusEffects());list.sort(Comparator.comparingInt(StatusEffectInstance::getDuration));
        int count=Math.min(StarHudConfig.potionMax,list.size()),bh=Math.max(36,34+count*19),bw=140;
        StarHudLayoutStore.State s=StarHudLayoutStore.get("potion_hud",12,228);s.scale=clampScale(s.scale);int rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);
        transform(c,s);panel(c,bw,bh,edit);
        CincoDraw.rounded(c,10,8,27,25,9,0x46FF55D1);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("⚗",true),18,12,PINK);c.drawText(client.textRenderer,StarFonts.text("EFFECTS",true),34,12,0xFFF8F3FA,false);c.drawText(client.textRenderer,StarFonts.text("›",true),bw-18,12,0xFFEAD5F7,false);
        for(int i=0;i<count;i++){StatusEffectInstance e=list.get(i);int yy=35+i*19;String name=e.getEffectType().value().getName().getString(),dur=time(e.getDuration()/20);CincoDraw.rounded(c,7,yy-1,bw-7,yy+16,8,0x2B120B1A);c.drawText(client.textRenderer,StarFonts.text(trim(name,14),false),13,yy+3,0xFFEDE7F0,false);c.drawText(client.textRenderer,StarFonts.text(dur,true),bw-12-client.textRenderer.getWidth(dur),yy+3,PURPLE,false);}
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("potion_hud",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }

    private static void moduleList(DrawContext c,MinecraftClient client,boolean edit){
        List<ClientModule> list=new ArrayList<>();for(ClientModule m:CincoClient.MODULES)if(m.enabled&&!hiddenHudModule(m))list.add(m);
        list.sort(Comparator.comparingInt((ClientModule m)->client.textRenderer.getWidth(m.name)).reversed());int count=Math.min(StarHudConfig.moduleMax,list.size());
        int bw=174,bh=Math.max(38,34+count*20);StarHudLayoutStore.State s=StarHudLayoutStore.get("module_list",Math.max(12,client.getWindow().getScaledWidth()-188),126);s.scale=clampScale(s.scale);int rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);
        transform(c,s);panel(c,bw,bh,edit);
        CincoDraw.rounded(c,9,8,26,25,9,0x46FF55D1);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("✦",true),17,12,PINK);c.drawText(client.textRenderer,StarFonts.text("Modules",true),33,12,0xFFF8F3FA,false);
        for(int i=0;i<count;i++){
            ClientModule m=list.get(i);int yy=34+i*20;
            CincoDraw.rounded(c,7,yy-1,bw-7,yy+17,9,0x39120B1A);
            String icon=moduleGlyph(m.name);
            c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text(icon,true),17,yy+4,0xFFFF72D9);
            c.drawText(client.textRenderer,StarFonts.text(trim(m.name,17),true),30,yy+4,0xFFF2EAF6,false);
            CincoDraw.rounded(c,bw-30,yy+2,bw-10,yy+15,7,0xC7FF55D1);
            CincoDraw.rounded(c,bw-20,yy+4,bw-12,yy+13,5,0xFFFFFFFF);
        }
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("module_list",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }
    private static boolean hiddenHudModule(ClientModule m){return m.name.equals("HUD")||m.name.equals("Font Style")||m.name.equals("Client Profile")||m.name.equals("Animations")||m.name.equals("Custom Accessory")||m.name.equals("Player Changer")||m.name.equals("GTA Skin Changer");}
    private static String moduleGlyph(String n){String s=n.toLowerCase();if(s.contains("container")||s.contains("radar"))return"◆";if(s.contains("performance"))return"ϟ";if(s.contains("save"))return"⚙";if(s.contains("font"))return"Aa";if(s.contains("opacity"))return"◐";if(s.contains("star"))return"✦";if(s.contains("fov"))return"◉";if(s.contains("animation"))return"▣";if(s.contains("crystal"))return"✧";if(s.contains("sky"))return"☁";if(s.contains("coordinate"))return"●";return"✦";}

    private static void spotify(DrawContext c,MinecraftClient client,boolean edit){
        ClientModule m=CincoClient.findModule("Spotify HUD");if(m==null||!m.enabled)return;
        StarHudLayoutStore.State s=StarHudLayoutStore.get("spotify_hud",Math.max(12,client.getWindow().getScaledWidth()-282),12);s.scale=clampScale(s.scale);
        int bw=270,bh=92+Math.max(0,SpotifyHudConfig.lyrics?SpotifyHudConfig.lyricLines:0)*11,rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);transform(c,s);panel(c,bw,bh,edit);
        int tx=14;
        if(SpotifyHudConfig.albumCover){CincoDraw.rounded(c,11,11,69,69,29,0x8B10101E);CincoDraw.rounded(c,18,18,62,62,22,0xFF1ED760);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("●",true),40,34,0xFF07130B);tx=82;}
        if(SpotifyHudConfig.song)c.drawText(client.textRenderer,StarFonts.text(trim(SpotifyHudController.title(),25),true),tx,14,0xFFF8F4FA,false);
        if(SpotifyHudConfig.artist)c.drawText(client.textRenderer,StarFonts.text(trim(SpotifyHudController.artist(),27),false),tx,30,0xFFB8ADBE,false);
        if(SpotifyHudConfig.controls){String pp=SpotifyHudController.playing()?"Ⅱ":"▶";controlPill(c,client,tx,47,"◀");controlPill(c,client,tx+35,47,pp);controlPill(c,client,tx+70,47,"▶");}
        if(SpotifyHudConfig.progress){double dur=SpotifyHudController.durationSeconds(),pos=SpotifyHudController.positionSeconds(),f=dur<=0?0:Math.max(0,Math.min(1,pos/dur));int py=73;CincoDraw.rounded(c,12,py,bw-12,py+4,2,0x53453A54);CincoDraw.rounded(c,12,py,12+(int)((bw-24)*f),py+4,2,PINK);CincoDraw.rounded(c,9+(int)((bw-24)*f),py-3,15+(int)((bw-24)*f),py+7,5,0xFFFFFFFF);}
        if(SpotifyHudConfig.lyrics){String lyr=SpotifyHudController.lyrics();if(lyr==null||lyr.isBlank())lyr="Lyrics unavailable";int yy=84;for(int i=0;i<SpotifyHudConfig.lyricLines;i++)c.drawText(client.textRenderer,StarFonts.text(i==0?trim(lyr,40):"",false),11,yy+i*11,i==0?0xFFD7C9E0:0xFF9D91A5,false);}
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("spotify_hud",m,s,s.x,s.y,rw,rh));
    }
    private static void controlPill(DrawContext c,MinecraftClient mc,int x,int y,String text){CincoDraw.rounded(c,x,y,x+28,y+22,11,0x6A3B2950);CincoDraw.rounded(c,x+1,y+1,x+27,y+21,10,0x5B100B18);c.drawCenteredTextWithShadow(mc.textRenderer,StarFonts.text(text,true),x+14,y+7,0xFFF6EFF9);}

    private static void closeButton(DrawContext c,MinecraftClient client,int bw){CincoDraw.rounded(c,bw-17,3,bw-3,17,7,0xA8FF4B8D);c.drawCenteredTextWithShadow(client.textRenderer,Text.literal("×"),bw-10,5,0xFFFFFFFF);}

    private static void handleEditor(MinecraftClient client){
        int sw=client.getWindow().getScaledWidth(),sh=client.getWindow().getScaledHeight();double mx=client.mouse.getX()*sw/Math.max(1.0,client.getWindow().getWidth()),my=client.mouse.getY()*sh/Math.max(1.0,client.getWindow().getHeight());boolean left=GLFW.glfwGetMouseButton(client.getWindow().getHandle(),GLFW.GLFW_MOUSE_BUTTON_LEFT)==GLFW.GLFW_PRESS;
        if(left&&!lastLeft){for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(!inside(mx,my,h))continue;double lx=(mx-h.x)/h.state.scale,ly=(my-h.y)/h.state.scale;if(lx>=h.w/h.state.scale-18&&ly<=18){if(h.id.equals("spotify_hud"))h.module.enabled=false;else if(h.id.equals("region_map"))StarHudConfig.regionMap=false;else if(h.id.equals("status_bar"))StarHudConfig.statusBar=false;else if(h.id.equals("potion_hud"))StarHudConfig.potionHud=false;else if(h.id.equals("module_list"))StarHudConfig.moduleList=false;StarHudConfig.save();ConfigStore.save();lastLeft=left;return;}if(h.id.equals("spotify_hud")&&spotifyClick(lx,ly)){lastLeft=left;return;}dragging=h.id;dragOffsetX=mx-h.x;dragOffsetY=my-h.y;break;}}
        if(left&&dragging!=null)for(Hit h:hits)if(h.id.equals(dragging)){h.state.x=(int)Math.round(mx-dragOffsetX);h.state.y=(int)Math.round(my-dragOffsetY);break;}
        if(!left&&lastLeft&&dragging!=null){dragging=null;StarHudLayoutStore.save();}lastLeft=left;
    }
    private static boolean spotifyClick(double lx,double ly){int tx=SpotifyHudConfig.albumCover?82:14;if(SpotifyHudConfig.controls&&ly>=45&&ly<=70){if(lx>=tx&&lx<tx+31){SpotifyHudController.control(SpotifyHudController.Control.PREVIOUS);return true;}if(lx>=tx+31&&lx<tx+66){SpotifyHudController.control(SpotifyHudController.Control.PLAY_PAUSE);return true;}if(lx>=tx+66&&lx<tx+101){SpotifyHudController.control(SpotifyHudController.Control.NEXT);return true;}}if(SpotifyHudConfig.progress&&ly>=69&&ly<=82){SpotifyHudController.seek(Math.max(0,Math.min(1,(lx-12)/246.0)));return true;}return false;}

    public static boolean onScroll(MinecraftClient client,double mouseX,double mouseY,double vertical){if(!(client.currentScreen instanceof ChatScreen))return false;int sw=client.getWindow().getScaledWidth(),sh=client.getWindow().getScaledHeight();double mx=mouseX*sw/Math.max(1.0,client.getWindow().getWidth()),my=mouseY*sh/Math.max(1.0,client.getWindow().getHeight());for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(!inside(mx,my,h))continue;h.state.scale=Math.max(.5f,Math.min(2.5f,h.state.scale+(vertical>0?.1f:-.1f)));StarHudLayoutStore.save();return true;}return false;}
    private static boolean inside(double mx,double my,Hit h){return mx>=h.x&&mx<=h.x+h.w&&my>=h.y&&my<=h.y+h.h;}
    private static float clampScale(float s){return Math.max(.5f,Math.min(2.5f,s));}
    private static void clampPosition(MinecraftClient c,StarHudLayoutStore.State s,int w,int h){int sw=c.getWindow().getScaledWidth(),sh=c.getWindow().getScaledHeight();s.x=Math.max(0,Math.min(Math.max(0,sw-w),s.x));s.y=Math.max(0,Math.min(Math.max(0,sh-h),s.y));}
    private static int ping(MinecraftClient c){if(c.getNetworkHandler()==null||c.player==null)return 0;var entry=c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());return entry==null?0:entry.getLatency();}
    private static String time(int sec){return String.format("%d:%02d",Math.max(0,sec)/60,Math.max(0,sec)%60);}
    private static String trim(String s,int max){if(s==null)return"";return s.length()<=max?s:s.substring(0,max-1)+"…";}
}
