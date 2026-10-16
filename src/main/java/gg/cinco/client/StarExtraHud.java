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

/** Premium draggable translucent HUD suite. Open chat to edit. */
public final class StarExtraHud {
    private static final int PINK=0xFFFF55C8;
    private static final int PURPLE=0xFFB66CFF;
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
        int alpha=(int)Math.max(28,Math.min(68,StarHudConfig.opacity*72));
        int bg=(alpha<<24)|0x090611;
        int radius=Math.min(22,Math.max(13,h/5));
        CincoDraw.rounded(c,-3,-3,w+3,h+3,radius+3,0x163E0C58);
        CincoDraw.rounded(c,-1,-1,w+1,h+1,radius+1,edit?0xB8FF55C8:0x668C5AE8);
        CincoDraw.rounded(c,0,0,w,h,radius,bg);
        CincoDraw.rounded(c,13,2,w-13,4,2,0x62FF55C8);
    }
    private static void transform(DrawContext c,StarHudLayoutStore.State s){c.getMatrices().pushMatrix();c.getMatrices().translate(s.x,s.y);c.getMatrices().scale(s.scale,s.scale);}

    private static void statusBar(DrawContext c,MinecraftClient client,boolean edit){
        StarHudLayoutStore.State s=StarHudLayoutStore.get("status_bar",Math.max(12,client.getWindow().getScaledWidth()/2-120),12);s.scale=clampScale(s.scale);
        int bw=242,bh=26,rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);transform(c,s);panel(c,bw,bh,edit);
        CincoDraw.rounded(c,7,5,23,21,8,0x3CFF55C8);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("✦",true),15,9,PINK);
        int x=31;
        if(StarHudConfig.statusClock){String t=LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));tag(c,client,x,"TIME",t);x+=69;}
        if(StarHudConfig.statusFps){tag(c,client,x,"FPS",Integer.toString(client.getCurrentFps()));x+=59;}
        if(StarHudConfig.statusPing){tag(c,client,x,"PING",ping(client)+"ms");x+=66;}
        if(StarHudConfig.statusCoords)c.drawText(client.textRenderer,StarFonts.text((int)client.player.getX()+", "+(int)client.player.getZ(),true),x,8,0xFFF3ECF8,false);
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("status_bar",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }
    private static void tag(DrawContext c,MinecraftClient mc,int x,String k,String v){c.drawText(mc.textRenderer,StarFonts.text(k,false),x,8,0xFFAD9CB9,false);c.drawText(mc.textRenderer,StarFonts.text(v,true),x+mc.textRenderer.getWidth(k)+5,8,0xFFF8F4FA,false);}

    private static void regionMap(DrawContext c,MinecraftClient client,boolean edit){
        StarHudLayoutStore.State s=StarHudLayoutStore.get("region_map",12,76);s.scale=clampScale(s.scale);
        int bw=132,bh=116,rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);
        int pcx=client.player.getBlockX()>>4,pcz=client.player.getBlockZ()>>4,rx=Math.floorDiv(pcx,32),rz=Math.floorDiv(pcz,32);
        transform(c,s);panel(c,bw,bh,edit);
        CincoDraw.rounded(c,8,7,24,23,8,0x3BFF55C8);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("◆",true),16,11,PINK);
        c.drawText(client.textRenderer,StarFonts.text("REGION",true),30,10,0xFFF7F2FA,false);
        c.drawText(client.textRenderer,StarFonts.text("R "+rx+", "+rz+"   "+quadrant(client.player.getBlockX(),client.player.getBlockZ()),false),10,27,0xFFB6A8BF,false);
        int cols=7,rows=4,cell=16,mx=9,my=44;
        for(int z=0;z<rows;z++)for(int x=0;x<cols;x++){
            int rrX=rx+x-3,rrZ=rz+z-2,xx=mx+x*cell,yy=my+z*cell,col=regionColor(rrX,rrZ);
            CincoDraw.rounded(c,xx,yy,xx+13,yy+13,7,col);
            if(rrX==rx&&rrZ==rz){CincoDraw.rounded(c,xx+1,yy+1,xx+12,yy+12,6,0xFFFFFFFF);CincoDraw.rounded(c,xx+4,yy+4,xx+9,yy+9,3,PINK);}
        }
        c.drawText(client.textRenderer,StarFonts.text("Chunk "+pcx+", "+pcz,false),10,103,0xFFD2C7D8,false);
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("region_map",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }
    private static int regionColor(int x,int z){int n=Math.floorMod(x*31+z*17,6);return switch(n){case 0->0xE86A4FDB;case 1->0xE83BB7A0;case 2->0xE86DC66C;case 3->0xE8D7B34E;case 4->0xE8F15DB7;default->0xE85D74CF;};}
    private static String quadrant(int x,int z){if(x>=0&&z<0)return"NE";if(x<0&&z<0)return"NW";if(x>=0)return"SE";return"SW";}

    private static void potionHud(DrawContext c,MinecraftClient client,boolean edit){
        List<StatusEffectInstance> list=new ArrayList<>(client.player.getStatusEffects());list.sort(Comparator.comparingInt(StatusEffectInstance::getDuration));
        int count=Math.min(StarHudConfig.potionMax,list.size()),bh=Math.max(34,28+count*19),bw=152;
        StarHudLayoutStore.State s=StarHudLayoutStore.get("potion_hud",12,205);s.scale=clampScale(s.scale);int rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);
        transform(c,s);panel(c,bw,bh,edit);CincoDraw.rounded(c,8,7,24,23,8,0x3BFF55C8);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("✦",true),16,11,PINK);c.drawText(client.textRenderer,StarFonts.text("EFFECTS",true),30,10,0xFFF3ECF8,false);
        for(int i=0;i<count;i++){StatusEffectInstance e=list.get(i);int yy=30+i*19;String name=e.getEffectType().value().getName().getString(),dur=time(e.getDuration()/20);CincoDraw.rounded(c,7,yy-2,bw-7,yy+15,9,0x290D0916);c.drawText(client.textRenderer,StarFonts.text(trim(name,16),false),13,yy+2,0xFFEDE7F0,false);c.drawText(client.textRenderer,StarFonts.text(dur,true),bw-13-client.textRenderer.getWidth(dur),yy+2,PURPLE,false);}
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("potion_hud",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }

    private static void moduleList(DrawContext c,MinecraftClient client,boolean edit){
        List<ClientModule> list=new ArrayList<>();for(ClientModule m:CincoClient.MODULES)if(m.enabled&&!hiddenHudModule(m))list.add(m);
        list.sort(Comparator.comparingInt((ClientModule m)->client.textRenderer.getWidth(m.name)).reversed());int count=Math.min(StarHudConfig.moduleMax,list.size());
        int bw=190,bh=Math.max(38,31+count*20);StarHudLayoutStore.State s=StarHudLayoutStore.get("module_list",Math.max(12,client.getWindow().getScaledWidth()-208),142);s.scale=clampScale(s.scale);int rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);
        transform(c,s);panel(c,bw,bh,edit);
        CincoDraw.rounded(c,8,7,24,23,8,0x3BFF55C8);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("✦",true),16,11,PINK);c.drawText(client.textRenderer,StarFonts.text("Modules",true),30,10,0xFFF8F3FA,false);
        for(int i=0;i<count;i++){
            ClientModule m=list.get(i);int yy=31+i*20,col=gradient(i,count);
            CincoDraw.rounded(c,7,yy-1,bw-7,yy+16,9,0x300B0812);
            CincoDraw.rounded(c,11,yy+3,23,yy+15,6,0x3F6E35A0);
            c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("✦",true),17,yy+6,col);
            c.drawText(client.textRenderer,StarFonts.text(trim(m.name,18),true),29,yy+3,0xFFF2EAF6,false);
            // premium mockup-style status pill on the right
            CincoDraw.rounded(c,bw-31,yy+2,bw-10,yy+14,6,0x69FF55C8);
            CincoDraw.rounded(c,bw-20,yy+4,bw-12,yy+12,4,0xFFFFFFFF);
        }
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("module_list",CincoClient.findModule("HUD"),s,s.x,s.y,rw,rh));
    }
    private static boolean hiddenHudModule(ClientModule m){return m.name.equals("HUD")||m.name.equals("Font Style")||m.name.equals("Client Profile")||m.name.equals("Animations")||m.name.equals("Custom Accessory")||m.name.equals("Player Changer")||m.name.equals("GTA Skin Changer");}
    private static int gradient(int i,int n){double t=n<=1?0:i/(double)(n-1);int r=255,g=(int)(86+80*(1-t)),b=(int)(205+45*(1-t));return 0xFF000000|(r<<16)|(g<<8)|b;}

    private static void spotify(DrawContext c,MinecraftClient client,boolean edit){
        ClientModule m=CincoClient.findModule("Spotify HUD");if(m==null||!m.enabled)return;
        StarHudLayoutStore.State s=StarHudLayoutStore.get("spotify_hud",Math.max(12,client.getWindow().getScaledWidth()-300),12);s.scale=clampScale(s.scale);
        int bw=286,bh=98+Math.max(0,SpotifyHudConfig.lyrics?SpotifyHudConfig.lyricLines:0)*11,rw=Math.round(bw*s.scale),rh=Math.round(bh*s.scale);clampPosition(client,s,rw,rh);transform(c,s);panel(c,bw,bh,edit);
        int tx=12;
        if(SpotifyHudConfig.albumCover){CincoDraw.rounded(c,9,9,73,73,32,0x7830124A);CincoDraw.rounded(c,16,16,66,66,25,0x4D0B0815);for(int i=0;i<7;i++)CincoDraw.rounded(c,23+i*6,24+(i%3)*6,26+i*6,60-(i%2)*7,2,0xB77E58E8);c.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text("✦",true),41,38,PINK);tx=84;}
        if(SpotifyHudConfig.song)c.drawText(client.textRenderer,StarFonts.text(trim(SpotifyHudController.title(),25),true),tx,14,0xFFF8F4FA,false);
        if(SpotifyHudConfig.artist)c.drawText(client.textRenderer,StarFonts.text(trim(SpotifyHudController.artist(),27),false),tx,30,0xFFB8ADBE,false);
        if(SpotifyHudConfig.controls){String pp=SpotifyHudController.playing()?"Ⅱ":"▶";controlPill(c,client,tx,49,"◀");controlPill(c,client,tx+34,49,pp);controlPill(c,client,tx+68,49,"▶");}
        if(SpotifyHudConfig.progress){double dur=SpotifyHudController.durationSeconds(),pos=SpotifyHudController.positionSeconds(),f=dur<=0?0:Math.max(0,Math.min(1,pos/dur));int py=78;CincoDraw.rounded(c,11,py,bw-11,py+5,3,0x53352A3E);CincoDraw.rounded(c,11,py,11+(int)((bw-22)*f),py+5,3,PINK);CincoDraw.rounded(c,8+(int)((bw-22)*f),py-3,14+(int)((bw-22)*f),py+8,5,0xDFFFFFFF);}
        if(SpotifyHudConfig.lyrics){String lyr=SpotifyHudController.lyrics();if(lyr==null||lyr.isBlank())lyr="Lyrics unavailable";int yy=89;for(int i=0;i<SpotifyHudConfig.lyricLines;i++)c.drawText(client.textRenderer,StarFonts.text(i==0?trim(lyr,41):"",false),11,yy+i*11,i==0?0xFFE7C9FF:0xFF9D91A5,false);}
        if(edit)closeButton(c,client,bw);c.getMatrices().popMatrix();hits.add(new Hit("spotify_hud",m,s,s.x,s.y,rw,rh));
    }
    private static void controlPill(DrawContext c,MinecraftClient mc,int x,int y,String text){CincoDraw.rounded(c,x,y,x+26,y+20,10,0x55321A43);CincoDraw.rounded(c,x+1,y+1,x+25,y+19,9,0x460C0814);c.drawCenteredTextWithShadow(mc.textRenderer,StarFonts.text(text,true),x+13,y+6,0xFFF6EFF9);}

    private static void closeButton(DrawContext c,MinecraftClient client,int bw){CincoDraw.rounded(c,bw-18,3,bw-3,18,8,0xB8FF4B8D);c.drawCenteredTextWithShadow(client.textRenderer,Text.literal("×"),bw-10,6,0xFFFFFFFF);}

    private static void handleEditor(MinecraftClient client){
        int sw=client.getWindow().getScaledWidth(),sh=client.getWindow().getScaledHeight();double mx=client.mouse.getX()*sw/Math.max(1.0,client.getWindow().getWidth()),my=client.mouse.getY()*sh/Math.max(1.0,client.getWindow().getHeight());boolean left=GLFW.glfwGetMouseButton(client.getWindow().getHandle(),GLFW.GLFW_MOUSE_BUTTON_LEFT)==GLFW.GLFW_PRESS;
        if(left&&!lastLeft){for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(!inside(mx,my,h))continue;double lx=(mx-h.x)/h.state.scale,ly=(my-h.y)/h.state.scale;if(lx>=h.w/h.state.scale-18&&ly<=18){if(h.id.equals("spotify_hud"))h.module.enabled=false;else if(h.id.equals("region_map"))StarHudConfig.regionMap=false;else if(h.id.equals("status_bar"))StarHudConfig.statusBar=false;else if(h.id.equals("potion_hud"))StarHudConfig.potionHud=false;else if(h.id.equals("module_list"))StarHudConfig.moduleList=false;StarHudConfig.save();ConfigStore.save();lastLeft=left;return;}if(h.id.equals("spotify_hud")&&spotifyClick(lx,ly)){lastLeft=left;return;}dragging=h.id;dragOffsetX=mx-h.x;dragOffsetY=my-h.y;break;}}
        if(left&&dragging!=null)for(Hit h:hits)if(h.id.equals(dragging)){h.state.x=(int)Math.round(mx-dragOffsetX);h.state.y=(int)Math.round(my-dragOffsetY);break;}
        if(!left&&lastLeft&&dragging!=null){dragging=null;StarHudLayoutStore.save();}lastLeft=left;
    }
    private static boolean spotifyClick(double lx,double ly){
        int tx=SpotifyHudConfig.albumCover?84:12;
        if(SpotifyHudConfig.controls&&ly>=47&&ly<=72){if(lx>=tx&&lx<tx+31){SpotifyHudController.control(SpotifyHudController.Control.PREVIOUS);return true;}if(lx>=tx+31&&lx<tx+65){SpotifyHudController.control(SpotifyHudController.Control.PLAY_PAUSE);return true;}if(lx>=tx+65&&lx<tx+99){SpotifyHudController.control(SpotifyHudController.Control.NEXT);return true;}}
        if(SpotifyHudConfig.progress&&ly>=73&&ly<=88){SpotifyHudController.seek(Math.max(0,Math.min(1,(lx-11)/264.0)));return true;}
        return false;
    }

    public static boolean onScroll(MinecraftClient client,double mouseX,double mouseY,double vertical){if(!(client.currentScreen instanceof ChatScreen))return false;int sw=client.getWindow().getScaledWidth(),sh=client.getWindow().getScaledHeight();double mx=mouseX*sw/Math.max(1.0,client.getWindow().getWidth()),my=mouseY*sh/Math.max(1.0,client.getWindow().getHeight());for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(!inside(mx,my,h))continue;h.state.scale=Math.max(.5f,Math.min(2.5f,h.state.scale+(vertical>0?.1f:-.1f)));StarHudLayoutStore.save();return true;}return false;}
    private static boolean inside(double mx,double my,Hit h){return mx>=h.x&&mx<=h.x+h.w&&my>=h.y&&my<=h.y+h.h;}
    private static float clampScale(float s){return Math.max(.5f,Math.min(2.5f,s));}
    private static void clampPosition(MinecraftClient c,StarHudLayoutStore.State s,int w,int h){int sw=c.getWindow().getScaledWidth(),sh=c.getWindow().getScaledHeight();s.x=Math.max(0,Math.min(Math.max(0,sw-w),s.x));s.y=Math.max(0,Math.min(Math.max(0,sh-h),s.y));}
    private static int ping(MinecraftClient c){if(c.getNetworkHandler()==null||c.player==null)return 0;var entry=c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());return entry==null?0:entry.getLatency();}
    private static String time(int sec){return String.format("%d:%02d",Math.max(0,sec)/60,Math.max(0,sec)%60);}
    private static String trim(String s,int max){if(s==null)return"";return s.length()<=max?s:s.substring(0,max-1)+"…";}
}
