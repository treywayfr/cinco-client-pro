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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Clean through-wall player and underground-light ESP with shared Star visual presets. */
public final class CleanEspWorldRenderer {
    private static final Map<UUID, Vec3d> SMOOTH_TRACER_TARGETS = new HashMap<>();

    private CleanEspWorldRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        ClientModule playerEsp = CincoClient.findModule("Player ESP");
        ClientModule tracers = CincoClient.findModule("Tracers");
        ClientModule lightFinder = CincoClient.findModule("Light Finder");
        boolean drawPlayers = playerEsp != null && playerEsp.enabled;
        boolean drawTracers = tracers != null && tracers.enabled;
        boolean drawLights = lightFinder != null && lightFinder.enabled;
        if (!drawPlayers && !drawTracers && !drawLights) return;

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = context.camera().getPos();
        Vec3d crosshairAnchor = crosshairAnchor(camera, context.camera().getYaw(), context.camera().getPitch());
        int style = style();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(switch (style) {
            case 2 -> 2.05F;
            case 3 -> 1.00F;
            case 4 -> 2.40F;
            default -> 1.40F;
        });

        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        if (drawPlayers || drawTracers)
            renderPlayers(buffer, matrix, client, camera, crosshairAnchor, playerEsp, tracers, style);
        if (drawLights) renderLights(buffer, matrix, client, camera, lightFinder, style);

        try {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } catch (IllegalStateException ignored) {
            // One-frame empty buffers are normal while changing dimensions or unloading chunks.
        }

        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderPlayers(BufferBuilder b, Matrix4f m, MinecraftClient client, Vec3d camera,
                                      Vec3d crosshairAnchor, ClientModule esp, ClientModule tracers, int style) {
        double range = esp != null && esp.enabled ? esp.setting : 0.0;
        if (tracers != null && tracers.enabled) range = Math.max(range, tracers.setting);
        double rangeSq = range * range;

        int outlineAlpha = switch (style) {
            case 2 -> 250;
            case 3 -> 165;
            case 4 -> 255;
            default -> 225;
        };
        int tracerAlpha = switch (style) {
            case 2 -> 180;
            case 3 -> 85;
            case 4 -> 185;
            default -> 140;
        };
        float cornerFraction = switch (style) {
            case 2 -> 0.33F;
            case 3 -> 0.20F;
            case 4 -> 0.40F;
            default -> 0.27F;
        };
        int accent = withAlpha(CincoTheme.accentLight(), outlineAlpha);
        int tracerColor = withAlpha(CincoTheme.accent(), tracerAlpha);
        int rendered = 0;
        int cap = StarPerformance.active(client) ? 24 : 48;

        // Every tracer shares one small camera-forward endpoint, so the lines converge cleanly on
        // the crosshair rather than chasing the player's interpolated eye position around the screen.
        float anchorX = (float) (crosshairAnchor.x - camera.x);
        float anchorY = (float) (crosshairAnchor.y - camera.y);
        float anchorZ = (float) (crosshairAnchor.z - camera.z);

        for (PlayerEntity player : client.world.getPlayers()) {
            if (rendered >= cap) break;
            if (player == client.player || !player.isAlive()) continue;
            double distanceSq = client.player.squaredDistanceTo(player);
            if (distanceSq > rangeSq) continue;

            if (esp != null && esp.enabled && esp.setting3 >= 0.5) {
                Box box = player.getBoundingBox().expand(style == 4 ? 0.055 : 0.035);
                float x0 = (float) (box.minX - camera.x);
                float y0 = (float) (box.minY - camera.y);
                float z0 = (float) (box.minZ - camera.z);
                float x1 = (float) (box.maxX - camera.x);
                float y1 = (float) (box.maxY - camera.y);
                float z1 = (float) (box.maxZ - camera.z);
                if (style == 4) emitFullBox(b, m, x0, y0, z0, x1, y1, z1, accent);
                else emitCornerBox(b, m, x0, y0, z0, x1, y1, z1, accent, cornerFraction);
            }

            if (tracers != null && tracers.enabled && distanceSq <= tracers.setting * tracers.setting) {
                Vec3d rawTarget = new Vec3d(player.getX(), player.getY() + player.getHeight() * 0.52, player.getZ());
                Vec3d old = SMOOTH_TRACER_TARGETS.get(player.getUuid());
                Vec3d smooth = old == null ? rawTarget : old.lerp(rawTarget, 0.32);
                SMOOTH_TRACER_TARGETS.put(player.getUuid(), smooth);
                float tx = (float) (smooth.x - camera.x);
                float ty = (float) (smooth.y - camera.y);
                float tz = (float) (smooth.z - camera.z);
                emitLine(b, m, tx, ty, tz, anchorX, anchorY, anchorZ, tracerColor);
            }
            rendered++;
        }
    }

    private static Vec3d crosshairAnchor(Vec3d camera, float yawDegrees, float pitchDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double cosPitch = Math.cos(pitch);
        Vec3d forward = new Vec3d(
                -Math.sin(yaw) * cosPitch,
                -Math.sin(pitch),
                Math.cos(yaw) * cosPitch);
        return camera.add(forward.multiply(0.34));
    }

    private static void renderLights(BufferBuilder b, Matrix4f m, MinecraftClient client, Vec3d camera,
                                     ClientModule module, int style) {
        double rangeSq = module.setting * module.setting;
        float sizeScale = (float) Math.max(0.4, Math.min(1.4, module.setting3));
        int rendered = 0;
        int cap = StarPerformance.lightRenderCap(client);
        if (style == 3) cap = Math.max(24, cap * 2 / 3);

        for (BlockPos pos : EnhancedWorldScan.lightHotspots()) {
            if (rendered >= cap) break;
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;
            double dx = cx - client.player.getX();
            double dy = cy - client.player.getY();
            double dz = cz - client.player.getZ();
            if (dx * dx + dy * dy + dz * dz > rangeSq) continue;

            int level = client.world.getLightLevel(LightType.BLOCK, pos);
            int base = level >= 14 ? 0x00FFF7B0 : level >= 11 ? 0x00FFD65C : 0x00FFAA48;
            int alpha = switch (style) {
                case 2 -> 245;
                case 3 -> 155;
                case 4 -> 250;
                default -> 220;
            };
            int color = (alpha << 24) | base;
            float styleScale = switch (style) {
                case 2 -> 1.18F;
                case 3 -> 0.72F;
                case 4 -> 1.30F;
                default -> 1.0F;
            };
            float r = (0.34F + Math.max(0, level - 8) * 0.035F) * sizeScale * styleScale;
            float x = (float) (cx - camera.x);
            float y = (float) (cy - camera.y);
            float z = (float) (cz - camera.z);
            if (style == 3) emitMinimalLight(b, m, x, y, z, r, color);
            else emitLightHalo(b, m, x, y, z, r, color, style == 2 || style == 4);
            rendered++;
        }
    }

    private static void emitMinimalLight(BufferBuilder b, Matrix4f m, float x, float y, float z, float r, int c) {
        emitLine(b,m,x-r,y,z,x+r,y,z,c);
        emitLine(b,m,x,y-r,z,x,y+r,z,c);
        emitLine(b,m,x,y,z-r,x,y,z+r,c);
    }

    private static void emitLightHalo(BufferBuilder b, Matrix4f m, float x, float y, float z,
                                      float r, int c, boolean extraRing) {
        float topY = y + r;
        float bottomY = y - r;
        float xr = x + r, xl = x - r, zf = z + r, zb = z - r;
        emitLine(b,m,x,topY,z,xr,y,z,c); emitLine(b,m,x,topY,z,xl,y,z,c);
        emitLine(b,m,x,topY,z,x,y,zf,c); emitLine(b,m,x,topY,z,x,y,zb,c);
        emitLine(b,m,x,bottomY,z,xr,y,z,c); emitLine(b,m,x,bottomY,z,xl,y,z,c);
        emitLine(b,m,x,bottomY,z,x,y,zf,c); emitLine(b,m,x,bottomY,z,x,y,zb,c);
        emitLine(b,m,xr,y,z,x,y,zf,c); emitLine(b,m,x,y,zf,xl,y,z,c);
        emitLine(b,m,xl,y,z,x,y,zb,c); emitLine(b,m,x,y,zb,xr,y,z,c);
        int faint = withAlpha(c, extraRing ? 125 : 92);
        emitLine(b,m,x-r*0.38F,y,z,x+r*0.38F,y,z,faint);
        emitLine(b,m,x,y-r*0.38F,z,x,y+r*0.38F,z,faint);
        emitLine(b,m,x,y,z-r*0.38F,x,y,z+r*0.38F,faint);
        if (extraRing) {
            float rr = r * 0.70F;
            emitLine(b,m,x-rr,y-rr*0.2F,z,x+rr,y-rr*0.2F,z,faint);
            emitLine(b,m,x+rr,y-rr*0.2F,z,x+rr,y+rr*0.2F,z,faint);
            emitLine(b,m,x+rr,y+rr*0.2F,z,x-rr,y+rr*0.2F,z,faint);
            emitLine(b,m,x-rr,y+rr*0.2F,z,x-rr,y-rr*0.2F,z,faint);
        }
    }

    private static void emitCornerBox(BufferBuilder b, Matrix4f m,
                                      float x0, float y0, float z0, float x1, float y1, float z1,
                                      int c, float fraction) {
        cornerEdge(b,m,x0,y0,z0,x1,y0,z0,c,fraction); cornerEdge(b,m,x1,y0,z0,x1,y0,z1,c,fraction);
        cornerEdge(b,m,x1,y0,z1,x0,y0,z1,c,fraction); cornerEdge(b,m,x0,y0,z1,x0,y0,z0,c,fraction);
        cornerEdge(b,m,x0,y1,z0,x1,y1,z0,c,fraction); cornerEdge(b,m,x1,y1,z0,x1,y1,z1,c,fraction);
        cornerEdge(b,m,x1,y1,z1,x0,y1,z1,c,fraction); cornerEdge(b,m,x0,y1,z1,x0,y1,z0,c,fraction);
        cornerEdge(b,m,x0,y0,z0,x0,y1,z0,c,fraction); cornerEdge(b,m,x1,y0,z0,x1,y1,z0,c,fraction);
        cornerEdge(b,m,x1,y0,z1,x1,y1,z1,c,fraction); cornerEdge(b,m,x0,y0,z1,x0,y1,z1,c,fraction);
    }

    private static void emitFullBox(BufferBuilder b, Matrix4f m,
                                    float x0, float y0, float z0, float x1, float y1, float z1, int c) {
        emitLine(b,m,x0,y0,z0,x1,y0,z0,c); emitLine(b,m,x1,y0,z0,x1,y0,z1,c);
        emitLine(b,m,x1,y0,z1,x0,y0,z1,c); emitLine(b,m,x0,y0,z1,x0,y0,z0,c);
        emitLine(b,m,x0,y1,z0,x1,y1,z0,c); emitLine(b,m,x1,y1,z0,x1,y1,z1,c);
        emitLine(b,m,x1,y1,z1,x0,y1,z1,c); emitLine(b,m,x0,y1,z1,x0,y1,z0,c);
        emitLine(b,m,x0,y0,z0,x0,y1,z0,c); emitLine(b,m,x1,y0,z0,x1,y1,z0,c);
        emitLine(b,m,x1,y0,z1,x1,y1,z1,c); emitLine(b,m,x0,y0,z1,x0,y1,z1,c);
    }

    private static void cornerEdge(BufferBuilder b, Matrix4f m,
                                   float ax, float ay, float az, float bx, float by, float bz,
                                   int color, float fraction) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        emitLine(b,m,ax,ay,az,ax+dx*fraction,ay+dy*fraction,az+dz*fraction,color);
        emitLine(b,m,bx-dx*fraction,by-dy*fraction,bz-dz*fraction,bx,by,bz,color);
    }

    private static int style() {
        ClientModule module = CincoClient.findModule("ESP Style");
        return module == null ? 1 : Math.max(1, Math.min(4, (int) Math.round(module.setting)));
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static void emitLine(BufferBuilder b, Matrix4f m,
                                 float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        b.vertex(m, x0, y0, z0).color(color);
        b.vertex(m, x1, y1, z1).color(color);
    }
}
