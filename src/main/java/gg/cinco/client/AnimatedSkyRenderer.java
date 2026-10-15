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
 * Deep-space sky decoration for Sky Changer.
 *
 * This intentionally contains no DEBUG_LINES/line ribbons. The old filament renderer could read
 * like glowing wires stretched across the sky. The new renderer uses only filled triangles:
 * softly drifting nebula clouds and crisp filled star sprites over the dark sky color supplied by
 * VisualModules/ClientWorldSkyMixin.
 */
public final class AnimatedSkyRenderer {
    private static final double SKY_RADIUS = 360.0;

    private AnimatedSkyRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!VisualModules.skyEnabled() || client.world == null || client.player == null) return;
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        int tier = StarPerformance.animationTier(client);
        int starCount = tier == 0 ? 70 : tier == 1 ? 140 : 240;
        int cloudCount = tier == 0 ? 4 : tier == 1 ? 7 : 10;
        int cloudSegments = tier == 0 ? 8 : tier == 1 ? 11 : 14;
        double time = System.currentTimeMillis() / 1000.0 * VisualModules.skySpeed();
        double intensity = VisualModules.skyIntensity();
        int style = VisualModules.skyStyle();
        int[] palette = palette(style);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        drawNebula(matrices, cloudCount, cloudSegments, time, intensity, palette);
        drawStars(matrices, starCount, time, intensity, palette);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void drawNebula(MatrixStack matrices, int cloudCount, int segments,
                                   double time, double intensity, int[] palette) {
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f m = matrices.peek().getPositionMatrix();

        for (int i = 0; i < cloudCount; i++) {
            double seed = i * 19.731 + 4.17;
            double baseAz = hash(seed) * Math.PI * 2.0;
            double baseEl = 0.18 + hash(seed + 8.3) * 1.20;
            double drift = Math.sin(time * (0.018 + (i % 3) * 0.006) + seed) * 0.055;
            double az = baseAz + drift;
            double el = clamp(baseEl + Math.cos(time * 0.014 + seed) * 0.025, 0.10, 1.48);
            double angularRadius = 0.10 + hash(seed + 2.4) * 0.16;

            int colorA = palette[i % palette.length];
            int colorB = palette[(i + 1) % palette.length];
            int alphaA = (int)Math.round((20 + hash(seed + 5.0) * 24) * intensity);
            int alphaB = (int)Math.round((10 + hash(seed + 7.0) * 17) * intensity);

            // Three overlapping filled haze petals create a cloud instead of a geometric circle.
            nebulaFan(b, m, az, el, angularRadius, segments, SKY_RADIUS,
                    withAlpha(colorA, alphaA), 0x00000000, seed);
            nebulaFan(b, m, az + angularRadius * 0.55, el + angularRadius * 0.18,
                    angularRadius * 0.78, segments, SKY_RADIUS - 0.7,
                    withAlpha(colorB, alphaB), 0x00000000, seed + 11.0);
            nebulaFan(b, m, az - angularRadius * 0.43, el - angularRadius * 0.20,
                    angularRadius * 0.62, segments, SKY_RADIUS - 1.2,
                    withAlpha(mixRgb(colorA, 0xFFFFFFFF, 0.22), Math.max(4, alphaB - 2)),
                    0x00000000, seed + 23.0);
        }

        try { BufferRenderer.drawWithGlobalProgram(b.end()); }
        catch (IllegalStateException ignored) {}
    }

    private static void nebulaFan(BufferBuilder b, Matrix4f m,
                                  double az, double el, double angularRadius, int segments,
                                  double radius, int centerColor, int edgeColor, double seed) {
        double[] center = sphere(radius, az, el);
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2.0 / segments;
            double a1 = (i + 1) * Math.PI * 2.0 / segments;
            double r0 = angularRadius * (0.82 + hash(seed + i * 3.17) * 0.34);
            double r1 = angularRadius * (0.82 + hash(seed + (i + 1) * 3.17) * 0.34);
            double[] p0 = sphere(radius,
                    az + Math.cos(a0) * r0,
                    clamp(el + Math.sin(a0) * r0 * 0.68, 0.035, 1.53));
            double[] p1 = sphere(radius,
                    az + Math.cos(a1) * r1,
                    clamp(el + Math.sin(a1) * r1 * 0.68, 0.035, 1.53));
            tri(b, m, center, p0, p1, centerColor, edgeColor, edgeColor);
        }
    }

    private static void drawStars(MatrixStack matrices, int count, double time,
                                  double intensity, int[] palette) {
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f m = matrices.peek().getPositionMatrix();

        for (int i = 0; i < count; i++) {
            double seed = i * 41.137 + 1.913;
            double az = hash(seed) * Math.PI * 2.0;
            double el = 0.055 + Math.pow(hash(seed + 9.1), 0.72) * 1.46;
            double twinkle = 0.70 + 0.30 * Math.sin(time * (0.34 + hash(seed + 2.2) * 0.45) + seed);
            double rare = hash(seed + 18.7);
            double size = (rare > 0.965 ? 0.78 : rare > 0.88 ? 0.42 : 0.18 + hash(seed + 4.4) * 0.16);
            int base = rare > 0.84 ? palette[(int)(hash(seed + 30.0) * palette.length) % palette.length] : 0xFFFFFFFF;
            int alpha = (int)Math.round((125 + 120 * twinkle) * intensity);
            filledStar(b, m, SKY_RADIUS - 2.0 - hash(seed + 20.0) * 2.0,
                    az, el, size, withAlpha(base, alpha), rare > 0.965);
        }

        try { BufferRenderer.drawWithGlobalProgram(b.end()); }
        catch (IllegalStateException ignored) {}
    }

    private static void filledStar(BufferBuilder b, Matrix4f m, double radius,
                                   double az, double el, double size, int color, boolean large) {
        double[] c = sphere(radius, az, el);
        double[] right = {-Math.sin(az), 0.0, Math.cos(az)};
        double[] up = {-Math.cos(az) * Math.sin(el), Math.cos(el), -Math.sin(az) * Math.sin(el)};

        double sx = size * (large ? 1.55 : 1.0);
        double sy = size * (large ? 0.72 : 1.0);
        double[] left = offset(c, right, -sx);
        double[] rightP = offset(c, right, sx);
        double[] top = offset(c, up, sy);
        double[] bottom = offset(c, up, -sy);
        tri(b, m, top, rightP, bottom, color, color, color);
        tri(b, m, top, bottom, left, color, color, color);

        if (large) {
            int soft = withAlpha(color, Math.max(18, ((color >>> 24) & 0xFF) / 3));
            double[] left2 = offset(c, right, -sx * 0.48);
            double[] right2 = offset(c, right, sx * 0.48);
            double[] top2 = offset(c, up, sy * 2.2);
            double[] bottom2 = offset(c, up, -sy * 2.2);
            tri(b, m, top2, right2, bottom2, soft, soft, soft);
            tri(b, m, top2, bottom2, left2, soft, soft, soft);
        }
    }

    private static int[] palette(int style) {
        return switch (style) {
            case 2 -> new int[]{0xFF5D66FF, 0xFF709CFF, 0xFFB5CBFF};
            case 3 -> new int[]{0xFF8A4FFF, 0xFFC160FF, 0xFF5D5BFF};
            case 4 -> new int[]{0xFFFF4FA6, 0xFF8C48FF, 0xFFFF91D0};
            case 5 -> new int[]{0xFF61D8FF, 0xFF5577FF, 0xFFB7EEFF};
            case 6 -> new int[]{0xFF52FFB8, 0xFF4E9FFF, 0xFF8DFFD8};
            case 7 -> new int[]{0xFF7563FF, 0xFF382A73, 0xFFB4A8FF};
            case 8 -> new int[]{0xFFFF6FD8, 0xFF66C8FF, 0xFF9A6CFF, 0xFFFFC86D};
            default -> new int[]{0xFF7457FF, 0xFFB05CFF, 0xFF526BFF};
        };
    }

    private static double[] sphere(double radius, double az, double el) {
        double ce = Math.cos(el);
        return new double[]{Math.cos(az) * ce * radius, Math.sin(el) * radius, Math.sin(az) * ce * radius};
    }

    private static double[] offset(double[] base, double[] axis, double amount) {
        return new double[]{base[0] + axis[0] * amount, base[1] + axis[1] * amount, base[2] + axis[2] * amount};
    }

    private static double hash(double n) {
        double x = Math.sin(n * 12.9898 + 78.233) * 43758.5453123;
        return x - Math.floor(x);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static int mixRgb(int a, int b, double t) {
        t = clamp(t, 0.0, 1.0);
        int ar=(a>>16)&255, ag=(a>>8)&255, ab=a&255;
        int br=(b>>16)&255, bg=(b>>8)&255, bb=b&255;
        int r=(int)Math.round(ar+(br-ar)*t), g=(int)Math.round(ag+(bg-ag)*t), bl=(int)Math.round(ab+(bb-ab)*t);
        return 0xFF000000 | (r<<16) | (g<<8) | bl;
    }

    private static void tri(BufferBuilder b, Matrix4f m,
                            double[] a, double[] d, double[] e,
                            int ca, int cd, int ce) {
        b.vertex(m, (float)a[0], (float)a[1], (float)a[2]).color(ca);
        b.vertex(m, (float)d[0], (float)d[1], (float)d[2]).color(cd);
        b.vertex(m, (float)e[0], (float)e[1], (float)e[2]).color(ce);
    }
}
