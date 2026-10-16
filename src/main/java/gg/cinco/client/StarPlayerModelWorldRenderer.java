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

    private static int trackedEntityId=Integer.MIN_VALUE;
    private static float smoothBodyYaw,smoothHeadYaw,smoothPitch,smoothMove;
    private static long lastFrameNanos;

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

        float targetBody=client.player.getBodyYaw();
        float targetHead=MathHelper.lerpAngleDegrees(tick,client.player.lastHeadYaw,client.player.headYaw);
        float targetPitch=MathHelper.lerp(tick,client.player.lastPitch,client.player.getPitch());
        float targetMove=(float)Math.min(1.0,client.player.getVelocity().horizontalLength()*5.8);
        smooth(client.player.getId(),targetBody,targetHead,targetPitch,targetMove);

        MatrixStack matrices=new MatrixStack();
        matrices.translate(px,py,pz);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f-smoothBodyYaw));
        float modelScale=(float)Math.max(.80,Math.min(1.25,skin.setting2));
        matrices.scale(modelScale,modelScale,modelScale);
        if(client.player.isSneaking())matrices.translate(0,-.08,.08);

        float limbSwing=(client.player.age+tick)*1.45f;
        float relativeHead=MathHelper.wrapDegrees(smoothHeadYaw-smoothBodyYaw);
        StarPlayerModels.Pose pose=new StarPlayerModels.Pose(limbSwing,smoothMove,relativeHead,smoothPitch,client.player.age+tick);
        if(character==1){
            // CJ is now the real GTASA-derived rig/texture, not Star's old procedural lookalike.
            GtaCjImportedModel.render(matrices,context.consumers(),pose);
        }else{
            StarPlayerModels.render(character,matrices,context.consumers(),pose,skin.setting3>=.5);
        }
    }

    private static void smooth(int id,float body,float head,float pitch,float move){
        long now=System.nanoTime();
        if(trackedEntityId!=id||lastFrameNanos==0){
            trackedEntityId=id;smoothBodyYaw=body;smoothHeadYaw=head;smoothPitch=pitch;smoothMove=move;lastFrameNanos=now;return;
        }
        float dt=Math.max(.001f,Math.min(.10f,(now-lastFrameNanos)/1_000_000_000f));
        lastFrameNanos=now;
        float turnAlpha=1f-(float)Math.exp(-22f*dt);
        float poseAlpha=1f-(float)Math.exp(-18f*dt);
        smoothBodyYaw=approachAngle(smoothBodyYaw,body,turnAlpha);
        smoothHeadYaw=approachAngle(smoothHeadYaw,head,turnAlpha);
        smoothPitch=MathHelper.lerp(poseAlpha,smoothPitch,pitch);
        smoothMove=MathHelper.lerp(poseAlpha,smoothMove,move);
    }

    private static float approachAngle(float from,float to,float alpha){
        return from+MathHelper.wrapDegrees(to-from)*alpha;
    }

    public static boolean enabled(){ClientModule m=module();return m!=null&&m.enabled;}
    private static ClientModule module(){ClientModule m=CincoClient.findModule("Player Changer");return m!=null?m:CincoClient.findModule("GTA Skin Changer");}
}
