package gg.cinco.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Player-locked cosmetic world effects for 1.21.11.
 *
 * The attachment point uses the exact same interpolated player transform as the accessory renderer.
 * There is deliberately no extra follow smoothing: camera movement, jumping and strafing therefore
 * cannot make the aura lag behind or rubber-band away from the rendered player.
 */
public final class StarEffectRenderer {
    private StarEffectRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientModule module = CincoClient.findModule("Animations");
        if (module == null || !module.enabled || client.player == null || client.world == null || context.consumers() == null) return;

        int style = Math.max(1, Math.min(14, (int)Math.round(module.setting)));
        double speed = Math.max(0.15, Math.min(3.0, module.setting2));
        float intensity = (float)Math.max(0.15, Math.min(1.0, module.setting3));
        int tier = StarPerformance.animationTier(client);
        double t = System.currentTimeMillis() / 1000.0 * speed;

        float tickDelta = client.getRenderTickCounter().getTickProgress(false);
        Vec3d camera = context.worldState().cameraRenderState.pos;
        Vec3d renderedPlayer = new Vec3d(
                MathHelper.lerp(tickDelta, client.player.lastX, client.player.getX()),
                MathHelper.lerp(tickDelta, client.player.lastY, client.player.getY()),
                MathHelper.lerp(tickDelta, client.player.lastZ, client.player.getZ()));
        Vec3d p = renderedPlayer.subtract(camera);

        VertexConsumer b = context.consumers().getBuffer(RenderLayers.linesTranslucent());
        int base = CosmeticColorStore.effectColor(255);
        int bright = CosmeticColorStore.lighten(base, 0.30, (int)(155 + 95 * intensity));
        int main = withAlpha(base, (int)(125 + 120 * intensity));
        int soft = CosmeticColorStore.darken(base, 0.35, (int)(42 + 90 * intensity));
        float width = tier == 0 ? 1.20F : tier == 1 ? 1.55F : 1.85F;

        switch (style) {
            case 2 -> aura(b,p.x,p.y,p.z,t,main,soft,tier,true,width);
            case 3 -> jumpCircles(b,p.x,p.y,p.z,t,main,tier,width);
            case 4 -> speedTrails(b,p.x,p.y,p.z,t,main,soft,tier,client.player.getVelocity(),width);
            case 5 -> orbitRunes(b,p.x,p.y,p.z,t,main,tier,width);
            case 6 -> lightning(b,p.x,p.y,p.z,t,bright,soft,tier,width);
            case 7 -> footstepRings(b,p.x,p.y,p.z,t,main,tier,width);
            case 8 -> cosmicPulse(b,p.x,p.y,p.z,t,main,soft,tier,width);
            case 9 -> spiralAura(b,p.x,p.y,p.z,t,bright,soft,tier,width);
            case 10 -> starOrbit(b,p.x,p.y,p.z,t,bright,main,tier,width);
            case 11 -> cometTrail(b,p.x,p.y,p.z,t,bright,soft,tier,width);
            case 12 -> shockwave(b,p.x,p.y,p.z,t,bright,soft,tier,width);
            case 13 -> galaxyCage(b,p.x,p.y,p.z,t,main,soft,tier,width);
            case 14 -> crownAura(b,p.x,p.y,p.z,t,bright,main,tier,width);
            default -> aura(b,p.x,p.y,p.z,t,main,soft,tier,false,width);
        }
    }

    private static void aura(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,boolean reverse,float w){
        int rings=tier==0?2:tier==1?3:4,seg=tier==0?20:36;
        double dir=reverse?-1.0:1.0;
        for(int r=0;r<rings;r++) ring(b,x,y+0.15+r*0.42+Math.sin(t*1.5+r)*0.035,z,
                0.40+r*0.055+Math.sin(t*1.8+r*0.7)*0.025,seg,r%2==0?c:soft,w);
        int sparks=tier==0?7:tier==1?12:18;
        for(int i=0;i<sparks;i++){
            double a=i*2.399+t*0.55*dir,rr=0.50+0.09*Math.sin(t+i*1.31);
            cross(b,x+Math.cos(a)*rr,y+0.15+((i*0.27+t*0.14)%1.65),z+Math.sin(a)*rr,0.033+(i%3)*0.007,c,w);
        }
    }

    private static void jumpCircles(VertexConsumer b,double x,double y,double z,double t,int c,int tier,float w){
        int seg=tier==0?22:40,count=tier==0?2:tier==1?3:4;
        for(int i=0;i<count;i++){
            double p=(t*0.55+i/(double)count)%1.0;
            ring(b,x,y+0.028+i*0.008,z,0.18+p*1.00,seg,withAlpha(c,(int)(220*(1.0-p))),w);
        }
    }

    private static void speedTrails(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,Vec3d velocity,float w){
        double vx=velocity.x,vz=velocity.z,len=Math.hypot(vx,vz);
        if(len<0.025){vx=-Math.sin(t*0.15);vz=Math.cos(t*0.15);len=1.0;}
        vx/=len;vz/=len;
        int lines=tier==0?4:tier==1?7:10;
        for(int i=0;i<lines;i++){
            double side=(i-(lines-1)/2.0)*0.085,sx=x+vz*side,sz=z-vx*side,sy=y+0.13+(i%4)*0.36;
            double length=0.72+(i%3)*0.24+0.08*Math.sin(t*2.4+i);
            line(b,sx,sy,sz,sx-vx*length,sy+0.025*Math.sin(t+i),sz-vz*length,i%2==0?c:soft,w);
        }
    }

    private static void orbitRunes(VertexConsumer b,double x,double y,double z,double t,int c,int tier,float w){
        int count=tier==0?5:tier==1?8:11;
        for(int i=0;i<count;i++){
            double a=t*0.58+i*Math.PI*2/count,r=0.66+0.07*Math.sin(t*1.2+i);
            diamond(b,x+Math.cos(a)*r,y+0.30+(i%5)*0.29,z+Math.sin(a)*r,0.075,c,w);
        }
        ring(b,x,y+0.93,z,0.72,tier==0?24:42,withAlpha(c,95),w);
    }

    private static void lightning(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){
        int bolts=tier==0?3:tier==1?5:7,steps=tier==0?4:7;
        for(int i=0;i<bolts;i++){
            double a=i*Math.PI*2/bolts+t*0.14,bx=x+Math.cos(a)*0.50,bz=z+Math.sin(a)*0.50,lx=bx,ly=y+0.06,lz=bz;
            for(int s=1;s<=steps;s++){
                double yy=y+0.06+1.72*s/steps,j=0.055*Math.sin(t*7+i*4.7+s*2.1);
                double nx=bx+Math.cos(a+Math.PI/2)*j,nz=bz+Math.sin(a+Math.PI/2)*j;
                line(b,lx,ly,lz,nx,yy,nz,s%2==0?soft:c,w);lx=nx;ly=yy;lz=nz;
            }
        }
    }

    private static void footstepRings(VertexConsumer b,double x,double y,double z,double t,int c,int tier,float w){
        int seg=tier==0?18:34;double p=0.20+0.055*(0.5+0.5*Math.sin(t*3.2));
        ring(b,x-0.16,y+0.027,z,p,seg,c,w);ring(b,x+0.16,y+0.029,z,p,seg,withAlpha(c,160),w);
        if(tier>0) ring(b,x,y+0.032,z,0.46+0.05*Math.sin(t*1.7),seg,withAlpha(c,70),w);
    }

    private static void cosmicPulse(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){
        int seg=tier==0?20:38;double r=0.58+0.06*Math.sin(t*1.5);
        ring(b,x,y+0.95,z,r,seg,c,w);verticalRing(b,x,y+0.95,z,r,seg,c,0,w);verticalRing(b,x,y+0.95,z,r,seg,soft,Math.PI/2,w);
        int sparks=tier==0?5:tier==1?9:14;for(int i=0;i<sparks;i++){double a=i*2.399+t*0.30;cross(b,x+Math.cos(a)*(r+0.12),y+0.42+(i%5)*0.27,z+Math.sin(a)*(r+0.12),0.038,c,w);}
    }

    private static void spiralAura(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){
        int seg=tier==0?24:tier==1?40:64;
        double px=x+0.50,py=y+0.10,pz=z;
        for(int i=1;i<=seg;i++){
            double u=i/(double)seg,a=u*Math.PI*4.0+t*0.65,r=0.50-0.12*u;
            double nx=x+Math.cos(a)*r,ny=y+0.10+u*1.72,nz=z+Math.sin(a)*r;
            line(b,px,py,pz,nx,ny,nz,i%3==0?soft:c,w);px=nx;py=ny;pz=nz;
        }
    }

    private static void starOrbit(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){
        int count=tier==0?5:tier==1?8:12;
        for(int i=0;i<count;i++){
            double a=t*0.42+i*Math.PI*2/count,r=0.63+0.05*Math.sin(t+i);
            star(b,x+Math.cos(a)*r,y+0.40+(i%4)*0.35,z+Math.sin(a)*r,0.06,i%2==0?c:soft,w);
        }
    }

    private static void cometTrail(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){
        int count=tier==0?5:tier==1?8:12;
        for(int i=0;i<count;i++){
            double phase=t*0.50+i*0.52,a=phase,r=0.56+0.03*i;
            double hx=x+Math.cos(a)*r,hy=y+0.30+(i%5)*0.30,hz=z+Math.sin(a)*r;
            double tx=hx-Math.cos(a)*0.34,ty=hy-0.04,tz=hz-Math.sin(a)*0.34;
            line(b,hx,hy,hz,tx,ty,tz,i%2==0?c:soft,w);
            cross(b,hx,hy,hz,0.035,c,w);
        }
    }

    private static void shockwave(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){
        int seg=tier==0?22:40,count=tier==0?2:4;
        for(int i=0;i<count;i++){
            double p=(t*0.42+i/(double)count)%1.0;
            double r=0.32+p*1.25;
            int color=withAlpha(i%2==0?c:soft,(int)(220*(1.0-p)));
            ring(b,x,y+0.04+i*0.018,z,r,seg,color,w);
        }
    }

    private static void galaxyCage(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){
        int seg=tier==0?22:40;double r=0.72;
        ring(b,x,y+0.92,z,r,seg,c,w);
        verticalRing(b,x,y+0.92,z,r,seg,soft,t*0.08,w);
        verticalRing(b,x,y+0.92,z,r,seg,c,Math.PI/2+t*0.05,w);
        if(tier>0) verticalRing(b,x,y+0.92,z,r*0.78,seg,withAlpha(c,110),Math.PI/4,w);
    }

    private static void crownAura(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){
        int points=tier==0?6:10;double cy=y+1.95,r=0.44;
        for(int i=0;i<points;i++){
            double a0=i*Math.PI*2/points,a1=(i+1)*Math.PI*2/points;
            double x0=x+Math.cos(a0)*r,z0=z+Math.sin(a0)*r,x1=x+Math.cos(a1)*r,z1=z+Math.sin(a1)*r;
            double peakY=cy+0.16+0.06*((i&1)==0?1:0);
            double mx=(x0+x1)*0.5,mz=(z0+z1)*0.5;
            line(b,x0,cy,z0,mx,peakY,mz,c,w);line(b,mx,peakY,mz,x1,cy,z1,soft,w);
        }
        ring(b,x,cy-0.02,z,r,points*3,withAlpha(c,150),w);
    }

    private static int withAlpha(int c,int a){return(Math.max(0,Math.min(255,a))<<24)|(c&0x00FFFFFF);}
    private static void ring(VertexConsumer b,double x,double y,double z,double r,int seg,int c,float w){double px=x+r,pz=z;for(int i=1;i<=seg;i++){double a=i*Math.PI*2/seg,nx=x+Math.cos(a)*r,nz=z+Math.sin(a)*r;line(b,px,y,pz,nx,y,nz,c,w);px=nx;pz=nz;}}
    private static void verticalRing(VertexConsumer b,double x,double y,double z,double r,int seg,int c,double rot,float w){double px=x+Math.cos(rot)*r,py=y,pz=z+Math.sin(rot)*r;for(int i=1;i<=seg;i++){double a=i*Math.PI*2/seg,h=Math.cos(a)*r,nx=x+Math.cos(rot)*h,nz=z+Math.sin(rot)*h,ny=y+Math.sin(a)*r;line(b,px,py,pz,nx,ny,nz,c,w);px=nx;py=ny;pz=nz;}}
    private static void diamond(VertexConsumer b,double x,double y,double z,double s,int c,float w){line(b,x,y+s,z,x+s,y,z,c,w);line(b,x+s,y,z,x,y-s,z,c,w);line(b,x,y-s,z,x-s,y,z,c,w);line(b,x-s,y,z,x,y+s,z,c,w);}
    private static void star(VertexConsumer b,double x,double y,double z,double s,int c,float w){line(b,x-s,y,z,x+s,y,z,c,w);line(b,x,y-s,z,x,y+s,z,c,w);line(b,x,y,z-s,x,y,z+s,c,w);}
    private static void cross(VertexConsumer b,double x,double y,double z,double s,int c,float w){line(b,x-s,y,z,x+s,y,z,c,w);line(b,x,y-s,z,x,y+s,z,c,w);}
    private static void line(VertexConsumer b,double x0,double y0,double z0,double x1,double y1,double z1,int c,float w){vertex(b,x0,y0,z0,c,w);vertex(b,x1,y1,z1,c,w);}
    private static void vertex(VertexConsumer b,double x,double y,double z,int c,float w){b.vertex((float)x,(float)y,(float)z).color(c).normal(0,1,0).lineWidth(w);}
}
