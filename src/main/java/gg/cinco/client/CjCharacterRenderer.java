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
import net.minecraft.util.math.RotationAxis;

/** Original low-poly CJ-inspired street character; no GTA game assets are bundled. */
public final class CjCharacterRenderer {
    private static Identifier texture;
    private CjCharacterRenderer() {}

    public static void render(AbstractClientPlayerEntity player, float bodyYaw, float tickDelta,
                              MatrixStack matrices, VertexConsumerProvider consumers, int light, int outfit) {
        outfit = Math.max(1, Math.min(6, outfit));
        int skin = 0xFF6F432F;
        int skinHi = 0xFF865640;
        int skinDark = 0xFF4D2D22;
        int hair = 0xFF111111;
        int white = 0xFFF2F1EB;
        int gold = 0xFFD6B35A;
        int top = switch (outfit) {
            case 2 -> 0xFF2E6F43;
            case 3 -> 0xFF17191C;
            case 4 -> 0xFF7A2B2F;
            case 5 -> 0xFF34704B;
            case 6 -> 0xFF595A60;
            default -> 0xFFF1EFE8;
        };
        int pants = switch (outfit) {
            case 2 -> 0xFF5B563F;
            case 3 -> 0xFF161D27;
            case 6 -> 0xFF24262C;
            default -> 0xFF24364D;
        };
        int topDark = shade(top, 0.68f);
        int pantsDark = shade(pants, 0.70f);

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
        if (player.isSneaking()) matrices.translate(0.0, -0.08, 0.08);
        VertexConsumer v = consumers.getBuffer(RenderLayer.getEntitySolid(texture()));
        MatrixStack.Entry e = matrices.peek();

        double speed = player.getVelocity().horizontalLength();
        float motion = (float)Math.min(1.0, speed * 4.5);
        float phase = (player.age + tickDelta) * 0.62f;
        float leg = (float)Math.sin(phase) * 0.22f * motion;
        float arm = -leg * 0.88f;

        // Sneakers and layered soles.
        box(v,e,-0.25f,0.01f,-0.22f+leg,-0.015f,0.065f,0.17f+leg,0xFF17191C,light);
        box(v,e,0.015f,0.01f,-0.22f-leg,0.25f,0.065f,0.17f-leg,0xFF17191C,light);
        box(v,e,-0.235f,0.065f,-0.21f+leg,-0.025f,0.145f,0.14f+leg,white,light);
        box(v,e,0.025f,0.065f,-0.21f-leg,0.235f,0.145f,0.14f-leg,white,light);
        box(v,e,-0.22f,0.115f,-0.225f+leg,-0.04f,0.135f,-0.202f+leg,0xFFD9D8D3,light);
        box(v,e,0.04f,0.115f,-0.225f-leg,0.22f,0.135f,-0.202f-leg,0xFFD9D8D3,light);

        // Jeans/trousers with separate thighs and calves for less blocky proportions.
        box(v,e,-0.245f,0.15f,-0.13f+leg,-0.025f,0.70f,0.14f+leg,pantsDark,light);
        box(v,e,0.025f,0.15f,-0.13f-leg,0.245f,0.70f,0.14f-leg,pantsDark,light);
        box(v,e,-0.255f,0.66f,-0.145f,-0.018f,1.00f,0.15f,pants,light);
        box(v,e,0.018f,0.66f,-0.145f,0.255f,1.00f,0.15f,pants,light);
        box(v,e,-0.245f,0.86f,-0.162f,0.245f,0.92f,0.162f,0xFF171719,light);
        box(v,e,-0.045f,0.855f,-0.170f,0.045f,0.925f,-0.160f,gold,light);

        // Athletic torso: narrow waist, broad shoulders. Outfit 1 is the classic tank silhouette.
        box(v,e,-0.23f,0.92f,-0.14f,0.23f,1.30f,0.145f,top,light);
        box(v,e,-0.285f,1.28f,-0.145f,0.285f,1.48f,0.15f,top,light);
        if (outfit == 1) {
            box(v,e,-0.085f,1.18f,-0.154f,0.085f,1.49f,-0.145f,skin,light);
            box(v,e,-0.22f,1.39f,-0.154f,-0.115f,1.49f,-0.145f,skin,light);
            box(v,e,0.115f,1.39f,-0.154f,0.22f,1.49f,-0.145f,skin,light);
        } else if (outfit == 4 || outfit == 5) {
            box(v,e,-0.10f,1.00f,-0.158f,0.10f,1.43f,-0.148f,white,light);
            box(v,e,-0.245f,1.02f,-0.163f,-0.11f,1.44f,-0.151f,topDark,light);
            box(v,e,0.11f,1.02f,-0.163f,0.245f,1.44f,-0.151f,topDark,light);
        }
        // Seams, pocket, chain.
        box(v,e,-0.215f,1.02f,-0.160f,-0.205f,1.43f,-0.150f,topDark,light);
        box(v,e,0.205f,1.02f,-0.160f,0.215f,1.43f,-0.150f,topDark,light);
        box(v,e,-0.19f,1.14f,-0.166f,-0.08f,1.22f,-0.157f,shade(top,0.82f),light);
        box(v,e,-0.012f,1.31f,-0.170f,0.012f,1.43f,-0.160f,gold,light);
        box(v,e,-0.045f,1.30f,-0.171f,0.045f,1.318f,-0.160f,gold,light);

        // Arms, elbows and hands with symmetric motion.
        box(v,e,-0.39f,1.16f,-0.095f+arm,-0.285f,1.43f,0.095f+arm, outfit==1?skin:top,light);
        box(v,e,0.285f,1.16f,-0.095f-arm,0.39f,1.43f,0.095f-arm, outfit==1?skin:top,light);
        box(v,e,-0.415f,0.80f,-0.085f+arm,-0.305f,1.18f,0.085f+arm,skin,light);
        box(v,e,0.305f,0.80f,-0.085f-arm,0.415f,1.18f,0.085f-arm,skin,light);
        box(v,e,-0.43f,0.72f,-0.10f+arm,-0.29f,0.83f,0.10f+arm,skinHi,light);
        box(v,e,0.29f,0.72f,-0.10f-arm,0.43f,0.83f,0.10f-arm,skinHi,light);

        // Neck and a shaped head made from jaw/cheek/cranium sections rather than one cube.
        box(v,e,-0.09f,1.46f,-0.085f,0.09f,1.57f,0.085f,skinDark,light);
        box(v,e,-0.16f,1.57f,-0.145f,0.16f,1.72f,0.15f,skin,light);      // jaw
        box(v,e,-0.19f,1.68f,-0.165f,0.19f,1.86f,0.16f,skin,light);      // cheeks
        box(v,e,-0.17f,1.84f,-0.145f,0.17f,1.94f,0.14f,skinDark,light);  // forehead
        box(v,e,-0.205f,1.70f,-0.06f,-0.185f,1.80f,0.06f,skinDark,light);
        box(v,e,0.185f,1.70f,-0.06f,0.205f,1.80f,0.06f,skinDark,light);

        // Close-cut hair/fade, brows, eyes, nose, lips and subtle goatee shadow.
        box(v,e,-0.172f,1.855f,-0.148f,0.172f,1.955f,0.13f,hair,light);
        box(v,e,-0.187f,1.76f,-0.12f,-0.169f,1.88f,0.09f,0xFF2C201C,light);
        box(v,e,0.169f,1.76f,-0.12f,0.187f,1.88f,0.09f,0xFF2C201C,light);
        box(v,e,-0.125f,1.785f,-0.176f,-0.025f,1.805f,-0.165f,hair,light);
        box(v,e,0.025f,1.785f,-0.176f,0.125f,1.805f,-0.165f,hair,light);
        box(v,e,-0.112f,1.748f,-0.179f,-0.025f,1.770f,-0.168f,white,light);
        box(v,e,0.025f,1.748f,-0.179f,0.112f,1.770f,-0.168f,white,light);
        box(v,e,-0.070f,1.750f,-0.184f,-0.045f,1.771f,-0.174f,0xFF201716,light);
        box(v,e,0.045f,1.750f,-0.184f,0.070f,1.771f,-0.174f,0xFF201716,light);
        box(v,e,-0.028f,1.665f,-0.205f,0.030f,1.742f,-0.170f,skinDark,light);
        box(v,e,-0.080f,1.620f,-0.183f,0.080f,1.642f,-0.172f,0xFF5D342D,light);
        box(v,e,-0.055f,1.575f,-0.176f,0.055f,1.615f,-0.166f,0xFF2A1A18,light);

        matrices.pop();
    }

    private static Identifier texture() {
        if (texture != null) return texture;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 2, 2, false);
        for (int y=0;y<2;y++) for (int x=0;x<2;x++) image.setColor(x,y,0xFFFFFFFF);
        texture = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(
                "star_cj_white", new NativeImageBackedTexture(image));
        return texture;
    }

    private static int shade(int color, float f) {
        int a=(color>>>24)&255, r=(color>>>16)&255, g=(color>>>8)&255, b=color&255;
        r=Math.max(0,Math.min(255,Math.round(r*f))); g=Math.max(0,Math.min(255,Math.round(g*f))); b=Math.max(0,Math.min(255,Math.round(b*f)));
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
