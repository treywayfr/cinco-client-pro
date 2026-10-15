package gg.cinco.client;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/** Tiny textureless solid-geometry helper used by Star's local character presets on 1.21.11. */
public final class StarCharacterGeometry {
    private StarCharacterGeometry() {}

    public static Drawer begin(MatrixStack matrices, VertexConsumerProvider consumers) {
        return new Drawer(matrices.peek().getPositionMatrix(), consumers.getBuffer(RenderLayers.debugQuads()));
    }

    public static final class Drawer {
        private final Matrix4f matrix;
        private final VertexConsumer out;
        private Drawer(Matrix4f matrix, VertexConsumer out) { this.matrix = matrix; this.out = out; }

        public void box(float x0,float y0,float z0,float x1,float y1,float z1,int color) {
            quad(x0,y0,z0, x1,y0,z0, x1,y1,z0, x0,y1,z0, color);
            quad(x1,y0,z1, x0,y0,z1, x0,y1,z1, x1,y1,z1, color);
            quad(x0,y0,z1, x0,y0,z0, x0,y1,z0, x0,y1,z1, shade(color,.82f));
            quad(x1,y0,z0, x1,y0,z1, x1,y1,z1, x1,y1,z0, shade(color,.92f));
            quad(x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1, lighten(color,.10f));
            quad(x0,y0,z1, x1,y0,z1, x1,y0,z0, x0,y0,z0, shade(color,.72f));
        }

        public void quad(float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz,int color) {
            vertex(ax,ay,az,color); vertex(bx,by,bz,color); vertex(cx,cy,cz,color); vertex(dx,dy,dz,color);
        }

        private void vertex(float x,float y,float z,int color) { out.vertex(matrix,x,y,z).color(color); }
    }

    public static int shade(int color,float k){int a=(color>>>24)&255,r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;r=Math.max(0,Math.min(255,Math.round(r*k)));g=Math.max(0,Math.min(255,Math.round(g*k)));b=Math.max(0,Math.min(255,Math.round(b*k)));return(a<<24)|(r<<16)|(g<<8)|b;}
    public static int lighten(int color,float k){int a=(color>>>24)&255,r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;r=Math.round(r+(255-r)*k);g=Math.round(g+(255-g)*k);b=Math.round(b+(255-b)*k);return(a<<24)|(r<<16)|(g<<8)|b;}
}
