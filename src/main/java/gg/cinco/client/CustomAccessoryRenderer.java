package gg.cinco.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Solid player cosmetics locked to Minecraft's interpolated render transform. */
public final class CustomAccessoryRenderer {
    private CustomAccessoryRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client=MinecraftClient.getInstance();
        ClientModule module=CincoClient.findModule("Custom Accessory");
        if(module==null||!module.enabled||client.player==null||client.world==null||context.consumers()==null)return;

        int style=Math.max(1,Math.min(14,(int)Math.round(module.setting)));
        double size=Math.max(0.60,Math.min(1.80,module.setting2));
        double motion=Math.max(0.0,Math.min(2.50,module.setting3));
        double t=System.currentTimeMillis()/1000.0*motion;
        int tier=StarPerformance.animationTier(client);

        float tickDelta=client.getRenderTickCounter().getTickProgress(false);
        Vec3d camera=context.worldState().cameraRenderState.pos;
        Vec3d renderedPlayer=new Vec3d(
                MathHelper.lerp(tickDelta,client.player.lastX,client.player.getX()),
                MathHelper.lerp(tickDelta,client.player.lastY,client.player.getY()),
                MathHelper.lerp(tickDelta,client.player.lastZ,client.player.getZ()));
        Vec3d p=renderedPlayer.subtract(camera);
        float bodyYaw=MathHelper.lerpAngleDegrees(tickDelta,client.player.prevBodyYaw,client.player.bodyYaw);
        double yaw=Math.toRadians(bodyYaw);

        VertexConsumer b=context.consumers().getBuffer(RenderLayers.debugQuads());
        int base=CosmeticColorStore.accessoryColor(255);
        int main=CosmeticColorStore.color(CosmeticColorStore.accessory(),232);
        int light=CosmeticColorStore.lighten(base,0.34,248);
        int shade=CosmeticColorStore.darken(base,0.46,218);
        int glow=CosmeticColorStore.color(CosmeticColorStore.accessory(),110);

        switch(style){
            case 2->demonWings(b,p.x,p.y,p.z,yaw,size,t,main,light,shade);
            case 3->halo(b,p.x,p.y,p.z,yaw,size,t,main,light,shade,tier);
            case 4->crown(b,p.x,p.y,p.z,yaw,size,t,main,light,shade,tier);
            case 5->orbitStars(b,p.x,p.y,p.z,yaw,size,t,main,light,shade,tier);
            case 6->horns(b,p.x,p.y,p.z,yaw,size,t,main,light,shade,tier);
            case 7->energyRing(b,p.x,p.y,p.z,yaw,size,t,main,light,shade,glow,tier);
            case 8->cape(b,p.x,p.y,p.z,yaw,size,t,main,light,shade,tier);
            case 9->dragonWings(b,p.x,p.y,p.z,yaw,size,t,main,light,shade);
            case 10->butterflyWings(b,p.x,p.y,p.z,yaw,size,t,main,light,shade);
            case 11->shoulderOrbs(b,p.x,p.y,p.z,yaw,size,t,main,light,shade,tier);
            case 12->galaxyCrown(b,p.x,p.y,p.z,yaw,size,t,main,light,shade,tier);
            case 13->backBlades(b,p.x,p.y,p.z,yaw,size,t,main,light,shade);
            case 14->floatingCrystals(b,p.x,p.y,p.z,yaw,size,t,main,light,shade,tier);
            default->angelWings(b,p.x,p.y,p.z,yaw,size,t,main,light,shade);
        }
    }

    private static void angelWings(VertexConsumer b,double x,double y,double z,double yaw,double s,double t,int main,int light,int shade){
        double flap=Math.sin(t*1.45)*0.045;
        for(int side:new int[]{-1,1}){
            double[] root={0.16*side,1.47,-0.10},upper={0.55*side,1.72+flap,-0.17};
            double[] tip={1.18*side,1.50+flap*0.8,-0.27},mid={1.06*side,1.08,-0.31};
            double[] low={0.74*side,0.66,-0.28},inner={0.28*side,0.92,-0.15};
            triLocal(b,x,y,z,yaw,root,upper,tip,s,light);triLocal(b,x,y,z,yaw,root,tip,mid,s,main);
            triLocal(b,x,y,z,yaw,root,mid,low,s,main);triLocal(b,x,y,z,yaw,root,low,inner,s,shade);
            for(int i=0;i<5;i++){double fy=1.40-i*0.13,outer=1.03-i*0.085;
                triLocal(b,x,y,z,yaw,new double[]{0.27*side,fy,-0.185-i*0.006},new double[]{outer*side,fy-0.11,-0.32-i*0.010},new double[]{(outer-0.13)*side,fy-0.27,-0.30-i*0.010},s,i<2?light:main);}
        }
    }

    private static void demonWings(VertexConsumer b,double x,double y,double z,double yaw,double s,double t,int main,int light,int shade){
        double flap=Math.sin(t*1.65)*0.038;
        for(int side:new int[]{-1,1}){
            double[] root={0.15*side,1.45,-0.11},top={0.58*side,1.72+flap,-0.18},point={1.22*side,1.52+flap,-0.30};
            double[] notch1={0.87*side,1.18,-0.27},point2={1.12*side,0.81,-0.33},notch2={0.67*side,0.91,-0.23},bottom={0.26*side,1.11,-0.14};
            triLocal(b,x,y,z,yaw,root,top,point,s,light);triLocal(b,x,y,z,yaw,root,point,notch1,s,main);
            triLocal(b,x,y,z,yaw,root,notch1,point2,s,shade);triLocal(b,x,y,z,yaw,root,point2,notch2,s,main);triLocal(b,x,y,z,yaw,root,notch2,bottom,s,shade);
        }
    }

    private static void dragonWings(VertexConsumer b,double x,double y,double z,double yaw,double s,double t,int main,int light,int shade){
        double flap=Math.sin(t*1.15)*0.030;
        for(int side:new int[]{-1,1}){
            double[] root={0.17*side,1.46,-0.12},joint={0.62*side,1.78+flap,-0.22},tip={1.42*side,1.60+flap,-0.43};
            double[] claw={1.20*side,1.08,-0.38},low={0.84*side,0.56,-0.31},inner={0.28*side,0.92,-0.16};
            triLocal(b,x,y,z,yaw,root,joint,tip,s,light);triLocal(b,x,y,z,yaw,root,tip,claw,s,main);
            triLocal(b,x,y,z,yaw,root,claw,low,s,shade);triLocal(b,x,y,z,yaw,root,low,inner,s,main);
            solidRibbonSegment(b,x,y,z,yaw,root,tip,0.035,s,light);solidRibbonSegment(b,x,y,z,yaw,root,low,0.028,s,shade);
        }
    }

    private static void butterflyWings(VertexConsumer b,double x,double y,double z,double yaw,double s,double t,int main,int light,int shade){
        double flap=0.035*Math.sin(t*1.4);
        for(int side:new int[]{-1,1}){
            double[] root={0.14*side,1.36,-0.10};
            double[] a={0.78*side,1.86+flap,-0.22},b1={1.10*side,1.48+flap,-0.26},c={0.72*side,1.18,-0.22};
            double[] d={0.92*side,0.82,-0.25},e={0.36*side,0.78,-0.16};
            triLocal(b,x,y,z,yaw,root,a,b1,s,light);triLocal(b,x,y,z,yaw,root,b1,c,s,main);
            triLocal(b,x,y,z,yaw,root,c,d,s,main);triLocal(b,x,y,z,yaw,root,d,e,s,shade);
        }
    }

    private static void halo(VertexConsumer b,double x,double y,double z,double yaw,double s,double t,int main,int light,int shade,int tier){int seg=tier==0?18:tier==1?28:40;double yy=2.12+Math.sin(t*1.1)*0.012;annulusHorizontal(b,x,y,z,yaw,0,yy,-0.015,0.38,0.27,0.055,seg,s,main,light,shade);}
    private static void crown(VertexConsumer b,double x,double y,double z,double yaw,double s,double t,int main,int light,int shade,int tier){int seg=tier==0?12:18;double yy=2.02+Math.sin(t)*0.008;annulusHorizontal(b,x,y,z,yaw,0,yy,-0.01,0.29,0.22,0.10,seg,s,main,light,shade);for(int i=0;i<6;i++){double a=i*Math.PI*2/6,cx=Math.cos(a)*0.255,cz=Math.sin(a)*0.255,tx=Math.cos(a)*0.19,tz=Math.sin(a)*0.19;triLocal(b,x,y,z,yaw,new double[]{cx-Math.sin(a)*0.07,yy+0.06,cz+Math.cos(a)*0.07},new double[]{cx+Math.sin(a)*0.07,yy+0.06,cz-Math.cos(a)*0.07},new double[]{tx,yy+0.30+(i%2)*0.055,tz},s,i%2==0?light:main);}}

    private static void galaxyCrown(VertexConsumer b,double x,double y,double z,double yaw,double s,double t,int main,int light,int shade,int tier){crown(b,x,y,z,yaw,s,t,main,light,shade,tier);int count=tier==0?4:7;for(int i=0;i<count;i++){double a=t*0.34+i*Math.PI*2/count;diamond(b,x,y,z,yaw,Math.cos(a)*0.44,2.28+0.05*Math.sin(t+i),Math.sin(a)*0.30,0.055,s,light,shade);}}

    private static void orbitStars(VertexConsumer b,double x,double y,double z,double yaw,double s,double t,int main,int light,int shade,int tier){int count=tier==0?5:tier==1?7:9;for(int i=0;i<count;i++){double a=t*0.42+i*Math.PI*2/count;diamond(b,x,y,z,yaw,Math.cos(a)*0.64,0.68+(i%4)*0.31+Math.sin(t*1.1+i)*0.028,Math.sin(a)*0.43,0.075+(i%3)*0.012,s,i%2==0?light:main,shade);}}

    private static void shoulderOrbs(VertexConsumer b,double x,double y,double z,double yaw,double s,double t,int main,int light,int shade,int tier){int count=tier==0?4:6;for(int i=0;i<count;i++){double side=(i%2==0?-1:1),a=t*0.48+i*1.7;double lx=side*(0.38+0.10*Math.cos(a)),ly=1.25+(i/2)*0.20+0.05*Math.sin(a),lz=-0.05+0.16*Math.sin(a);diamond(b,x,y,z,yaw,lx,ly,lz,0.085,s,i%3==0?light:main,shade);}}

    private static void floatingCrystals(VertexConsumer b,double x,double y,double z,double yaw,double s,double t,int main,int light,int shade,int tier){int count=tier==0?5:tier==1?7:10;for(int i=0;i<count;i++){double a=t*0.28+i*Math.PI*2/count,r=0.58+0.08*(i%3);diamond(b,x,y,z,yaw,Math.cos(a)*r,0.45+(i%5)*0.32+0.04*Math.sin(t+i),-0.14+Math.sin(a)*0.40,0.065+(i%2)*0.02,s,i%2==0?light:main,shade);}}

    private static void horns(VertexConsumer b,double x,double y,double z,double yaw,double s,double t,int main,int light,int shade,int tier){for(int side:new int[]{-1,1}){double wobble=Math.sin(t+side)*0.006;double[][] pts={{0.13*side,1.96,-0.01},{0.19*side,2.10+wobble,-0.015},{0.29*side,2.20+wobble,-0.005},{0.27*side,2.32,0.015},{0.17*side,2.40,0.025}};for(int i=0;i<pts.length-1;i++){double width=0.075*(1-i/(double)(pts.length-1)*0.72);solidRibbonSegment(b,x,y,z,yaw,pts[i],pts[i+1],width,s,i==0?shade:(i==pts.length-2?light:main));}}}

    private static void energyRing(VertexConsumer b,double x,double y,double z,double yaw,double s,double t,int main,int light,int shade,int glow,int tier){int seg=tier==0?24:tier==1?36:52;double pulse=0.72+Math.sin(t*1.2)*0.024;verticalRibbonRing(b,x,y,z,yaw,0,1.02,0,pulse,0.97,0.065,seg,s,t*0.12,main,light);if(tier>0)verticalRibbonRing(b,x,y,z,yaw,0,1.02,0,pulse*0.87,0.82,0.032,seg,s,-t*0.08,glow,shade);}

    private static void cape(VertexConsumer b,double x,double y,double z,double yaw,double s,double t,int main,int light,int shade,int tier){int cols=tier==0?3:tier==1?5:7,rows=tier==0?4:6;for(int row=0;row<rows;row++){double v0=row/(double)rows,v1=(row+1)/(double)rows,y0=1.52-v0*1.02,y1=1.52-v1*1.02;for(int col=0;col<cols;col++){double u0=col/(double)cols,u1=(col+1)/(double)cols,x0=-0.31+u0*0.62,x1=-0.31+u1*0.62;double z00=-0.14-v0*0.28+Math.sin(t*1.15+col*.42+row*.28)*.018*v0,z01=-0.14-v0*.28+Math.sin(t*1.15+(col+1)*.42+row*.28)*.018*v0,z10=-0.14-v1*.28+Math.sin(t*1.15+col*.42+(row+1)*.28)*.024*v1,z11=-0.14-v1*.28+Math.sin(t*1.15+(col+1)*.42+(row+1)*.28)*.024*v1;quadLocal(b,x,y,z,yaw,new double[]{x0,y0,z00},new double[]{x1,y0,z01},new double[]{x1,y1,z11},new double[]{x0,y1,z10},s,((row+col)&1)==0?main:shade);}}quadLocal(b,x,y,z,yaw,new double[]{-.32,1.54,-.12},new double[]{.32,1.54,-.12},new double[]{.29,1.42,-.15},new double[]{-.29,1.42,-.15},s,light);}

    private static void backBlades(VertexConsumer b,double x,double y,double z,double yaw,double s,double t,int main,int light,int shade){double[][] roots={{-0.18,1.42,-0.13},{0.18,1.42,-0.13},{-0.18,1.16,-0.14},{0.18,1.16,-0.14}};for(int i=0;i<roots.length;i++){double side=i%2==0?-1:1;double[] tip={0.72*side,1.85-(i/2)*0.72,-0.42};solidRibbonSegment(b,x,y,z,yaw,roots[i],tip,0.065,s,i<2?light:main);double[] tip2={0.58*side,tip[1]-0.24,-0.35};solidRibbonSegment(b,x,y,z,yaw,tip,tip2,0.045,s,shade);}}

    private static void annulusHorizontal(VertexConsumer b,double x,double y,double z,double yaw,double cx,double cy,double cz,double outer,double inner,double thickness,int seg,double s,int main,int light,int shade){double yTop=cy+thickness*.5,yBot=cy-thickness*.5;for(int i=0;i<seg;i++){double a0=i*Math.PI*2/seg,a1=(i+1)*Math.PI*2/seg;double[] o0={cx+Math.cos(a0)*outer,yTop,cz+Math.sin(a0)*outer},o1={cx+Math.cos(a1)*outer,yTop,cz+Math.sin(a1)*outer},i1={cx+Math.cos(a1)*inner,yTop,cz+Math.sin(a1)*inner},i0={cx+Math.cos(a0)*inner,yTop,cz+Math.sin(a0)*inner};quadLocal(b,x,y,z,yaw,o0,o1,i1,i0,s,light);double[] bo0={o0[0],yBot,o0[2]},bo1={o1[0],yBot,o1[2]};quadLocal(b,x,y,z,yaw,bo0,bo1,o1,o0,s,main);double[] bi0={i0[0],yBot,i0[2]},bi1={i1[0],yBot,i1[2]};quadLocal(b,x,y,z,yaw,i0,i1,bi1,bi0,s,shade);}}
    private static void verticalRibbonRing(VertexConsumer b,double x,double y,double z,double yaw,double cx,double cy,double cz,double rx,double ry,double halfWidth,int seg,double s,double rotation,int main,int light){for(int i=0;i<seg;i++){double a0=i*Math.PI*2/seg,a1=(i+1)*Math.PI*2/seg;double[] o0=verticalPoint(cx,cy,cz,rx+halfWidth,ry+halfWidth,a0,rotation),o1=verticalPoint(cx,cy,cz,rx+halfWidth,ry+halfWidth,a1,rotation),i0=verticalPoint(cx,cy,cz,Math.max(.05,rx-halfWidth),Math.max(.05,ry-halfWidth),a0,rotation),i1=verticalPoint(cx,cy,cz,Math.max(.05,rx-halfWidth),Math.max(.05,ry-halfWidth),a1,rotation);quadLocal(b,x,y,z,yaw,o0,o1,i1,i0,s,(i&1)==0?light:main);}}
    private static double[] verticalPoint(double cx,double cy,double cz,double rx,double ry,double a,double rot){double h=Math.cos(a)*rx;return new double[]{cx+Math.cos(rot)*h,cy+Math.sin(a)*ry,cz+Math.sin(rot)*h};}

    private static void diamond(VertexConsumer b,double x,double y,double z,double yaw,double cx,double cy,double cz,double r,double s,int topColor,int bottomColor){double[] top={cx,cy+r*1.55,cz},bottom={cx,cy-r*1.55,cz},left={cx-r,cy,cz},right={cx+r,cy,cz},front={cx,cy,cz+r},back={cx,cy,cz-r};triLocal(b,x,y,z,yaw,top,left,front,s,topColor);triLocal(b,x,y,z,yaw,top,front,right,s,topColor);triLocal(b,x,y,z,yaw,top,right,back,s,topColor);triLocal(b,x,y,z,yaw,top,back,left,s,topColor);triLocal(b,x,y,z,yaw,bottom,front,left,s,bottomColor);triLocal(b,x,y,z,yaw,bottom,right,front,s,bottomColor);triLocal(b,x,y,z,yaw,bottom,back,right,s,bottomColor);triLocal(b,x,y,z,yaw,bottom,left,back,s,bottomColor);}
    private static void solidRibbonSegment(VertexConsumer b,double x,double y,double z,double yaw,double[] a,double[] d,double width,double s,int color){double dx=d[0]-a[0],dy=d[1]-a[1],len=Math.max(.0001,Math.hypot(dx,dy)),px=-dy/len*width,py=dx/len*width,z0=-width*.72,z1=width*.72;double[] a0={a[0]+px,a[1]+py,a[2]+z0},a1={a[0]-px,a[1]-py,a[2]+z0},d0={d[0]+px,d[1]+py,d[2]+z1},d1={d[0]-px,d[1]-py,d[2]+z1};quadLocal(b,x,y,z,yaw,a0,d0,d1,a1,s,color);double[] ba0={a0[0],a0[1],a[2]-z0},ba1={a1[0],a1[1],a[2]-z0},bd0={d0[0],d0[1],d[2]-z1},bd1={d1[0],d1[1],d[2]-z1};quadLocal(b,x,y,z,yaw,ba1,bd1,bd0,ba0,s,color);}
    private static void quadLocal(VertexConsumer b,double x,double y,double z,double yaw,double[] a,double[] d,double[] e,double[] f,double s,int color){vertex(b,world(x,y,z,yaw,a[0],a[1],a[2],s),color);vertex(b,world(x,y,z,yaw,d[0],d[1],d[2],s),color);vertex(b,world(x,y,z,yaw,e[0],e[1],e[2],s),color);vertex(b,world(x,y,z,yaw,f[0],f[1],f[2],s),color);}
    private static void triLocal(VertexConsumer b,double x,double y,double z,double yaw,double[] a,double[] d,double[] e,double s,int color){double[] aw=world(x,y,z,yaw,a[0],a[1],a[2],s),dw=world(x,y,z,yaw,d[0],d[1],d[2],s),ew=world(x,y,z,yaw,e[0],e[1],e[2],s);vertex(b,aw,color);vertex(b,dw,color);vertex(b,ew,color);vertex(b,ew,color);}
    private static double[] world(double x,double y,double z,double yaw,double lx,double ly,double lz,double s){lx*=s;ly*=s;lz*=s;double rightX=Math.cos(yaw),rightZ=Math.sin(yaw),forwardX=-Math.sin(yaw),forwardZ=Math.cos(yaw);return new double[]{x+rightX*lx+forwardX*lz,y+ly,z+rightZ*lx+forwardZ*lz};}
    private static void vertex(VertexConsumer b,double[] p,int color){b.vertex((float)p[0],(float)p[1],(float)p[2]).color(color);}
}
