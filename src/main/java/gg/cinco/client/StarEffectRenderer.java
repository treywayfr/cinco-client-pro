package gg.cinco.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Detailed player-locked Locker cosmetic effects for 1.21.11. */
public final class StarEffectRenderer {
    private StarEffectRenderer() {}
    private static double originX,originY,originZ,currentScale=1.0;

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientModule module = CincoClient.findModule("Animations");
        if (module == null || !module.enabled || client.player == null || client.world == null || context.consumers() == null) return;

        int style = Math.max(1, Math.min(20, (int)Math.round(module.setting)));
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
        originX=p.x;originY=p.y;originZ=p.z;currentScale=LockerCosmeticStore.size("effect",style);

        VertexConsumer b = context.consumers().getBuffer(RenderLayers.linesTranslucent());
        CosmeticColorStore.Profile profile=LockerCosmeticStore.resolvedColor("effect",style);
        int base = CosmeticColorStore.color(profile,255);
        int bright = CosmeticColorStore.lighten(base, 0.30, (int)(155 + 95 * intensity));
        int main = withAlpha(base, (int)(125 + 120 * intensity));
        int soft = CosmeticColorStore.darken(base, 0.35, (int)(42 + 90 * intensity));
        float width = (tier == 0 ? 1.20F : tier == 1 ? 1.55F : 1.85F) * (float)Math.max(.75,Math.min(1.35,currentScale));

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
            case 15 -> meteorHalo(b,p.x,p.y,p.z,t,bright,soft,tier,width);
            case 16 -> voidHelix(b,p.x,p.y,p.z,t,bright,soft,tier,width);
            case 17 -> heartBurst(b,p.x,p.y,p.z,t,bright,soft,tier,width);
            case 18 -> solarRings(b,p.x,p.y,p.z,t,bright,main,tier,width);
            case 19 -> runeStorm(b,p.x,p.y,p.z,t,bright,soft,tier,width);
            case 20 -> auroraVeil(b,p.x,p.y,p.z,t,bright,soft,tier,width);
            default -> aura(b,p.x,p.y,p.z,t,main,soft,tier,false,width);
        }
        if(tier>0)detailPass(b,p.x,p.y,p.z,t,style,withAlpha(bright,110),width*.72f);
    }

    private static void aura(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,boolean reverse,float w){
        int rings=tier==0?3:tier==1?4:5,seg=tier==0?24:42;double dir=reverse?-1.0:1.0;
        for(int r=0;r<rings;r++) ring(b,x,y+0.15+r*0.36+Math.sin(t*1.5+r)*0.035,z,0.40+r*0.055+Math.sin(t*1.8+r*.7)*0.025,seg,r%2==0?c:soft,w);
        int sparks=tier==0?8:tier==1?14:22;for(int i=0;i<sparks;i++){double a=i*2.399+t*.55*dir,rr=.50+.09*Math.sin(t+i*1.31);cross(b,x+Math.cos(a)*rr,y+.15+((i*.27+t*.14)%1.65),z+Math.sin(a)*rr,.033+(i%3)*.007,c,w);}
    }
    private static void jumpCircles(VertexConsumer b,double x,double y,double z,double t,int c,int tier,float w){int seg=tier==0?24:44,count=tier==0?3:tier==1?4:5;for(int i=0;i<count;i++){double p=(t*.55+i/(double)count)%1.0;ring(b,x,y+.028+i*.008,z,.18+p*1.00,seg,withAlpha(c,(int)(220*(1-p))),w);}}
    private static void speedTrails(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,Vec3d velocity,float w){double vx=velocity.x,vz=velocity.z,len=Math.hypot(vx,vz);if(len<.025){vx=-Math.sin(t*.15);vz=Math.cos(t*.15);len=1;}vx/=len;vz/=len;int lines=tier==0?5:tier==1?9:13;for(int i=0;i<lines;i++){double side=(i-(lines-1)/2.0)*.075,sx=x+vz*side,sz=z-vx*side,sy=y+.13+(i%5)*.30,length=.72+(i%3)*.24+.08*Math.sin(t*2.4+i);line(b,sx,sy,sz,sx-vx*length,sy+.025*Math.sin(t+i),sz-vz*length,i%2==0?c:soft,w);}}
    private static void orbitRunes(VertexConsumer b,double x,double y,double z,double t,int c,int tier,float w){int count=tier==0?6:tier==1?10:14;for(int i=0;i<count;i++){double a=t*.58+i*Math.PI*2/count,r=.66+.07*Math.sin(t*1.2+i);diamond(b,x+Math.cos(a)*r,y+.27+(i%6)*.25,z+Math.sin(a)*r,.075,c,w);}ring(b,x,y+.93,z,.72,tier==0?28:48,withAlpha(c,95),w);}
    private static void lightning(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){int bolts=tier==0?4:tier==1?6:9,steps=tier==0?5:8;for(int i=0;i<bolts;i++){double a=i*Math.PI*2/bolts+t*.14,bx=x+Math.cos(a)*.50,bz=z+Math.sin(a)*.50,lx=bx,ly=y+.06,lz=bz;for(int s=1;s<=steps;s++){double yy=y+.06+1.72*s/steps,j=.055*Math.sin(t*7+i*4.7+s*2.1),nx=bx+Math.cos(a+Math.PI/2)*j,nz=bz+Math.sin(a+Math.PI/2)*j;line(b,lx,ly,lz,nx,yy,nz,s%2==0?soft:c,w);lx=nx;ly=yy;lz=nz;}}}
    private static void footstepRings(VertexConsumer b,double x,double y,double z,double t,int c,int tier,float w){int seg=tier==0?20:38;double p=.20+.055*(.5+.5*Math.sin(t*3.2));ring(b,x-.16,y+.027,z,p,seg,c,w);ring(b,x+.16,y+.029,z,p,seg,withAlpha(c,160),w);ring(b,x,y+.032,z,.46+.05*Math.sin(t*1.7),seg,withAlpha(c,70),w);}
    private static void cosmicPulse(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){int seg=tier==0?24:44;double r=.58+.06*Math.sin(t*1.5);ring(b,x,y+.95,z,r,seg,c,w);verticalRing(b,x,y+.95,z,r,seg,c,0,w);verticalRing(b,x,y+.95,z,r,seg,soft,Math.PI/2,w);int sparks=tier==0?7:tier==1?12:18;for(int i=0;i<sparks;i++){double a=i*2.399+t*.30;cross(b,x+Math.cos(a)*(r+.12),y+.38+(i%6)*.24,z+Math.sin(a)*(r+.12),.038,c,w);}}
    private static void spiralAura(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){int seg=tier==0?30:tier==1?52:78;for(int strand=0;strand<2;strand++){double px=x+.50,py=y+.10,pz=z;for(int i=1;i<=seg;i++){double u=i/(double)seg,a=u*Math.PI*4+t*.65+strand*Math.PI,r=.50-.12*u,nx=x+Math.cos(a)*r,ny=y+.10+u*1.72,nz=z+Math.sin(a)*r;line(b,px,py,pz,nx,ny,nz,(i+strand)%3==0?soft:c,w);px=nx;py=ny;pz=nz;}}}
    private static void starOrbit(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){int count=tier==0?6:tier==1?10:15;for(int i=0;i<count;i++){double a=t*.42+i*Math.PI*2/count,r=.63+.05*Math.sin(t+i);star(b,x+Math.cos(a)*r,y+.35+(i%5)*.31,z+Math.sin(a)*r,.06,i%2==0?c:soft,w);}}
    private static void cometTrail(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){int count=tier==0?6:tier==1?10:15;for(int i=0;i<count;i++){double a=t*.50+i*.52,r=.56+.025*i,hx=x+Math.cos(a)*r,hy=y+.26+(i%6)*.26,hz=z+Math.sin(a)*r,tx=hx-Math.cos(a)*.38,ty=hy-.05,tz=hz-Math.sin(a)*.38;line(b,hx,hy,hz,tx,ty,tz,i%2==0?c:soft,w);cross(b,hx,hy,hz,.035,c,w);}}
    private static void shockwave(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){int seg=tier==0?26:46,count=tier==0?3:5;for(int i=0;i<count;i++){double p=(t*.42+i/(double)count)%1,r=.32+p*1.25;ring(b,x,y+.04+i*.018,z,r,seg,withAlpha(i%2==0?c:soft,(int)(220*(1-p))),w);}}
    private static void galaxyCage(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){int seg=tier==0?26:48;double r=.72;ring(b,x,y+.92,z,r,seg,c,w);verticalRing(b,x,y+.92,z,r,seg,soft,t*.08,w);verticalRing(b,x,y+.92,z,r,seg,c,Math.PI/2+t*.05,w);verticalRing(b,x,y+.92,z,r*.78,seg,withAlpha(c,110),Math.PI/4,w);if(tier>1)verticalRing(b,x,y+.92,z,r*.60,seg,withAlpha(c,80),-Math.PI/4,w);}
    private static void crownAura(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){int points=tier==0?8:12;double cy=y+1.95,r=.44;for(int i=0;i<points;i++){double a0=i*Math.PI*2/points,a1=(i+1)*Math.PI*2/points,x0=x+Math.cos(a0)*r,z0=z+Math.sin(a0)*r,x1=x+Math.cos(a1)*r,z1=z+Math.sin(a1)*r,peakY=cy+.16+.06*((i&1)==0?1:0),mx=(x0+x1)*.5,mz=(z0+z1)*.5;line(b,x0,cy,z0,mx,peakY,mz,c,w);line(b,mx,peakY,mz,x1,cy,z1,soft,w);}ring(b,x,cy-.02,z,r,points*3,withAlpha(c,150),w);}

    private static void meteorHalo(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){ring(b,x,y+1.78,z,.58,tier==0?28:52,c,w);int n=tier==0?5:tier==1?8:12;for(int i=0;i<n;i++){double a=t*.46+i*Math.PI*2/n,r=.62+.06*(i%3),hx=x+Math.cos(a)*r,hy=y+.35+(i%5)*.31,hz=z+Math.sin(a)*r;star(b,hx,hy,hz,.045,c,w);line(b,hx,hy,hz,hx-Math.cos(a)*.30,hy+.18,hz-Math.sin(a)*.30,soft,w);}}
    private static void voidHelix(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){int seg=tier==0?36:tier==1?60:90;for(int strand=0;strand<3;strand++){double px=x+Math.cos(strand*2.094+t)*.48,py=y+.05,pz=z+Math.sin(strand*2.094+t)*.48;for(int i=1;i<=seg;i++){double u=i/(double)seg,a=t*.65+u*Math.PI*4+strand*2.094,r=.48-.10*u,nx=x+Math.cos(a)*r,ny=y+.05+u*1.80,nz=z+Math.sin(a)*r;line(b,px,py,pz,nx,ny,nz,strand==1?soft:c,w);px=nx;py=ny;pz=nz;}}}
    private static void heartBurst(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){int hearts=tier==0?4:tier==1?7:10;for(int h=0;h<hearts;h++){double a=t*.25+h*Math.PI*2/hearts,cx=x+Math.cos(a)*(.52+.05*(h%2)),cy=y+.38+(h%5)*.30,cz=z+Math.sin(a)*(.52+.05*(h%2));heart(b,cx,cy,cz,.07+.015*(h%3),h%2==0?c:soft,w);}}
    private static void solarRings(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){double r=.62+.04*Math.sin(t);ring(b,x,y+.95,z,r,tier==0?28:54,c,w);verticalRing(b,x,y+.95,z,r,tier==0?28:54,soft,t*.18,w);verticalRing(b,x,y+.95,z,r,tier==0?28:54,c,-t*.13+Math.PI/2,w);int n=tier==0?4:8;for(int i=0;i<n;i++){double a=t*.32+i*Math.PI*2/n;star(b,x+Math.cos(a)*.82,y+.95+.18*Math.sin(a*2),z+Math.sin(a)*.82,.04,c,w);}}
    private static void runeStorm(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){int n=tier==0?7:tier==1?12:18;for(int i=0;i<n;i++){double a=t*.7+i*Math.PI*2/n,r=.50+.22*((i%3)/2.0),yy=y+.18+(i%7)*.25;diamond(b,x+Math.cos(a)*r,yy,z+Math.sin(a)*r,.055,i%2==0?c:soft,w);if(i%3==0)line(b,x+Math.cos(a)*r,yy,z+Math.sin(a)*r,x+Math.cos(a+.22)*r,yy+.15,z+Math.sin(a+.22)*r,c,w);}}
    private static void auroraVeil(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){int bands=tier==0?4:tier==1?7:10,steps=tier==0?8:14;for(int band=0;band<bands;band++){double a=band*Math.PI*2/bands+t*.10;double px=x+Math.cos(a)*.62,py=y+.08,pz=z+Math.sin(a)*.62;for(int i=1;i<=steps;i++){double u=i/(double)steps,rr=.62+.08*Math.sin(t*1.2+band+u*5),nx=x+Math.cos(a+.18*Math.sin(u*3+t))*rr,ny=y+.08+u*1.72,nz=z+Math.sin(a+.18*Math.sin(u*3+t))*rr;line(b,px,py,pz,nx,ny,nz,(band+i)%2==0?c:soft,w);px=nx;py=ny;pz=nz;}}}
    private static void detailPass(VertexConsumer b,double x,double y,double z,double t,int style,int c,float w){if(style==4||style==11||style==15||style==20)return;int n=4;for(int i=0;i<n;i++){double a=t*.17+i*Math.PI/2;cross(b,x+Math.cos(a)*.76,y+.24+i*.39,z+Math.sin(a)*.76,.022,c,w);}}

    private static void heart(VertexConsumer b,double x,double y,double z,double s,int c,float w){double px=x,py=y-s*.2,pz=z;for(int i=1;i<=24;i++){double a=i*Math.PI*2/24,sn=Math.sin(a),hx=s*sn*sn*sn*1.05,hy=s*(.8125*Math.cos(a)-.3125*Math.cos(2*a)-.125*Math.cos(3*a)-.0625*Math.cos(4*a));double nx=x+hx,ny=y+hy,nz=z;line(b,px,py,pz,nx,ny,nz,c,w);px=nx;py=ny;pz=nz;}}
    private static int withAlpha(int c,int a){return(Math.max(0,Math.min(255,a))<<24)|(c&0x00FFFFFF);}
    private static void ring(VertexConsumer b,double x,double y,double z,double r,int seg,int c,float w){double px=x+r,pz=z;for(int i=1;i<=seg;i++){double a=i*Math.PI*2/seg,nx=x+Math.cos(a)*r,nz=z+Math.sin(a)*r;line(b,px,y,pz,nx,y,nz,c,w);px=nx;pz=nz;}}
    private static void verticalRing(VertexConsumer b,double x,double y,double z,double r,int seg,int c,double rot,float w){double px=x+Math.cos(rot)*r,py=y,pz=z+Math.sin(rot)*r;for(int i=1;i<=seg;i++){double a=i*Math.PI*2/seg,h=Math.cos(a)*r,nx=x+Math.cos(rot)*h,nz=z+Math.sin(rot)*h,ny=y+Math.sin(a)*r;line(b,px,py,pz,nx,ny,nz,c,w);px=nx;py=ny;pz=nz;}}
    private static void diamond(VertexConsumer b,double x,double y,double z,double s,int c,float w){line(b,x,y+s,z,x+s,y,z,c,w);line(b,x+s,y,z,x,y-s,z,c,w);line(b,x,y-s,z,x-s,y,z,c,w);line(b,x-s,y,z,x,y+s,z,c,w);}
    private static void star(VertexConsumer b,double x,double y,double z,double s,int c,float w){line(b,x-s,y,z,x+s,y,z,c,w);line(b,x,y-s,z,x,y+s,z,c,w);line(b,x,y,z-s,x,y,z+s,c,w);}
    private static void cross(VertexConsumer b,double x,double y,double z,double s,int c,float w){line(b,x-s,y,z,x+s,y,z,c,w);line(b,x,y-s,z,x,y+s,z,c,w);}
    private static void line(VertexConsumer b,double x0,double y0,double z0,double x1,double y1,double z1,int c,float w){vertex(b,x0,y0,z0,c,w);vertex(b,x1,y1,z1,c,w);}
    private static void vertex(VertexConsumer b,double x,double y,double z,int c,float w){double sx=originX+(x-originX)*currentScale,sy=originY+(y-originY)*currentScale,sz=originZ+(z-originZ)*currentScale;b.vertex((float)sx,(float)sy,(float)sz).color(c).normal(0,1,0).lineWidth(w);}
}
