package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** First-person CJ forearm/hand with a tapered silhouette instead of one rectangular cuboid. */
public final class CjReferenceHandRenderer {
    private static Identifier texture;
    private static final int SIDES = 8;

    private CjReferenceHandRenderer() {}

    public static boolean render(boolean right, MatrixStack matrices, VertexConsumerProvider consumers,
                                 int light, AbstractClientPlayerEntity player) {
        ClientModule module = CincoClient.findModule("Player Changer");
        if (module == null || !module.enabled || player != MinecraftClient.getInstance().player
                || Math.round(module.setting) != 5) return false;

        int skin = 0xFF6A402E;
        int skinLight = 0xFF85533D;
        int skinDark = 0xFF4A2C21;
        VertexConsumer v = consumers.getBuffer(RenderLayer.getEntitySolid(texture()));

        matrices.push();
        matrices.translate(right ? 0.22 : -0.22, -0.13, -0.70);
        matrices.scale(1.04F, 1.30F, 1.04F);
        MatrixStack.Entry e = matrices.peek();

        // Long forearm like the reference, tapering naturally toward the wrist.
        frustumY(v,e,0.0f,0.00f,0.0f,0.105f,0.120f,
                0.0f,0.56f,0.0f,0.078f,0.092f,skin,light);
        // Wrist + broader palm.
        frustumY(v,e,0.0f,0.53f,0.0f,0.080f,0.092f,
                0.0f,0.74f,0.0f,0.112f,0.105f,skinLight,light);
        frustumY(v,e,0.0f,0.72f,0.0f,0.110f,0.104f,
                0.0f,0.82f,0.0f,0.095f,0.090f,skinLight,light);

        // Four compact fingers plus side thumb produce the open/fist-like silhouette from the shots.
        float start = -0.078f;
        for (int i=0;i<4;i++) {
            float fx0 = start + i * 0.040f;
            box(v,e,fx0,0.80f,-0.058f,fx0+0.029f,0.90f,0.052f,
                    i == 0 || i == 3 ? skin : skinLight,light);
        }
        float tx0 = right ? 0.085f : -0.132f;
        float tx1 = right ? 0.132f : -0.085f;
        box(v,e,tx0,0.68f,-0.050f,tx1,0.80f,0.060f,skin,light);
        box(v,e,-0.086f,0.50f,-0.100f,0.086f,0.535f,-0.088f,skinDark,light);

        matrices.pop();
        return true;
    }

    private static void frustumY(VertexConsumer v, MatrixStack.Entry e,
                                 float x0,float y0,float z0,float rx0,float rz0,
                                 float x1,float y1,float z1,float rx1,float rz1,
                                 int color,int light) {
        for (int i=0;i<SIDES;i++) {
            double a0=Math.PI*2.0*i/SIDES;
            double a1=Math.PI*2.0*(i+1)/SIDES;
            float c0=(float)Math.cos(a0), s0=(float)Math.sin(a0);
            float c1=(float)Math.cos(a1), s1=(float)Math.sin(a1);
            float[] p0={x0+c0*rx0,y0,z0+s0*rz0};
            float[] p1={x0+c1*rx0,y0,z0+s1*rz0};
            float[] q1={x1+c1*rx1,y1,z1+s1*rz1};
            float[] q0={x1+c0*rx1,y1,z1+s0*rz1};
            double am=(a0+a1)*0.5;
            float nx=(float)Math.cos(am), nz=(float)Math.sin(am);
            quad(v,e,p0,p1,q1,q0,nx,0,nz,color,light);
        }
    }

    private static Identifier texture() {
        if (texture != null) return texture;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 2, 2, false);
        for (int y=0;y<2;y++) for (int x=0;x<2;x++) image.setColor(x,y,0xFFFFFFFF);
        texture = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(
                "star_cj_reference_hand_white", new NativeImageBackedTexture(image));
        return texture;
    }

    private static void box(VertexConsumer v, MatrixStack.Entry e, float x0,float y0,float z0,
                            float x1,float y1,float z1,int color,int light) {
        float[] a={x0,y0,z0}, b={x1,y0,z0}, c={x1,y1,z0}, d={x0,y1,z0};
        float[] e0={x0,y0,z1}, f={x1,y0,z1}, g={x1,y1,z1}, h={x0,y1,z1};
        quad(v,e,a,b,c,d,0,0,-1,color,light); quad(v,e,f,e0,h,g,0,0,1,color,light);
        quad(v,e,e0,a,d,h,-1,0,0,color,light); quad(v,e,b,f,g,c,1,0,0,color,light);
        quad(v,e,d,c,g,h,0,1,0,color,light); quad(v,e,e0,f,b,a,0,-1,0,color,light);
    }

    private static void quad(VertexConsumer v, MatrixStack.Entry e, float[] a,float[] b,float[] c,float[] d,
                             float nx,float ny,float nz,int color,int light) {
        vertex(v,e,a,nx,ny,nz,color,light); vertex(v,e,b,nx,ny,nz,color,light);
        vertex(v,e,c,nx,ny,nz,color,light); vertex(v,e,d,nx,ny,nz,color,light);
    }

    private static void vertex(VertexConsumer v, MatrixStack.Entry e, float[] p,
                               float nx,float ny,float nz,int color,int light) {
        v.vertex(e.getPositionMatrix(),p[0],p[1],p[2]).color(color).texture(0.5F,0.5F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e,nx,ny,nz);
    }
}
