package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Real 3D Locker viewport: renders the same Star character geometry and attached 3D cosmetics. */
public final class Locker3dPreviewRenderer {
    private Locker3dPreviewRenderer() {}

    public static void renderSkin(DrawContext c,int x,int y,int w,int h,int character,float yaw,float pitch,float delta){
        StarCrispUi.panel(c,x,y,w,h,14,true);
        renderModel(c,x,y,w,h,character,yaw,pitch,delta,null,0,0xFFFFFFFF,1.0,false);
    }

    public static void renderCosmetic(DrawContext c,int x,int y,int w,int h,String kind,int style,String name,int color,double size,boolean rainbow,float yaw,float pitch,float delta){
        StarCrispUi.panel(c,x,y,w,h,14,true);
        int character=1; // CJ mannequin gives accessories a real humanoid body.
        renderModel(c,x,y,w,h,character,yaw,pitch,delta,kind,style,color,size,rainbow);
    }

    private static void renderModel(DrawContext c,int x,int y,int w,int h,int character,float yaw,float pitch,float delta,
                                    String kind,int style,int color,double size,boolean rainbow){
        c.enableScissor(x+3,y+3,x+w-3,y+h-3);
        MatrixStack m=c.getMatrices();
        m.push();
        float px=x+w*.5f,py=y+h*.91f;
        float scale=Math.max(34f,Math.min(72f,Math.min(w*.28f,h*.40f)));
        m.translate(px,py,220);
        m.scale(scale,-scale,scale);
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-6f+pitch*.22f));
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        double sec=System.nanoTime()/1_000_000_000.0;
        float swing=(float)(sec*2.1),amount=.16f;
        StarPlayerModels.render(character,m,c.getVertexConsumers(),new StarPlayerModels.Pose(swing,amount,0,0,(float)sec),true);
        if(kind!=null)renderCosmeticGeometry(m,c,kind,style,color,size,sec);
        m.pop();
        c.getVertexConsumers().draw();
        c.disableScissor();
    }

    private static void renderCosmeticGeometry(MatrixStack m,DrawContext c,String kind,int style,int color,double size,double t){
        StarCharacterGeometry.Drawer d=StarCharacterGeometry.begin(m,c.getVertexConsumers());
        int main=color,light=StarCharacterGeometry.lighten(color,.30f),dark=StarCharacterGeometry.shade(color,.58f);
        float s=(float)Math.max(.60,Math.min(1.8,size));
        if("effect".equals(kind)){effect(m,d,style,s,t,main,light,dark);return;}
        switch(style){
            case 1,2,9,10,15 -> wings(d,style,s,t,main,light,dark);
            case 3,7 -> ring(m,d,0,2.12f,0,.34f*s,.035f,36,main,light);
            case 4,12 -> crown(m,d,s,main,light,dark,style==12);
            case 5,11,14 -> orbitGems(d,s,t,style==14?10:7,main,light,dark);
            case 6,16,20 -> headPieces(d,style,s,main,light,dark);
            case 8 -> cape(d,s,t,main,light,dark);
            case 13 -> blades(d,s,main,light,dark);
            case 17 -> tail(d,s,t,main,light,dark);
            case 18 -> staff(d,s,t,main,light,dark);
            case 19 -> backpack(m,d,s,t,main,light,dark);
            default -> wings(d,1,s,t,main,light,dark);
        }
    }

    private static void wings(StarCharacterGeometry.Drawer d,int style,float s,double t,int main,int light,int dark){
        float flap=(float)Math.sin(t*1.4)*.045f;
        for(int side:new int[]{-1,1}){
            float sg=side;float tip=(style==9?1.30f:style==15?1.22f:1.10f)*s;
            int membrane=style==2?dark:main;
            d.tri(.16f*sg,1.46f,-.12f,.56f*sg*s,1.78f+flap,-.20f,tip*sg,1.52f,-.34f,light);
            d.tri(.16f*sg,1.46f,-.12f,tip*sg,1.52f,-.34f,.93f*sg*s,.96f,-.32f,membrane);
            d.tri(.16f*sg,1.46f,-.12f,.93f*sg*s,.96f,-.32f,.39f*sg*s,.67f,-.20f,dark);
            for(int i=0;i<6;i++){float yy=1.42f-i*.12f,xx=(.90f-i*.08f)*s;d.tri(.22f*sg,yy,-.17f,xx*sg,yy-.10f,-.37f,(xx-.14f)*sg,yy-.27f,-.31f,i<2?light:main);}
        }
    }

    private static void crown(MatrixStack m,StarCharacterGeometry.Drawer d,float s,int main,int light,int dark,boolean galaxy){
        ring(m,d,0,2.10f,0,.28f*s,.035f,30,main,light);
        for(int i=0;i<8;i++){double a=i*Math.PI*2/8;float x=(float)Math.cos(a)*.245f*s,z=(float)Math.sin(a)*.245f*s;gem(d,x,2.27f+(i%2)*.07f,z,.055f,i%2==0?light:main,dark);}
        if(galaxy)for(int i=0;i<5;i++){double a=i*Math.PI*2/5;gem(d,(float)Math.cos(a)*.39f*s,2.25f,(float)Math.sin(a)*.31f*s,.035f,light,dark);}
    }

    private static void orbitGems(StarCharacterGeometry.Drawer d,float s,double t,int n,int main,int light,int dark){for(int i=0;i<n;i++){double a=t*.55+i*Math.PI*2/n;float r=.54f*s;gem(d,(float)Math.cos(a)*r,.72f+(i%5)*.25f,(float)Math.sin(a)*.38f*s,.055f,i%2==0?light:main,dark);}}

    private static void headPieces(StarCharacterGeometry.Drawer d,int style,float s,int main,int light,int dark){
        if(style==16){for(int side:new int[]{-1,1})d.tri(.09f*side,2.02f,-.04f,.30f*side*s,2.31f,.00f,.04f*side,2.21f,-.14f,main);return;}
        for(int side:new int[]{-1,1}){float sg=side;d.tri(.11f*sg,2.00f,-.02f,.25f*sg*s,2.23f,.01f,.39f*sg*s,2.40f,.05f,style==20?light:main);if(style==20)gem(d,.42f*sg*s,2.41f,.05f,.05f,light,dark);}
    }

    private static void cape(StarCharacterGeometry.Drawer d,float s,double t,int main,int light,int dark){for(int r=0;r<7;r++){float y0=1.48f-r*.14f,y1=y0-.15f,z0=-.17f-r*.025f,z1=z0-.035f;d.quad(-.30f*s,y0,z0,.30f*s,y0,z0,.28f*s,y1,z1,-.28f*s,y1,z1,(r&1)==0?main:dark);}d.quad(-.31f*s,1.51f,-.14f,.31f*s,1.51f,-.14f,.28f*s,1.42f,-.17f,-.28f*s,1.42f,-.17f,light);}

    private static void blades(StarCharacterGeometry.Drawer d,float s,int main,int light,int dark){for(int side:new int[]{-1,1}){float sg=side;d.tri(.14f*sg,1.45f,-.16f,.72f*sg*s,1.88f,-.36f,.48f*sg*s,1.15f,-.30f,light);d.tri(.14f*sg,1.12f,-.16f,.69f*sg*s,.54f,-.34f,.44f*sg*s,1.28f,-.28f,main);}}

    private static void tail(StarCharacterGeometry.Drawer d,float s,double t,int main,int light,int dark){float lx=0,ly=.88f,lz=-.17f;for(int i=1;i<=9;i++){float u=i/9f,nx=(float)Math.sin(t*.7+u*2.7)*.18f*u*s,ny=.88f-u*.63f,nz=-.17f-u*.65f*s;float r=.042f*(1-u*.4f);d.box(Math.min(lx,nx)-r,Math.min(ly,ny)-r,Math.min(lz,nz)-r,Math.max(lx,nx)+r,Math.max(ly,ny)+r,Math.max(lz,nz)+r,(i&1)==0?main:dark);lx=nx;ly=ny;lz=nz;}d.tri(lx,ly+.14f,lz,lx-.12f,ly-.04f,lz,lx+.12f,ly-.04f,lz,light);}

    private static void staff(StarCharacterGeometry.Drawer d,float s,double t,int main,int light,int dark){d.box(.43f*s,.30f,-.18f,.48f*s,1.76f,-.13f,main);gem(d,.455f*s,1.88f,-.155f,.12f,light,dark);}

    private static void backpack(MatrixStack m,StarCharacterGeometry.Drawer d,float s,double t,int main,int light,int dark){d.box(-.24f*s,.72f,-.43f,.24f*s,1.43f,-.20f,dark);d.box(-.19f*s,.79f,-.48f,.19f*s,1.34f,-.42f,main);ring(m,d,0,1.06f,-.50f,.31f*s,.025f,28,light,main);}

    private static void effect(MatrixStack m,StarCharacterGeometry.Drawer d,int style,float s,double t,int main,int light,int dark){
        int mode=(style-1)%6;
        if(mode==0||mode==3){for(int i=0;i<4;i++)ring(m,d,0,.34f+i*.42f,0,(.42f+i*.045f)*s,.018f,30,i%2==0?main:light,dark);}
        if(mode==1||mode==4){for(int i=0;i<9;i++){double a=t*.65+i*Math.PI*2/9;gem(d,(float)Math.cos(a)*.58f*s,.30f+(i%5)*.34f,(float)Math.sin(a)*.44f*s,.04f,i%2==0?light:main,dark);}}
        if(mode==2||mode==5){for(int i=0;i<3;i++){m.push();m.translate(0,.90f,0);m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(i*60f));ring(m,d,0,0,0,(.48f+i*.08f)*s,.015f,30,i%2==0?light:main,dark);m.pop();}}
        if(style>=15){for(int i=0;i<6;i++){double a=t*.4+i*Math.PI/3;gem(d,(float)Math.cos(a)*.72f*s,1.05f+(float)Math.sin(a*2)*.42f,(float)Math.sin(a)*.52f*s,.035f,light,dark);}}
    }

    private static void ring(MatrixStack m,StarCharacterGeometry.Drawer d,float cx,float cy,float cz,float r,float thick,int seg,int main,int alt){for(int i=0;i<seg;i++){double a0=i*Math.PI*2/seg,a1=(i+1)*Math.PI*2/seg;float x0=cx+(float)Math.cos(a0)*r,z0=cz+(float)Math.sin(a0)*r,x1=cx+(float)Math.cos(a1)*r,z1=cz+(float)Math.sin(a1)*r;float rr=thick;d.box(Math.min(x0,x1)-rr,cy-rr,Math.min(z0,z1)-rr,Math.max(x0,x1)+rr,cy+rr,Math.max(z0,z1)+rr,(i&1)==0?main:alt);}}

    private static void gem(StarCharacterGeometry.Drawer d,float x,float y,float z,float r,int top,int bottom){d.tri(x,y+r*1.5f,z,x-r,y,z,x,y,z+r,top);d.tri(x,y+r*1.5f,z,x,y,z+r,x+r,y,z,top);d.tri(x,y+r*1.5f,z,x+r,y,z,x,y,z-r,top);d.tri(x,y+r*1.5f,z,x,y,z-r,x-r,y,z,top);d.tri(x,y-r*1.5f,z,x,y,z+r,x-r,y,z,bottom);d.tri(x,y-r*1.5f,z,x+r,y,z,x,y,z+r,bottom);d.tri(x,y-r*1.5f,z,x,y,z-r,x+r,y,z,bottom);d.tri(x,y-r*1.5f,z,x-r,y,z,x,y,z-r,bottom);}
}
