package gg.cinco.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Clean player/tracer/light ESP using Fabric 1.21.11 world render consumers. */
public final class CleanEspWorldRenderer {
    private static final Map<UUID, Vec3d> SMOOTH_TRACER_TARGETS=new HashMap<>();
    private CleanEspWorldRenderer(){}

    public static void render(WorldRenderContext context){
        MinecraftClient client=MinecraftClient.getInstance(); if(client.player==null||client.world==null||context.consumers()==null)return;
        ClientModule playerEsp=CincoClient.findModule("Player ESP"),tracers=CincoClient.findModule("Tracers"),lightFinder=CincoClient.findModule("Light Finder");
        boolean drawPlayers=playerEsp!=null&&playerEsp.enabled,drawTracers=tracers!=null&&tracers.enabled,drawLights=lightFinder!=null&&lightFinder.enabled;
        if(!drawPlayers&&!drawTracers&&!drawLights)return;
        Vec3d camera=context.worldState().cameraRenderState.pos;
        Vec3d anchor=crosshairAnchor(camera,client.gameRenderer.getCamera().getYaw(),client.gameRenderer.getCamera().getPitch());
        int style=style(); float width=switch(style){case 2->2.05F;case 3->1.0F;case 4->2.40F;default->1.40F;};
        VertexConsumer b=context.consumers().getBuffer(RenderLayers.linesTranslucent());
        if(drawPlayers||drawTracers)renderPlayers(b,client,camera,anchor,playerEsp,tracers,style,width);
        if(drawLights)renderLights(b,client,camera,lightFinder,style,width);
    }

    private static void renderPlayers(VertexConsumer b,MinecraftClient client,Vec3d camera,Vec3d anchor,ClientModule esp,ClientModule tracers,int style,float width){
        double range=esp!=null&&esp.enabled?esp.setting:0;if(tracers!=null&&tracers.enabled)range=Math.max(range,tracers.setting);double rangeSq=range*range;
        int outlineAlpha=switch(style){case 2->250;case 3->165;case 4->255;default->225;},tracerAlpha=switch(style){case 2->180;case 3->85;case 4->185;default->140;};
        float fraction=switch(style){case 2->0.33F;case 3->0.20F;case 4->0.40F;default->0.27F;};
        int accent=withAlpha(CincoTheme.accentLight(),outlineAlpha),tracerColor=withAlpha(CincoTheme.accent(),tracerAlpha),rendered=0,cap=StarPerformance.active(client)?24:48;
        float ax=(float)(anchor.x-camera.x),ay=(float)(anchor.y-camera.y),az=(float)(anchor.z-camera.z);
        for(PlayerEntity p:client.world.getPlayers()){
            if(rendered>=cap)break;if(p==client.player||!p.isAlive())continue;double d=client.player.squaredDistanceTo(p);if(d>rangeSq)continue;
            if(esp!=null&&esp.enabled&&esp.setting3>=0.5){Box box=p.getBoundingBox().expand(style==4?0.055:0.035);float x0=(float)(box.minX-camera.x),y0=(float)(box.minY-camera.y),z0=(float)(box.minZ-camera.z),x1=(float)(box.maxX-camera.x),y1=(float)(box.maxY-camera.y),z1=(float)(box.maxZ-camera.z);if(style==4)emitFullBox(b,x0,y0,z0,x1,y1,z1,accent,width);else emitCornerBox(b,x0,y0,z0,x1,y1,z1,accent,fraction,width);}
            if(tracers!=null&&tracers.enabled&&d<=tracers.setting*tracers.setting){Vec3d raw=new Vec3d(p.getX(),p.getY()+p.getHeight()*0.52,p.getZ()),old=SMOOTH_TRACER_TARGETS.get(p.getUuid()),smooth=old==null?raw:old.lerp(raw,0.32);SMOOTH_TRACER_TARGETS.put(p.getUuid(),smooth);emitLine(b,(float)(smooth.x-camera.x),(float)(smooth.y-camera.y),(float)(smooth.z-camera.z),ax,ay,az,tracerColor,width);}
            rendered++;
        }
    }

    private static Vec3d crosshairAnchor(Vec3d camera,float yawDegrees,float pitchDegrees){double yaw=Math.toRadians(yawDegrees),pitch=Math.toRadians(pitchDegrees),cp=Math.cos(pitch);return camera.add(new Vec3d(-Math.sin(yaw)*cp,-Math.sin(pitch),Math.cos(yaw)*cp).multiply(0.34));}

    private static void renderLights(VertexConsumer b,MinecraftClient client,Vec3d camera,ClientModule module,int style,float width){
        double rangeSq=module.setting*module.setting;float sizeScale=(float)Math.max(0.4,Math.min(1.4,module.setting3));int rendered=0,cap=StarPerformance.lightRenderCap(client);if(style==3)cap=Math.max(24,cap*2/3);
        for(BlockPos pos:EnhancedWorldScan.lightHotspots()){
            if(rendered>=cap)break;double cx=pos.getX()+0.5,cy=pos.getY()+0.5,cz=pos.getZ()+0.5,dx=cx-client.player.getX(),dy=cy-client.player.getY(),dz=cz-client.player.getZ();if(dx*dx+dy*dy+dz*dz>rangeSq)continue;
            int level=client.world.getLightLevel(LightType.BLOCK,pos),base=level>=14?0x00FFF7B0:level>=11?0x00FFD65C:0x00FFAA48,alpha=switch(style){case 2->245;case 3->155;case 4->250;default->220;};
            int color=(alpha<<24)|base;float styleScale=switch(style){case 2->1.18F;case 3->0.72F;case 4->1.30F;default->1.0F;};float r=(0.34F+Math.max(0,level-8)*0.035F)*sizeScale*styleScale,x=(float)(cx-camera.x),y=(float)(cy-camera.y),z=(float)(cz-camera.z);
            if(style==3)emitMinimalLight(b,x,y,z,r,color,width);else emitLightHalo(b,x,y,z,r,color,style==2||style==4,width);rendered++;
        }
    }

    private static void emitMinimalLight(VertexConsumer b,float x,float y,float z,float r,int c,float w){emitLine(b,x-r,y,z,x+r,y,z,c,w);emitLine(b,x,y-r,z,x,y+r,z,c,w);emitLine(b,x,y,z-r,x,y,z+r,c,w);}
    private static void emitLightHalo(VertexConsumer b,float x,float y,float z,float r,int c,boolean extra,float w){float top=y+r,bottom=y-r,xr=x+r,xl=x-r,zf=z+r,zb=z-r;emitLine(b,x,top,z,xr,y,z,c,w);emitLine(b,x,top,z,xl,y,z,c,w);emitLine(b,x,top,z,x,y,zf,c,w);emitLine(b,x,top,z,x,y,zb,c,w);emitLine(b,x,bottom,z,xr,y,z,c,w);emitLine(b,x,bottom,z,xl,y,z,c,w);emitLine(b,x,bottom,z,x,y,zf,c,w);emitLine(b,x,bottom,z,x,y,zb,c,w);emitLine(b,xr,y,z,x,y,zf,c,w);emitLine(b,x,y,zf,xl,y,z,c,w);emitLine(b,xl,y,z,x,y,zb,c,w);emitLine(b,x,y,zb,xr,y,z,c,w);int faint=withAlpha(c,extra?125:92);emitLine(b,x-r*.38F,y,z,x+r*.38F,y,z,faint,w);emitLine(b,x,y-r*.38F,z,x,y+r*.38F,z,faint,w);emitLine(b,x,y,z-r*.38F,x,y,z+r*.38F,faint,w);}

    private static void emitCornerBox(VertexConsumer b,float x0,float y0,float z0,float x1,float y1,float z1,int c,float f,float w){corner(b,x0,y0,z0,x1,y0,z0,c,f,w);corner(b,x1,y0,z0,x1,y0,z1,c,f,w);corner(b,x1,y0,z1,x0,y0,z1,c,f,w);corner(b,x0,y0,z1,x0,y0,z0,c,f,w);corner(b,x0,y1,z0,x1,y1,z0,c,f,w);corner(b,x1,y1,z0,x1,y1,z1,c,f,w);corner(b,x1,y1,z1,x0,y1,z1,c,f,w);corner(b,x0,y1,z1,x0,y1,z0,c,f,w);corner(b,x0,y0,z0,x0,y1,z0,c,f,w);corner(b,x1,y0,z0,x1,y1,z0,c,f,w);corner(b,x1,y0,z1,x1,y1,z1,c,f,w);corner(b,x0,y0,z1,x0,y1,z1,c,f,w);}
    private static void emitFullBox(VertexConsumer b,float x0,float y0,float z0,float x1,float y1,float z1,int c,float w){emitLine(b,x0,y0,z0,x1,y0,z0,c,w);emitLine(b,x1,y0,z0,x1,y0,z1,c,w);emitLine(b,x1,y0,z1,x0,y0,z1,c,w);emitLine(b,x0,y0,z1,x0,y0,z0,c,w);emitLine(b,x0,y1,z0,x1,y1,z0,c,w);emitLine(b,x1,y1,z0,x1,y1,z1,c,w);emitLine(b,x1,y1,z1,x0,y1,z1,c,w);emitLine(b,x0,y1,z1,x0,y1,z0,c,w);emitLine(b,x0,y0,z0,x0,y1,z0,c,w);emitLine(b,x1,y0,z0,x1,y1,z0,c,w);emitLine(b,x1,y0,z1,x1,y1,z1,c,w);emitLine(b,x0,y0,z1,x0,y1,z1,c,w);}
    private static void corner(VertexConsumer b,float ax,float ay,float az,float bx,float by,float bz,int c,float f,float w){float dx=bx-ax,dy=by-ay,dz=bz-az;emitLine(b,ax,ay,az,ax+dx*f,ay+dy*f,az+dz*f,c,w);emitLine(b,bx-dx*f,by-dy*f,bz-dz*f,bx,by,bz,c,w);}
    private static int style(){ClientModule m=CincoClient.findModule("ESP Style");return m==null?1:Math.max(1,Math.min(4,(int)Math.round(m.setting)));}
    private static int withAlpha(int c,int a){return(Math.max(0,Math.min(255,a))<<24)|(c&0x00FFFFFF);}
    private static void emitLine(VertexConsumer b,float x0,float y0,float z0,float x1,float y1,float z1,int c,float w){vertex(b,x0,y0,z0,c,w);vertex(b,x1,y1,z1,c,w);}
    private static void vertex(VertexConsumer b,float x,float y,float z,int c,float w){b.vertex(x,y,z).color(c).normal(0,1,0).lineWidth(w);}
}
