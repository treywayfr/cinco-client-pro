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

/**
 * Lightweight close-range detail pass for Player Changer. Extra geometry is concentrated on
 * faces, hair, seams and accessories so the characters read better without multiplying the
 * polygon count of the whole body.
 */
public final class CharacterPremiumOverlay {
    private static Identifier texture;

    private CharacterPremiumOverlay() {}

    public static void render(AbstractClientPlayerEntity player, float bodyYaw, float tickDelta,
                              MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        ClientModule module = CincoClient.findModule("Player Changer");
        if (module == null) module = CincoClient.findModule("GTA Skin Changer");
        if (module == null || !module.enabled || module.setting3 < 0.5) return;

        int character = Math.max(1, Math.min(4, (int) Math.round(module.setting)));
        int palette = Math.max(1, Math.min(4, (int) Math.round(module.setting2)));

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
        if (player.isSneaking()) matrices.translate(0.0, -0.08, 0.08);
        VertexConsumer v = consumers.getBuffer(RenderLayer.getEntitySolid(getTexture()));
        MatrixStack.Entry e = matrices.peek();

        switch (character) {
            case 2 -> cartoon(v, e, light);
            case 3 -> anime(v, e, palette, light);
            case 4 -> wooden(v, e, light);
            default -> franklin(v, e, palette, light);
        }
        matrices.pop();
    }

    private static void franklin(VertexConsumer v, MatrixStack.Entry e, int palette, int light) {
        int skin = 0xFF835136;
        int skinHi = 0xFF9A6446;
        int skinDark = 0xFF563326;
        int hair = 0xFF141110;
        int beard = 0xFF241817;
        int gold = 0xFFE3BA59;
        int denim = palette == 3 ? 0xFF202935 : 0xFF27364A;
        int shirt = switch (palette) {
            case 2 -> 0xFF202326;
            case 3 -> 0xFF214B72;
            case 4 -> 0xFF742E2A;
            default -> 0xFF2C784A;
        };

        // Face depth: inner ears, cheek planes, projected nose and jaw breakup.
        box(v,e,-0.214f,1.690f,-0.040f,-0.198f,1.756f,0.030f,skinDark,light);
        box(v,e,0.198f,1.690f,-0.040f,0.214f,1.756f,0.030f,skinDark,light);
        box(v,e,-0.154f,1.650f,-0.200f,-0.080f,1.704f,-0.188f,skinHi,light);
        box(v,e,0.080f,1.650f,-0.200f,0.154f,1.704f,-0.188f,skinHi,light);
        box(v,e,-0.030f,1.661f,-0.230f,0.030f,1.716f,-0.206f,skinDark,light);
        box(v,e,-0.044f,1.647f,-0.232f,0.044f,1.666f,-0.213f,skin,light);
        box(v,e,-0.128f,1.572f,-0.198f,-0.086f,1.626f,-0.188f,beard,light);
        box(v,e,0.086f,1.572f,-0.198f,0.128f,1.626f,-0.188f,beard,light);
        box(v,e,-0.040f,1.555f,-0.205f,0.040f,1.598f,-0.194f,beard,light);
        box(v,e,-0.055f,1.622f,-0.212f,0.055f,1.630f,-0.205f,0xFF9B6253,light);

        // More natural fade/hairline silhouette.
        for (int i = 0; i < 5; i++) {
            float x0 = -0.145f + i * 0.058f;
            float y0 = 1.890f - Math.abs(2-i)*0.008f;
            box(v,e,x0,y0,-0.188f,x0+0.045f,1.918f,-0.170f,hair,light);
        }
        box(v,e,-0.198f,1.730f,-0.150f,-0.186f,1.838f,0.060f,0xFF31211D,light);
        box(v,e,0.186f,1.730f,-0.150f,0.198f,1.838f,0.060f,0xFF31211D,light);

        // Shirt stitching, cuff edges and pocket flap.
        box(v,e,-0.302f,1.160f,-0.112f,-0.286f,1.375f,0.105f,shade(shirt,0.68f),light);
        box(v,e,0.286f,1.160f,-0.112f,0.302f,1.375f,0.105f,shade(shirt,0.68f),light);
        box(v,e,-0.201f,1.286f,-0.190f,-0.106f,1.301f,-0.182f,shade(shirt,1.18f),light);
        box(v,e,-0.195f,1.222f,-0.192f,-0.113f,1.236f,-0.184f,shade(shirt,0.70f),light);

        // Watch, buckle, jean seams and sneaker laces.
        box(v,e,-0.454f,0.806f,-0.095f,-0.356f,0.858f,-0.079f,0xFF111318,light);
        box(v,e,-0.438f,0.816f,-0.100f,-0.372f,0.848f,-0.090f,gold,light);
        box(v,e,-0.030f,0.864f,-0.181f,0.030f,0.913f,-0.168f,gold,light);
        box(v,e,-0.008f,0.722f,-0.151f,0.008f,0.858f,-0.143f,shade(denim,0.62f),light);
        box(v,e,-0.220f,0.810f,-0.153f,-0.102f,0.827f,-0.145f,shade(denim,1.22f),light);
        box(v,e,0.102f,0.810f,-0.153f,0.220f,0.827f,-0.145f,shade(denim,1.22f),light);
        for (int i=0;i<3;i++) {
            float y=0.100f+i*0.013f;
            box(v,e,-0.205f,y,-0.194f,-0.060f,y+0.006f,-0.187f,0xFFB8BDB9,light);
            box(v,e,0.060f,y,-0.194f,0.205f,y+0.006f,-0.187f,0xFFB8BDB9,light);
        }
    }

    private static void cartoon(VertexConsumer v, MatrixStack.Entry e, int light) {
        int yellow = 0xFFFFD735;
        int yellowDark = 0xFFD89E16;
        int red = 0xFFF05B3D;
        int blue = 0xFF315EBC;

        // Stepped spikes make the head silhouette much more recognizable without expensive curves.
        for (int i=0;i<7;i++) {
            float x=-0.175f+i*0.058f;
            float h=0.10f+(i%2)*0.035f;
            box(v,e,x,1.790f,-0.060f,x+0.042f,1.790f+h,0.045f,yellow,light);
        }
        box(v,e,-0.236f,1.455f,-0.025f,-0.218f,1.535f,0.024f,yellowDark,light);
        box(v,e,0.218f,1.455f,-0.025f,0.236f,1.535f,0.024f,yellowDark,light);
        box(v,e,-0.140f,1.506f,-0.196f,-0.006f,1.520f,-0.188f,0xFF3B2A19,light);
        box(v,e,0.006f,1.506f,-0.196f,0.140f,1.520f,-0.188f,0xFF3B2A19,light);
        box(v,e,-0.104f,1.360f,-0.207f,0.104f,1.384f,-0.195f,0xFF6A351F,light);
        box(v,e,-0.055f,1.362f,-0.211f,0.055f,1.373f,-0.202f,0xFFFDF7E7,light);
        box(v,e,-0.205f,0.817f,-0.150f,0.205f,0.835f,-0.142f,shade(red,0.72f),light);
        box(v,e,-0.010f,0.606f,-0.151f,0.010f,0.815f,-0.144f,shade(blue,0.72f),light);
        box(v,e,-0.195f,0.042f,-0.171f,-0.025f,0.060f,-0.162f,0xFFFFFFFF,light);
        box(v,e,0.025f,0.042f,-0.171f,0.195f,0.060f,-0.162f,0xFFFFFFFF,light);
    }

    private static void anime(VertexConsumer v, MatrixStack.Entry e, int palette, int light) {
        int hair = switch (palette) {
            case 2 -> 0xFF18191D;
            case 3 -> 0xFF6A347F;
            case 4 -> 0xFFB66175;
            default -> 0xFF2C2548;
        };
        int accent = palette == 3 ? 0xFF72D9FF : 0xFFFF7ACD;
        int dark = shade(hair,0.68f);

        // Layered fringe and side strands.
        for (int i=0;i<4;i++) {
            float x=-0.186f+i*0.105f;
            box(v,e,x,1.760f,-0.199f,x+0.052f,1.892f-(i%2)*0.035f,-0.190f,
                    i==1 || i==2 ? shade(hair,1.18f) : hair,light);
        }
        box(v,e,-0.220f,1.250f,0.115f,-0.176f,1.720f,0.170f,dark,light);
        box(v,e,0.176f,1.250f,0.115f,0.220f,1.720f,0.170f,dark,light);
        box(v,e,-0.137f,1.740f,-0.204f,-0.010f,1.754f,-0.197f,dark,light);
        box(v,e,0.010f,1.740f,-0.204f,0.137f,1.754f,-0.197f,dark,light);
        box(v,e,-0.073f,1.701f,-0.209f,-0.040f,1.738f,-0.202f,accent,light);
        box(v,e,0.040f,1.701f,-0.209f,0.073f,1.738f,-0.202f,accent,light);
        box(v,e,-0.060f,1.716f,-0.213f,-0.051f,1.730f,-0.208f,0xFFFFFFFF,light);
        box(v,e,0.051f,1.716f,-0.213f,0.060f,1.730f,-0.208f,0xFFFFFFFF,light);
        box(v,e,-0.015f,1.655f,-0.207f,0.015f,1.686f,-0.198f,0xFFD99D91,light);
        box(v,e,-0.052f,1.615f,-0.204f,0.052f,1.628f,-0.197f,0xFFC8677F,light);

        // Bow and skirt pleats.
        box(v,e,-0.105f,1.295f,-0.180f,-0.010f,1.360f,-0.168f,accent,light);
        box(v,e,0.010f,1.295f,-0.180f,0.105f,1.360f,-0.168f,accent,light);
        box(v,e,-0.026f,1.300f,-0.186f,0.026f,1.355f,-0.172f,shade(accent,0.72f),light);
        for(int i=0;i<5;i++) {
            float x=-0.225f+i*0.112f;
            box(v,e,x,0.690f,-0.185f,x+0.018f,0.910f,-0.175f,0xFF151A27,light);
        }
        box(v,e,-0.175f,0.610f,-0.095f,-0.030f,0.635f,0.095f,accent,light);
        box(v,e,0.030f,0.610f,-0.095f,0.175f,0.635f,0.095f,accent,light);
    }

    private static void wooden(VertexConsumer v, MatrixStack.Entry e, int light) {
        int wood = 0xFF9A6236;
        int lightWood = 0xFFC18149;
        int dark = 0xFF5F351D;
        int black = 0xFF14110F;

        // More expressive face.
        box(v,e,-0.125f,1.603f,-0.207f,-0.035f,1.675f,-0.197f,0xFFEBD9B9,light);
        box(v,e,0.035f,1.603f,-0.207f,0.125f,1.675f,-0.197f,0xFFEBD9B9,light);
        box(v,e,-0.085f,1.620f,-0.213f,-0.052f,1.662f,-0.204f,black,light);
        box(v,e,0.052f,1.620f,-0.213f,0.085f,1.662f,-0.204f,black,light);
        box(v,e,-0.132f,1.690f,-0.202f,-0.030f,1.708f,-0.194f,dark,light);
        box(v,e,0.030f,1.690f,-0.202f,0.132f,1.708f,-0.194f,dark,light);
        box(v,e,-0.025f,1.545f,-0.222f,0.025f,1.605f,-0.203f,lightWood,light);
        box(v,e,-0.090f,1.490f,-0.212f,0.090f,1.520f,-0.202f,black,light);
        box(v,e,-0.058f,1.494f,-0.216f,0.058f,1.507f,-0.207f,0xFFE6D6B5,light);

        // Bark grain and club wrapping.
        box(v,e,-0.205f,1.188f,-0.242f,-0.060f,1.205f,-0.232f,dark,light);
        box(v,e,0.078f,1.030f,-0.243f,0.198f,1.047f,-0.233f,lightWood,light);
        box(v,e,-0.155f,0.842f,-0.242f,0.105f,0.858f,-0.232f,dark,light);
        box(v,e,-0.025f,0.705f,-0.244f,0.025f,1.315f,-0.235f,shade(wood,0.76f),light);
        box(v,e,0.500f,0.300f,-0.020f,0.585f,0.345f,0.020f,black,light);
        box(v,e,0.515f,0.245f,-0.025f,0.595f,0.282f,0.025f,0xFF3B2518,light);
    }

    private static Identifier getTexture() {
        if (texture != null) return texture;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 2, 2, false);
        for (int y=0;y<2;y++) for(int x=0;x<2;x++) image.setColor(x,y,0xFFFFFFFF);
        texture = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(
                "cinco_character_premium_white", new NativeImageBackedTexture(image));
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
