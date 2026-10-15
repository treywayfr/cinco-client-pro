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

/** Lightweight first-person arm replacement matching the selected Player Changer character. */
public final class PlayerChangerHandRenderer {
    private static Identifier texture;

    private PlayerChangerHandRenderer() {}

    public static boolean render(boolean right, MatrixStack matrices, VertexConsumerProvider consumers,
                                 int light, AbstractClientPlayerEntity player) {
        ClientModule module = CincoClient.findModule("Player Changer");
        if (module == null) module = CincoClient.findModule("GTA Skin Changer");
        if (module == null || !module.enabled || module.setting3 < 0.5
                || player != MinecraftClient.getInstance().player) return false;

        int character = Math.max(1, Math.min(4, (int) Math.round(module.setting)));
        int palette = Math.max(1, Math.min(4, (int) Math.round(module.setting2)));
        VertexConsumer v = consumers.getBuffer(RenderLayer.getEntitySolid(getTexture()));

        // Custom-character hands intentionally sit much farther out than vanilla. This gives the
        // Player Changer first-person view a real extended arm instead of a tiny block at the edge.
        matrices.push();
        matrices.translate(right ? -0.035 : 0.035, -0.10, -0.52);
        matrices.scale(1.0F, 1.18F, 1.0F);
        MatrixStack.Entry e = matrices.peek();

        float x0 = right ? 0.125f : -0.375f;
        float x1 = right ? 0.375f : -0.125f;
        float z0 = -0.125f, z1 = 0.125f;

        switch (character) {
            case 2 -> cartoon(v, e, x0, x1, z0, z1, light);
            case 3 -> anime(v, e, x0, x1, z0, z1, palette, light);
            case 4 -> wooden(v, e, x0, x1, z0, z1, light);
            default -> franklin(v, e, x0, x1, z0, z1, palette, right, light);
        }
        matrices.pop();
        return true;
    }

    private static void franklin(VertexConsumer v, MatrixStack.Entry e, float x0, float x1,
                                 float z0, float z1, int palette, boolean right, int light) {
        int skin = 0xFF835136;
        int skinHi = 0xFF9A6446;
        int shirt = switch (palette) {
            case 2 -> 0xFF202326;
            case 3 -> 0xFF214B72;
            case 4 -> 0xFF742E2A;
            default -> 0xFF2C784A;
        };
        box(v,e,x0,0.00f,z0,x1,0.43f,z1,shirt,light);
        box(v,e,x0,0.43f,z0,x1,0.75f,z1,skin,light);
        box(v,e,x0-0.006f,0.405f,z0-0.006f,x1+0.006f,0.438f,z1+0.006f,shade(shirt,0.68f),light);
        box(v,e,x0+0.018f,0.56f,z0-0.008f,x1-0.018f,0.73f,z0+0.012f,skinHi,light);
        if (!right) {
            box(v,e,x0-0.010f,0.455f,z0-0.010f,x1+0.010f,0.505f,z1+0.010f,0xFF141519,light);
            box(v,e,x0-0.015f,0.465f,z0-0.016f,x0+0.090f,0.495f,z0+0.002f,0xFFE1B957,light);
        }
    }

    private static void cartoon(VertexConsumer v, MatrixStack.Entry e, float x0, float x1,
                                float z0, float z1, int light) {
        box(v,e,x0,0.00f,z0,x1,0.34f,z1,0xFFF05B3D,light);
        box(v,e,x0,0.34f,z0,x1,0.75f,z1,0xFFFFD735,light);
        box(v,e,x0-0.005f,0.325f,z0-0.005f,x1+0.005f,0.355f,z1+0.005f,0xFFCA4432,light);
    }

    private static void anime(VertexConsumer v, MatrixStack.Entry e, float x0, float x1,
                              float z0, float z1, int palette, int light) {
        int sleeve = switch (palette) {
            case 2 -> 0xFF191B22;
            case 3 -> 0xFF394A67;
            case 4 -> 0xFF8C3D57;
            default -> 0xFF252939;
        };
        int accent = palette == 3 ? 0xFF72D9FF : 0xFFFF7ACD;
        box(v,e,x0,0.00f,z0,x1,0.46f,z1,sleeve,light);
        box(v,e,x0,0.46f,z0,x1,0.75f,z1,0xFFF0C5B5,light);
        box(v,e,x0-0.006f,0.415f,z0-0.006f,x1+0.006f,0.455f,z1+0.006f,accent,light);
    }

    private static void wooden(VertexConsumer v, MatrixStack.Entry e, float x0, float x1,
                               float z0, float z1, int light) {
        box(v,e,x0,0.00f,z0,x1,0.75f,z1,0xFF9A6236,light);
        box(v,e,x0+0.030f,0.12f,z0-0.008f,x0+0.055f,0.68f,z0+0.008f,0xFF5F351D,light);
        box(v,e,x1-0.065f,0.22f,z1-0.008f,x1-0.035f,0.60f,z1+0.008f,0xFFC18149,light);
    }

    private static Identifier getTexture() {
        if (texture != null) return texture;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 2, 2, false);
        for (int y=0;y<2;y++) for(int x=0;x<2;x++) image.setColor(x,y,0xFFFFFFFF);
        texture = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(
                "star_player_hand_white", new NativeImageBackedTexture(image));
        return texture;
    }

    private static int shade(int color, float factor) {
        int a=(color>>>24)&255, r=(color>>>16)&255, g=(color>>>8)&255, b=color&255;
        r=Math.max(0,Math.min(255,Math.round(r*factor)));
        g=Math.max(0,Math.min(255,Math.round(g*factor)));
        b=Math.max(0,Math.min(255,Math.round(b*factor)));
        return (a<<24)|(r<<16)|(g<<8)|b;
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
