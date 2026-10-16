package gg.cinco.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Extra high-detail layer for Locker accessories/effects. Heavy micro-detail is performance gated. */
public final class PremiumCosmeticDetailRenderer {
    private PremiumCosmeticDetailRenderer(){}

    public static void render(WorldRenderContext ctx){
        MinecraftClient client=MinecraftClient.getInstance();
        if(client.player==null||client.world==null||ctx.consumers()==null)return;
        int tier=StarPerformance.animationTier(client);if(tier<=0)return;
        ClientModule acc=CincoClient.findModule("Custom Accessory"),fx=CincoClient.findModule("Animations");
        if((acc==null||!acc.enabled)&&(fx==null||!fx.enabled))return;
        float td=client.getRenderTickCounter().getTickProgress(false);
        Vec3d camera=ctx.worldState().cameraRenderState.pos;
        Vec3d rp=new Vec3d(MathHelper.lerp(td,client.player.lastX,client.player.getX()),MathHelper.lerp(td,client.player.lastY,client.player.getY()),MathHelper.lerp(td,client.player.lastZ,client.player.getZ()));
        Vec3d p=rp.subtract(camera);double yaw=Math.toRadians(client.player.bodyYaw),t=System.currentTimeMillis()/1000.0;
        VertexConsumer b=ctx.consumers().getBuffer(RenderLayers.debugQuads());
        if(acc!=null&&acc.enabled)accessoryDetail(b,p,yaw,acc,tier,t);
        if(fx!=null&&fx.enabled)effectDetail(b,p,yaw,fx,tier,t);
    }

    private static void accessoryDetail(VertexConsumer b,Vec3d p,double yaw,ClientModule m,int tier,double t){
        int style=Math.max(1,Math.min(20,(int)Math.round(m.setting)));double s=LockerCosmeticStore.size("accessory",style);
        CosmeticColorStore.Profile profile=LockerCosmeticStore.resolvedColor("accessory",style);
        int main=CosmeticColorStore.color(profile,190),light=CosmeticColorStore.lighten(CosmeticColorStore.color(profile,255),.46,220),dark=CosmeticColorStore.darken(CosmeticColorStore.color(profile,255),.55,175);
        if(style==1||style==2||style==9||style==10||style==15){
            int feathers=tier==1?5:10;for(int side:new int[]{-1,1})for(int i=0;i<feathers;i++){double u=(i+.5)/feathers;double x0=.20*side,y0=1.48-u*.65,z0=-.20;double x1=(.72+.38*u)*side,y1=1.54-u*.82,z1=-.34-.05*u;ribbon(b,p,yaw,x0,y0,z0,x1,y1,z1,.009*s,s,i%3==0?light:main);if(tier>1&&i%2==0)gem(b,p,yaw,x1,y1,z1,.025*s,s,light,dark);}
        }else if(style==3||style==7){
            microRing(b,p,yaw,0,2.12,-.02,.46*s,style==7?.38*s:.33*s,t*.14,light,main,tier==1?20:36);
            if(tier>1)for(int i=0;i<8;i++){double a=t*.32+i*Math.PI/4;gem(b,p,yaw,Math.cos(a)*.46*s,2.12+Math.sin(a*2)*.035,Math.sin(a)*.30*s,.025*s,s,light,dark);}
        }else if(style==4||style==12){
            int n=tier==1?6:12;for(int i=0;i<n;i++){double a=t*.14+i*Math.PI*2/n;gem(b,p,yaw,Math.cos(a)*.38*s,2.20+.04*Math.sin(t+i),Math.sin(a)*.28*s,.03*s,s,i%2==0?light:main,dark);}
        }else if(style==5||style==11||style==14){
            int n=tier==1?6:14;for(int i=0;i<n;i++){double a=-t*.28+i*Math.PI*2/n,r=.72+.07*Math.sin(t+i);gem(b,p,yaw,Math.cos(a)*r*s,.38+(i%6)*.28,Math.sin(a)*.48*s,.026*s,s,i%2==0?light:main,dark);}
        }else if(style==6||style==16||style==20){
            for(int side:new int[]{-1,1})for(int i=0;i<(tier==1?2:4);i++){double x=(.18+.08*i)*side,y=2.05+.10*i,z=-.01+.025*i;ribbon(b,p,yaw,x,y,z,x+.09*side,y+.10,z+.02,.012*s,s,i%2==0?light:main);}
        }else if(style==8){
            int seams=tier==1?4:8;for(int i=1;i<seams;i++){double u=i/(double)seams;ribbon(b,p,yaw,-.28+u*.56,1.48,-.175,-.25+u*.50,.48,-.43,.007*s,s,i%2==0?light:dark);}
        }else if(style==13){
            for(int side:new int[]{-1,1}){double sg=side;for(int i=0;i<(tier==1?3:6);i++){double y=1.72-i*.19;ribbon(b,p,yaw,.16*sg,1.42,-.17,(.58+.07*i)*sg,y,-.39,.009*s,s,i%2==0?light:main);}}
        }else if(style==17){
            for(int i=0;i<(tier==1?4:8);i++){double u=(i+1)/9.0,a=t*.7+u*2.6;gem(b,p,yaw,Math.sin(a)*.16*u,.82-u*.55,-.17-u*.62,.020*s,s,i%2==0?light:main,dark);}
        }else if(style==18){
            microRing(b,p,yaw,.46,1.84,-.16,.34*s,.27*s,t*.33,light,main,tier==1?18:34);if(tier>1)for(int i=0;i<7;i++){double a=t*.5+i*Math.PI*2/7;gem(b,p,yaw,.46+Math.cos(a)*.39*s,1.84+Math.sin(a)*.10,-.16+Math.sin(a)*.25*s,.024*s,s,light,dark);}
        }else if(style==19){
            microRing(b,p,yaw,0,1.05,-.49,.48*s,.31*s,-t*.24,light,main,tier==1?20:36);if(tier>1)for(int i=0;i<8;i++){double a=t*.43+i*Math.PI/4;gem(b,p,yaw,Math.cos(a)*.49*s,1.05+Math.sin(a)*.31,-.50,.024*s,s,light,dark);}
        }
    }

    private static void effectDetail(VertexConsumer b,Vec3d p,double yaw,ClientModule m,int tier,double t){
        int style=Math.max(1,Math.min(20,(int)Math.round(m.setting)));double s=LockerCosmeticStore.size("effect",style);
        CosmeticColorStore.Profile profile=LockerCosmeticStore.resolvedColor("effect",style);
        int main=CosmeticColorStore.color(profile,135),light=CosmeticColorStore.lighten(CosmeticColorStore.color(profile,255),.48,190),dark=CosmeticColorStore.darken(CosmeticColorStore.color(profile,255),.60,100);
        int n=tier==1?7:16;
        for(int i=0;i<n;i++){double a=t*(.22+.02*(style%5))+i*Math.PI*2/n,r=.64+.10*Math.sin(t*.7+i*1.7),y=.20+(i%7)*.27+.04*Math.sin(t+i);gem(b,p,yaw,Math.cos(a)*r*s,y,Math.sin(a)*r*.72*s,.018*s*(1+(i%3)*.18),s,i%2==0?light:main,dark);}
        int rings=tier==1?1:3;for(int i=0;i<rings;i++){double yy=.45+i*.55+.035*Math.sin(t+i),r=(.48+i*.09)*s;microRing(b,p,yaw,0,yy,0,r,r*.66,t*.08*(i%2==0?1:-1),i%2==0?main:light,dark,tier==1?18:30);}
        if(tier>1&&(style==6||style==9||style==16||style==19||style==20)){for(int i=0;i<10;i++){double a=i*.93+t*.4,x=Math.cos(a)*.55*s,z=Math.sin(a)*.44*s,y=.25+(i%5)*.34;ribbon(b,p,yaw,x,y,z,x+Math.sin(a)*.09,y+.15,z-Math.cos(a)*.09,.006*s,s,i%2==0?light:main);}}
    }

    private static void microRing(VertexConsumer b,Vec3d p,double yaw,double cx,double cy,double cz,double rx,double rz,double rot,int a,int d,int seg){for(int i=0;i<seg;i++){double q0=i*Math.PI*2/seg+rot,q1=(i+1)*Math.PI*2/seg+rot;double x0=cx+Math.cos(q0)*rx,z0=cz+Math.sin(q0)*rz,x1=cx+Math.cos(q1)*rx,z1=cz+Math.sin(q1)*rz;ribbon(b,p,yaw,x0,cy,z0,x1,cy,z1,.008,1,(i&1)==0?a:d);}}
    private static void gem(VertexConsumer b,Vec3d p,double yaw,double x,double y,double z,double r,double s,int top,int bottom){double[][] q={{x,y+r*1.5,z},{x,y-r*1.5,z},{x-r,y,z},{x+r,y,z},{x,y,z+r},{x,y,z-r}};tri(b,p,yaw,q[0],q[2],q[4],s,top);tri(b,p,yaw,q[0],q[4],q[3],s,top);tri(b,p,yaw,q[0],q[3],q[5],s,top);tri(b,p,yaw,q[0],q[5],q[2],s,top);tri(b,p,yaw,q[1],q[4],q[2],s,bottom);tri(b,p,yaw,q[1],q[3],q[4],s,bottom);tri(b,p,yaw,q[1],q[5],q[3],s,bottom);tri(b,p,yaw,q[1],q[2],q[5],s,bottom);}
    private static void ribbon(VertexConsumer b,Vec3d p,double yaw,double x0,double y0,double z0,double x1,double y1,double z1,double r,double s,int color){double dx=x1-x0,dz=z1-z0,len=Math.max(.0001,Math.hypot(dx,dz)),px=-dz/len*r,pz=dx/len*r;quad(b,p,yaw,new double[]{x0+px,y0,z0+pz},new double[]{x1+px,y1,z1+pz},new double[]{x1-px,y1,z1-pz},new double[]{x0-px,y0,z0-pz},s,color);}
    private static void tri(VertexConsumer b,Vec3d p,double yaw,double[] a,double[] q,double[] r,double s,int c){v(b,p,yaw,a,s,c);v(b,p,yaw,q,s,c);v(b,p,yaw,r,s,c);v(b,p,yaw,r,s,c);}
    private static void quad(VertexConsumer b,Vec3d p,double yaw,double[] a,double[] q,double[] r,double[] d,double s,int c){v(b,p,yaw,a,s,c);v(b,p,yaw,q,s,c);v(b,p,yaw,r,s,c);v(b,p,yaw,d,s,c);}
    private static void v(VertexConsumer b,Vec3d p,double yaw,double[] a,double s,int c){double x=a[0]*s,z=a[2]*s,rx=x*Math.cos(yaw)-z*Math.sin(yaw),rz=x*Math.sin(yaw)+z*Math.cos(yaw);b.vertex((float)(p.x+rx),(float)(p.y+a[1]*s),(float)(p.z+rz)).color(c);}
}
