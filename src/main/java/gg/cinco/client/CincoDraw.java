package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;

/** Fast UI primitives tuned for the Star Client dashboard. */
public final class CincoDraw {
    private CincoDraw() {}

    /**
     * Very cheap rounded rectangle approximation. The previous supersampled implementation issued
     * many tiny fill calls for every panel, which multiplied badly across the three-column GUI.
     * This uses a small fixed number of spans while preserving a clean rounded silhouette.
     */
    public static void rounded(DrawContext c,int left,int top,int right,int bottom,int radius,int color){
        int w=right-left,h=bottom-top;if(w<=0||h<=0)return;
        int r=Math.max(0,Math.min(radius,Math.min(w,h)/2));
        if(r<=2){c.fill(left,top,right,bottom,color);return;}
        int a=Math.max(1,r/4),b=Math.max(a+1,r/2),d=Math.max(b+1,(r*3)/4);
        // center + side body
        c.fill(left+r,top,right-r,bottom,color);
        c.fill(left,top+r,right,bottom-r,color);
        // four compact corner bands; seven fills total for the whole rounded rect
        c.fill(left+d,top,left+w-d,top+a,color);
        c.fill(left+b,top+a,left+w-b,top+b,color);
        c.fill(left+a,top+b,left+w-a,top+r,color);
        c.fill(left+a,bottom-r,left+w-a,bottom-b,color);
        c.fill(left+b,bottom-b,left+w-b,bottom-a,color);
        c.fill(left+d,bottom-a,left+w-d,bottom,color);
    }

    public static void bubble(DrawContext c,int left,int top,int right,int bottom,int radius,int borderColor,int fillColor){
        rounded(c,left,top,right,bottom,radius,borderColor);
        if(right-left>2&&bottom-top>2)rounded(c,left+1,top+1,right-1,bottom-1,Math.max(2,radius-1),fillColor);
    }

    public static void softShadow(DrawContext c,int left,int top,int right,int bottom,int radius,int rgb,int alpha){
        // One broad low-alpha layer is enough at Minecraft UI scale and far cheaper than stacked AA.
        int a=Math.max(2,alpha/4);
        rounded(c,left-3,top-3,right+3,bottom+3,radius+3,(a<<24)|(rgb&0x00FFFFFF));
    }

    public static void line(DrawContext c,int x0,int y0,int x1,int y1,int color,int thickness){
        int dx=Math.abs(x1-x0),sx=x0<x1?1:-1,dy=-Math.abs(y1-y0),sy=y0<y1?1:-1,error=dx+dy;
        while(true){c.fill(x0-thickness/2,y0-thickness/2,x0+thickness/2+1,y0+thickness/2+1,color);if(x0==x1&&y0==y1)break;int twice=2*error;if(twice>=dy){error+=dy;x0+=sx;}if(twice<=dx){error+=dx;y0+=sy;}}
    }
}
