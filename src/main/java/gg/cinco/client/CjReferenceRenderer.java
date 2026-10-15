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
 * Smoother low-poly CJ preset shaped from the user's front/side/back Minecraft references.
 * It uses original procedural geometry and colors; no GTA model/texture assets are bundled.
 */
public final class CjReferenceRenderer {
    private static Identifier texture;
    private static final int SIDES = 8;

    private CjReferenceRenderer() {}

    public static void render(AbstractClientPlayerEntity player, float bodyYaw, float tickDelta,
                              MatrixStack matrices, VertexConsumerProvider consumers, int light,
                              int outfit, boolean detailed) {
        outfit = Math.max(1, Math.min(6, outfit));

        int skin = 0xFF6A402E;
        int skinLight = 0xFF83523B;
        int skinDark = 0xFF3F261D;
        int hair = 0xFF0E0E0F;
        int eye = 0xFF171112;
        int white = 0xFFF3F2EE;
        int sole = 0xFFE6E6E3;
        int belt = 0xFF291B16;
        int buckle = 0xFFC9A34E;
        int denim = switch (outfit) {
            case 2 -> 0xFF344635;
            case 3 -> 0xFF202833;
            case 6 -> 0xFF292A30;
            default -> 0xFF263A55;
        };
        int denimDark = shade(denim, 0.70f);
        int top = switch (outfit) {
            case 2 -> 0xFF2F7044;
            case 3 -> 0xFF17191C;
            case 4 -> 0xFF7B2D32;
            case 5 -> 0xFF37714D;
            case 6 -> 0xFF5B5C62;
            default -> white;
        };

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
        if (player.isSneaking()) matrices.translate(0.0, -0.07, 0.08);

        VertexConsumer v = consumers.getBuffer(RenderLayer.getEntitySolid(texture()));
        MatrixStack.Entry e = matrices.peek();

        double speed = player.getVelocity().horizontalLength();
        float motion = (float)Math.min(1.0, speed * 4.8);
        float phase = (player.age + tickDelta) * 0.58f;
        float legSwing = (float)Math.sin(phase) * 0.105f * motion;
        float armSwing = -legSwing * 1.35f;

        // --- Shoes / feet: mostly black with the light sole visible in the reference. ---
        shoe(v, e, -0.135f, 0.02f, legSwing, light, sole);
        shoe(v, e,  0.135f, 0.02f, -legSwing, light, sole);

        // --- Long slim legs, tapered around knees/ankles instead of Minecraft cubes. ---
        limb(v,e,-0.135f,0.16f,legSwing, -0.135f,0.56f,legSwing*0.65f, 0.115f,0.105f,denimDark,light);
        limb(v,e,-0.135f,0.55f,legSwing*0.65f, -0.135f,0.96f,0.0f, 0.125f,0.145f,denim,light);
        limb(v,e, 0.135f,0.16f,-legSwing, 0.135f,0.56f,-legSwing*0.65f, 0.115f,0.105f,denimDark,light);
        limb(v,e, 0.135f,0.55f,-legSwing*0.65f, 0.135f,0.96f,0.0f, 0.125f,0.145f,denim,light);

        // Hip/waist shape and belt.
        frustumY(v,e,0,0.83f,0,0.235f,0.145f, 0,1.00f,0,0.255f,0.150f,denim,light);
        frustumY(v,e,0,0.955f,0,0.255f,0.154f, 0,1.005f,0,0.245f,0.151f,belt,light);
        box(v,e,-0.040f,0.955f,-0.164f,0.040f,1.010f,-0.151f,buckle,light);

        // --- Torso. Classic outfit is a white tank with exposed shoulders like the screenshots. ---
        if (outfit == 1) {
            // Skin shoulder/chest shell behind the tank so the arm openings read naturally.
            frustumY(v,e,0,1.18f,0,0.245f,0.125f, 0,1.46f,0,0.315f,0.148f,skin,light);
            // Tapered tank body: narrow waist, broader chest.
            frustumY(v,e,0,0.99f,-0.002f,0.215f,0.125f, 0,1.38f,-0.002f,0.245f,0.135f,white,light);
            // Tank straps and ribbing/highlights.
            box(v,e,-0.220f,1.31f,-0.150f,-0.145f,1.465f,-0.137f,white,light);
            box(v,e, 0.145f,1.31f,-0.150f, 0.220f,1.465f,-0.137f,white,light);
            box(v,e,-0.080f,1.30f,-0.153f,0.080f,1.455f,-0.142f,skin,light);
            box(v,e,-0.185f,1.05f,-0.139f,-0.170f,1.34f,-0.133f,0xFFD7D7D4,light);
            box(v,e, 0.170f,1.05f,-0.139f, 0.185f,1.34f,-0.133f,0xFFD7D7D4,light);
        } else {
            frustumY(v,e,0,0.99f,0,0.225f,0.130f, 0,1.44f,0,0.305f,0.150f,top,light);
            box(v,e,-0.280f,1.25f,-0.145f,-0.220f,1.43f,0.145f,shade(top,0.86f),light);
            box(v,e, 0.220f,1.25f,-0.145f, 0.280f,1.43f,0.145f,shade(top,0.86f),light);
        }

        // --- Arms: shoulder -> elbow -> wrist -> hand. Long, slim, and slightly angled. ---
        renderArm(v,e,-1,armSwing,skin,skinLight,light,detailed);
        renderArm(v,e, 1,-armSwing,skin,skinLight,light,detailed);

        // Neck.
        limb(v,e,0,1.43f,0, 0,1.59f,0, 0.083f,0.095f,skinDark,light);

        // --- Head: stacked octagonal frustums create a much rounder silhouette than cubes. ---
        frustumY(v,e,0,1.56f,-0.005f,0.125f,0.115f, 0,1.66f,-0.010f,0.160f,0.140f,skinDark,light); // jaw/chin
        frustumY(v,e,0,1.64f,-0.010f,0.165f,0.145f, 0,1.84f,-0.005f,0.190f,0.160f,skin,light);      // face
        frustumY(v,e,0,1.82f,-0.002f,0.188f,0.158f, 0,1.94f,0.008f,0.168f,0.145f,skinDark,light);     // cranium
        frustumY(v,e,0,1.885f,0.010f,0.174f,0.148f, 0,1.972f,0.016f,0.150f,0.132f,hair,light);         // close cut hair

        // Ears.
        box(v,e,-0.207f,1.71f,-0.055f,-0.180f,1.80f,0.060f,skinDark,light);
        box(v,e, 0.180f,1.71f,-0.055f, 0.207f,1.80f,0.060f,skinDark,light);

        // Face planes/details. Front is negative Z in this renderer.
        box(v,e,-0.125f,1.795f,-0.167f,-0.025f,1.812f,-0.158f,hair,light);
        box(v,e, 0.025f,1.795f,-0.167f, 0.125f,1.812f,-0.158f,hair,light);
        box(v,e,-0.112f,1.753f,-0.174f,-0.025f,1.771f,-0.164f,0xFFE7E2DB,light);
        box(v,e, 0.025f,1.753f,-0.174f, 0.112f,1.771f,-0.164f,0xFFE7E2DB,light);
        box(v,e,-0.070f,1.754f,-0.181f,-0.045f,1.773f,-0.171f,eye,light);
        box(v,e, 0.045f,1.754f,-0.181f, 0.070f,1.773f,-0.171f,eye,light);
        box(v,e,-0.030f,1.670f,-0.194f,0.032f,1.744f,-0.166f,skinDark,light); // nose
        box(v,e,-0.078f,1.628f,-0.181f,0.078f,1.646f,-0.170f,0xFF7C463E,light); // lips

        if (detailed) {
            // Short beard/goatee shadow and sharper hairline like the close front reference.
            box(v,e,-0.054f,1.582f,-0.174f,0.054f,1.625f,-0.165f,0xFF231817,light);
            box(v,e,-0.145f,1.865f,-0.156f,-0.055f,1.888f,-0.147f,hair,light);
            box(v,e,-0.055f,1.875f,-0.157f, 0.055f,1.895f,-0.148f,hair,light);
            box(v,e, 0.055f,1.865f,-0.156f, 0.145f,1.888f,-0.147f,hair,light);
            // Jean fly, pockets and subtle tank/denim seams.
            box(v,e,-0.010f,0.72f,-0.135f,0.010f,0.96f,-0.127f,shade(denim,0.56f),light);
            box(v,e,-0.225f,0.84f,-0.151f,-0.105f,0.858f,-0.143f,shade(denim,1.20f),light);
            box(v,e, 0.105f,0.84f,-0.151f, 0.225f,0.858f,-0.143f,shade(denim,1.20f),light);
        }

        matrices.pop();
    }

    private static void renderArm(VertexConsumer v, MatrixStack.Entry e, int side, float swing,
                                  int skin, int skinLight, int light, boolean detailed) {
        float sx = side * 0.325f;
        float ex = side * 0.370f;
        float wx = side * 0.395f;
        float hz = swing * 0.45f;
        float ez = swing;
        float wz = swing * 1.28f;

        limb(v,e,sx,1.39f,hz, ex,1.10f,ez, 0.090f,0.075f,skin,light);
        limb(v,e,ex,1.105f,ez, wx,0.78f,wz, 0.073f,0.060f,skin,light);
        limb(v,e,wx,0.79f,wz, side*0.402f,0.675f,wz*1.03f, 0.072f,0.082f,skinLight,light);

        if (detailed) {
            // Thumb/finger silhouette at the lowered hand.
            float tx0 = side > 0 ? 0.405f : -0.445f;
            float tx1 = side > 0 ? 0.445f : -0.405f;
            box(v,e,tx0,0.690f,wz-0.030f,tx1,0.755f,wz+0.015f,skinLight,light);
        }
    }

    private static void shoe(VertexConsumer v, MatrixStack.Entry e, float cx, float y, float zShift,
                             int light, int sole) {
        int black = 0xFF111216;
        box(v,e,cx-0.125f,y,-0.235f+zShift,cx+0.125f,y+0.085f,0.155f+zShift,black,light);
        box(v,e,cx-0.118f,y+0.082f,-0.220f+zShift,cx+0.118f,y+0.135f,0.135f+zShift,black,light);
        box(v,e,cx-0.128f,y-0.004f,-0.242f+zShift,cx+0.128f,y+0.020f,0.165f+zShift,sole,light);
        for (int i=0;i<3;i++) {
            float yy = y + 0.052f + i * 0.013f;
            box(v,e,cx-0.078f,yy,-0.239f+zShift,cx+0.078f,yy+0.005f,-0.231f+zShift,0xFFC9CBC9,light);
        }
    }

    /** Octagonal tapered tube between arbitrary points for smooth limbs. */
    private static void limb(VertexConsumer v, MatrixStack.Entry e,
                             float x0,float y0,float z0, float x1,float y1,float z1,
                             float r0,float r1,int color,int light) {
        float ax=x1-x0, ay=y1-y0, az=z1-z0;
        float len=(float)Math.sqrt(ax*ax+ay*ay+az*az);
        if (len < 0.0001f) return;
        ax/=len; ay/=len; az/=len;

        float rx,ry,rz;
        if (Math.abs(ay) > 0.86f) { rx=1; ry=0; rz=0; }
        else { rx=0; ry=1; rz=0; }

        // u = axis x ref
        float ux=ay*rz-az*ry, uy=az*rx-ax*rz, uz=ax*ry-ay*rx;
        float ul=(float)Math.sqrt(ux*ux+uy*uy+uz*uz);
        ux/=ul; uy/=ul; uz/=ul;
        // w = axis x u
        float wx=ay*uz-az*uy, wy=az*ux-ax*uz, wz=ax*uy-ay*ux;

        for (int i=0;i<SIDES;i++) {
            double a0=Math.PI*2.0*i/SIDES;
            double a1=Math.PI*2.0*(i+1)/SIDES;
            float c0=(float)Math.cos(a0), s0=(float)Math.sin(a0);
            float c1=(float)Math.cos(a1), s1=(float)Math.sin(a1);
            float[] p0={x0+(ux*c0+wx*s0)*r0, y0+(uy*c0+wy*s0)*r0, z0+(uz*c0+wz*s0)*r0};
            float[] p1={x0+(ux*c1+wx*s1)*r0, y0+(uy*c1+wy*s1)*r0, z0+(uz*c1+wz*s1)*r0};
            float[] q1={x1+(ux*c1+wx*s1)*r1, y1+(uy*c1+wy*s1)*r1, z1+(uz*c1+wz*s1)*r1};
            float[] q0={x1+(ux*c0+wx*s0)*r1, y1+(uy*c0+wy*s0)*r1, z1+(uz*c0+wz*s0)*r1};
            double am=(a0+a1)*0.5;
            float cm=(float)Math.cos(am), sm=(float)Math.sin(am);
            float nx=ux*cm+wx*sm, ny=uy*cm+wy*sm, nz=uz*cm+wz*sm;
            quad(v,e,p0,p1,q1,q0,nx,ny,nz,color,light);
        }
    }

    /** Vertical octagonal elliptical frustum for torso/head forms. */
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
            float nl=(float)Math.sqrt(nx*nx+nz*nz);
            quad(v,e,p0,p1,q1,q0,nx/nl,0,nz/nl,color,light);
        }
    }

    private static Identifier texture() {
        if (texture != null) return texture;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 2, 2, false);
        for (int y=0;y<2;y++) for (int x=0;x<2;x++) image.setColor(x,y,0xFFFFFFFF);
        texture = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(
                "star_cj_reference_white", new NativeImageBackedTexture(image));
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
