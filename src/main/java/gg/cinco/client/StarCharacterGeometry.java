package gg.cinco.client;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/** Shared solid-geometry helper for Star's articulated Locker character models. */
public final class StarCharacterGeometry {
    private StarCharacterGeometry() {}

    public static Drawer begin(MatrixStack matrices, VertexConsumerProvider consumers) {
        return new Drawer(matrices, consumers.getBuffer(RenderLayers.debugQuads()));
    }

    public static final class Drawer {
        private final MatrixStack matrices;
        private final VertexConsumer out;
        private Drawer(MatrixStack matrices, VertexConsumer out) { this.matrices = matrices; this.out = out; }

        public void box(float x0,float y0,float z0,float x1,float y1,float z1,int color) {
            quad(x0,y0,z0, x1,y0,z0, x1,y1,z0, x0,y1,z0, color);
            quad(x1,y0,z1, x0,y0,z1, x0,y1,z1, x1,y1,z1, color);
            quad(x0,y0,z1, x0,y0,z0, x0,y1,z0, x0,y1,z1, shade(color,.82f));
            quad(x1,y0,z0, x1,y0,z1, x1,y1,z1, x1,y1,z0, shade(color,.92f));
            quad(x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1, lighten(color,.10f));
            quad(x0,y0,z1, x1,y0,z1, x1,y0,z0, x0,y0,z0, shade(color,.72f));
        }

        public void prismY(float cx,float y0,float y1,float cz,float rx,float rz,int segments,int color) {
            segments=Math.max(6,Math.min(24,segments));
            int side=shade(color,.92f), bottom=shade(color,.72f), top=lighten(color,.08f);
            for(int i=0;i<segments;i++){
                double a0=i*Math.PI*2.0/segments,a1=(i+1)*Math.PI*2.0/segments;
                float x0=cx+(float)Math.cos(a0)*rx,z0=cz+(float)Math.sin(a0)*rz;
                float x1=cx+(float)Math.cos(a1)*rx,z1=cz+(float)Math.sin(a1)*rz;
                quad(x0,y0,z0,x1,y0,z1,x1,y1,z1,x0,y1,z0,(i&1)==0?color:side);
                tri(cx,y1,cz,x0,y1,z0,x1,y1,z1,top);
                tri(cx,y0,cz,x1,y0,z1,x0,y0,z0,bottom);
            }
        }

        public void discY(float cx,float y,float cz,float rx,float rz,int segments,int color) {
            segments=Math.max(8,Math.min(32,segments));
            for(int i=0;i<segments;i++){
                double a0=i*Math.PI*2.0/segments,a1=(i+1)*Math.PI*2.0/segments;
                tri(cx,y,cz,cx+(float)Math.cos(a0)*rx,y,cz+(float)Math.sin(a0)*rz,
                        cx+(float)Math.cos(a1)*rx,y,cz+(float)Math.sin(a1)*rz,color);
            }
        }

        public void quad(float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz,int color) {
            vertex(ax,ay,az,color); vertex(bx,by,bz,color); vertex(cx,cy,cz,color); vertex(dx,dy,dz,color);
        }

        public void tri(float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,int color) {
            vertex(ax,ay,az,color); vertex(bx,by,bz,color); vertex(cx,cy,cz,color); vertex(cx,cy,cz,color);
        }

        private void vertex(float x,float y,float z,int color) {
            out.vertex(matrices.peek().getPositionMatrix(),x,y,z).color(color);
        }
    }

    public static int shade(int color,float k){int a=(color>>>24)&255,r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;r=Math.max(0,Math.min(255,Math.round(r*k)));g=Math.max(0,Math.min(255,Math.round(g*k)));b=Math.max(0,Math.min(255,Math.round(b*k)));return(a<<24)|(r<<16)|(g<<8)|b;}
    public static int lighten(int color,float k){int a=(color>>>24)&255,r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;r=Math.round(r+(255-r)*k);g=Math.round(g+(255-g)*k);b=Math.round(b+(255-b)*k);return(a<<24)|(r<<16)|(g<<8)|b;}
}
