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
import org.joml.Matrix4f;

/**
 * Procedural animated sky filaments. No texture pack is required: the sky is built from flowing
 * luminous curves around the camera so it keeps moving smoothly at any resolution.
 */
public final class AnimatedSkyRenderer {
    private AnimatedSkyRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!VisualModules.skyEnabled() || client.world == null || client.player == null) return;
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        int tier = StarPerformance.animationTier(client);
        int ribbons = tier == 0 ? 7 : tier == 1 ? 13 : 22;
        int segments = tier == 0 ? 30 : tier == 1 ? 42 : 58;
        double time = System.currentTimeMillis() / 1000.0 * VisualModules.skySpeed();
        int color = VisualModules.skyFilamentColor();
        float intensity = (float)VisualModules.skyIntensity();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        // Wide dim pass + crisp bright core gives the video-inspired liquid-light look without blur.
        drawFilaments(matrices, ribbons, segments, time, withAlpha(color, (int)(34 + 58 * intensity)), 4.0F);
        drawFilaments(matrices, ribbons, segments, time, withAlpha(color, (int)(112 + 118 * intensity)), 1.45F);

        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void drawFilaments(MatrixStack matrices, int ribbons, int segments,
                                      double time, int color, float width) {
        RenderSystem.lineWidth(width);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        final double radius = 92.0;

        for (int ribbon = 0; ribbon < ribbons; ribbon++) {
            double seed = ribbon * 1.731 + 0.37;
            double twist = 1.0 + (ribbon % 5) * 0.31;
            double drift = time * (0.13 + (ribbon % 4) * 0.035) + seed;
            double[] prev = point(radius, seed, twist, drift, 0.0);
            for (int s = 1; s <= segments; s++) {
                double u = s / (double)segments;
                double[] next = point(radius, seed, twist, drift, u);
                line(buffer, matrix,
                        (float)prev[0], (float)prev[1], (float)prev[2],
                        (float)next[0], (float)next[1], (float)next[2], color);
                prev = next;
            }
        }

        // Slow crossing arcs stop the pattern from reading as simple latitude lines.
        int arcCount = Math.max(4, ribbons / 2);
        for (int arc = 0; arc < arcCount; arc++) {
            double seed = arc * 2.47 + 0.9;
            double[] prev = arcPoint(radius, seed, time, 0.0);
            for (int s = 1; s <= segments / 2; s++) {
                double u = s / (double)(segments / 2);
                double[] next = arcPoint(radius, seed, time, u);
                line(buffer, matrix,
                        (float)prev[0], (float)prev[1], (float)prev[2],
                        (float)next[0], (float)next[1], (float)next[2], color);
                prev = next;
            }
        }

        try {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } catch (IllegalStateException ignored) {
            // A renderer transition should never crash the client.
        }
    }

    private static double[] point(double radius, double seed, double twist, double drift, double u) {
        double azimuth = u * Math.PI * 2.0 + seed * 0.58 + Math.sin(drift * 0.47) * 0.24;
        double elevation = 0.11
                + 0.50 * (0.5 + 0.5 * Math.sin(azimuth * twist + drift))
                + 0.18 * Math.sin(azimuth * 2.7 - drift * 0.62 + seed);
        elevation = Math.max(0.035, Math.min(1.38, elevation));
        double wobble = 1.0 + 0.035 * Math.sin(azimuth * 4.0 + drift * 1.3 + seed);
        double r = radius * wobble;
        double cosE = Math.cos(elevation);
        return new double[]{
                Math.cos(azimuth) * cosE * r,
                Math.sin(elevation) * r,
                Math.sin(azimuth) * cosE * r
        };
    }

    private static double[] arcPoint(double radius, double seed, double time, double u) {
        double a = (u - 0.5) * Math.PI * 1.55;
        double heading = seed + time * 0.055;
        double elevation = 0.35 + 0.58 * Math.cos(a * 0.72 + seed * 0.31)
                + 0.08 * Math.sin(time * 0.7 + seed + u * 8.0);
        elevation = Math.max(0.06, Math.min(1.42, elevation));
        double azimuth = heading + a + Math.sin(a * 2.0 + seed) * 0.22;
        double cosE = Math.cos(elevation);
        return new double[]{
                Math.cos(azimuth) * cosE * radius,
                Math.sin(elevation) * radius,
                Math.sin(azimuth) * cosE * radius
        };
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static void line(BufferBuilder b, Matrix4f m,
                             float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        b.vertex(m, x0, y0, z0).color(color);
        b.vertex(m, x1, y1, z1).color(color);
    }
}
