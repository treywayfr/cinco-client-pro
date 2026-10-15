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
import net.minecraft.util.math.Vec3d;

/**
 * Original procedural low-poly character renderer. GTA Skin Changer now replaces the entire
 * Minecraft player mesh and the main slider selects one of four character looks:
 * 1 Franklin-inspired streetwear, 2 yellow spiky cartoon kid, 3 anime-styled heroine,
 * 4 wooden sahur meme character. No external game/model assets are bundled.
 */
public final class GtaCharacterRenderer {
    private static Identifier whiteTexture;

    private GtaCharacterRenderer() {}

    public static void render(AbstractClientPlayerEntity player, float bodyYaw, float tickDelta,
                              MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        ClientModule module = CincoClient.findModule("GTA Skin Changer");
        if (module == null || !module.enabled) return;

        int character = Math.max(1, Math.min(4, (int) Math.round(module.setting)));
        int palette = Math.max(1, Math.min(4, (int) Math.round(module.setting2)));

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
        if (player.isSneaking()) matrices.translate(0.0, -0.08, 0.08);

        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntitySolid(getWhiteTexture()));
        MatrixStack.Entry entry = matrices.peek();
        double horizontalSpeed = player.getVelocity().horizontalLength();
        float motion = (float) Math.min(1.0, horizontalSpeed * 4.5);
        float phase = (player.age + tickDelta) * 0.62F;
        float legSwing = (float) Math.sin(phase) * 0.29F * motion;
        float armSwing = -legSwing * 0.92F;

        switch (character) {
            case 2 -> renderSpikyKid(vertices, entry, legSwing, armSwing, light);
            case 3 -> renderAnimeHeroine(vertices, entry, legSwing, armSwing, palette, light);
            case 4 -> renderWoodenSahur(vertices, entry, legSwing, armSwing, light);
            default -> renderFranklin(vertices, entry, legSwing, armSwing, palette, light);
        }
        matrices.pop();
    }

    private static void renderFranklin(VertexConsumer v, MatrixStack.Entry e, float legSwing, float armSwing,
                                       int palette, int light) {
        int green = switch (palette) {
            case 2 -> 0xFF202326;
            case 3 -> 0xFF214B72;
            case 4 -> 0xFF742E2A;
            default -> 0xFF2C784A;
        };
        int greenDark = shade(green, 0.72f);
        int tee = 0xFFF4F1E9;
        int denim = palette == 3 ? 0xFF202935 : 0xFF27364A;
        int denimDark = shade(denim, 0.72f);
        int shoe = 0xFFF1F1ED;
        int sole = 0xFF181B1E;
        int skin = 0xFF835136;
        int skinShade = 0xFF673E2D;
        int hair = 0xFF151211;
        int beard = 0xFF231816;
        int gold = 0xFFD6AD52;

        // White sneakers with dark soles.
        box(v,e,-0.245f,0.01f,-0.19f+legSwing,-0.015f,0.075f,0.18f+legSwing,sole,light);
        box(v,e,0.015f,0.01f,-0.19f-legSwing,0.245f,0.075f,0.18f-legSwing,sole,light);
        box(v,e,-0.235f,0.075f,-0.185f+legSwing,-0.025f,0.145f,0.15f+legSwing,shoe,light);
        box(v,e,0.025f,0.075f,-0.185f-legSwing,0.235f,0.145f,0.15f-legSwing,shoe,light);

        // Jeans: tapered calves and slightly wider thighs.
        cylinder(v,e,new Vec3d(-0.13,0.77,-legSwing*0.12),new Vec3d(-0.13,0.13,legSwing),0.112,8,denimDark,light);
        cylinder(v,e,new Vec3d(0.13,0.77,legSwing*0.12),new Vec3d(0.13,0.13,-legSwing),0.112,8,denimDark,light);
        cylinder(v,e,new Vec3d(-0.13,1.00,0),new Vec3d(-0.13,0.70,-legSwing*0.08),0.135,8,denim,light);
        cylinder(v,e,new Vec3d(0.13,1.00,0),new Vec3d(0.13,0.70,legSwing*0.08),0.135,8,denim,light);

        // Human torso with a green open overshirt around a white tee.
        frustum(v,e,0,0.88f,1.46f,0.225f,0.315f,0.145f,0.17f,green,light);
        box(v,e,-0.115f,0.98f,-0.176f,0.115f,1.39f,-0.162f,tee,light);
        // Open-shirt lapels and lower split.
        box(v,e,-0.195f,1.12f,-0.183f,-0.112f,1.43f,-0.169f,greenDark,light);
        box(v,e,0.112f,1.12f,-0.183f,0.195f,1.43f,-0.169f,greenDark,light);
        box(v,e,-0.22f,0.90f,-0.174f,-0.02f,1.08f,-0.162f,green,light);
        box(v,e,0.02f,0.90f,-0.174f,0.22f,1.08f,-0.162f,green,light);
        // Belt and buckle.
        box(v,e,-0.225f,0.86f,-0.166f,0.225f,0.91f,0.166f,0xFF1A1A1B,light);
        box(v,e,-0.035f,0.855f,-0.174f,0.035f,0.915f,-0.165f,gold,light);

        // Shoulders, sleeves, forearms, hands.
        cylinder(v,e,new Vec3d(-0.33,1.39,0),new Vec3d(-0.37,1.17,armSwing*0.20),0.112,8,green,light);
        cylinder(v,e,new Vec3d(0.33,1.39,0),new Vec3d(0.37,1.17,-armSwing*0.20),0.112,8,green,light);
        cylinder(v,e,new Vec3d(-0.37,1.17,armSwing*0.20),new Vec3d(-0.405,0.78,armSwing),0.083,8,skin,light);
        cylinder(v,e,new Vec3d(0.37,1.17,-armSwing*0.20),new Vec3d(0.405,0.78,-armSwing),0.083,8,skin,light);
        ellipsoid(v,e,-0.405f,0.745f,armSwing,0.095f,0.105f,0.085f,-1.45f,1.45f,5,8,skin,light);
        ellipsoid(v,e,0.405f,0.745f,-armSwing,0.095f,0.105f,0.085f,-1.45f,1.45f,5,8,skin,light);
        // Watch on left wrist.
        cylinder(v,e,new Vec3d(-0.405,0.84,armSwing*0.90),new Vec3d(-0.405,0.79,armSwing*0.96),0.091,8,0xFF101214,light);
        box(v,e,-0.452f,0.802f,armSwing-0.092f,-0.358f,0.852f,armSwing-0.082f,gold,light);

        // Neck and more face-like head proportions.
        cylinder(v,e,new Vec3d(0,1.44,0),new Vec3d(0,1.55,0),0.105,9,skinShade,light);
        ellipsoid(v,e,0,1.73f,0,0.205f,0.255f,0.19f,-1.48f,1.48f,9,12,skin,light);
        // Ears.
        ellipsoid(v,e,-0.205f,1.73f,0,0.035f,0.070f,0.038f,-1.4f,1.4f,4,7,skinShade,light);
        ellipsoid(v,e,0.205f,1.73f,0,0.035f,0.070f,0.038f,-1.4f,1.4f,4,7,skinShade,light);
        // Short fade / textured top.
        ellipsoid(v,e,0,1.79f,0.012f,0.211f,0.218f,0.196f,0.30f,1.50f,5,12,hair,light);
        box(v,e,-0.19f,1.70f,0.13f,-0.165f,1.84f,-0.10f,hair,light);
        box(v,e,0.165f,1.70f,0.13f,0.19f,1.84f,-0.10f,hair,light);

        // Eyes, brows, nose, beard and mouth on the forward (-Z) side.
        box(v,e,-0.115f,1.748f,-0.194f,-0.025f,1.772f,-0.188f,0xFFF3EEE3,light);
        box(v,e,0.025f,1.748f,-0.194f,0.115f,1.772f,-0.188f,0xFFF3EEE3,light);
        box(v,e,-0.075f,1.751f,-0.198f,-0.047f,1.770f,-0.192f,0xFF241A16,light);
        box(v,e,0.047f,1.751f,-0.198f,0.075f,1.770f,-0.192f,0xFF241A16,light);
        box(v,e,-0.12f,1.788f,-0.196f,-0.02f,1.806f,-0.190f,hair,light);
        box(v,e,0.02f,1.788f,-0.196f,0.12f,1.806f,-0.190f,hair,light);
        box(v,e,-0.025f,1.665f,-0.215f,0.025f,1.735f,-0.190f,skinShade,light);
        box(v,e,-0.105f,1.575f,-0.195f,0.105f,1.635f,-0.184f,beard,light);
        box(v,e,-0.070f,1.625f,-0.199f,0.070f,1.646f,-0.192f,0xFF4D2E25,light);
        // Thin chain over the tee.
        box(v,e,-0.055f,1.315f,-0.181f,-0.045f,1.405f,-0.175f,gold,light);
        box(v,e,0.045f,1.315f,-0.181f,0.055f,1.405f,-0.175f,gold,light);
        box(v,e,-0.045f,1.305f,-0.181f,0.045f,1.316f,-0.175f,gold,light);
    }

    private static void renderSpikyKid(VertexConsumer v, MatrixStack.Entry e, float legSwing, float armSwing, int light) {
        int yellow = 0xFFFFD735;
        int yellowShade = 0xFFE4B822;
        int shirt = 0xFFF05B3D;
        int shorts = 0xFF3A65C7;
        int shoe = 0xFF4A6FD4;
        int white = 0xFFF9F9F5;
        int pupil = 0xFF202020;

        box(v,e,-0.20f,0.02f,-0.16f+legSwing,-0.02f,0.11f,0.14f+legSwing,shoe,light);
        box(v,e,0.02f,0.02f,-0.16f-legSwing,0.20f,0.11f,0.14f-legSwing,shoe,light);
        cylinder(v,e,new Vec3d(-0.11,0.66,0),new Vec3d(-0.11,0.10,legSwing),0.085,7,yellow,light);
        cylinder(v,e,new Vec3d(0.11,0.66,0),new Vec3d(0.11,0.10,-legSwing),0.085,7,yellow,light);
        box(v,e,-0.22f,0.60f,-0.14f,0.22f,0.82f,0.14f,shorts,light);
        frustum(v,e,0,0.80f,1.28f,0.20f,0.25f,0.13f,0.145f,shirt,light);
        cylinder(v,e,new Vec3d(-0.27,1.19,0),new Vec3d(-0.31,0.78,armSwing),0.073,7,yellow,light);
        cylinder(v,e,new Vec3d(0.27,1.19,0),new Vec3d(0.31,0.78,-armSwing),0.073,7,yellow,light);
        ellipsoid(v,e,0,1.49f,0,0.22f,0.30f,0.18f,-1.48f,1.48f,8,10,yellow,light);
        // Cartoon muzzle / protruding nose.
        box(v,e,-0.025f,1.43f,-0.222f,0.06f,1.52f,-0.176f,yellowShade,light);
        // Big white eyes and pupils.
        box(v,e,-0.135f,1.515f,-0.188f,-0.010f,1.615f,-0.180f,white,light);
        box(v,e,0.010f,1.515f,-0.188f,0.135f,1.615f,-0.180f,white,light);
        box(v,e,-0.070f,1.545f,-0.194f,-0.038f,1.590f,-0.187f,pupil,light);
        box(v,e,0.038f,1.545f,-0.194f,0.070f,1.590f,-0.187f,pupil,light);
        box(v,e,-0.085f,1.375f,-0.192f,0.085f,1.395f,-0.184f,0xFF6B3D20,light);
        // Signature-looking spiky yellow hair silhouette.
        for (int i=0;i<6;i++) {
            double x=-0.16+i*0.064;
            double z=(i%2==0?-0.015:0.02);
            cylinder(v,e,new Vec3d(x,1.72,z),new Vec3d(x+(i-2.5)*0.012,1.91,z),0.052,5,yellow,light);
        }
    }

    private static void renderAnimeHeroine(VertexConsumer v, MatrixStack.Entry e, float legSwing, float armSwing,
                                           int palette, int light) {
        int skin = 0xFFF0C5B5;
        int hair = switch (palette) {
            case 2 -> 0xFF18191D;
            case 3 -> 0xFF6A347F;
            case 4 -> 0xFFB66175;
            default -> 0xFF2C2548;
        };
        int hairHi = shade(hair,1.35f);
        int top = palette == 4 ? 0xFF8C3D57 : 0xFF252939;
        int accent = palette == 3 ? 0xFF72D9FF : 0xFFFF7ACD;
        int skirt = 0xFF202636;
        int socks = 0xFFF2F0F4;
        int shoes = 0xFF171920;

        box(v,e,-0.18f,0.02f,-0.15f+legSwing,-0.02f,0.10f,0.14f+legSwing,shoes,light);
        box(v,e,0.02f,0.02f,-0.15f-legSwing,0.18f,0.10f,0.14f-legSwing,shoes,light);
        cylinder(v,e,new Vec3d(-0.10,0.72,0),new Vec3d(-0.10,0.09,legSwing),0.078,8,socks,light);
        cylinder(v,e,new Vec3d(0.10,0.72,0),new Vec3d(0.10,0.09,-legSwing),0.078,8,socks,light);
        // Short skirt with flared silhouette.
        frustum(v,e,0,0.68f,0.94f,0.27f,0.19f,0.18f,0.13f,skirt,light);
        box(v,e,-0.27f,0.68f,-0.182f,0.27f,0.72f,0.182f,accent,light);
        // Fitted jacket/top.
        frustum(v,e,0,0.92f,1.42f,0.18f,0.265f,0.125f,0.15f,top,light);
        box(v,e,-0.09f,1.08f,-0.158f,0.09f,1.36f,-0.149f,0xFFF5F2F6,light);
        box(v,e,-0.04f,1.10f,-0.164f,0.04f,1.31f,-0.155f,accent,light);
        cylinder(v,e,new Vec3d(-0.29,1.34,0),new Vec3d(-0.34,0.83,armSwing),0.073,8,skin,light);
        cylinder(v,e,new Vec3d(0.29,1.34,0),new Vec3d(0.34,0.83,-armSwing),0.073,8,skin,light);
        cylinder(v,e,new Vec3d(-0.29,1.35,0),new Vec3d(-0.31,1.14,armSwing*0.2),0.095,8,top,light);
        cylinder(v,e,new Vec3d(0.29,1.35,0),new Vec3d(0.31,1.14,-armSwing*0.2),0.095,8,top,light);
        cylinder(v,e,new Vec3d(0,1.42,0),new Vec3d(0,1.50,0),0.09,8,skin,light);
        ellipsoid(v,e,0,1.70f,0,0.205f,0.245f,0.185f,-1.48f,1.48f,8,12,skin,light);
        // Long hair mass behind head and shoulders.
        ellipsoid(v,e,0,1.73f,0.09f,0.235f,0.27f,0.20f,-1.48f,1.48f,8,12,hair,light);
        box(v,e,-0.215f,1.05f,0.08f,-0.105f,1.70f,0.18f,hair,light);
        box(v,e,0.105f,1.05f,0.08f,0.215f,1.70f,0.18f,hair,light);
        box(v,e,-0.09f,1.32f,0.11f,0.09f,1.72f,0.19f,hairHi,light);
        // Bangs.
        box(v,e,-0.19f,1.73f,-0.188f,-0.03f,1.88f,-0.177f,hair,light);
        box(v,e,0.03f,1.73f,-0.188f,0.19f,1.88f,-0.177f,hair,light);
        box(v,e,-0.04f,1.72f,-0.192f,0.04f,1.82f,-0.181f,hairHi,light);
        // Large stylized eyes.
        box(v,e,-0.125f,1.695f,-0.190f,-0.020f,1.745f,-0.183f,0xFFFFFFFF,light);
        box(v,e,0.020f,1.695f,-0.190f,0.125f,1.745f,-0.183f,0xFFFFFFFF,light);
        box(v,e,-0.076f,1.702f,-0.196f,-0.040f,1.739f,-0.189f,accent,light);
        box(v,e,0.040f,1.702f,-0.196f,0.076f,1.739f,-0.189f,accent,light);
        box(v,e,-0.060f,1.625f,-0.193f,0.060f,1.638f,-0.187f,0xFFB55D73,light);
    }

    private static void renderWoodenSahur(VertexConsumer v, MatrixStack.Entry e, float legSwing, float armSwing, int light) {
        int wood = 0xFF9A6236;
        int woodLight = 0xFFB97843;
        int woodDark = 0xFF653B21;
        int black = 0xFF15110F;

        cylinder(v,e,new Vec3d(-0.12,0.64,0),new Vec3d(-0.12,0.08,legSwing),0.085,7,woodDark,light);
        cylinder(v,e,new Vec3d(0.12,0.64,0),new Vec3d(0.12,0.08,-legSwing),0.085,7,woodDark,light);
        box(v,e,-0.20f,0.01f,-0.15f+legSwing,-0.02f,0.09f,0.16f+legSwing,black,light);
        box(v,e,0.02f,0.01f,-0.15f-legSwing,0.20f,0.09f,0.16f-legSwing,black,light);

        // Tall rounded wooden log body and head, with lighter cut-ring bands.
        cylinder(v,e,new Vec3d(0,0.60,0),new Vec3d(0,1.42,0),0.235,12,wood,light);
        cylinder(v,e,new Vec3d(0,0.64,0),new Vec3d(0,0.69,0),0.242,12,woodLight,light);
        cylinder(v,e,new Vec3d(0,1.35,0),new Vec3d(0,1.40,0),0.242,12,woodLight,light);
        ellipsoid(v,e,0,1.59f,0,0.22f,0.24f,0.20f,-1.48f,1.48f,7,11,woodLight,light);
        cylinder(v,e,new Vec3d(-0.23,1.28,0),new Vec3d(-0.38,0.79,armSwing),0.060,7,woodDark,light);
        cylinder(v,e,new Vec3d(0.23,1.28,0),new Vec3d(0.38,0.79,-armSwing),0.060,7,woodDark,light);
        // Simple face.
        box(v,e,-0.115f,1.61f,-0.199f,-0.045f,1.665f,-0.192f,black,light);
        box(v,e,0.045f,1.61f,-0.199f,0.115f,1.665f,-0.192f,black,light);
        box(v,e,-0.085f,1.50f,-0.201f,0.085f,1.525f,-0.193f,black,light);
        // Wood-grain stripes.
        box(v,e,-0.19f,0.90f,-0.238f,0.19f,0.925f,-0.228f,woodDark,light);
        box(v,e,-0.15f,1.08f,-0.239f,0.15f,1.102f,-0.229f,woodLight,light);
        box(v,e,-0.10f,1.22f,-0.239f,0.10f,1.238f,-0.229f,woodDark,light);
        // Wooden club in the right hand.
        cylinder(v,e,new Vec3d(0.39,0.82,-armSwing),new Vec3d(0.54,0.30,-armSwing+0.08),0.055,8,woodLight,light);
        cylinder(v,e,new Vec3d(0.54,0.30,-armSwing+0.08),new Vec3d(0.59,0.13,-armSwing+0.10),0.078,8,woodDark,light);
    }

    private static Identifier getWhiteTexture() {
        if (whiteTexture != null) return whiteTexture;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 2, 2, false);
        for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) image.setColor(x, y, 0xFFFFFFFF);
        whiteTexture = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(
                "cinco_character_lowpoly_white", new NativeImageBackedTexture(image));
        return whiteTexture;
    }

    private static int shade(int color, float factor) {
        int a=(color>>>24)&255, r=(color>>>16)&255, g=(color>>>8)&255, b=color&255;
        r=Math.min(255,Math.max(0,Math.round(r*factor)));
        g=Math.min(255,Math.max(0,Math.round(g*factor)));
        b=Math.min(255,Math.max(0,Math.round(b*factor)));
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    private static void frustum(VertexConsumer v, MatrixStack.Entry e, float cx, float y0, float y1,
                                float bottomHalfWidth, float topHalfWidth,
                                float bottomHalfDepth, float topHalfDepth, int color, int light) {
        float[][] b={{cx-bottomHalfWidth,y0,-bottomHalfDepth},{cx+bottomHalfWidth,y0,-bottomHalfDepth},
                {cx+bottomHalfWidth,y0,bottomHalfDepth},{cx-bottomHalfWidth,y0,bottomHalfDepth}};
        float[][] t={{cx-topHalfWidth,y1,-topHalfDepth},{cx+topHalfWidth,y1,-topHalfDepth},
                {cx+topHalfWidth,y1,topHalfDepth},{cx-topHalfWidth,y1,topHalfDepth}};
        quad(v,e,b[0],b[1],t[1],t[0],0,0,-1,color,light);
        quad(v,e,b[2],b[3],t[3],t[2],0,0,1,color,light);
        quad(v,e,b[3],b[0],t[0],t[3],-1,0,0,color,light);
        quad(v,e,b[1],b[2],t[2],t[1],1,0,0,color,light);
        quad(v,e,t[0],t[1],t[2],t[3],0,1,0,color,light);
        quad(v,e,b[3],b[2],b[1],b[0],0,-1,0,color,light);
    }

    private static void box(VertexConsumer v, MatrixStack.Entry e, float x0,float y0,float z0,
                            float x1,float y1,float z1,int color,int light) {
        float[] a={x0,y0,z0}, b={x1,y0,z0}, c={x1,y1,z0}, d={x0,y1,z0};
        float[] e0={x0,y0,z1}, f={x1,y0,z1}, g={x1,y1,z1}, h={x0,y1,z1};
        quad(v,e,a,b,c,d,0,0,-1,color,light); quad(v,e,f,e0,h,g,0,0,1,color,light);
        quad(v,e,e0,a,d,h,-1,0,0,color,light); quad(v,e,b,f,g,c,1,0,0,color,light);
        quad(v,e,d,c,g,h,0,1,0,color,light); quad(v,e,e0,f,b,a,0,-1,0,color,light);
    }

    private static void cylinder(VertexConsumer v, MatrixStack.Entry e, Vec3d start, Vec3d end,
                                 double radius, int sides, int color, int light) {
        Vec3d axis=end.subtract(start); double len=axis.length(); if(len<0.0001)return;
        Vec3d n=axis.multiply(1.0/len);
        Vec3d ref=Math.abs(n.y)<0.9?new Vec3d(0,1,0):new Vec3d(1,0,0);
        Vec3d u=n.crossProduct(ref).normalize(); Vec3d w=n.crossProduct(u).normalize();
        for(int i=0;i<sides;i++){
            double a0=Math.PI*2.0*i/sides,a1=Math.PI*2.0*(i+1)/sides;
            Vec3d r0=u.multiply(Math.cos(a0)*radius).add(w.multiply(Math.sin(a0)*radius));
            Vec3d r1=u.multiply(Math.cos(a1)*radius).add(w.multiply(Math.sin(a1)*radius));
            Vec3d p0=start.add(r0),p1=start.add(r1),p2=end.add(r1),p3=end.add(r0);
            Vec3d normal=r0.add(r1).normalize();
            quad(v,e,toF(p0),toF(p1),toF(p2),toF(p3),(float)normal.x,(float)normal.y,(float)normal.z,color,light);
        }
    }

    private static void ellipsoid(VertexConsumer v, MatrixStack.Entry e, float cx,float cy,float cz,
                                  float rx,float ry,float rz,float latMin,float latMax,
                                  int latSegments,int lonSegments,int color,int light) {
        for(int iy=0;iy<latSegments;iy++){
            double p0=latMin+(latMax-latMin)*iy/latSegments;
            double p1=latMin+(latMax-latMin)*(iy+1)/latSegments;
            for(int ix=0;ix<lonSegments;ix++){
                double a0=Math.PI*2.0*ix/lonSegments,a1=Math.PI*2.0*(ix+1)/lonSegments;
                float[] q0=spherePoint(cx,cy,cz,rx,ry,rz,p0,a0);
                float[] q1=spherePoint(cx,cy,cz,rx,ry,rz,p0,a1);
                float[] q2=spherePoint(cx,cy,cz,rx,ry,rz,p1,a1);
                float[] q3=spherePoint(cx,cy,cz,rx,ry,rz,p1,a0);
                double pm=(p0+p1)*0.5,am=(a0+a1)*0.5;
                float nx=(float)(Math.cos(pm)*Math.cos(am));
                float ny=(float)Math.sin(pm);
                float nz=(float)(Math.cos(pm)*Math.sin(am));
                quad(v,e,q0,q1,q2,q3,nx,ny,nz,color,light);
            }
        }
    }

    private static float[] spherePoint(float cx,float cy,float cz,float rx,float ry,float rz,double phi,double angle){
        double cp=Math.cos(phi);
        return new float[]{cx+(float)(rx*cp*Math.cos(angle)),cy+(float)(ry*Math.sin(phi)),cz+(float)(rz*cp*Math.sin(angle))};
    }

    private static float[] toF(Vec3d p){return new float[]{(float)p.x,(float)p.y,(float)p.z};}

    private static void quad(VertexConsumer v, MatrixStack.Entry e, float[] a,float[] b,float[] c,float[] d,
                             float nx,float ny,float nz,int color,int light){
        vertex(v,e,a,nx,ny,nz,color,light);vertex(v,e,b,nx,ny,nz,color,light);
        vertex(v,e,c,nx,ny,nz,color,light);vertex(v,e,d,nx,ny,nz,color,light);
    }

    private static void vertex(VertexConsumer v, MatrixStack.Entry e, float[] p,
                               float nx,float ny,float nz,int color,int light){
        v.vertex(e.getPositionMatrix(),p[0],p[1],p[2]).color(color).texture(0.5F,0.5F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e,nx,ny,nz);
    }
}
