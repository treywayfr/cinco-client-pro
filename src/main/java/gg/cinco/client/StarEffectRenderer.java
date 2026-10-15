package gg.cinco.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Lightweight world-space cosmetic animations inspired by the supplied client showcase video. */
public final class StarEffectRenderer {
    private StarEffectRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientModule module = CincoClient.findModule("Animations");
        if (module == null || !module.enabled || client.player == null || client.world == null) return;
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        int style = Math.max(1, Math.min(8, (int)Math.round(module.setting)));
        double speed = Math.max(0.2, Math.min(3.0, module.setting2));
        float intensity = (float)Math.max(0.2, Math.min(1.0, module.setting3));
        int tier = StarPerformance.animationTier(client);
        double t = System.currentTimeMillis() / 1000.0 * speed;

        Vec3d cam = context.camera().getPos();
        double px = client.player.getX() - cam.x;
        double py = client.player.getY() - cam.y;
        double pz = client.player.getZ() - cam.z;

        int color = styleColor(style);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(tier == 0 ? 1.25F : 1.8F);

        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f m = matrices.peek().getPositionMatrix();
        int alpha = (int)(120 + 120 * intensity);
        int c = withAlpha(color, alpha);
        int soft = withAlpha(color, (int)(45 + 80 * intensity));

        switch (style) {
            case 2 -> aura(b, m, px, py, pz, t, c, soft, tier, true);
            case 3 -> jumpCircles(b, m, px, py, pz, t, c, tier, client.player.isOnGround());
            case 4 -> speedTrails(b, m, px, py, pz, t, c, soft, tier, client.player.getVelocity());
            case 5 -> orbitRunes(b, m, px, py, pz, t, c, tier);
            case 6 -> lightning(b, m, px, py, pz, t, c, soft, tier);
            case 7 -> footstepRings(b, m, px, py, pz, t, c, tier);
            case 8 -> cosmicPulse(b, m, px, py, pz, t, c, soft, tier);
            default -> aura(b, m, px, py, pz, t, c, soft, tier, false);
        }

        try { BufferRenderer.drawWithGlobalProgram(b.end()); }
        catch (IllegalStateException ignored) {}

        RenderSystem.lineWidth(1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void aura(BufferBuilder b, Matrix4f m, double x, double y, double z,
                             double t, int c, int soft, int tier, boolean purple) {
        int rings = tier == 0 ? 2 : 4;
        int seg = tier == 0 ? 20 : 36;
        for (int r = 0; r < rings; r++) {
            double yy = y + 0.18 + r * 0.43 + Math.sin(t * 1.7 + r) * 0.05;
            double radius = 0.40 + r * 0.06 + Math.sin(t * 2.0 + r * 0.8) * 0.04;
            ring(b, m, x, yy, z, radius, seg, r % 2 == 0 ? c : soft);
        }
        int sparks = tier == 0 ? 8 : 18;
        for (int i = 0; i < sparks; i++) {
            double a = i * 2.399 + t * (purple ? 0.75 : 0.48);
            double rr = 0.48 + 0.12 * Math.sin(t + i * 1.31);
            double yy = y + 0.15 + ((i * 0.29 + t * 0.18) % 1.7);
            double sx = x + Math.cos(a) * rr;
            double sz = z + Math.sin(a) * rr;
            cross(b, m, sx, yy, sz, 0.035 + (i % 3) * 0.008, c);
        }
    }

    private static void jumpCircles(BufferBuilder b, Matrix4f m, double x, double y, double z,
                                    double t, int c, int tier, boolean onGround) {
        int seg = tier == 0 ? 22 : 40;
        int count = tier == 0 ? 2 : 4;
        double lift = onGround ? 0.025 : -0.05;
        for (int i = 0; i < count; i++) {
            double phase = (t * 0.62 + i / (double)count) % 1.0;
            double r = 0.18 + phase * 1.08;
            int cc = withAlpha(c, (int)(220 * (1.0 - phase)));
            ring(b, m, x, y + lift + i * 0.012, z, r, seg, cc);
        }
    }

    private static void speedTrails(BufferBuilder b, Matrix4f m, double x, double y, double z,
                                    double t, int c, int soft, int tier, Vec3d velocity) {
        double vx = velocity.x, vz = velocity.z;
        double len = Math.hypot(vx, vz);
        if (len < 0.025) {
            vx = -Math.sin(t * 0.35); vz = Math.cos(t * 0.35); len = 1.0;
        }
        vx /= len; vz /= len;
        int lines = tier == 0 ? 5 : 11;
        for (int i = 0; i < lines; i++) {
            double side = (i - (lines - 1) / 2.0) * 0.09;
            double sx = x + vz * side;
            double sz = z - vx * side;
            double sy = y + 0.12 + (i % 4) * 0.38;
            double length = 0.75 + (i % 3) * 0.28 + 0.10 * Math.sin(t * 3.0 + i);
            line(b, m, sx, sy, sz, sx - vx * length, sy + 0.04 * Math.sin(t + i), sz - vz * length,
                    i % 2 == 0 ? c : soft);
        }
    }

    private static void orbitRunes(BufferBuilder b, Matrix4f m, double x, double y, double z,
                                   double t, int c, int tier) {
        int count = tier == 0 ? 6 : 10;
        for (int i = 0; i < count; i++) {
            double a = t * 0.7 + i * Math.PI * 2.0 / count;
            double r = 0.65 + 0.10 * Math.sin(t * 1.4 + i);
            double yy = y + 0.25 + (i % 5) * 0.32;
            double sx = x + Math.cos(a) * r;
            double sz = z + Math.sin(a) * r;
            diamond(b, m, sx, yy, sz, 0.08, c);
        }
        ring(b, m, x, y + 0.95, z, 0.72, tier == 0 ? 24 : 42, withAlpha(c, 95));
    }

    private static void lightning(BufferBuilder b, Matrix4f m, double x, double y, double z,
                                  double t, int c, int soft, int tier) {
        int bolts = tier == 0 ? 4 : 8;
        int steps = tier == 0 ? 4 : 7;
        for (int i = 0; i < bolts; i++) {
            double a = i * Math.PI * 2.0 / bolts + t * 0.19;
            double bx = x + Math.cos(a) * 0.50;
            double bz = z + Math.sin(a) * 0.50;
            double lastX = bx, lastY = y + 0.05, lastZ = bz;
            for (int s = 1; s <= steps; s++) {
                double yy = y + 0.05 + 1.75 * s / steps;
                double jitter = 0.07 * Math.sin(t * 8.0 + i * 4.7 + s * 2.1);
                double nx = bx + Math.cos(a + Math.PI / 2) * jitter;
                double nz = bz + Math.sin(a + Math.PI / 2) * jitter;
                line(b, m, lastX, lastY, lastZ, nx, yy, nz, s % 2 == 0 ? soft : c);
                lastX = nx; lastY = yy; lastZ = nz;
            }
        }
    }

    private static void footstepRings(BufferBuilder b, Matrix4f m, double x, double y, double z,
                                      double t, int c, int tier) {
        int seg = tier == 0 ? 18 : 34;
        double pulse = 0.20 + 0.08 * (0.5 + 0.5 * Math.sin(t * 4.0));
        ring(b, m, x - 0.16, y + 0.025, z, pulse, seg, c);
        ring(b, m, x + 0.16, y + 0.027, z, pulse, seg, withAlpha(c, 150));
        ring(b, m, x, y + 0.03, z, 0.48 + 0.08 * Math.sin(t * 2.0), seg, withAlpha(c, 75));
    }

    private static void cosmicPulse(BufferBuilder b, Matrix4f m, double x, double y, double z,
                                    double t, int c, int soft, int tier) {
        int seg = tier == 0 ? 20 : 38;
        double r = 0.58 + 0.08 * Math.sin(t * 1.8);
        ring(b, m, x, y + 0.95, z, r, seg, c);
        verticalRing(b, m, x, y + 0.95, z, r, seg, c, 0.0);
        verticalRing(b, m, x, y + 0.95, z, r, seg, soft, Math.PI / 2.0);
        int sparks = tier == 0 ? 6 : 14;
        for (int i = 0; i < sparks; i++) {
            double a = i * 2.399 + t * 0.35;
            cross(b, m, x + Math.cos(a) * (r + 0.12), y + 0.4 + (i % 5) * 0.27,
                    z + Math.sin(a) * (r + 0.12), 0.04, c);
        }
    }

    private static int styleColor(int style) {
        return switch (style) {
            case 1 -> 0xFFFFD65A;
            case 2, 5, 8 -> 0xFFA75CFF;
            case 3 -> 0xFF7B68FF;
            case 4 -> 0xFF65A8FF;
            case 6 -> 0xFFD568FF;
            case 7 -> 0xFFB786FF;
            default -> 0xFFA75CFF;
        };
    }

    private static void ring(BufferBuilder b, Matrix4f m, double x, double y, double z,
                             double r, int seg, int c) {
        double px = x + r, pz = z;
        for (int i = 1; i <= seg; i++) {
            double a = i * Math.PI * 2.0 / seg;
            double nx = x + Math.cos(a) * r, nz = z + Math.sin(a) * r;
            line(b, m, px, y, pz, nx, y, nz, c);
            px = nx; pz = nz;
        }
    }

    private static void verticalRing(BufferBuilder b, Matrix4f m, double x, double y, double z,
                                     double r, int seg, int c, double rot) {
        double px = x + Math.cos(rot) * r, py = y, pz = z + Math.sin(rot) * r;
        for (int i = 1; i <= seg; i++) {
            double a = i * Math.PI * 2.0 / seg;
            double h = Math.cos(a) * r;
            double nx = x + Math.cos(rot) * h;
            double nz = z + Math.sin(rot) * h;
            double ny = y + Math.sin(a) * r;
            line(b, m, px, py, pz, nx, ny, nz, c);
            px = nx; py = ny; pz = nz;
        }
    }

    private static void diamond(BufferBuilder b, Matrix4f m, double x, double y, double z, double s, int c) {
        line(b,m,x,y+s,z,x+s,y,z,c); line(b,m,x+s,y,z,x,y-s,z,c);
        line(b,m,x,y-s,z,x-s,y,z,c); line(b,m,x-s,y,z,x,y+s,z,c);
    }

    private static void cross(BufferBuilder b, Matrix4f m, double x, double y, double z, double s, int c) {
        line(b,m,x-s,y,z,x+s,y,z,c); line(b,m,x,y-s,z,x,y+s,z,c);
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static void line(BufferBuilder b, Matrix4f m,
                             double x0, double y0, double z0, double x1, double y1, double z1, int c) {
        b.vertex(m, (float)x0, (float)y0, (float)z0).color(c);
        b.vertex(m, (float)x1, (float)y1, (float)z1).color(c);
    }
}
