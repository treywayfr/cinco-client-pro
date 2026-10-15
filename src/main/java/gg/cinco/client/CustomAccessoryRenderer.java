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

/** Local-only procedural accessories for the player's model. */
public final class CustomAccessoryRenderer {
    private CustomAccessoryRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientModule module = CincoClient.findModule("Custom Accessory");
        if (module == null || !module.enabled || client.player == null || client.world == null) return;
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        int style = Math.max(1, Math.min(8, (int)Math.round(module.setting)));
        double size = Math.max(0.60, Math.min(1.80, module.setting2));
        double motion = Math.max(0.0, Math.min(2.50, module.setting3));
        double t = System.currentTimeMillis() / 1000.0 * motion;
        int tier = StarPerformance.animationTier(client);

        Vec3d cam = context.camera().getPos();
        double x = client.player.getX() - cam.x;
        double y = client.player.getY() - cam.y;
        double z = client.player.getZ() - cam.z;
        double yaw = Math.toRadians(client.player.getBodyYaw());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(tier == 0 ? 1.4F : 2.0F);

        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f m = matrices.peek().getPositionMatrix();
        int bright = color(style, 235);
        int dim = color(style, 92);

        switch (style) {
            case 2 -> demonWings(b, m, x, y, z, yaw, size, t, bright, dim);
            case 3 -> halo(b, m, x, y, z, yaw, size, t, bright, tier);
            case 4 -> crown(b, m, x, y, z, yaw, size, t, bright);
            case 5 -> orbitStars(b, m, x, y, z, yaw, size, t, bright, tier);
            case 6 -> horns(b, m, x, y, z, yaw, size, t, bright);
            case 7 -> energyRing(b, m, x, y, z, yaw, size, t, bright, dim, tier);
            case 8 -> cape(b, m, x, y, z, yaw, size, t, bright, dim, tier);
            default -> angelWings(b, m, x, y, z, yaw, size, t, bright, dim);
        }

        try { BufferRenderer.drawWithGlobalProgram(b.end()); }
        catch (IllegalStateException ignored) {}

        RenderSystem.lineWidth(1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void angelWings(BufferBuilder b, Matrix4f m, double x, double y, double z, double yaw,
                                   double s, double t, int c, int dim) {
        double flap = Math.sin(t * 2.0) * 0.09;
        for (int side : new int[]{-1, 1}) {
            double[][] spine = {
                    {0.18 * side, 1.45, 0.10}, {0.52 * side, 1.55 + flap, 0.18},
                    {0.90 * side, 1.40 + flap, 0.24}, {1.15 * side, 1.05, 0.30},
                    {0.92 * side, 0.72, 0.27}, {0.48 * side, 0.90, 0.18}, {0.18 * side, 1.20, 0.10}
            };
            polyLocal(b,m,x,y,z,yaw,spine,s,c,true);
            for (int i = 0; i < 5; i++) {
                double sy = 1.36 - i * 0.13;
                pLine(b,m,x,y,z,yaw,0.34*side,sy,0.16,(0.93-i*0.08)*side,sy-0.18,0.26,s,dim);
            }
        }
    }

    private static void demonWings(BufferBuilder b, Matrix4f m, double x, double y, double z, double yaw,
                                   double s, double t, int c, int dim) {
        double flap = Math.sin(t * 2.5) * 0.07;
        for (int side : new int[]{-1,1}) {
            double[][] outline = {
                    {0.17*side,1.42,0.12},{0.58*side,1.62+flap,0.20},{1.18*side,1.48,0.30},
                    {0.86*side,1.13,0.25},{1.10*side,0.78,0.32},{0.60*side,0.88,0.20},
                    {0.18*side,1.16,0.10}
            };
            polyLocal(b,m,x,y,z,yaw,outline,s,c,true);
            pLine(b,m,x,y,z,yaw,0.30*side,1.35,0.14,1.18*side,1.48,0.30,s,dim);
            pLine(b,m,x,y,z,yaw,0.35*side,1.26,0.14,1.10*side,0.78,0.32,s,dim);
        }
    }

    private static void halo(BufferBuilder b, Matrix4f m, double x, double y, double z, double yaw,
                             double s, double t, int c, int tier) {
        int seg = tier == 0 ? 24 : 42;
        double cy = 2.08 + Math.sin(t * 1.4) * 0.025;
        ellipseLocal(b,m,x,y,z,yaw,0.35,cy,0.0,0.35,0.16,seg,s,c);
    }

    private static void crown(BufferBuilder b, Matrix4f m, double x, double y, double z, double yaw,
                              double s, double t, int c) {
        double yy = 1.98 + Math.sin(t * 1.3) * 0.02;
        double[][] pts = {
                {-0.26,yy,0.0},{-0.18,yy+0.25,0.0},{-0.07,yy+0.08,0.0},
                {0.0,yy+0.31,0.0},{0.07,yy+0.08,0.0},{0.18,yy+0.25,0.0},{0.26,yy,0.0}
        };
        polyLocal(b,m,x,y,z,yaw,pts,s,c,false);
        pLine(b,m,x,y,z,yaw,-0.26,yy,0.0,0.26,yy,0.0,s,c);
    }

    private static void orbitStars(BufferBuilder b, Matrix4f m, double x, double y, double z, double yaw,
                                   double s, double t, int c, int tier) {
        int count = tier == 0 ? 5 : 8;
        for (int i=0;i<count;i++) {
            double a = t * 0.8 + i * Math.PI * 2.0 / count;
            double lx = Math.cos(a) * 0.62;
            double lz = Math.sin(a) * 0.34;
            double ly = 0.58 + (i % 4) * 0.34 + Math.sin(t*1.7+i)*0.05;
            starLocal(b,m,x,y,z,yaw,lx,ly,lz,0.07,s,c);
        }
    }

    private static void horns(BufferBuilder b, Matrix4f m, double x, double y, double z, double yaw,
                              double s, double t, int c) {
        for (int side : new int[]{-1,1}) {
            double wobble = Math.sin(t*1.6 + side) * 0.015;
            double[][] horn = {
                    {0.13*side,1.94,0.0},{0.20*side,2.08+wobble,0.01},
                    {0.28*side,2.18+wobble,0.02},{0.24*side,2.30,0.03},{0.15*side,2.36,0.04}
            };
            polyLocal(b,m,x,y,z,yaw,horn,s,c,false);
        }
    }

    private static void energyRing(BufferBuilder b, Matrix4f m, double x, double y, double z, double yaw,
                                   double s, double t, int c, int dim, int tier) {
        int seg = tier == 0 ? 28 : 48;
        double pulse = 0.70 + Math.sin(t*2.0)*0.05;
        verticalEllipseLocal(b,m,x,y,z,yaw,0.0,0.95,0.0,pulse,0.98,seg,s,c,t*0.35);
        verticalEllipseLocal(b,m,x,y,z,yaw,0.0,0.95,0.0,pulse*0.82,0.78,seg,s,dim,-t*0.25);
    }

    private static void cape(BufferBuilder b, Matrix4f m, double x, double y, double z, double yaw,
                             double s, double t, int c, int dim, int tier) {
        int strips = tier == 0 ? 4 : 7;
        for (int i=0;i<strips;i++) {
            double lx = -0.28 + i * (0.56 / Math.max(1, strips-1));
            double wave = Math.sin(t*2.0 + i*0.8) * 0.07;
            pLine(b,m,x,y,z,yaw,lx,1.50,0.16,lx,0.55,0.42+wave,s,i%2==0?c:dim);
        }
        pLine(b,m,x,y,z,yaw,-0.30,1.50,0.16,0.30,1.50,0.16,s,c);
        pLine(b,m,x,y,z,yaw,-0.30,0.55,0.42,0.30,0.55,0.42,s,dim);
    }

    private static int color(int style, int alpha) {
        int rgb = switch (style) {
            case 1 -> 0xF2F4FF;
            case 2,6,7,8 -> 0xA45CFF;
            case 3 -> 0xFFF2B7;
            case 4 -> 0xFFD85F;
            case 5 -> 0xB984FF;
            default -> 0xA45CFF;
        };
        return (Math.max(0,Math.min(255,alpha))<<24) | rgb;
    }

    private static void ellipseLocal(BufferBuilder b, Matrix4f m, double x,double y,double z,double yaw,
                                     double cx,double cy,double cz,double rx,double rz,int seg,double s,int c) {
        double[] prev = world(x,y,z,yaw,cx+rx,cy,cz,s);
        for(int i=1;i<=seg;i++) {
            double a=i*Math.PI*2.0/seg;
            double[] next=world(x,y,z,yaw,cx+Math.cos(a)*rx,cy,cz+Math.sin(a)*rz,s);
            line(b,m,prev,next,c); prev=next;
        }
    }

    private static void verticalEllipseLocal(BufferBuilder b, Matrix4f m, double x,double y,double z,double yaw,
                                             double cx,double cy,double cz,double rx,double ry,int seg,double s,int c,double rot) {
        double[] prev = world(x,y,z,yaw,cx+Math.cos(rot)*rx,cy,cz+Math.sin(rot)*rx,s);
        for(int i=1;i<=seg;i++) {
            double a=i*Math.PI*2.0/seg;
            double h=Math.cos(a)*rx;
            double[] next=world(x,y,z,yaw,cx+Math.cos(rot)*h,cy+Math.sin(a)*ry,cz+Math.sin(rot)*h,s);
            line(b,m,prev,next,c); prev=next;
        }
    }

    private static void starLocal(BufferBuilder b, Matrix4f m,double x,double y,double z,double yaw,
                                  double lx,double ly,double lz,double r,double s,int c) {
        pLine(b,m,x,y,z,yaw,lx-r,ly,lz,lx+r,ly,lz,s,c);
        pLine(b,m,x,y,z,yaw,lx,ly-r,lz,lx,ly+r,lz,s,c);
        pLine(b,m,x,y,z,yaw,lx,ly,lz-r,lx,ly,lz+r,s,c);
    }

    private static void polyLocal(BufferBuilder b, Matrix4f m,double x,double y,double z,double yaw,
                                  double[][] pts,double s,int c,boolean close) {
        for(int i=1;i<pts.length;i++) {
            pLine(b,m,x,y,z,yaw,pts[i-1][0],pts[i-1][1],pts[i-1][2],pts[i][0],pts[i][1],pts[i][2],s,c);
        }
        if(close && pts.length>2) {
            double[] a=pts[pts.length-1], d=pts[0];
            pLine(b,m,x,y,z,yaw,a[0],a[1],a[2],d[0],d[1],d[2],s,c);
        }
    }

    private static void pLine(BufferBuilder b, Matrix4f m,double x,double y,double z,double yaw,
                              double ax,double ay,double az,double bx,double by,double bz,double s,int c) {
        line(b,m,world(x,y,z,yaw,ax,ay,az,s),world(x,y,z,yaw,bx,by,bz,s),c);
    }

    private static double[] world(double x,double y,double z,double yaw,double lx,double ly,double lz,double s) {
        lx*=s; ly*=s; lz*=s;
        double rightX=Math.cos(yaw), rightZ=Math.sin(yaw);
        double forwardX=-Math.sin(yaw), forwardZ=Math.cos(yaw);
        return new double[]{x + rightX*lx + forwardX*lz, y + ly, z + rightZ*lx + forwardZ*lz};
    }

    private static void line(BufferBuilder b, Matrix4f m,double[] a,double[] d,int c) {
        b.vertex(m,(float)a[0],(float)a[1],(float)a[2]).color(c);
        b.vertex(m,(float)d[0],(float)d[1],(float)d[2]).color(c);
    }
}
