package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

/** Lightweight animated Locker mannequin preview; intentionally independent from world rendering APIs. */
public final class LockerPreviewRenderer {
    private LockerPreviewRenderer() {}

    public static void render(DrawContext c, TextRenderer tr, int x, int y, int w, int h,
                              String kind, int style, String name, int color, double size, boolean rainbow) {
        StarCrispUi.panel(c,x,y,w,h,12,true);
        c.drawText(tr,StarFonts.text("LIVE PREVIEW",true),x+14,y+12,0xFFFFFFFF,false);
        c.drawText(tr,StarFonts.text(trim(name,24),false),x+14,y+29,StarCrispUi.PURPLE_HI,false);
        c.drawText(tr,StarFonts.text(rainbow?"ANIMATED RAINBOW":"CUSTOM COLOR",false),x+14,y+44,rainbow?0xFFFFFFFF:0xFFAAA1B2,false);

        int cx=x+w/2, baseY=y+h-35;
        double s=Math.max(.55,Math.min(1.55,size));
        int main=color;
        int light=CosmeticColorStore.lighten(main,.34,255);
        int dark=CosmeticColorStore.darken(main,.45,255);
        long ms=System.currentTimeMillis();
        double t=ms/1000.0;

        // mannequin
        int body=0xFF24202A, edge=0xFF6D6376;
        box(c,cx-14,baseY-82,cx+14,baseY-48,edge,body); // torso
        box(c,cx-11,baseY-108,cx+11,baseY-85,edge,0xFF302A36); // head
        box(c,cx-24,baseY-80,cx-15,baseY-47,edge,0xFF302A36);
        box(c,cx+15,baseY-80,cx+24,baseY-47,edge,0xFF302A36);
        box(c,cx-13,baseY-47,cx-3,baseY-8,edge,0xFF302A36);
        box(c,cx+3,baseY-47,cx+13,baseY-8,edge,0xFF302A36);

        if ("accessory".equals(kind)) drawAccessory(c,cx,baseY,style,s,t,main,light,dark);
        else drawEffect(c,cx,baseY,style,s,t,main,light,dark);

        c.drawCenteredTextWithShadow(tr,Text.literal("SIZE  "+String.format(java.util.Locale.ROOT,"%.2fx",size)),cx,y+h-19,0xFFBFB5C7);
    }

    private static void drawAccessory(DrawContext c,int cx,int by,int style,double s,double t,int main,int light,int dark){
        if(style==1||style==2||style==9||style==10||style==15){
            int spread=(int)(48*s),top=by-92,mid=by-60,bottom=by-25;
            wing(c,cx-13,top,cx-spread,mid,cx-(int)(spread*.72),bottom,style==2?dark:main,light);
            wing(c,cx+13,top,cx+spread,mid,cx+(int)(spread*.72),bottom,style==2?dark:main,light);
            if(style==15){for(int i=0;i<4;i++){int yy=by-45+i*7;line(c,cx-14,yy,cx-(int)(30*s)-i*5,yy+14,light);line(c,cx+14,yy,cx+(int)(30*s)+i*5,yy+14,light);}}
        } else if(style==3||style==7){
            ring(c,cx,by-119,(int)(28*s),main,2); if(style==7)ring(c,cx,by-63,(int)(42*s),light,2);
        } else if(style==4||style==12){
            int r=(int)(23*s),yy=by-116; ring(c,cx,yy,r,main,2); for(int i=-2;i<=2;i++){int px=cx+i*r/2;line(c,px,yy,px,yy-(i%2==0?16:10),light);} if(style==12) ring(c,cx,yy-7,r+9,dark,1);
        } else if(style==5||style==11||style==14){
            int count=style==14?8:6;for(int i=0;i<count;i++){double a=t*.8+i*Math.PI*2/count;int px=cx+(int)(Math.cos(a)*38*s),py=by-62+(int)(Math.sin(a)*18*s);diamond(c,px,py,5+(i%2)*2,i%2==0?main:light);}
        } else if(style==6||style==16||style==20){
            int yy=by-110;if(style==16){tri(c,cx-10,yy,cx-30,yy-26,cx-4,yy-18,main);tri(c,cx+10,yy,cx+30,yy-26,cx+4,yy-18,main);}else{for(int side:new int[]{-1,1}){int sx=cx+side*9;line(c,sx,yy,sx+side*(int)(24*s),yy-28,main);line(c,sx+side*12,yy-14,sx+side*(int)(30*s),yy-22,light);}}
        } else if(style==8){
            tri(c,cx-12,by-84,cx+12,by-84,cx,by-14,main);line(c,cx-8,by-77,cx-3,by-22,light);line(c,cx+8,by-77,cx+3,by-22,dark);
        } else if(style==13||style==18||style==19){
            if(style==19){box(c,cx-23,by-82,cx+23,by-28,light,dark);ring(c,cx,by-56,(int)(29*s),main,2);}else{line(c,cx-17,by-75,cx+28,by-8,main);line(c,cx+17,by-75,cx-28,by-8,light);if(style==18)diamond(c,cx+30,by-10,8,light);}
        } else if(style==17){
            int px=cx+13,py=by-48;for(int i=0;i<7;i++){int nx=px+8,ny=py+7+(i%2==0?2:-2);line(c,px,py,nx,ny,i<4?main:light);px=nx;py=ny;}tri(c,px,py,px+9,py-5,px+8,py+7,light);
        }
    }

    private static void drawEffect(DrawContext c,int cx,int by,int style,double s,double t,int main,int light,int dark){
        int mid=by-59;
        if(style==1||style==2||style==3||style==7||style==8||style==18){
            int rings=style==18?4:3;for(int i=0;i<rings;i++){int rr=(int)((26+i*9)*s);int yy=mid+(i-1)*20;ring(c,cx,yy,rr,i%2==0?main:light,1+(i%2));}
        } else if(style==4||style==11){
            for(int i=0;i<8;i++){int yy=by-92+i*10;int len=(int)((24+i%3*9)*s);line(c,cx-4,yy,cx-len,yy+8,i%2==0?main:light);}
        } else if(style==5||style==10||style==19){
            int count=style==19?12:8;for(int i=0;i<count;i++){double a=t*.7+i*Math.PI*2/count;int px=cx+(int)(Math.cos(a)*45*s),py=mid+(int)(Math.sin(a)*27*s);diamond(c,px,py,4+(i%3),i%2==0?main:light);}
        } else if(style==6){
            for(int i=0;i<6;i++){int px=cx-42+i*15,py=by-102;for(int j=0;j<5;j++){int nx=px+(j%2==0?7:-5),ny=py+18;line(c,px,py,nx,ny,j%2==0?light:main);px=nx;py=ny;}}
        } else if(style==9||style==16){
            int px=cx,py=by-105;for(int i=0;i<28;i++){double a=i*.55+t*.8;int nx=cx+(int)(Math.cos(a)*(12+i*.9)*s),ny=by-105+i*3;line(c,px,py,nx,ny,i%3==0?light:main);px=nx;py=ny;}
        } else if(style==12){
            for(int i=0;i<4;i++)ring(c,cx,by-12,(int)((28+i*14+12*Math.sin(t+i))*s),i%2==0?main:dark,1);
        } else if(style==13){
            ring(c,cx,mid,(int)(48*s),main,2);ring(c,cx,mid,(int)(35*s),dark,1);line(c,cx-48,mid,cx+48,mid,light);line(c,cx,mid-45,cx,mid+45,light);
        } else if(style==14||style==15){
            ring(c,cx,by-115,(int)(27*s),main,2);for(int i=-3;i<=3;i++){line(c,cx+i*7,by-112,cx+i*7,by-128-(Math.abs(i)%2)*7,light);} if(style==15){for(int i=0;i<8;i++){double a=t+i*.8;diamond(c,cx+(int)(Math.cos(a)*42*s),mid+(int)(Math.sin(a)*32*s),4,main);}}
        } else if(style==17){
            for(int i=0;i<7;i++){double a=t*.5+i;int px=cx+(int)(Math.cos(a)*40*s),py=mid+(int)(Math.sin(a)*28*s);heart(c,px,py,main);}
        } else if(style==20){
            for(int i=0;i<5;i++){int rr=(int)((24+i*8)*s);ring(c,cx,mid,rr,i%2==0?main:light,1);}for(int i=0;i<7;i++){double a=t+i;diamond(c,cx+(int)(Math.cos(a)*52*s),mid+(int)(Math.sin(a)*30*s),4,light);}
        }
    }

    private static void wing(DrawContext c,int rx,int ry,int tx,int ty,int bx,int by,int main,int light){tri(c,rx,ry,tx,ty,bx,by,main);line(c,rx,ry,tx,ty,light);line(c,rx,ry,bx,by,light);for(int i=1;i<=4;i++){double u=i/5.0;int px=(int)(rx+(tx-rx)*u),py=(int)(ry+(ty-ry)*u);line(c,rx+(int)((bx-rx)*u*.25),ry+(int)((by-ry)*u*.25),px,py,light);}}
    private static void heart(DrawContext c,int x,int y,int col){c.fill(x-6,y-3,x,y+3,col);c.fill(x,y-3,x+6,y+3,col);c.fill(x-4,y-6,x+4,y+6,col);c.fill(x-2,y+5,x+2,y+8,col);}
    private static void diamond(DrawContext c,int x,int y,int r,int col){tri(c,x,y-r,x+r,y,x,y+r,col);tri(c,x,y-r,x-r,y,x,y+r,col);}
    private static void ring(DrawContext c,int cx,int cy,int r,int col,int thick){for(int i=0;i<48;i++){double a0=i*Math.PI*2/48,a1=(i+1)*Math.PI*2/48;line(c,cx+(int)(Math.cos(a0)*r),cy+(int)(Math.sin(a0)*r*.48),cx+(int)(Math.cos(a1)*r),cy+(int)(Math.sin(a1)*r*.48),col,thick);}}
    private static void box(DrawContext c,int x0,int y0,int x1,int y1,int border,int fill){c.fill(x0,y0,x1,y1,border);c.fill(x0+1,y0+1,x1-1,y1-1,fill);}
    private static void tri(DrawContext c,int ax,int ay,int bx,int by,int cx,int cy,int col){for(int i=0;i<=18;i++){double u=i/18.0;int lx=(int)(ax+(bx-ax)*u),ly=(int)(ay+(by-ay)*u),rx=(int)(ax+(cx-ax)*u),ry=(int)(ay+(cy-ay)*u);line(c,lx,ly,rx,ry,col);}}
    private static void line(DrawContext c,int x0,int y0,int x1,int y1,int col){line(c,x0,y0,x1,y1,col,1);}
    private static void line(DrawContext c,int x0,int y0,int x1,int y1,int col,int thick){int steps=Math.max(Math.abs(x1-x0),Math.abs(y1-y0));if(steps==0){c.fill(x0,y0,x0+thick,y0+thick,col);return;}for(int i=0;i<=steps;i++){double u=i/(double)steps;int x=(int)Math.round(x0+(x1-x0)*u),y=(int)Math.round(y0+(y1-y0)*u);c.fill(x,y,x+thick,y+thick,col);}}
    private static String trim(String s,int n){return s.length()<=n?s:s.substring(0,Math.max(1,n-1))+"…";}
}
