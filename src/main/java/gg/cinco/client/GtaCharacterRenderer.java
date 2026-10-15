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
 * Procedural low-poly human renderer for the local player. It uses rounded/prismatic geometry
 * instead of Minecraft cubes, so enabling GTA Skin Changer actually replaces the blocky body.
 * The mesh is original and only Franklin-inspired through streetwear palette/proportions.
 */
public final class GtaCharacterRenderer {
    private static Identifier whiteTexture;

    private GtaCharacterRenderer() {}

    public static void render(AbstractClientPlayerEntity player, float bodyYaw, float tickDelta,
                              MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        ClientModule module = CincoClient.findModule("GTA Skin Changer");
        if (module == null || !module.enabled) return;

        int outfit = Math.max(1, Math.min(4, (int) Math.round(module.setting)));
        int shirt = switch (outfit) {
            case 2 -> 0xFFE7E7E3;
            case 3 -> 0xFF26282B;
            case 4 -> 0xFF2F6595;
            default -> 0xFF2F7447;
        };
        int undershirt = outfit == 3 ? 0xFFB7B7B2 : 0xFFF1F0E8;
        int jeans = outfit == 2 ? 0xFF2B3138 : outfit == 4 ? 0xFF232B36 : 0xFF28364A;
        int shoes = 0xFF17191C;
        int skin = 0xFF845237;
        int skinShade = 0xFF69402E;
        int hair = 0xFF171413;

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
        if (player.isSneaking()) matrices.translate(0.0, -0.08, 0.08);

        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntitySolid(getWhiteTexture()));
        MatrixStack.Entry entry = matrices.peek();

        double horizontalSpeed = player.getVelocity().horizontalLength();
        float motion = (float) Math.min(1.0, horizontalSpeed * 4.5);
        float phase = (player.age + tickDelta) * 0.62F;
        float legSwing = (float) Math.sin(phase) * 0.28F * motion;
        float armSwing = -legSwing * 0.9F;

        // Shoes and tapered legs.
        box(vertices, entry, -0.24f, 0.01f, -0.16f + legSwing, -0.02f, 0.12f, 0.16f + legSwing, shoes, light);
        box(vertices, entry, 0.02f, 0.01f, -0.16f - legSwing, 0.24f, 0.12f, 0.16f - legSwing, shoes, light);
        cylinder(vertices, entry, new Vec3d(-0.13, 0.73, -legSwing * 0.15),
                new Vec3d(-0.13, 0.11, legSwing), 0.115, 7, jeans, light);
        cylinder(vertices, entry, new Vec3d(0.13, 0.73, legSwing * 0.15),
                new Vec3d(0.13, 0.11, -legSwing), 0.115, 7, jeans, light);

        // Human-shaped tapered torso with a narrow waist and broader shoulders.
        frustum(vertices, entry, 0.0f, 0.73f, 1.42f,
                0.22f, 0.31f, 0.145f, 0.165f, shirt, light);
        // White tee panel beneath the open green/colored top.
        box(vertices, entry, -0.10f, 0.93f, -0.171f, 0.10f, 1.34f, -0.158f, undershirt, light);

        // Arms use 7-sided prisms rather than rectangular Minecraft arms.
        cylinder(vertices, entry, new Vec3d(-0.34, 1.33, 0.0),
                new Vec3d(-0.39, 0.76, armSwing), 0.085, 7, skin, light);
        cylinder(vertices, entry, new Vec3d(0.34, 1.33, 0.0),
                new Vec3d(0.39, 0.76, -armSwing), 0.085, 7, skin, light);
        // Short sleeves around the shoulders.
        cylinder(vertices, entry, new Vec3d(-0.33, 1.34, 0.0),
                new Vec3d(-0.36, 1.12, armSwing * 0.25), 0.105, 7, shirt, light);
        cylinder(vertices, entry, new Vec3d(0.33, 1.34, 0.0),
                new Vec3d(0.36, 1.12, -armSwing * 0.25), 0.105, 7, shirt, light);

        // Neck and rounded low-poly head.
        cylinder(vertices, entry, new Vec3d(0.0, 1.40, 0.0), new Vec3d(0.0, 1.50, 0.0),
                0.105, 8, skinShade, light);
        ellipsoid(vertices, entry, 0.0f, 1.68f, 0.0f, 0.205f, 0.245f, 0.19f,
                -1.50f, 1.50f, 7, 10, skin, light);
        // Hair cap and a small jaw/beard accent make the silhouette more Franklin-like without using GTA assets.
        ellipsoid(vertices, entry, 0.0f, 1.70f, 0.0f, 0.211f, 0.251f, 0.196f,
                0.38f, 1.50f, 4, 10, hair, light);
        box(vertices, entry, -0.105f, 1.535f, -0.193f, 0.105f, 1.575f, -0.181f, hair, light);

        // Minimal facial features on the forward (-Z) side.
        box(vertices, entry, -0.105f, 1.695f, -0.194f, -0.035f, 1.715f, -0.188f, 0xFFF5F2E7, light);
        box(vertices, entry, 0.035f, 1.695f, -0.194f, 0.105f, 1.715f, -0.188f, 0xFFF5F2E7, light);
        box(vertices, entry, -0.073f, 1.697f, -0.198f, -0.045f, 1.714f, -0.192f, 0xFF211B18, light);
        box(vertices, entry, 0.045f, 1.697f, -0.198f, 0.073f, 1.714f, -0.192f, 0xFF211B18, light);

        matrices.pop();
    }

    private static Identifier getWhiteTexture() {
        if (whiteTexture != null) return whiteTexture;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 2, 2, false);
        for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) image.setColor(x, y, 0xFFFFFFFF);
        whiteTexture = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(
                "cinco_gta_lowpoly_white", new NativeImageBackedTexture(image));
        return whiteTexture;
    }

    private static void frustum(VertexConsumer v, MatrixStack.Entry e, float cx, float y0, float y1,
                                float bottomHalfWidth, float topHalfWidth,
                                float bottomHalfDepth, float topHalfDepth, int color, int light) {
        float[][] b = {{cx-bottomHalfWidth,y0,-bottomHalfDepth},{cx+bottomHalfWidth,y0,-bottomHalfDepth},
                {cx+bottomHalfWidth,y0,bottomHalfDepth},{cx-bottomHalfWidth,y0,bottomHalfDepth}};
        float[][] t = {{cx-topHalfWidth,y1,-topHalfDepth},{cx+topHalfWidth,y1,-topHalfDepth},
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
        Vec3d axis = end.subtract(start);
        double len = axis.length();
        if (len < 0.0001) return;
        Vec3d n = axis.multiply(1.0 / len);
        Vec3d ref = Math.abs(n.y) < 0.9 ? new Vec3d(0,1,0) : new Vec3d(1,0,0);
        Vec3d u = n.crossProduct(ref).normalize();
        Vec3d w = n.crossProduct(u).normalize();
        for (int i=0;i<sides;i++) {
            double a0 = Math.PI * 2.0 * i / sides;
            double a1 = Math.PI * 2.0 * (i+1) / sides;
            Vec3d r0 = u.multiply(Math.cos(a0)*radius).add(w.multiply(Math.sin(a0)*radius));
            Vec3d r1 = u.multiply(Math.cos(a1)*radius).add(w.multiply(Math.sin(a1)*radius));
            Vec3d p0=start.add(r0), p1=start.add(r1), p2=end.add(r1), p3=end.add(r0);
            Vec3d normal = r0.add(r1).normalize();
            quad(v,e,toF(p0),toF(p1),toF(p2),toF(p3),(float)normal.x,(float)normal.y,(float)normal.z,color,light);
        }
    }

    private static void ellipsoid(VertexConsumer v, MatrixStack.Entry e, float cx,float cy,float cz,
                                  float rx,float ry,float rz,float latMin,float latMax,
                                  int latSegments,int lonSegments,int color,int light) {
        for (int iy=0; iy<latSegments; iy++) {
            double p0=latMin+(latMax-latMin)*iy/latSegments;
            double p1=latMin+(latMax-latMin)*(iy+1)/latSegments;
            for (int ix=0; ix<lonSegments; ix++) {
                double a0=Math.PI*2.0*ix/lonSegments, a1=Math.PI*2.0*(ix+1)/lonSegments;
                float[] q0=spherePoint(cx,cy,cz,rx,ry,rz,p0,a0);
                float[] q1=spherePoint(cx,cy,cz,rx,ry,rz,p0,a1);
                float[] q2=spherePoint(cx,cy,cz,rx,ry,rz,p1,a1);
                float[] q3=spherePoint(cx,cy,cz,rx,ry,rz,p1,a0);
                double pm=(p0+p1)*0.5, am=(a0+a1)*0.5;
                float nx=(float)(Math.cos(pm)*Math.cos(am));
                float ny=(float)Math.sin(pm);
                float nz=(float)(Math.cos(pm)*Math.sin(am));
                quad(v,e,q0,q1,q2,q3,nx,ny,nz,color,light);
            }
        }
    }

    private static float[] spherePoint(float cx,float cy,float cz,float rx,float ry,float rz,double phi,double angle) {
        double cp=Math.cos(phi);
        return new float[]{cx+(float)(rx*cp*Math.cos(angle)), cy+(float)(ry*Math.sin(phi)),
                cz+(float)(rz*cp*Math.sin(angle))};
    }

    private static float[] toF(Vec3d p) { return new float[]{(float)p.x,(float)p.y,(float)p.z}; }

    private static void quad(VertexConsumer v, MatrixStack.Entry e, float[] a,float[] b,float[] c,float[] d,
                             float nx,float ny,float nz,int color,int light) {
        vertex(v,e,a,nx,ny,nz,color,light); vertex(v,e,b,nx,ny,nz,color,light);
        vertex(v,e,c,nx,ny,nz,color,light); vertex(v,e,d,nx,ny,nz,color,light);
    }

    private static void vertex(VertexConsumer v, MatrixStack.Entry e, float[] p,
                               float nx,float ny,float nz,int color,int light) {
        v.vertex(e.getPositionMatrix(), p[0], p[1], p[2])
                .color(color)
                .texture(0.5F, 0.5F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(e, nx, ny, nz);
    }
}
