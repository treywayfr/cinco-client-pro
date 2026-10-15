package gg.cinco.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;

/** Lightweight world-space cosmetic animations using the 1.21.11 world render consumer. */
public final class StarEffectRenderer {
    private StarEffectRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientModule module = CincoClient.findModule("Animations");
        if (module == null || !module.enabled || client.player == null || client.world == null || context.consumers() == null) return;

        int style=Math.max(1,Math.min(8,(int)Math.round(module.setting)));
        double speed=Math.max(0.2,Math.min(3.0,module.setting2));
        float intensity=(float)Math.max(0.2,Math.min(1.0,module.setting3));
        int tier=StarPerformance.animationTier(client);
        double t=System.currentTimeMillis()/1000.0*speed;
        Vec3d cam=context.worldState().cameraRenderState.pos;
        double px=client.player.getX()-cam.x,py=client.player.getY()-cam.y,pz=client.player.getZ()-cam.z;

        VertexConsumer b=context.consumers().getBuffer(RenderLayers.linesTranslucent());
        int color=styleColor(style),alpha=(int)(120+120*intensity);
        int c=withAlpha(color,alpha),soft=withAlpha(color,(int)(45+80*intensity));
        float width=tier==0?1.25F:1.8F;

        switch(style){
            case 2->aura(b,px,py,pz,t,c,soft,tier,true,width);
            case 3->jumpCircles(b,px,py,pz,t,c,tier,client.player.isOnGround(),width);
            case 4->speedTrails(b,px,py,pz,t,c,soft,tier,client.player.getVelocity(),width);
            case 5->orbitRunes(b,px,py,pz,t,c,tier,width);
            case 6->lightning(b,px,py,pz,t,c,soft,tier,width);
            case 7->footstepRings(b,px,py,pz,t,c,tier,width);
            case 8->cosmicPulse(b,px,py,pz,t,c,soft,tier,width);
            default->aura(b,px,py,pz,t,c,soft,tier,false,width);
        }
    }

    private static void aura(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,boolean purple,float w){
        int rings=tier==0?2:4,seg=tier==0?20:36;
        for(int r=0;r<rings;r++) ring(b,x,y+0.18+r*0.43+Math.sin(t*1.7+r)*0.05,z,
                0.40+r*0.06+Math.sin(t*2.0+r*0.8)*0.04,seg,r%2==0?c:soft,w);
        int sparks=tier==0?8:18;
        for(int i=0;i<sparks;i++){
            double a=i*2.399+t*(purple?0.75:0.48),rr=0.48+0.12*Math.sin(t+i*1.31);
            cross(b,x+Math.cos(a)*rr,y+0.15+((i*0.29+t*0.18)%1.7),z+Math.sin(a)*rr,0.035+(i%3)*0.008,c,w);
        }
    }
    private static void jumpCircles(VertexConsumer b,double x,double y,double z,double t,int c,int tier,boolean onGround,float w){
        int seg=tier==0?22:40,count=tier==0?2:4; double lift=onGround?0.025:-0.05;
        for(int i=0;i<count;i++){double p=(t*0.62+i/(double)count)%1.0; ring(b,x,y+lift+i*0.012,z,0.18+p*1.08,seg,withAlpha(c,(int)(220*(1.0-p))),w);}
    }
    private static void speedTrails(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,Vec3d velocity,float w){
        double vx=velocity.x,vz=velocity.z,len=Math.hypot(vx,vz); if(len<0.025){vx=-Math.sin(t*0.35);vz=Math.cos(t*0.35);len=1.0;} vx/=len;vz/=len;
        int lines=tier==0?5:11;
        for(int i=0;i<lines;i++){double side=(i-(lines-1)/2.0)*0.09,sx=x+vz*side,sz=z-vx*side,sy=y+0.12+(i%4)*0.38,length=0.75+(i%3)*0.28+0.10*Math.sin(t*3.0+i); line(b,sx,sy,sz,sx-vx*length,sy+0.04*Math.sin(t+i),sz-vz*length,i%2==0?c:soft,w);}
    }
    private static void orbitRunes(VertexConsumer b,double x,double y,double z,double t,int c,int tier,float w){
        int count=tier==0?6:10; for(int i=0;i<count;i++){double a=t*0.7+i*Math.PI*2/count,r=0.65+0.10*Math.sin(t*1.4+i); diamond(b,x+Math.cos(a)*r,y+0.25+(i%5)*0.32,z+Math.sin(a)*r,0.08,c,w);} ring(b,x,y+0.95,z,0.72,tier==0?24:42,withAlpha(c,95),w);
    }
    private static void lightning(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){
        int bolts=tier==0?4:8,steps=tier==0?4:7; for(int i=0;i<bolts;i++){double a=i*Math.PI*2/bolts+t*0.19,bx=x+Math.cos(a)*0.50,bz=z+Math.sin(a)*0.50,lx=bx,ly=y+0.05,lz=bz; for(int s=1;s<=steps;s++){double yy=y+0.05+1.75*s/steps,j=0.07*Math.sin(t*8+i*4.7+s*2.1),nx=bx+Math.cos(a+Math.PI/2)*j,nz=bz+Math.sin(a+Math.PI/2)*j; line(b,lx,ly,lz,nx,yy,nz,s%2==0?soft:c,w);lx=nx;ly=yy;lz=nz;}}
    }
    private static void footstepRings(VertexConsumer b,double x,double y,double z,double t,int c,int tier,float w){int seg=tier==0?18:34;double p=0.20+0.08*(0.5+0.5*Math.sin(t*4));ring(b,x-0.16,y+0.025,z,p,seg,c,w);ring(b,x+0.16,y+0.027,z,p,seg,withAlpha(c,150),w);ring(b,x,y+0.03,z,0.48+0.08*Math.sin(t*2),seg,withAlpha(c,75),w);}
    private static void cosmicPulse(VertexConsumer b,double x,double y,double z,double t,int c,int soft,int tier,float w){int seg=tier==0?20:38;double r=0.58+0.08*Math.sin(t*1.8);ring(b,x,y+0.95,z,r,seg,c,w);verticalRing(b,x,y+0.95,z,r,seg,c,0,w);verticalRing(b,x,y+0.95,z,r,seg,soft,Math.PI/2,w);int sparks=tier==0?6:14;for(int i=0;i<sparks;i++){double a=i*2.399+t*0.35;cross(b,x+Math.cos(a)*(r+0.12),y+0.4+(i%5)*0.27,z+Math.sin(a)*(r+0.12),0.04,c,w);}}

    private static int styleColor(int s){return switch(s){case 1->0xFFFFD65A;case 2,5,8->0xFFA75CFF;case 3->0xFF7B68FF;case 4->0xFF65A8FF;case 6->0xFFD568FF;case 7->0xFFB786FF;default->0xFFA75CFF;};}
    private static int withAlpha(int c,int a){return(Math.max(0,Math.min(255,a))<<24)|(c&0x00FFFFFF);}
    private static void ring(VertexConsumer b,double x,double y,double z,double r,int seg,int c,float w){double px=x+r,pz=z;for(int i=1;i<=seg;i++){double a=i*Math.PI*2/seg,nx=x+Math.cos(a)*r,nz=z+Math.sin(a)*r;line(b,px,y,pz,nx,y,nz,c,w);px=nx;pz=nz;}}
    private static void verticalRing(VertexConsumer b,double x,double y,double z,double r,int seg,int c,double rot,float w){double px=x+Math.cos(rot)*r,py=y,pz=z+Math.sin(rot)*r;for(int i=1;i<=seg;i++){double a=i*Math.PI*2/seg,h=Math.cos(a)*r,nx=x+Math.cos(rot)*h,nz=z+Math.sin(rot)*h,ny=y+Math.sin(a)*r;line(b,px,py,pz,nx,ny,nz,c,w);px=nx;py=ny;pz=nz;}}
    private static void diamond(VertexConsumer b,double x,double y,double z,double s,int c,float w){line(b,x,y+s,z,x+s,y,z,c,w);line(b,x+s,y,z,x,y-s,z,c,w);line(b,x,y-s,z,x-s,y,z,c,w);line(b,x-s,y,z,x,y+s,z,c,w);}
    private static void cross(VertexConsumer b,double x,double y,double z,double s,int c,float w){line(b,x-s,y,z,x+s,y,z,c,w);line(b,x,y-s,z,x,y+s,z,c,w);}
    private static void line(VertexConsumer b,double x0,double y0,double z0,double x1,double y1,double z1,int c,float w){vertex(b,x0,y0,z0,c,w);vertex(b,x1,y1,z1,c,w);}
    private static void vertex(VertexConsumer b,double x,double y,double z,int c,float w){b.vertex((float)x,(float)y,(float)z).color(c).normal(0,1,0).lineWidth(w);}
}
