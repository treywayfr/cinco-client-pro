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

/** Thin Minecraft-style through-wall outlines for storage blocks, with per-type colors/tracers. */
public final class StorageEspWorldRenderer {
    private StorageEspWorldRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientModule module = CincoClient.findModule("Storage ESP");
        if (module == null || !module.enabled || client.player == null || client.world == null) return;
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        int style = style();
        Vec3d camera = context.camera().getPos();
        Vec3d playerEye = client.player.getEyePos();
        float playerX = (float) (playerEye.x - camera.x);
        float playerY = (float) (playerEye.y - camera.y);
        float playerZ = (float) (playerEye.z - camera.z);
        double range = module.setting;
        double rangeSq = range * range;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(switch (style) {
            case 2 -> 2.05F; // Neon
            case 3 -> 1.0F;  // Minimal
            case 4 -> 2.45F; // Bold
            default -> 1.35F; // Clean
        });

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int rendered = 0;
        final int renderCap = StarPerformance.storageRenderCap(client);
        for (BlockPos pos : EnhancedWorldScan.storage()) {
            if (rendered >= renderCap) break;
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;
            double dx = cx - camera.x;
            double dy = cy - camera.y;
            double dz = cz - camera.z;
            double playerDistanceSq = client.player.squaredDistanceTo(cx, cy, cz);
            if (playerDistanceSq > rangeSq) continue;

            BlockState state = client.world.getBlockState(pos);
            StorageEspConfig.TypeKey type = StorageEspConfig.typeOf(state);
            if (type == null) continue;
            StorageEspConfig.Entry setting = StorageEspConfig.get(type);
            if (!setting.highlight && !setting.tracer) continue;

            float x0 = (float) (pos.getX() - camera.x - 0.0025);
            float y0 = (float) (pos.getY() - camera.y - 0.0025);
            float z0 = (float) (pos.getZ() - camera.z - 0.0025);
            float x1 = x0 + 1.005F;
            float y1 = y0 + 1.005F;
            float z1 = z0 + 1.005F;

            int farAlpha = style == 3 ? 70 : style == 2 ? 135 : 105;
            int nearAlpha = style == 3 ? 180 : 245;
            int outline = distanceFade(setting.color, Math.sqrt(playerDistanceSq), range, farAlpha, nearAlpha);
            if (setting.highlight) {
                if (style == 3) emitCornerBox(buffer, matrix, x0, y0, z0, x1, y1, z1, outline, 0.28F);
                else emitBox(buffer, matrix, x0, y0, z0, x1, y1, z1, outline);
            }
            if (setting.tracer) {
                int tracerAlpha = style == 3 ? 82 : style == 2 ? 175 : 135;
                int tracer = withAlpha(setting.color, Math.min(tracerAlpha, (outline >>> 24) & 0xFF));
                emitLine(buffer, matrix, (float) dx, (float) dy, (float) dz,
                        playerX, playerY, playerZ, tracer);
            }
            rendered++;
        }

        try {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } catch (IllegalStateException ignored) {}

        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static int style() {
        ClientModule module = CincoClient.findModule("ESP Style");
        return module == null ? 1 : Math.max(1, Math.min(4, (int) Math.round(module.setting)));
    }

    private static int distanceFade(int color, double distance, double range, int farAlpha, int nearAlpha) {
        double t = range <= 0.001 ? 0.0 : Math.max(0.0, Math.min(1.0, distance / range));
        int alpha = (int) Math.round(nearAlpha + (farAlpha - nearAlpha) * t);
        return withAlpha(color, alpha);
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
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

    private static void emitCornerBox(BufferBuilder b, Matrix4f m,
                                      float x0, float y0, float z0, float x1, float y1, float z1,
                                      int c, float fraction) {
        corner(b,m,x0,y0,z0,x1,y0,z0,c,fraction); corner(b,m,x1,y0,z0,x1,y0,z1,c,fraction);
        corner(b,m,x1,y0,z1,x0,y0,z1,c,fraction); corner(b,m,x0,y0,z1,x0,y0,z0,c,fraction);
        corner(b,m,x0,y1,z0,x1,y1,z0,c,fraction); corner(b,m,x1,y1,z0,x1,y1,z1,c,fraction);
        corner(b,m,x1,y1,z1,x0,y1,z1,c,fraction); corner(b,m,x0,y1,z1,x0,y1,z0,c,fraction);
        corner(b,m,x0,y0,z0,x0,y1,z0,c,fraction); corner(b,m,x1,y0,z0,x1,y1,z0,c,fraction);
        corner(b,m,x1,y0,z1,x1,y1,z1,c,fraction); corner(b,m,x0,y0,z1,x0,y1,z1,c,fraction);
    }

    private static void corner(BufferBuilder b, Matrix4f m, float ax,float ay,float az,float bx,float by,float bz,
                               int c,float f) {
        float dx=bx-ax, dy=by-ay, dz=bz-az;
        emitLine(b,m,ax,ay,az,ax+dx*f,ay+dy*f,az+dz*f,c);
        emitLine(b,m,bx-dx*f,by-dy*f,bz-dz*f,bx,by,bz,c);
    }

    private static void emitLine(BufferBuilder b, Matrix4f m,
                                 float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        b.vertex(m, x0, y0, z0).color(color);
        b.vertex(m, x1, y1, z1).color(color);
    }
}
