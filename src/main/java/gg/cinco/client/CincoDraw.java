package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;

/** Fast UI primitives tuned for the Star Client dashboard. */
public final class CincoDraw {
    private CincoDraw() {}

    /**
     * Smooth low-cost rounded rectangle. Small controls automatically become true capsules/circles,
     * while large panels keep a softer fixed radius so the whole client feels consistent without
     * turning giant windows into bubbles.
     */
    public static void rounded(DrawContext c,int left,int top,int right,int bottom,int radius,int color){
        int w=right-left,h=bottom-top;if(w<=0||h<=0)return;
        color=accentPink(color);
        int requested=Math.max(0,Math.min(radius,Math.min(w,h)/2));
        int r=h<=64?Math.max(requested,Math.min(w,h)/2):requested;
        r=Math.max(0,Math.min(r,Math.min(w,h)/2));
        if(r<=2){c.fill(left,top,right,bottom,color);return;}

        // Eight bands is still cheap, but much smoother than the older stepped 4-6 band shape.
        int bands=Math.min(8,Math.max(5,r));
        c.fill(left,top+r,right,bottom-r,color);
        for(int i=0;i<bands;i++){
            double y0=i/(double)bands,y1=(i+1)/(double)bands;
            int py0=top+(int)Math.round(y0*r),py1=top+(int)Math.round(y1*r);
            double mid=(y0+y1)*.5;
            double circle=Math.sqrt(Math.max(0,1-(1-mid)*(1-mid)));
            int inset=(int)Math.round(r*(1-circle));
            c.fill(left+inset,py0,right-inset,Math.max(py0+1,py1),color);
            int by0=bottom-(py1-top),by1=bottom-(py0-top);
            c.fill(left+inset,by0,right-inset,Math.max(by0+1,by1),color);
        }
    }

    public static void bubble(DrawContext c,int left,int top,int right,int bottom,int radius,int borderColor,int fillColor){
        rounded(c,left,top,right,bottom,radius,borderColor);
        if(right-left>2&&bottom-top>2)rounded(c,left+1,top+1,right-1,bottom-1,Math.max(2,radius-1),fillColor);
    }

    public static void softShadow(DrawContext c,int left,int top,int right,int bottom,int radius,int rgb,int alpha){
        int a=Math.max(2,alpha/4);
        rounded(c,left-3,top-3,right+3,bottom+3,radius+3,(a<<24)|(rgb&0x00FFFFFF));
    }

    public static void line(DrawContext c,int x0,int y0,int x1,int y1,int color,int thickness){
        color=accentPink(color);
        int dx=Math.abs(x1-x0),sx=x0<x1?1:-1,dy=-Math.abs(y1-y0),sy=y0<y1?1:-1,error=dx+dy;
        while(true){c.fill(x0-thickness/2,y0-thickness/2,x0+thickness/2+1,y0+thickness/2+1,color);if(x0==x1&&y0==y1)break;int twice=2*error;if(twice>=dy){error+=dy;x0+=sx;}if(twice<=dx){error+=dx;y0+=sy;}}
    }

    /** Turn old orange active accents into Star pink while preserving alpha. */
    private static int accentPink(int color){
        int a=(color>>>24)&255,r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;
        if(r>=210&&g>=70&&g<=190&&b<=125)return(a<<24)|0xFF55C8;
        return color;
    }
}
