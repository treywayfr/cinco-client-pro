package gg.cinco.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Detailed solid local Locker accessories rendered from the interpolated local-player position. */
public final class CustomAccessoryRenderer {
    private CustomAccessoryRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client=MinecraftClient.getInstance();
        ClientModule module=CincoClient.findModule("Custom Accessory");
        if(module==null||!module.enabled||client.player==null||client.world==null||context.consumers()==null)return;
        int style=Math.max(1,Math.min(20,(int)Math.round(module.setting)));
        double size=LockerCosmeticStore.size("accessory",style);
        double motion=Math.max(.1,Math.min(2.5,module.setting3));
        double t=System.currentTimeMillis()/1000.0*motion;
        int tier=StarPerformance.animationTier(client);
        float tickDelta=client.getRenderTickCounter().getTickProgress(false);
        Vec3d camera=context.worldState().cameraRenderState.pos;
        Vec3d rendered=new Vec3d(MathHelper.lerp(tickDelta,client.player.lastX,client.player.getX()),MathHelper.lerp(tickDelta,client.player.lastY,client.player.getY()),MathHelper.lerp(tickDelta,client.player.lastZ,client.player.getZ()));
        Vec3d p=rendered.subtract(camera);
        double yaw=Math.toRadians(client.player.bodyYaw);
        VertexConsumer b=context.consumers().getBuffer(RenderLayers.debugQuads());
        CosmeticColorStore.Profile profile=LockerCosmeticStore.resolvedColor("accessory",style);
        int base=CosmeticColorStore.color(profile,255),main=CosmeticColorStore.color(profile,235),light=CosmeticColorStore.lighten(base,.34,250),shade=CosmeticColorStore.darken(base,.46,220),glow=CosmeticColorStore.color(profile,120);
        switch(style){
            case 2->demonWings(b,p,yaw,size,t,main,light,shade);
            case 3->halo(b,p,yaw,size,t,main,light,shade,tier);
            case 4->crown(b,p,yaw,size,t,main,light,shade,tier);
            case 5->orbitCrystals(b,p,yaw,size,t,main,light,shade,tier);
            case 6->horns(b,p,yaw,size,t,main,light,shade);
            case 7->energyRing(b,p,yaw,size,t,main,light,shade,glow,tier);
            case 8->cape(b,p,yaw,size,t,main,light,shade,tier);
            case 9->dragonWings(b,p,yaw,size,t,main,light,shade);
            case 10->butterflyWings(b,p,yaw,size,t,main,light,shade);
            case 11->shoulderOrbs(b,p,yaw,size,t,main,light,shade,tier);
            case 12->galaxyCrown(b,p,yaw,size,t,main,light,shade,tier);
            case 13->backBlades(b,p,yaw,size,main,light,shade);
            case 14->floatingCrystals(b,p,yaw,size,t,main,light,shade,tier);
            case 15->phoenixWings(b,p,yaw,size,t,main,light,shade);
            case 16->catEars(b,p,yaw,size,t,main,light,shade);
            case 17->devilTail(b,p,yaw,size,t,main,light,shade);
            case 18->celestialStaff(b,p,yaw,size,t,main,light,shade,tier);
            case 19->orbitBackpack(b,p,yaw,size,t,main,light,shade,tier);
            case 20->crystalAntlers(b,p,yaw,size,t,main,light,shade);
            default->angelWings(b,p,yaw,size,t,main,light,shade);
        }
        detailAccents(b,p,yaw,size,t,style,light,shade,tier);
    }

    private static void angelWings(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade){double flap=Math.sin(t*1.35)*.035;for(int side:new int[]{-1,1}){double[] root={.16*side,1.47,-.10},upper={.55*side,1.72+flap,-.17},tip={1.18*side,1.50+flap*.8,-.27},mid={1.06*side,1.08,-.31},low={.74*side,.66,-.28},inner={.28*side,.92,-.15};tri(b,p,yaw,root,upper,tip,s,light);tri(b,p,yaw,root,tip,mid,s,main);tri(b,p,yaw,root,mid,low,s,main);tri(b,p,yaw,root,low,inner,s,shade);for(int i=0;i<7;i++){double fy=1.43-i*.105,o=1.08-i*.07;tri(b,p,yaw,new double[]{.25*side,fy,-.19},new double[]{o*side,fy-.10,-.34},new double[]{(o-.15)*side,fy-.26,-.30},s,i<2?light:main);}}}
    private static void demonWings(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade){double flap=Math.sin(t*1.55)*.03;for(int side:new int[]{-1,1}){double[] r={.15*side,1.45,-.11},a={.58*side,1.72+flap,-.18},q={1.22*side,1.52+flap,-.30},n={.87*side,1.18,-.27},q2={1.12*side,.81,-.33},n2={.67*side,.91,-.23},bot={.26*side,1.11,-.14};tri(b,p,yaw,r,a,q,s,light);tri(b,p,yaw,r,q,n,s,main);tri(b,p,yaw,r,n,q2,s,shade);tri(b,p,yaw,r,q2,n2,s,main);tri(b,p,yaw,r,n2,bot,s,shade);ribbon(b,p,yaw,r,q,.025,s,light);ribbon(b,p,yaw,r,q2,.022,s,shade);}}
    private static void dragonWings(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade){double flap=Math.sin(t)*.025;for(int side:new int[]{-1,1}){double[] root={.17*side,1.46,-.12},joint={.62*side,1.78+flap,-.22},tip={1.42*side,1.60+flap,-.43},claw={1.20*side,1.08,-.38},low={.84*side,.56,-.31},inner={.28*side,.92,-.16};tri(b,p,yaw,root,joint,tip,s,light);tri(b,p,yaw,root,tip,claw,s,main);tri(b,p,yaw,root,claw,low,s,shade);tri(b,p,yaw,root,low,inner,s,main);ribbon(b,p,yaw,root,tip,.035,s,light);ribbon(b,p,yaw,root,low,.028,s,shade);ribbon(b,p,yaw,joint,claw,.018,s,main);}}
    private static void butterflyWings(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade){double flap=.026*Math.sin(t*1.2);for(int side:new int[]{-1,1}){double[] root={.14*side,1.36,-.10},a={.78*side,1.86+flap,-.22},q={1.10*side,1.48+flap,-.26},c={.72*side,1.18,-.22},d={.92*side,.82,-.25},e={.36*side,.78,-.16};tri(b,p,yaw,root,a,q,s,light);tri(b,p,yaw,root,q,c,s,main);tri(b,p,yaw,root,c,d,s,main);tri(b,p,yaw,root,d,e,s,shade);diamond(b,p,yaw,.62*side,1.45,-.24,.07,s,light,shade);}}
    private static void halo(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade,int tier){annulus(b,p,yaw,0,2.12+Math.sin(t)*.008,-.015,.38,.27,.055,tier==0?24:tier==1?36:52,s,main,light,shade);if(tier>0)annulus(b,p,yaw,0,2.12,-.015,.27,.235,.018,32,s,light,main,shade);}
    private static void crown(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade,int tier){int seg=tier==0?16:24;double yy=2.02+Math.sin(t*.9)*.006;annulus(b,p,yaw,0,yy,-.01,.29,.22,.10,seg,s,main,light,shade);for(int i=0;i<8;i++){double a=i*Math.PI*2/8,cx=Math.cos(a)*.255,cz=Math.sin(a)*.255;tri(b,p,yaw,new double[]{cx-Math.sin(a)*.055,yy+.06,cz+Math.cos(a)*.055},new double[]{cx+Math.sin(a)*.055,yy+.06,cz-Math.cos(a)*.055},new double[]{Math.cos(a)*.19,yy+.29+(i%2)*.06,Math.sin(a)*.19},s,i%2==0?light:main);}}
    private static void galaxyCrown(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade,int tier){crown(b,p,yaw,s,t,main,light,shade,tier);int n=tier==0?5:9;for(int i=0;i<n;i++){double a=t*.30+i*Math.PI*2/n;diamond(b,p,yaw,Math.cos(a)*.44,2.28+.04*Math.sin(t+i),Math.sin(a)*.30,.055,s,light,shade);}}
    private static void orbitCrystals(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade,int tier){int n=tier==0?6:tier==1?9:12;for(int i=0;i<n;i++){double a=t*.38+i*Math.PI*2/n;diamond(b,p,yaw,Math.cos(a)*.64,.62+(i%5)*.27+Math.sin(t+i)*.022,Math.sin(a)*.43,.075+(i%3)*.012,s,i%2==0?light:main,shade);}}
    private static void shoulderOrbs(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade,int tier){int n=tier==0?4:8;for(int i=0;i<n;i++){double side=i%2==0?-1:1,a=t*.40+i*1.7;diamond(b,p,yaw,side*(.38+.08*Math.cos(a)),1.18+(i/2)*.17+.035*Math.sin(a),-.05+.13*Math.sin(a),.085,s,i%3==0?light:main,shade);}}
    private static void floatingCrystals(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade,int tier){int n=tier==0?6:tier==1?9:14;for(int i=0;i<n;i++){double a=t*.25+i*Math.PI*2/n,r=.58+.08*(i%3);diamond(b,p,yaw,Math.cos(a)*r,.38+(i%6)*.27+.03*Math.sin(t+i),-.14+Math.sin(a)*.40,.065+(i%2)*.02,s,i%2==0?light:main,shade);}}
    private static void horns(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade){for(int side:new int[]{-1,1}){double wobble=Math.sin(t+side)*.004;double[][] a={{.13*side,1.96,-.01},{.19*side,2.10+wobble,-.015},{.29*side,2.20+wobble,-.005},{.27*side,2.32,.015},{.17*side,2.40,.025}};for(int i=0;i<a.length-1;i++)ribbon(b,p,yaw,a[i],a[i+1],.075*(1-i/(double)(a.length-1)*.72),s,i==0?shade:(i==a.length-2?light:main));}}
    private static void energyRing(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade,int glow,int tier){int seg=tier==0?28:tier==1?42:60;double pulse=.72+Math.sin(t)*.018;verticalRing(b,p,yaw,0,1.02,0,pulse,.97,.065,seg,s,t*.10,main,light);verticalRing(b,p,yaw,0,1.02,0,pulse*.87,.82,.032,seg,s,-t*.07,glow,shade);if(tier>1)verticalRing(b,p,yaw,0,1.02,0,pulse*.68,.64,.018,seg,s,t*.16,light,main);}
    private static void cape(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade,int tier){int cols=tier==0?4:tier==1?6:8,rows=tier==0?5:8;for(int row=0;row<rows;row++){double v0=row/(double)rows,v1=(row+1)/(double)rows,y0=1.52-v0*1.02,y1=1.52-v1*1.02;for(int col=0;col<cols;col++){double u0=col/(double)cols,u1=(col+1)/(double)cols,x0=-.31+u0*.62,x1=-.31+u1*.62;double z00=-.14-v0*.28+Math.sin(t+col*.42+row*.28)*.014*v0,z01=-.14-v0*.28+Math.sin(t+(col+1)*.42+row*.28)*.014*v0,z10=-.14-v1*.28+Math.sin(t+col*.42+(row+1)*.28)*.018*v1,z11=-.14-v1*.28+Math.sin(t+(col+1)*.42+(row+1)*.28)*.018*v1;quad(b,p,yaw,new double[]{x0,y0,z00},new double[]{x1,y0,z01},new double[]{x1,y1,z11},new double[]{x0,y1,z10},s,((row+col)&1)==0?main:shade);}}quad(b,p,yaw,new double[]{-.32,1.54,-.12},new double[]{.32,1.54,-.12},new double[]{.29,1.42,-.15},new double[]{-.29,1.42,-.15},s,light);}
    private static void backBlades(VertexConsumer b,Vec3d p,double yaw,double s,int main,int light,int shade){double[][] roots={{-.18,1.42,-.13},{.18,1.42,-.13},{-.18,1.16,-.14},{.18,1.16,-.14}};for(int i=0;i<roots.length;i++){double side=i%2==0?-1:1;double[] tip={.72*side,1.85-(i/2)*.72,-.42},tip2={.58*side,tip[1]-.24,-.35};ribbon(b,p,yaw,roots[i],tip,.065,s,i<2?light:main);ribbon(b,p,yaw,tip,tip2,.045,s,shade);diamond(b,p,yaw,tip[0],tip[1],tip[2],.045,s,light,shade);}}

    private static void phoenixWings(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade){angelWings(b,p,yaw,s,t,main,light,shade);for(int side:new int[]{-1,1})for(int i=0;i<5;i++){double y=1.35-i*.15,x=(.55+i*.14)*side;ribbon(b,p,yaw,new double[]{.20*side,1.42-i*.07,-.13},new double[]{x,y-.34,-.42-i*.025},.055,s,i%2==0?light:main);diamond(b,p,yaw,x,y-.34,-.42-i*.025,.045,s,light,shade);}}
    private static void catEars(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade){double bob=Math.sin(t*1.7)*.008;for(int side:new int[]{-1,1}){tri(b,p,yaw,new double[]{.08*side,1.98,-.03},new double[]{.31*side,1.99,-.02},new double[]{.24*side,2.33+bob,-.01},s,main);tri(b,p,yaw,new double[]{.13*side,2.02,-.035},new double[]{.26*side,2.03,-.03},new double[]{.23*side,2.25+bob,-.025},s,light);ribbon(b,p,yaw,new double[]{.09*side,1.99,-.04},new double[]{.24*side,2.33+bob,-.02},.018,s,shade);}}
    private static void devilTail(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade){double[] last={0,.83,-.17};for(int i=1;i<=8;i++){double u=i/8.0;double[] next={Math.sin(t*.7+u*2.6)*.16*u,.82-u*.55,-.17-u*.62};ribbon(b,p,yaw,last,next,.055*(1-u*.35),s,i%2==0?main:shade);last=next;}tri(b,p,yaw,new double[]{last[0],last[1]+.16,last[2]},new double[]{last[0]-.13,last[1]-.03,last[2]},new double[]{last[0]+.13,last[1]-.03,last[2]},s,light);}
    private static void celestialStaff(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade,int tier){ribbon(b,p,yaw,new double[]{.46,.28,-.16},new double[]{.46,1.72,-.16},.035,s,main);diamond(b,p,yaw,.46,1.84,-.16,.14,s,light,shade);annulus(b,p,yaw,.46,1.84,-.16,.25,.20,.035,tier==0?20:34,s,main,light,shade);for(int i=0;i<(tier==0?3:6);i++){double a=t*.5+i*Math.PI*2/(tier==0?3:6);diamond(b,p,yaw,.46+Math.cos(a)*.33,1.84+Math.sin(a)*.09,-.16+Math.sin(a)*.22,.035,s,light,shade);}}
    private static void orbitBackpack(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade,int tier){quad(b,p,yaw,new double[]{-.27,1.46,-.19},new double[]{.27,1.46,-.19},new double[]{.23,.70,-.31},new double[]{-.23,.70,-.31},s,shade);quad(b,p,yaw,new double[]{-.21,1.39,-.32},new double[]{.21,1.39,-.32},new double[]{.18,.80,-.42},new double[]{-.18,.80,-.42},s,main);int n=tier==0?4:7;for(int i=0;i<n;i++){double a=t*.5+i*Math.PI*2/n;diamond(b,p,yaw,Math.cos(a)*.42,1.05+Math.sin(a)*.29,-.48,.055,s,light,shade);}}
    private static void crystalAntlers(VertexConsumer b,Vec3d p,double yaw,double s,double t,int main,int light,int shade){for(int side:new int[]{-1,1}){double[][] pts={{.12*side,1.97,-.02},{.18*side,2.14,-.01},{.27*side,2.29,.00},{.38*side,2.39,.02}};for(int i=0;i<pts.length-1;i++){ribbon(b,p,yaw,pts[i],pts[i+1],.04,s,i==0?shade:main);diamond(b,p,yaw,pts[i+1][0],pts[i+1][1],pts[i+1][2],.04+i*.008,s,light,shade);}ribbon(b,p,yaw,pts[1],new double[]{.40*side,2.17,.03},.025,s,main);ribbon(b,p,yaw,pts[2],new double[]{.48*side,2.31,.05},.022,s,light);}}

    private static void detailAccents(VertexConsumer b,Vec3d p,double yaw,double s,double t,int style,int light,int shade,int tier){if(tier==0)return;if(style==1||style==2||style==9||style==10||style==15){for(int side:new int[]{-1,1})for(int i=0;i<3;i++)ribbon(b,p,yaw,new double[]{.20*side,1.45-i*.12,-.20},new double[]{(.72-i*.08)*side,1.30-i*.18,-.34},.012,s,i%2==0?light:shade);}else if(style==3||style==4||style==12){for(int i=0;i<4;i++){double a=t*.18+i*Math.PI/2;diamond(b,p,yaw,Math.cos(a)*.34,2.20,Math.sin(a)*.22,.026,s,light,shade);}}}

    private static void annulus(VertexConsumer b,Vec3d p,double yaw,double cx,double cy,double cz,double outer,double inner,double thick,int seg,double s,int main,int light,int shade){double yt=cy+thick*.5,yb=cy-thick*.5;for(int i=0;i<seg;i++){double a0=i*Math.PI*2/seg,a1=(i+1)*Math.PI*2/seg;double[] o0={cx+Math.cos(a0)*outer,yt,cz+Math.sin(a0)*outer},o1={cx+Math.cos(a1)*outer,yt,cz+Math.sin(a1)*outer},i1={cx+Math.cos(a1)*inner,yt,cz+Math.sin(a1)*inner},i0={cx+Math.cos(a0)*inner,yt,cz+Math.sin(a0)*inner};quad(b,p,yaw,o0,o1,i1,i0,s,light);quad(b,p,yaw,new double[]{o0[0],yb,o0[2]},new double[]{o1[0],yb,o1[2]},o1,o0,s,main);quad(b,p,yaw,i0,i1,new double[]{i1[0],yb,i1[2]},new double[]{i0[0],yb,i0[2]},s,shade);}}
    private static void verticalRing(VertexConsumer b,Vec3d p,double yaw,double cx,double cy,double cz,double rx,double ry,double hw,int seg,double s,double rot,int main,int light){for(int i=0;i<seg;i++){double a0=i*Math.PI*2/seg,a1=(i+1)*Math.PI*2/seg;double[] o0=vpoint(cx,cy,cz,rx+hw,ry+hw,a0,rot),o1=vpoint(cx,cy,cz,rx+hw,ry+hw,a1,rot),i0=vpoint(cx,cy,cz,Math.max(.05,rx-hw),Math.max(.05,ry-hw),a0,rot),i1=vpoint(cx,cy,cz,Math.max(.05,rx-hw),Math.max(.05,ry-hw),a1,rot);quad(b,p,yaw,o0,o1,i1,i0,s,(i&1)==0?light:main);}}
    private static double[] vpoint(double cx,double cy,double cz,double rx,double ry,double a,double rot){double h=Math.cos(a)*rx;return new double[]{cx+Math.cos(rot)*h,cy+Math.sin(a)*ry,cz+Math.sin(rot)*h};}
    private static void diamond(VertexConsumer b,Vec3d p,double yaw,double cx,double cy,double cz,double r,double s,int top,int bottom){double[] t={cx,cy+r*1.55,cz},bt={cx,cy-r*1.55,cz},l={cx-r,cy,cz},rr={cx+r,cy,cz},f={cx,cy,cz+r},bk={cx,cy,cz-r};tri(b,p,yaw,t,l,f,s,top);tri(b,p,yaw,t,f,rr,s,top);tri(b,p,yaw,t,rr,bk,s,top);tri(b,p,yaw,t,bk,l,s,top);tri(b,p,yaw,bt,f,l,s,bottom);tri(b,p,yaw,bt,rr,f,s,bottom);tri(b,p,yaw,bt,bk,rr,s,bottom);tri(b,p,yaw,bt,l,bk,s,bottom);}
    private static void ribbon(VertexConsumer b,Vec3d p,double yaw,double[] a,double[] d,double width,double s,int c){double dx=d[0]-a[0],dy=d[1]-a[1],len=Math.max(.0001,Math.hypot(dx,dy)),px=-dy/len*width,py=dx/len*width,z0=-width*.72,z1=width*.72;double[] a0={a[0]+px,a[1]+py,a[2]+z0},a1={a[0]-px,a[1]-py,a[2]+z0},d0={d[0]+px,d[1]+py,d[2]+z1},d1={d[0]-px,d[1]-py,d[2]+z1};quad(b,p,yaw,a0,d0,d1,a1,s,c);quad(b,p,yaw,new double[]{a1[0],a1[1],a[2]-z0},new double[]{d1[0],d1[1],d[2]-z1},new double[]{d0[0],d0[1],d[2]-z1},new double[]{a0[0],a0[1],a[2]-z0},s,c);}
    private static void quad(VertexConsumer b,Vec3d p,double yaw,double[] a,double[] d,double[] e,double[] f,double s,int c){vertex(b,world(p,yaw,a,s),c);vertex(b,world(p,yaw,d,s),c);vertex(b,world(p,yaw,e,s),c);vertex(b,world(p,yaw,f,s),c);}
    private static void tri(VertexConsumer b,Vec3d p,double yaw,double[] a,double[] d,double[] e,double s,int c){vertex(b,world(p,yaw,a,s),c);vertex(b,world(p,yaw,d,s),c);vertex(b,world(p,yaw,e,s),c);vertex(b,world(p,yaw,e,s),c);}
    private static double[] world(Vec3d p,double yaw,double[] q,double s){double lx=q[0]*s,ly=q[1]*s,lz=q[2]*s,rx=Math.cos(yaw),rz=Math.sin(yaw),fx=-Math.sin(yaw),fz=Math.cos(yaw);return new double[]{p.x+rx*lx+fx*lz,p.y+ly,p.z+rz*lx+fz*lz};}
    private static void vertex(VertexConsumer b,double[] p,int c){b.vertex((float)p[0],(float)p[1],(float)p[2]).color(c);}
}
