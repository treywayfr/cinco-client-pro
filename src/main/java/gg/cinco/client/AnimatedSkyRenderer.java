package gg.cinco.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;

/**
 * Animated deep-space replacement sky for Sky Changer.
 *
 * Everything is rendered in camera-relative coordinates so the sky follows the player forever.
 * The vanilla sky is visually covered by a dark space dome, then nebulae, moving stars and
 * animated planets are layered in front. Rendering is intentionally low-poly / bounded so the
 * effect remains usable on lower-end systems.
 */
public final class AnimatedSkyRenderer {
    private static final double DOME_RADIUS = 430.0;
    private static final double NEBULA_RADIUS = 422.0;
    private static final double STAR_RADIUS = 414.0;
    private static final double PLANET_RADIUS = 404.0;

    private AnimatedSkyRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!VisualModules.skyEnabled() || client.world == null || client.player == null || context.consumers() == null) return;

        int tier = StarPerformance.animationTier(client);
        int starCount = tier == 0 ? 80 : tier == 1 ? 150 : 260;
        int cloudCount = tier == 0 ? 4 : tier == 1 ? 7 : 11;
        int cloudSegments = tier == 0 ? 8 : tier == 1 ? 12 : 16;
        int planetSegments = tier == 0 ? 14 : tier == 1 ? 20 : 28;

        double time = System.currentTimeMillis() / 1000.0 * VisualModules.skySpeed();
        double intensity = VisualModules.skyIntensity();
        int style = VisualModules.skyStyle();
        int[] palette = palette(style);

        VertexConsumer vertices = context.consumers().getBuffer(RenderLayers.debugQuads());

        drawSpaceDome(vertices, style, intensity);
        drawNebula(vertices, cloudCount, cloudSegments, time, intensity, palette, style);
        drawStars(vertices, starCount, time, intensity, palette, style);
        drawPlanets(vertices, planetSegments, time, intensity, palette, style);
    }

    /** Dark upper-hemisphere shell that makes Sky Changer read as actual deep space. */
    private static void drawSpaceDome(VertexConsumer b, int style, double intensity) {
        int azSteps = 18;
        int elSteps = 6;
        int base = domeColor(style);
        int top = mixRgb(base, 0xFF020107, 0.52);
        int alpha = (int)Math.round(215 + 34 * intensity);

        for (int ey = 0; ey < elSteps; ey++) {
            double e0 = 0.025 + ey * (1.50 - 0.025) / elSteps;
            double e1 = 0.025 + (ey + 1) * (1.50 - 0.025) / elSteps;
            double t = ey / (double)Math.max(1, elSteps - 1);
            int rowColor = withAlpha(mixRgb(base, top, t * 0.70), alpha);
            for (int ax = 0; ax < azSteps; ax++) {
                double a0 = ax * Math.PI * 2.0 / azSteps;
                double a1 = (ax + 1) * Math.PI * 2.0 / azSteps;
                double[] p00 = sphere(DOME_RADIUS, a0, e0);
                double[] p10 = sphere(DOME_RADIUS, a1, e0);
                double[] p11 = sphere(DOME_RADIUS, a1, e1);
                double[] p01 = sphere(DOME_RADIUS, a0, e1);
                quad(b, p00, p10, p11, p01, rowColor);
            }
        }
    }

    private static void drawNebula(VertexConsumer b, int cloudCount, int segments,
                                   double time, double intensity, int[] palette, int style) {
        for (int i = 0; i < cloudCount; i++) {
            double seed = i * 19.731 + style * 5.71 + 4.17;
            double baseAz = hash(seed) * Math.PI * 2.0;
            double baseEl = 0.16 + hash(seed + 8.3) * 1.23;

            // Slow real drift across the sky, with a smaller breathing wobble.
            double driftRate = 0.006 + hash(seed + 2.0) * 0.009;
            double az = baseAz + time * driftRate + Math.sin(time * 0.020 + seed) * 0.045;
            double el = clamp(baseEl + Math.cos(time * 0.015 + seed) * 0.035, 0.09, 1.49);
            double angularRadius = 0.11 + hash(seed + 2.4) * 0.19;

            int colorA = palette[i % palette.length];
            int colorB = palette[(i + 1) % palette.length];
            int alphaA = (int)Math.round((26 + hash(seed + 5.0) * 34) * intensity);
            int alphaB = (int)Math.round((14 + hash(seed + 7.0) * 24) * intensity);

            nebulaFan(b, az, el, angularRadius, segments, NEBULA_RADIUS,
                    withAlpha(colorA, alphaA), seed);
            nebulaFan(b, az + angularRadius * 0.55, el + angularRadius * 0.18,
                    angularRadius * 0.78, segments, NEBULA_RADIUS - 0.8,
                    withAlpha(colorB, alphaB), seed + 11.0);
            nebulaFan(b, az - angularRadius * 0.43, el - angularRadius * 0.20,
                    angularRadius * 0.62, segments, NEBULA_RADIUS - 1.5,
                    withAlpha(mixRgb(colorA, 0xFFFFFFFF, 0.25), Math.max(6, alphaB - 1)), seed + 23.0);
        }
    }

    private static void nebulaFan(VertexConsumer b, double az, double el, double angularRadius,
                                  int segments, double radius, int centerColor, double seed) {
        double[] center = sphere(radius, az, el);
        int edgeColor = centerColor & 0x00FFFFFF;
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2.0 / segments;
            double a1 = (i + 1) * Math.PI * 2.0 / segments;
            double r0 = angularRadius * (0.82 + hash(seed + i * 3.17) * 0.34);
            double r1 = angularRadius * (0.82 + hash(seed + (i + 1) * 3.17) * 0.34);
            double[] p0 = sphere(radius, az + Math.cos(a0) * r0,
                    clamp(el + Math.sin(a0) * r0 * 0.68, 0.035, 1.53));
            double[] p1 = sphere(radius, az + Math.cos(a1) * r1,
                    clamp(el + Math.sin(a1) * r1 * 0.68, 0.035, 1.53));
            tri(b, center, p0, p1, centerColor, edgeColor, edgeColor);
        }
    }

    private static void drawStars(VertexConsumer b, int count, double time,
                                  double intensity, int[] palette, int style) {
        for (int i = 0; i < count; i++) {
            double seed = i * 41.137 + style * 17.3 + 1.913;
            double baseAz = hash(seed) * Math.PI * 2.0;
            double elBase = 0.055 + Math.pow(hash(seed + 9.1), 0.72) * 1.46;

            // Stars actually travel slowly instead of only twinkling in place.
            double direction = (i & 1) == 0 ? 1.0 : -1.0;
            double az = baseAz + direction * time * (0.0025 + hash(seed + 6.6) * 0.0065);
            double el = clamp(elBase + Math.sin(time * 0.012 + seed) * 0.018, 0.045, 1.52);
            double twinkle = 0.64 + 0.36 * Math.sin(time * (0.38 + hash(seed + 2.2) * 0.50) + seed);
            double rare = hash(seed + 18.7);
            double size = rare > 0.965 ? 1.05 : rare > 0.88 ? 0.55 : 0.20 + hash(seed + 4.4) * 0.20;
            int base = rare > 0.80 ? palette[(int)(hash(seed + 30.0) * palette.length) % palette.length] : 0xFFFFFFFF;
            int alpha = (int)Math.round((130 + 120 * twinkle) * intensity);
            filledStar(b, STAR_RADIUS - hash(seed + 20.0) * 4.0,
                    az, el, size, withAlpha(base, alpha), rare > 0.965);
        }
    }

    /** Distinct moving celestial bodies per preset. */
    private static void drawPlanets(VertexConsumer b, int segments, double time,
                                    double intensity, int[] palette, int style) {
        double orbit = time * 0.005;
        double orbit2 = time * -0.0034;

        switch (style) {
            case 2 -> { // Blue Expanse
                planet(b, PLANET_RADIUS, 0.80 + orbit, 0.72, 0.145, segments,
                        withAlpha(0xFF4A68E8, (int)(225*intensity)), withAlpha(0xFF0A123B, 245));
                ringedPlanet(b, PLANET_RADIUS-2, 4.30 + orbit2, 0.41, 0.085, segments,
                        withAlpha(0xFF7FC5FF, (int)(220*intensity)), withAlpha(0xFF13205C, 245), 0xA090CFFF);
            }
            case 3 -> { // Violet Rift
                planet(b, PLANET_RADIUS, 5.18 + orbit2, 0.68, 0.17, segments,
                        withAlpha(0xFFA250FF, (int)(230*intensity)), withAlpha(0xFF1A083C, 245));
                moon(b, PLANET_RADIUS-3, 1.95 + orbit, 1.02, 0.055, segments, 0xD7D9C4FF);
            }
            case 4 -> { // Crimson Eclipse
                ringedPlanet(b, PLANET_RADIUS, 0.42 + orbit, 0.58, 0.15, segments,
                        withAlpha(0xFFE53B8A, (int)(235*intensity)), withAlpha(0xFF2B071C, 248), 0xB8FF6EC4);
                planet(b, PLANET_RADIUS-4, 3.72 + orbit2, 1.05, 0.058, segments, 0xD5FF91D0, 0xE5380A2B);
            }
            case 5 -> { // Ice Cosmos
                planet(b, PLANET_RADIUS, 4.55 + orbit, 0.80, 0.16, segments,
                        withAlpha(0xFF65D9FF, (int)(230*intensity)), withAlpha(0xFF081D42, 246));
                moon(b, PLANET_RADIUS-3, 1.10 + orbit2, 0.49, 0.067, segments, 0xD8C9F6FF);
            }
            case 6 -> { // Emerald Void
                planet(b, PLANET_RADIUS, 0.94 + orbit2, 0.62, 0.145, segments,
                        withAlpha(0xFF44E6A6, (int)(225*intensity)), withAlpha(0xFF05251D, 248));
                ringedPlanet(b, PLANET_RADIUS-2, 4.95 + orbit, 0.96, 0.070, segments,
                        0xD85DFFB8, 0xE409302D, 0x9A52FFB8);
            }
            case 7 -> { // Event Horizon / black-hole preset
                blackHole(b, PLANET_RADIUS, 5.35 + orbit, 0.62, 0.135, segments, intensity);
                moon(b, PLANET_RADIUS-4, 1.62 + orbit2, 1.02, 0.050, segments, 0xC9A99EFF);
            }
            case 8 -> { // Galactic Spectrum
                ringedPlanet(b, PLANET_RADIUS, 0.58 + orbit, 0.69, 0.155, segments,
                        withAlpha(0xFFFF63D8, (int)(230*intensity)), withAlpha(0xFF1A0B43, 248), 0xB566C8FF);
                planet(b, PLANET_RADIUS-3, 4.65 + orbit2, 1.02, 0.072, segments,
                        0xDD66C8FF, 0xE80B193D);
            }
            default -> { // Nebula Abyss
                planet(b, PLANET_RADIUS, 5.30 + orbit2, 0.71, 0.165, segments,
                        withAlpha(0xFF9A58FF, (int)(232*intensity)), withAlpha(0xFF13072E, 248));
                ringedPlanet(b, PLANET_RADIUS-3, 0.62 + orbit, 1.03, 0.072, segments,
                        0xD8FF67D8, 0xE82B0A45, 0xA7C76DFF);
            }
        }
    }

    private static void planet(VertexConsumer b, double radius, double az, double el, double angularSize,
                               int segments, int lightColor, int darkColor) {
        double[] center = sphere(radius, az, el);
        double[] right = {-Math.sin(az), 0.0, Math.cos(az)};
        double[] up = {-Math.cos(az) * Math.sin(el), Math.cos(el), -Math.sin(az) * Math.sin(el)};
        double size = radius * angularSize;
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2.0 / segments;
            double a1 = (i + 1) * Math.PI * 2.0 / segments;
            double[] p0 = tangentPoint(center, right, up, Math.cos(a0)*size, Math.sin(a0)*size);
            double[] p1 = tangentPoint(center, right, up, Math.cos(a1)*size, Math.sin(a1)*size);
            double light = 0.5 + 0.5 * Math.cos((a0+a1)*0.5 - 2.35);
            int edge = mixRgb(darkColor, lightColor, 0.20 + light*0.65);
            tri(b, center, p0, p1, mixRgb(darkColor, lightColor, 0.74), edge, edge);
        }
        // Thin atmospheric rim.
        ring(b, center, right, up, size*1.055, size*1.055, segments,
                withAlpha(lightColor, Math.max(36, ((lightColor>>>24)&255)/3)));
    }

    private static void moon(VertexConsumer b, double radius, double az, double el, double angularSize,
                             int segments, int color) {
        planet(b, radius, az, el, angularSize, segments, color, mixRgb(color, 0xFF05050A, 0.76));
    }

    private static void ringedPlanet(VertexConsumer b, double radius, double az, double el, double angularSize,
                                     int segments, int lightColor, int darkColor, int ringColor) {
        planet(b, radius, az, el, angularSize, segments, lightColor, darkColor);
        double[] center = sphere(radius - 0.5, az, el);
        double[] right = {-Math.sin(az), 0.0, Math.cos(az)};
        double[] up = {-Math.cos(az) * Math.sin(el), Math.cos(el), -Math.sin(az) * Math.sin(el)};
        double size = radius * angularSize;
        ring(b, center, right, up, size*1.72, size*0.46, segments, ringColor);
        ring(b, center, right, up, size*1.92, size*0.52, segments, withAlpha(ringColor, 84));
    }

    private static void blackHole(VertexConsumer b, double radius, double az, double el,
                                  double angularSize, int segments, double intensity) {
        double[] center = sphere(radius, az, el);
        double[] right = {-Math.sin(az), 0.0, Math.cos(az)};
        double[] up = {-Math.cos(az) * Math.sin(el), Math.cos(el), -Math.sin(az) * Math.sin(el)};
        double size = radius * angularSize;
        // Dark core.
        for (int i=0;i<segments;i++) {
            double a0=i*Math.PI*2/segments,a1=(i+1)*Math.PI*2/segments;
            double[] p0=tangentPoint(center,right,up,Math.cos(a0)*size*.62,Math.sin(a0)*size*.62);
            double[] p1=tangentPoint(center,right,up,Math.cos(a1)*size*.62,Math.sin(a1)*size*.62);
            tri(b,center,p0,p1,0xFC010006,0xF8060110,0xF8060110);
        }
        ring(b,center,right,up,size*1.30,size*.36,segments,withAlpha(0xFFFF63D8,(int)(210*intensity)));
        ring(b,center,right,up,size*1.55,size*.43,segments,withAlpha(0xFF8C5CFF,(int)(120*intensity)));
        ring(b,center,right,up,size*1.78,size*.50,segments,withAlpha(0xFF5E8DFF,(int)(58*intensity)));
    }

    private static void ring(VertexConsumer b, double[] center, double[] right, double[] up,
                             double rx, double ry, int segments, int color) {
        double thickness = Math.max(0.8, Math.min(rx, ry) * 0.055);
        for (int i=0;i<segments;i++) {
            double a0=i*Math.PI*2/segments,a1=(i+1)*Math.PI*2/segments;
            double[] o0=tangentPoint(center,right,up,Math.cos(a0)*rx,Math.sin(a0)*ry);
            double[] o1=tangentPoint(center,right,up,Math.cos(a1)*rx,Math.sin(a1)*ry);
            double[] i1=tangentPoint(center,right,up,Math.cos(a1)*(rx-thickness),Math.sin(a1)*(ry-thickness));
            double[] i0=tangentPoint(center,right,up,Math.cos(a0)*(rx-thickness),Math.sin(a0)*(ry-thickness));
            quad(b,o0,o1,i1,i0,color);
        }
    }

    private static void filledStar(VertexConsumer b, double radius,
                                   double az, double el, double size, int color, boolean large) {
        double[] c = sphere(radius, az, el);
        double[] right = {-Math.sin(az), 0.0, Math.cos(az)};
        double[] up = {-Math.cos(az) * Math.sin(el), Math.cos(el), -Math.sin(az) * Math.sin(el)};
        double sx = size * (large ? 1.55 : 1.0);
        double sy = size * (large ? 0.72 : 1.0);
        double[] left = offset(c, right, -sx), rightP = offset(c, right, sx);
        double[] top = offset(c, up, sy), bottom = offset(c, up, -sy);
        tri(b, top, rightP, bottom, color, color, color);
        tri(b, top, bottom, left, color, color, color);

        if (large) {
            int soft = withAlpha(color, Math.max(18, ((color >>> 24) & 0xFF) / 3));
            double[] left2 = offset(c, right, -sx * 0.48), right2 = offset(c, right, sx * 0.48);
            double[] top2 = offset(c, up, sy * 2.2), bottom2 = offset(c, up, -sy * 2.2);
            tri(b, top2, right2, bottom2, soft, soft, soft);
            tri(b, top2, bottom2, left2, soft, soft, soft);
        }
    }

    private static int domeColor(int style) {
        return switch (style) {
            case 2 -> 0xFF050A24;
            case 3 -> 0xFF120522;
            case 4 -> 0xFF170411;
            case 5 -> 0xFF031326;
            case 6 -> 0xFF031711;
            case 7 -> 0xFF010106;
            case 8 -> 0xFF0D051A;
            default -> 0xFF090319;
        };
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

    private static double[] tangentPoint(double[] center, double[] right, double[] up, double x, double y) {
        return new double[]{center[0] + right[0]*x + up[0]*y,
                center[1] + right[1]*x + up[1]*y,
                center[2] + right[2]*x + up[2]*y};
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
        int alpha=(int)Math.round(((a>>>24)&255)+(((b>>>24)&255)-((a>>>24)&255))*t);
        return (alpha<<24) | (r<<16) | (g<<8) | bl;
    }

    /** RenderLayers.debugQuads uses quads, so duplicate the last triangle vertex into a degenerate quad. */
    private static void tri(VertexConsumer b, double[] a, double[] d, double[] e, int ca, int cd, int ce) {
        vertex(b, a, ca); vertex(b, d, cd); vertex(b, e, ce); vertex(b, e, ce);
    }

    private static void quad(VertexConsumer b, double[] a, double[] d, double[] e, double[] f, int color) {
        vertex(b,a,color); vertex(b,d,color); vertex(b,e,color); vertex(b,f,color);
    }

    private static void vertex(VertexConsumer b, double[] p, int color) {
        b.vertex((float)p[0], (float)p[1], (float)p[2]).color(color);
    }
}
