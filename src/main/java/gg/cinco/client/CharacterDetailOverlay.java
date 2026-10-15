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
 * Fine geometry layered over the procedural character bodies. Keeping these details separate
 * makes the silhouettes inexpensive while allowing the face/clothes/accessories to be denser.
 */
public final class CharacterDetailOverlay {
    private static Identifier whiteTexture;

    private CharacterDetailOverlay() {}

    public static void render(AbstractClientPlayerEntity player, float bodyYaw, float tickDelta,
                              MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        ClientModule module = CincoClient.findModule("GTA Skin Changer");
        if (module == null || !module.enabled) return;
        int character = Math.max(1, Math.min(4, (int) Math.round(module.setting)));
        int palette = Math.max(1, Math.min(4, (int) Math.round(module.setting2)));

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
        if (player.isSneaking()) matrices.translate(0.0, -0.08, 0.08);
        VertexConsumer v = consumers.getBuffer(RenderLayer.getEntitySolid(getWhiteTexture()));
        MatrixStack.Entry e = matrices.peek();

        switch (character) {
            case 2 -> spikyKid(v, e, light);
            case 3 -> anime(v, e, palette, light);
            case 4 -> wooden(v, e, light);
            default -> franklin(v, e, palette, light);
        }
        matrices.pop();
    }

    private static void franklin(VertexConsumer v, MatrixStack.Entry e, int palette, int light) {
        int skin = 0xFF835136;
        int skinDark = 0xFF5D3829;
        int hair = 0xFF151211;
        int beard = 0xFF211716;
        int lip = 0xFF6F4135;
        int white = 0xFFF8F4ED;
        int pupil = 0xFF171414;
        int gold = 0xFFE0B958;
        int denim = palette == 3 ? 0xFF202935 : 0xFF27364A;
        int green = switch (palette) {
            case 2 -> 0xFF202326;
            case 3 -> 0xFF214B72;
            case 4 -> 0xFF742E2A;
            default -> 0xFF2C784A;
        };
        int greenHi = shade(green, 1.16f);
        int greenDark = shade(green, 0.68f);

        // Hairline and temple fade: multiple narrow planes break up the old helmet-like cap.
        box(v,e,-0.154f,1.870f,-0.168f,-0.082f,1.902f,-0.155f,hair,light);
        box(v,e,-0.078f,1.884f,-0.184f,0.000f,1.912f,-0.168f,hair,light);
        box(v,e,0.000f,1.884f,-0.184f,0.078f,1.912f,-0.168f,hair,light);
        box(v,e,0.082f,1.870f,-0.168f,0.154f,1.902f,-0.155f,hair,light);
        box(v,e,-0.194f,1.735f,-0.176f,-0.180f,1.843f,-0.125f,shade(hair,1.35f),light);
        box(v,e,0.180f,1.735f,-0.176f,0.194f,1.843f,-0.125f,shade(hair,1.35f),light);

        // Brows, eyelids and tiny catchlights make the face read at normal third-person distance.
        box(v,e,-0.124f,1.790f,-0.201f,-0.020f,1.808f,-0.194f,hair,light);
        box(v,e,0.020f,1.790f,-0.201f,0.124f,1.808f,-0.194f,hair,light);
        box(v,e,-0.111f,1.745f,-0.202f,-0.028f,1.754f,-0.196f,skinDark,light);
        box(v,e,0.028f,1.745f,-0.202f,0.111f,1.754f,-0.196f,skinDark,light);
        box(v,e,-0.072f,1.755f,-0.205f,-0.049f,1.774f,-0.198f,pupil,light);
        box(v,e,0.049f,1.755f,-0.205f,0.072f,1.774f,-0.198f,pupil,light);
        box(v,e,-0.060f,1.763f,-0.208f,-0.053f,1.770f,-0.204f,white,light);
        box(v,e,0.053f,1.763f,-0.208f,0.060f,1.770f,-0.204f,white,light);

        // Nose bridge, nostrils, lips, jawline and goatee layering.
        box(v,e,-0.022f,1.664f,-0.222f,0.022f,1.735f,-0.199f,skinDark,light);
        box(v,e,-0.030f,1.653f,-0.224f,-0.008f,1.667f,-0.216f,beard,light);
        box(v,e,0.008f,1.653f,-0.224f,0.030f,1.667f,-0.216f,beard,light);
        box(v,e,-0.072f,1.615f,-0.207f,0.072f,1.632f,-0.200f,lip,light);
        box(v,e,-0.054f,1.594f,-0.205f,0.054f,1.611f,-0.199f,beard,light);
        box(v,e,-0.126f,1.574f,-0.198f,-0.083f,1.642f,-0.190f,beard,light);
        box(v,e,0.083f,1.574f,-0.198f,0.126f,1.642f,-0.190f,beard,light);
        box(v,e,-0.032f,1.555f,-0.201f,0.032f,1.602f,-0.194f,beard,light);

        // Shirt seams, buttons, chest pocket and tee neckline.
        box(v,e,-0.116f,1.360f,-0.183f,0.116f,1.378f,-0.176f,0xFFD9D5CD,light);
        box(v,e,-0.014f,1.095f,-0.184f,0.014f,1.430f,-0.177f,greenDark,light);
        for (int i = 0; i < 4; i++) {
            float y = 1.12f + i * 0.075f;
            box(v,e,-0.009f,y,-0.189f,0.009f,y+0.018f,-0.181f,0xFF17191A,light);
        }
        box(v,e,-0.194f,1.205f,-0.189f,-0.112f,1.295f,-0.181f,greenHi,light);
        box(v,e,-0.184f,1.217f,-0.192f,-0.122f,1.228f,-0.184f,greenDark,light);

        // Necklace + pendant, belt loops, fly and jean pocket seams.
        box(v,e,-0.062f,1.318f,-0.188f,-0.052f,1.397f,-0.181f,gold,light);
        box(v,e,0.052f,1.318f,-0.188f,0.062f,1.397f,-0.181f,gold,light);
        box(v,e,-0.052f,1.310f,-0.188f,0.052f,1.322f,-0.181f,gold,light);
        box(v,e,-0.024f,1.285f,-0.191f,0.024f,1.325f,-0.183f,gold,light);
        for (float x : new float[]{-0.17f,-0.08f,0.08f,0.17f})
            box(v,e,x-0.010f,0.865f,-0.177f,x+0.010f,0.925f,-0.169f,0xFF151719,light);
        box(v,e,-0.010f,0.735f,-0.145f,0.010f,0.865f,-0.137f,shade(denim,0.65f),light);
        box(v,e,-0.215f,0.825f,-0.149f,-0.100f,0.844f,-0.141f,shade(denim,1.22f),light);
        box(v,e,0.100f,0.825f,-0.149f,0.215f,0.844f,-0.141f,shade(denim,1.22f),light);

        // Sneaker panels and laces.
        for (int i=0;i<3;i++) {
            float y=0.098f+i*0.012f;
            box(v,e,-0.205f,y,-0.190f,-0.065f,y+0.006f,-0.183f,0xFFBFC2BF,light);
            box(v,e,0.065f,y,-0.190f,0.205f,y+0.006f,-0.183f,0xFFBFC2BF,light);
        }
        box(v,e,-0.230f,0.074f,-0.192f,-0.025f,0.090f,-0.185f,0xFFFDFDFC,light);
        box(v,e,0.025f,0.074f,-0.192f,0.230f,0.090f,-0.185f,0xFFFDFDFC,light);
    }

    private static void spikyKid(VertexConsumer v, MatrixStack.Entry e, int light) {
        int yellow=0xFFFFD735, dark=0xFFD29A13, red=0xFFF05B3D, blue=0xFF315EBC, white=0xFFFFFFFF;
        // Ears + smile/teeth + cheek marks.
        box(v,e,-0.237f,1.445f,-0.040f,-0.211f,1.548f,0.035f,dark,light);
        box(v,e,0.211f,1.445f,-0.040f,0.237f,1.548f,0.035f,dark,light);
        box(v,e,-0.095f,1.368f,-0.199f,0.095f,1.392f,-0.191f,0xFF6A351F,light);
        box(v,e,-0.062f,1.370f,-0.203f,0.062f,1.382f,-0.196f,white,light);
        box(v,e,-0.158f,1.438f,-0.194f,-0.118f,1.448f,-0.188f,dark,light);
        box(v,e,0.118f,1.438f,-0.194f,0.158f,1.448f,-0.188f,dark,light);
        // Shirt collar and hem seams.
        box(v,e,-0.070f,1.245f,-0.153f,0.000f,1.278f,-0.147f,shade(red,1.18f),light);
        box(v,e,0.000f,1.245f,-0.153f,0.070f,1.278f,-0.147f,shade(red,1.18f),light);
        box(v,e,-0.205f,0.814f,-0.146f,0.205f,0.835f,-0.139f,shade(red,0.75f),light);
        // Shorts pockets and shoe soles.
        box(v,e,-0.196f,0.704f,-0.147f,-0.092f,0.720f,-0.140f,shade(blue,1.25f),light);
        box(v,e,0.092f,0.704f,-0.147f,0.196f,0.720f,-0.140f,shade(blue,1.25f),light);
        box(v,e,-0.199f,0.015f,-0.166f,-0.021f,0.038f,-0.159f,white,light);
        box(v,e,0.021f,0.015f,-0.166f,0.199f,0.038f,-0.159f,white,light);
    }

    private static void anime(VertexConsumer v, MatrixStack.Entry e, int palette, int light) {
        int skin=0xFFF0C5B5;
        int hair = switch (palette) { case 2->0xFF18191D; case 3->0xFF6A347F; case 4->0xFFB66175; default->0xFF2C2548; };
        int accent = palette==3?0xFF72D9FF:0xFFFF7ACD;
        int top=palette==4?0xFF8C3D57:0xFF252939;
        // Lashes, pupils, highlights and subtle blush.
        box(v,e,-0.132f,1.744f,-0.199f,-0.012f,1.756f,-0.192f,hair,light);
        box(v,e,0.012f,1.744f,-0.199f,0.132f,1.756f,-0.192f,hair,light);
        box(v,e,-0.071f,1.704f,-0.203f,-0.043f,1.738f,-0.196f,0xFF20202D,light);
        box(v,e,0.043f,1.704f,-0.203f,0.071f,1.738f,-0.196f,0xFF20202D,light);
        box(v,e,-0.062f,1.724f,-0.206f,-0.052f,1.736f,-0.200f,0xFFFFFFFF,light);
        box(v,e,0.052f,1.724f,-0.206f,0.062f,1.736f,-0.200f,0xFFFFFFFF,light);
        box(v,e,-0.155f,1.653f,-0.197f,-0.093f,1.670f,-0.190f,0xFFFFA6B7,light);
        box(v,e,0.093f,1.653f,-0.197f,0.155f,1.670f,-0.190f,0xFFFFA6B7,light);
        // Hair clips and layered fringe.
        box(v,e,-0.178f,1.795f,-0.195f,-0.105f,1.820f,-0.188f,accent,light);
        box(v,e,0.105f,1.795f,-0.195f,0.178f,1.820f,-0.188f,accent,light);
        box(v,e,-0.045f,1.754f,-0.199f,-0.012f,1.874f,-0.191f,shade(hair,1.18f),light);
        // Jacket piping, buttons and bow/tie detail.
        box(v,e,-0.190f,1.078f,-0.166f,-0.178f,1.390f,-0.158f,accent,light);
        box(v,e,0.178f,1.078f,-0.166f,0.190f,1.390f,-0.158f,accent,light);
        box(v,e,-0.055f,1.318f,-0.174f,0.000f,1.370f,-0.166f,accent,light);
        box(v,e,0.000f,1.318f,-0.174f,0.055f,1.370f,-0.166f,accent,light);
        for(int i=0;i<3;i++){float y=1.08f+i*0.075f; box(v,e,-0.008f,y,-0.174f,0.008f,y+0.016f,-0.167f,0xFFFFFFFF,light);}
        // Skirt pleats and shoe straps.
        for(float x:new float[]{-0.20f,-0.10f,0.0f,0.10f,0.20f})
            box(v,e,x-0.008f,0.690f,-0.186f,x+0.008f,0.895f,-0.179f,shade(top,1.25f),light);
        box(v,e,-0.165f,0.076f,-0.158f,-0.035f,0.092f,-0.151f,accent,light);
        box(v,e,0.035f,0.076f,-0.158f,0.165f,0.092f,-0.151f,accent,light);
    }

    private static void wooden(VertexConsumer v, MatrixStack.Entry e, int light) {
        int dark=0xFF653B21, lightWood=0xFFB97843, black=0xFF15110F, cream=0xFFFFE5B7;
        // Grain, knots and bark cracks on the front.
        box(v,e,-0.185f,0.785f,-0.246f,0.112f,0.805f,-0.238f,dark,light);
        box(v,e,-0.110f,0.966f,-0.246f,0.188f,0.984f,-0.238f,lightWood,light);
        box(v,e,-0.172f,1.164f,-0.246f,0.060f,1.181f,-0.238f,dark,light);
        box(v,e,0.092f,1.022f,-0.247f,0.132f,1.070f,-0.239f,0xFF4B2C19,light);
        box(v,e,-0.135f,0.838f,-0.247f,-0.094f,0.886f,-0.239f,0xFF4B2C19,light);
        // Eye highlights and toothy mouth.
        box(v,e,-0.096f,1.625f,-0.207f,-0.064f,1.648f,-0.200f,cream,light);
        box(v,e,0.064f,1.625f,-0.207f,0.096f,1.648f,-0.200f,cream,light);
        box(v,e,-0.085f,1.493f,-0.209f,0.085f,1.530f,-0.201f,black,light);
        for(int i=0;i<4;i++){float x=-0.072f+i*0.048f; box(v,e,x,1.500f,-0.213f,x+0.028f,1.520f,-0.205f,cream,light);}
        // Club wrapping bands.
        box(v,e,0.475f,0.495f,-0.010f,0.525f,0.535f,0.010f,black,light);
        box(v,e,0.500f,0.405f,0.010f,0.552f,0.445f,0.030f,black,light);
    }

    private static Identifier getWhiteTexture() {
        if (whiteTexture != null) return whiteTexture;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 2, 2, false);
        for (int y=0;y<2;y++) for(int x=0;x<2;x++) image.setColor(x,y,0xFFFFFFFF);
        whiteTexture = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(
                "cinco_character_detail_white", new NativeImageBackedTexture(image));
        return whiteTexture;
    }

    private static int shade(int color,float factor){
        int a=(color>>>24)&255,r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;
        r=Math.max(0,Math.min(255,Math.round(r*factor)));
        g=Math.max(0,Math.min(255,Math.round(g*factor)));
        b=Math.max(0,Math.min(255,Math.round(b*factor)));
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    private static void box(VertexConsumer v, MatrixStack.Entry e,float x0,float y0,float z0,
                            float x1,float y1,float z1,int color,int light){
        float[] a={x0,y0,z0},b={x1,y0,z0},c={x1,y1,z0},d={x0,y1,z0};
        float[] e0={x0,y0,z1},f={x1,y0,z1},g={x1,y1,z1},h={x0,y1,z1};
        quad(v,e,a,b,c,d,0,0,-1,color,light); quad(v,e,f,e0,h,g,0,0,1,color,light);
        quad(v,e,e0,a,d,h,-1,0,0,color,light); quad(v,e,b,f,g,c,1,0,0,color,light);
        quad(v,e,d,c,g,h,0,1,0,color,light); quad(v,e,e0,f,b,a,0,-1,0,color,light);
    }

    private static void quad(VertexConsumer v, MatrixStack.Entry e,float[] a,float[] b,float[] c,float[] d,
                             float nx,float ny,float nz,int color,int light){
        vertex(v,e,a,nx,ny,nz,color,light); vertex(v,e,b,nx,ny,nz,color,light);
        vertex(v,e,c,nx,ny,nz,color,light); vertex(v,e,d,nx,ny,nz,color,light);
    }

    private static void vertex(VertexConsumer v, MatrixStack.Entry e,float[] p,float nx,float ny,float nz,int color,int light){
        v.vertex(e.getPositionMatrix(),p[0],p[1],p[2]).color(color).texture(0.5F,0.5F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e,nx,ny,nz);
    }
}
