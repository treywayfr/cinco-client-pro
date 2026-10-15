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

/**
 * Clean through-wall world ESP inspired by modern minimal clients: thin corner boxes for players,
 * restrained world tracers, and compact 3D light halos instead of noisy screen-space nameplates.
 */
public final class CleanEspWorldRenderer {
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

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(1.45F);

        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        if (drawPlayers || drawTracers) renderPlayers(buffer, matrix, client, camera, playerEsp, tracers);
        if (drawLights) renderLights(buffer, matrix, client, camera, lightFinder);

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
                                      ClientModule esp, ClientModule tracers) {
        double range = esp != null && esp.enabled ? esp.setting : 0.0;
        if (tracers != null && tracers.enabled) range = Math.max(range, tracers.setting);
        double rangeSq = range * range;
        int accent = withAlpha(CincoTheme.accentLight(), 225);
        int tracerColor = withAlpha(CincoTheme.accent(), 150);
        int rendered = 0;

        for (PlayerEntity player : client.world.getPlayers()) {
            if (rendered >= 48) break;
            if (player == client.player || !player.isAlive()) continue;
            double distanceSq = client.player.squaredDistanceTo(player);
            if (distanceSq > rangeSq) continue;

            if (esp != null && esp.enabled && esp.setting3 >= 0.5) {
                Box box = player.getBoundingBox().expand(0.035);
                float x0 = (float) (box.minX - camera.x);
                float y0 = (float) (box.minY - camera.y);
                float z0 = (float) (box.minZ - camera.z);
                float x1 = (float) (box.maxX - camera.x);
                float y1 = (float) (box.maxY - camera.y);
                float z1 = (float) (box.maxZ - camera.z);
                emitCornerBox(b, m, x0, y0, z0, x1, y1, z1, accent, 0.27F);
            }

            if (tracers != null && tracers.enabled && distanceSq <= tracers.setting * tracers.setting) {
                float tx = (float) (player.getX() - camera.x);
                float ty = (float) (player.getY() + player.getHeight() * 0.52 - camera.y);
                float tz = (float) (player.getZ() - camera.z);
                emitLine(b, m, 0.0F, -0.05F, 0.0F, tx, ty, tz, tracerColor);
            }
            rendered++;
        }
    }

    private static void renderLights(BufferBuilder b, Matrix4f m, MinecraftClient client, Vec3d camera,
                                     ClientModule module) {
        double rangeSq = module.setting * module.setting;
        float sizeScale = (float) Math.max(0.4, Math.min(1.4, module.setting3));
        int rendered = 0;

        for (BlockPos pos : EnhancedWorldScan.lightHotspots()) {
            if (rendered >= 72) break;
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;
            double dx = cx - client.player.getX();
            double dy = cy - client.player.getY();
            double dz = cz - client.player.getZ();
            if (dx * dx + dy * dy + dz * dz > rangeSq) continue;

            int level = client.world.getLightLevel(LightType.BLOCK, pos);
            int color = level >= 14 ? 0xEFFFF7B0 : level >= 11 ? 0xE8FFD65C : 0xDAFFAA48;
            float r = (0.34F + Math.max(0, level - 8) * 0.035F) * sizeScale;
            float x = (float) (cx - camera.x);
            float y = (float) (cy - camera.y);
            float z = (float) (cz - camera.z);
            emitLightHalo(b, m, x, y, z, r, color);
            rendered++;
        }
    }

    private static void emitLightHalo(BufferBuilder b, Matrix4f m, float x, float y, float z, float r, int c) {
        // Octahedral halo: readable through walls without looking like a giant block/name tag.
        float topY = y + r;
        float bottomY = y - r;
        float xr = x + r, xl = x - r, zf = z + r, zb = z - r;
        emitLine(b,m,x,topY,z,xr,y,z,c); emitLine(b,m,x,topY,z,xl,y,z,c);
        emitLine(b,m,x,topY,z,x,y,zf,c); emitLine(b,m,x,topY,z,x,y,zb,c);
        emitLine(b,m,x,bottomY,z,xr,y,z,c); emitLine(b,m,x,bottomY,z,xl,y,z,c);
        emitLine(b,m,x,bottomY,z,x,y,zf,c); emitLine(b,m,x,bottomY,z,x,y,zb,c);
        emitLine(b,m,xr,y,z,x,y,zf,c); emitLine(b,m,x,y,zf,xl,y,z,c);
        emitLine(b,m,xl,y,z,x,y,zb,c); emitLine(b,m,x,y,zb,xr,y,z,c);
        int faint = withAlpha(c, 105);
        emitLine(b,m,x-r*0.38F,y,z,x+r*0.38F,y,z,faint);
        emitLine(b,m,x,y-r*0.38F,z,x,y+r*0.38F,z,faint);
        emitLine(b,m,x,y,z-r*0.38F,x,y,z+r*0.38F,faint);
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

    private static void cornerEdge(BufferBuilder b, Matrix4f m,
                                   float ax, float ay, float az, float bx, float by, float bz,
                                   int color, float fraction) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        emitLine(b,m,ax,ay,az,ax+dx*fraction,ay+dy*fraction,az+dz*fraction,color);
        emitLine(b,m,bx-dx*fraction,by-dy*fraction,bz-dz*fraction,bx,by,bz,color);
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
