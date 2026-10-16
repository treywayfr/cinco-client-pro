package gg.cinco.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

/** Replaces the local third-person body with Star's own articulated Locker model. */
public final class StarPlayerModelWorldRenderer {
    private StarPlayerModelWorldRenderer() {}

    public static void render(WorldRenderContext context){
        MinecraftClient client=MinecraftClient.getInstance();
        ClientModule skin=module();
        if(skin==null||!skin.enabled||client.player==null||client.world==null||context.consumers()==null)return;
        if(client.options.getPerspective().isFirstPerson())return;

        int character=Math.max(1,Math.min(2,(int)Math.round(skin.setting)));
        float tick=client.getRenderTickCounter().getTickProgress(false);
        Vec3d camera=context.worldState().cameraRenderState.pos;
        double px=MathHelper.lerp(tick,client.player.lastX,client.player.getX())-camera.x;
        double py=MathHelper.lerp(tick,client.player.lastY,client.player.getY())-camera.y;
        double pz=MathHelper.lerp(tick,client.player.lastZ,client.player.getZ())-camera.z;

        MatrixStack matrices=new MatrixStack();
        matrices.translate(px,py,pz);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f-client.player.bodyYaw));
        float modelScale=(float)Math.max(.80,Math.min(1.25,skin.setting2));
        matrices.scale(modelScale,modelScale,modelScale);
        if(client.player.isSneaking())matrices.translate(0,-.08,.08);

        float speed=(float)Math.min(1.0,client.player.getVelocity().horizontalLength()*5.8);
        float limbSwing=(client.player.age+tick)*1.45f;
        float headYaw=MathHelper.wrapDegrees(client.player.getYaw()-client.player.bodyYaw);
        float pitch=client.player.getPitch();
        StarPlayerModels.Pose pose=new StarPlayerModels.Pose(limbSwing,speed,headYaw,pitch,client.player.age+tick);
        StarPlayerModels.render(character,matrices,context.consumers(),pose,skin.setting3>=.5);
    }

    public static boolean enabled(){ClientModule m=module();return m!=null&&m.enabled;}
    private static ClientModule module(){ClientModule m=CincoClient.findModule("Player Changer");return m!=null?m:CincoClient.findModule("GTA Skin Changer");}
}
