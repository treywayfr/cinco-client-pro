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
            taperedPrismY(cx,y0,y1,cz,rx,rz,rx,rz,segments,color);
        }

        /** Low-poly tapered cylinder/frustum. Great for GTA-style torsos, limbs and wood bodies. */
        public void taperedPrismY(float cx,float y0,float y1,float cz,
                                  float rx0,float rz0,float rx1,float rz1,int segments,int color) {
            segments=Math.max(6,Math.min(32,segments));
            int bottom=shade(color,.72f),top=lighten(color,.08f);
            for(int i=0;i<segments;i++){
                double a0=i*Math.PI*2.0/segments,a1=(i+1)*Math.PI*2.0/segments;
                float x00=cx+(float)Math.cos(a0)*rx0,z00=cz+(float)Math.sin(a0)*rz0;
                float x01=cx+(float)Math.cos(a1)*rx0,z01=cz+(float)Math.sin(a1)*rz0;
                float x10=cx+(float)Math.cos(a0)*rx1,z10=cz+(float)Math.sin(a0)*rz1;
                float x11=cx+(float)Math.cos(a1)*rx1,z11=cz+(float)Math.sin(a1)*rz1;
                float light=.72f+.28f*(float)Math.max(0,Math.cos((a0+a1)*.5));
                int face=light>.96f?lighten(color,.07f):shade(color,light);
                quad(x00,y0,z00,x01,y0,z01,x11,y1,z11,x10,y1,z10,face);
                tri(cx,y1,cz,x10,y1,z10,x11,y1,z11,top);
                tri(cx,y0,cz,x01,y0,z01,x00,y0,z00,bottom);
            }
        }

        /** Smooth low-poly capsule aligned to Y. Matrix transforms can orient it for articulated limbs. */
        public void capsuleY(float cx,float y0,float y1,float cz,float rx,float rz,int segments,int color) {
            float capY=Math.max(.018f,Math.min(Math.abs(y1-y0)*.22f,rx*.78f));
            float lo=Math.min(y0,y1),hi=Math.max(y0,y1);
            taperedPrismY(cx,lo+capY,hi-capY,cz,rx,rz,rx,rz,segments,color);
            ellipsoid(cx,lo+capY,cz,rx,capY,rz,5,segments,color);
            ellipsoid(cx,hi-capY,cz,rx,capY,rz,5,segments,color);
        }

        public void discY(float cx,float y,float cz,float rx,float rz,int segments,int color) {
            segments=Math.max(8,Math.min(32,segments));
            for(int i=0;i<segments;i++){
                double a0=i*Math.PI*2.0/segments,a1=(i+1)*Math.PI*2.0/segments;
                tri(cx,y,cz,cx+(float)Math.cos(a0)*rx,y,cz+(float)Math.sin(a0)*rz,
                        cx+(float)Math.cos(a1)*rx,y,cz+(float)Math.sin(a1)*rz,color);
            }
        }

        /** Faceted ellipsoid: curved enough for faces/hands while keeping a deliberate game-model look. */
        public void ellipsoid(float cx,float cy,float cz,float rx,float ry,float rz,int rings,int segments,int color) {
            rings=Math.max(4,Math.min(18,rings));segments=Math.max(8,Math.min(32,segments));
            for(int r=0;r<rings;r++){
                double v0=-Math.PI/2.0+r*Math.PI/rings,v1=-Math.PI/2.0+(r+1)*Math.PI/rings;
                float y0=cy+(float)Math.sin(v0)*ry,y1=cy+(float)Math.sin(v1)*ry;
                float rr0=(float)Math.cos(v0),rr1=(float)Math.cos(v1);
                for(int s=0;s<segments;s++){
                    double a0=s*Math.PI*2.0/segments,a1=(s+1)*Math.PI*2.0/segments;
                    float x00=cx+(float)Math.cos(a0)*rx*rr0,z00=cz+(float)Math.sin(a0)*rz*rr0;
                    float x01=cx+(float)Math.cos(a1)*rx*rr0,z01=cz+(float)Math.sin(a1)*rz*rr0;
                    float x10=cx+(float)Math.cos(a0)*rx*rr1,z10=cz+(float)Math.sin(a0)*rz*rr1;
                    float x11=cx+(float)Math.cos(a1)*rx*rr1,z11=cz+(float)Math.sin(a1)*rz*rr1;
                    float light=.72f+.28f*(float)Math.max(0,Math.cos((a0+a1)*.5));
                    int face=light>.95f?lighten(color,.08f):shade(color,light);
                    quad(x00,y0,z00,x01,y0,z01,x11,y1,z11,x10,y1,z10,face);
                }
            }
        }

        /** Thin rounded patch that sits on a surface without reading as a Minecraft cuboid. */
        public void surfaceOval(float cx,float cy,float cz,float rx,float ry,float depth,int segments,int color) {
            ellipsoid(cx,cy,cz,rx,ry,depth,4,Math.max(8,segments),color);
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
