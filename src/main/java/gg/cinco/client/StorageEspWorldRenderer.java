package gg.cinco.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Thin storage outlines with straight or snake tracers for Fabric 1.21.11. */
public final class StorageEspWorldRenderer {
    private StorageEspWorldRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client=MinecraftClient.getInstance();
        ClientModule module=CincoClient.findModule("Storage ESP");
        if(module==null||!module.enabled||client.player==null||client.world==null||context.consumers()==null)return;

        int style=style();
        Vec3d camera=context.worldState().cameraRenderState.pos;
        float yaw=client.gameRenderer.getCamera().getYaw(),pitch=client.gameRenderer.getCamera().getPitch();
        Vec3d anchor=crosshairAnchor(camera,yaw,pitch);
        float anchorX=(float)(anchor.x-camera.x),anchorY=(float)(anchor.y-camera.y),anchorZ=(float)(anchor.z-camera.z);
        double range=module.setting,rangeSq=range*range,snakeTime=System.nanoTime()/1_000_000_000.0;
        float width=switch(style){case 2->2.05F;case 3->1.0F;case 4->2.45F;default->1.35F;};
        VertexConsumer buffer=context.consumers().getBuffer(RenderLayers.linesTranslucent());

        int rendered=0,renderCap=StarPerformance.storageRenderCap(client);
        for(BlockPos pos:EnhancedWorldScan.storage()){
            if(rendered>=renderCap)break;
            double cx=pos.getX()+0.5,cy=pos.getY()+0.5,cz=pos.getZ()+0.5;
            double dx=cx-camera.x,dy=cy-camera.y,dz=cz-camera.z;
            double playerDistanceSq=client.player.squaredDistanceTo(cx,cy,cz);
            if(playerDistanceSq>rangeSq)continue;
            BlockState state=client.world.getBlockState(pos);
            StorageEspConfig.TypeKey type=StorageEspConfig.typeOf(state); if(type==null)continue;
            StorageEspConfig.Entry setting=StorageEspConfig.get(type); if(!setting.highlight&&!setting.tracer)continue;

            float x0=(float)(pos.getX()-camera.x-0.0025),y0=(float)(pos.getY()-camera.y-0.0025),z0=(float)(pos.getZ()-camera.z-0.0025);
            float x1=x0+1.005F,y1=y0+1.005F,z1=z0+1.005F;
            int farAlpha=style==3?70:style==2?135:105,nearAlpha=style==3?180:245;
            int outline=distanceFade(setting.color,Math.sqrt(playerDistanceSq),range,farAlpha,nearAlpha);
            if(setting.highlight){if(style==3)emitCornerBox(buffer,x0,y0,z0,x1,y1,z1,outline,0.28F,width);else emitBox(buffer,x0,y0,z0,x1,y1,z1,outline,width);}
            if(setting.tracer){
                int tracerAlpha=style==3?82:style==2?175:135;
                int tracer=withAlpha(setting.color,Math.min(tracerAlpha,(outline>>>24)&0xFF));
                if(setting.snakeTracer)emitSnake(buffer,(float)dx,(float)dy,(float)dz,anchorX,anchorY,anchorZ,tracer,pos.asLong(),snakeTime,width);
                else emitLine(buffer,(float)dx,(float)dy,(float)dz,anchorX,anchorY,anchorZ,tracer,width);
            }
            rendered++;
        }
    }

    private static Vec3d crosshairAnchor(Vec3d camera,float yawDegrees,float pitchDegrees){double yaw=Math.toRadians(yawDegrees),pitch=Math.toRadians(pitchDegrees),cp=Math.cos(pitch);return camera.add(new Vec3d(-Math.sin(yaw)*cp,-Math.sin(pitch),Math.cos(yaw)*cp).multiply(0.34));}

    private static void emitSnake(VertexConsumer b,float x0,float y0,float z0,float x1,float y1,float z1,int color,long seed,double time,float width){
        double dx=x1-x0,dy=y1-y0,dz=z1-z0,length=Math.sqrt(dx*dx+dy*dy+dz*dz);if(length<0.001)return;
        double nx=dx/length,ny=dy/length,nz=dz/length,px=-nz,py=0,pz=nx,plen=Math.sqrt(px*px+pz*pz);
        if(plen<0.08){px=0;py=nz;pz=-ny;plen=Math.sqrt(py*py+pz*pz);}if(plen<0.001)return;px/=plen;py/=plen;pz/=plen;
        double qx=ny*pz-nz*py,qy=nz*px-nx*pz,qz=nx*py-ny*px;
        int segments=Math.max(12,Math.min(28,(int)Math.round(length*1.35)));double amplitude=Math.min(0.30,0.075+length*0.0065),phase=time*4.8+(seed&0xFFFFL)*0.0017;
        float lx=x0,ly=y0,lz=z0;int baseAlpha=(color>>>24)&255;
        for(int i=1;i<=segments;i++){double t=i/(double)segments,taper=Math.sin(Math.PI*t),wave=Math.sin(t*Math.PI*8-phase)*amplitude*taper,curl=Math.cos(t*Math.PI*4-phase*0.72)*amplitude*0.34*taper;
            float cx=(float)(x0+dx*t+px*wave+qx*curl),cy=(float)(y0+dy*t+py*wave+qy*curl),cz=(float)(z0+dz*t+pz*wave+qz*curl);
            int a=Math.max(28,Math.min(255,(int)Math.round(baseAlpha*(0.78+0.22*Math.sin(phase+t*Math.PI*6)))));emitLine(b,lx,ly,lz,cx,cy,cz,withAlpha(color,a),width);lx=cx;ly=cy;lz=cz;}
    }

    private static int style(){ClientModule m=CincoClient.findModule("ESP Style");return m==null?1:Math.max(1,Math.min(4,(int)Math.round(m.setting)));}
    private static int distanceFade(int color,double distance,double range,int farAlpha,int nearAlpha){double t=range<=0.001?0:Math.max(0,Math.min(1,distance/range));return withAlpha(color,(int)Math.round(nearAlpha+(farAlpha-nearAlpha)*t));}
    private static int withAlpha(int color,int alpha){return(Math.max(0,Math.min(255,alpha))<<24)|(color&0x00FFFFFF);}

    private static void emitBox(VertexConsumer b,float x0,float y0,float z0,float x1,float y1,float z1,int c,float w){
        emitLine(b,x0,y0,z0,x1,y0,z0,c,w);emitLine(b,x1,y0,z0,x1,y0,z1,c,w);emitLine(b,x1,y0,z1,x0,y0,z1,c,w);emitLine(b,x0,y0,z1,x0,y0,z0,c,w);
        emitLine(b,x0,y1,z0,x1,y1,z0,c,w);emitLine(b,x1,y1,z0,x1,y1,z1,c,w);emitLine(b,x1,y1,z1,x0,y1,z1,c,w);emitLine(b,x0,y1,z1,x0,y1,z0,c,w);
        emitLine(b,x0,y0,z0,x0,y1,z0,c,w);emitLine(b,x1,y0,z0,x1,y1,z0,c,w);emitLine(b,x1,y0,z1,x1,y1,z1,c,w);emitLine(b,x0,y0,z1,x0,y1,z1,c,w);
    }
    private static void emitCornerBox(VertexConsumer b,float x0,float y0,float z0,float x1,float y1,float z1,int c,float f,float w){corner(b,x0,y0,z0,x1,y0,z0,c,f,w);corner(b,x1,y0,z0,x1,y0,z1,c,f,w);corner(b,x1,y0,z1,x0,y0,z1,c,f,w);corner(b,x0,y0,z1,x0,y0,z0,c,f,w);corner(b,x0,y1,z0,x1,y1,z0,c,f,w);corner(b,x1,y1,z0,x1,y1,z1,c,f,w);corner(b,x1,y1,z1,x0,y1,z1,c,f,w);corner(b,x0,y1,z1,x0,y1,z0,c,f,w);corner(b,x0,y0,z0,x0,y1,z0,c,f,w);corner(b,x1,y0,z0,x1,y1,z0,c,f,w);corner(b,x1,y0,z1,x1,y1,z1,c,f,w);corner(b,x0,y0,z1,x0,y1,z1,c,f,w);}
    private static void corner(VertexConsumer b,float ax,float ay,float az,float bx,float by,float bz,int c,float f,float w){float dx=bx-ax,dy=by-ay,dz=bz-az;emitLine(b,ax,ay,az,ax+dx*f,ay+dy*f,az+dz*f,c,w);emitLine(b,bx-dx*f,by-dy*f,bz-dz*f,bx,by,bz,c,w);}
    private static void emitLine(VertexConsumer b,float x0,float y0,float z0,float x1,float y1,float z1,int c,float w){vertex(b,x0,y0,z0,c,w);vertex(b,x1,y1,z1,c,w);}
    private static void vertex(VertexConsumer b,float x,float y,float z,int c,float w){b.vertex(x,y,z).color(c).normal(0,1,0).lineWidth(w);}
}
