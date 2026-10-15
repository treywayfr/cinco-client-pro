package gg.cinco.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Traditional through-wall storage ESP: one Minecraft-style wireframe around each storage block. */
public final class StorageEspWorldRenderer {
    private StorageEspWorldRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientModule module = CincoClient.findModule("Storage ESP");
        if (module == null || !module.enabled || client.player == null || client.world == null) return;
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        Vec3d camera = context.camera().getPos();
        double rangeSq = module.setting * module.setting;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(2.0F);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Findings are distance-sorted, so a hard cap preserves the nearest/useful ESP while
        // preventing giant stashes from pushing thousands of line vertices every frame.
        int rendered = 0;
        final int renderCap = 220;
        for (BlockPos pos : EnhancedWorldScan.storage()) {
            if (rendered >= renderCap) break;
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;
            if (client.player.squaredDistanceTo(cx, cy, cz) > rangeSq) continue;

            BlockState state = client.world.getBlockState(pos);
            StorageEspConfig.TypeKey type = StorageEspConfig.typeOf(state);
            if (type == null) continue;
            StorageEspConfig.Entry setting = StorageEspConfig.get(type);
            if (!setting.highlight && !setting.tracer) continue;

            float x0 = (float) (pos.getX() - camera.x - 0.003);
            float y0 = (float) (pos.getY() - camera.y - 0.003);
            float z0 = (float) (pos.getZ() - camera.z - 0.003);
            float x1 = x0 + 1.006F;
            float y1 = y0 + 1.006F;
            float z1 = z0 + 1.006F;

            if (setting.highlight) emitBox(buffer, matrix, x0, y0, z0, x1, y1, z1, setting.color);
            if (setting.tracer) {
                float tx = (float) (cx - camera.x);
                float ty = (float) (cy - camera.y);
                float tz = (float) (cz - camera.z);
                emitLine(buffer, matrix, 0.0F, -0.08F, 0.0F, tx, ty, tz, setting.color);
            }
            rendered++;
        }

        try {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } catch (IllegalStateException ignored) {
            // Empty buffers can occur for one frame while chunks unload.
        }

        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void emitBox(BufferBuilder b, Matrix4f m,
                                float x0, float y0, float z0, float x1, float y1, float z1, int c) {
        emitLine(b,m,x0,y0,z0,x1,y0,z0,c); emitLine(b,m,x1,y0,z0,x1,y0,z1,c);
        emitLine(b,m,x1,y0,z1,x0,y0,z1,c); emitLine(b,m,x0,y0,z1,x0,y0,z0,c);
        emitLine(b,m,x0,y1,z0,x1,y1,z0,c); emitLine(b,m,x1,y1,z0,x1,y1,z1,c);
        emitLine(b,m,x1,y1,z1,x0,y1,z1,c); emitLine(b,m,x0,y1,z1,x0,y1,z0,c);
        emitLine(b,m,x0,y0,z0,x0,y1,z0,c); emitLine(b,m,x1,y0,z0,x1,y1,z0,c);
        emitLine(b,m,x1,y0,z1,x1,y1,z1,c); emitLine(b,m,x0,y0,z1,x0,y1,z1,c);
    }

    private static void emitLine(BufferBuilder b, Matrix4f m,
                                 float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        b.vertex(m, x0, y0, z0).color(color);
        b.vertex(m, x1, y1, z1).color(color);
    }
}
