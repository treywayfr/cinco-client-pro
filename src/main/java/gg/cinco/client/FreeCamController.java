package gg.cinco.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Perspective-only detached camera. The real network player is never teleported by Free Cam. */
public final class FreeCamController {
    private static OtherClientPlayerEntity camera;
    private static ClientWorld activeWorld;
    private static boolean active;
    private static float savedYaw,savedPitch,freeYaw,freePitch;
    private FreeCamController(){}
    public static boolean isActive(){return active;}

    public static void tick(MinecraftClient client){
        ClientModule module=CincoClient.findModule("Free Cam");boolean wanted=module!=null&&module.enabled;
        if(client==null||client.player==null||client.world==null||!wanted){disable(client);return;}
        if(!active||camera==null||activeWorld!=client.world)enable(client);if(!active||camera==null)return;
        client.player.setYaw(savedYaw);client.player.setPitch(savedPitch);client.player.setHeadYaw(savedYaw);client.player.setVelocity(0,0,0);
        camera.noClip=true;camera.setNoGravity(true);camera.setVelocity(0,0,0);camera.setYaw(freeYaw);camera.setPitch(freePitch);camera.setHeadYaw(freeYaw);
        if(client.getCameraEntity()!=camera)client.setCameraEntity(camera);if(client.currentScreen!=null||client.getWindow()==null)return;

        long window=client.getWindow().getHandle();double base=Math.max(0.05,module.setting)*0.24;
        boolean sprint=down(window,GLFW.GLFW_KEY_LEFT_CONTROL)||down(window,GLFW.GLFW_KEY_RIGHT_CONTROL);double speed=sprint?base*3.0:base;
        double forward=(down(window,GLFW.GLFW_KEY_W)?1:0)-(down(window,GLFW.GLFW_KEY_S)?1:0);
        double strafe=(down(window,GLFW.GLFW_KEY_D)?1:0)-(down(window,GLFW.GLFW_KEY_A)?1:0);
        double vertical=(down(window,GLFW.GLFW_KEY_SPACE)?1:0)-((down(window,GLFW.GLFW_KEY_LEFT_SHIFT)||down(window,GLFW.GLFW_KEY_RIGHT_SHIFT))?1:0);
        double yaw=Math.toRadians(freeYaw),dx=-Math.sin(yaw)*forward+Math.cos(yaw)*strafe,dz=Math.cos(yaw)*forward+Math.sin(yaw)*strafe,h=Math.hypot(dx,dz);
        if(h>0.0001){dx/=h;dz/=h;}double len=Math.sqrt(dx*dx+dz*dz+vertical*vertical);if(len>1){dx/=len;dz/=len;vertical/=len;}

        // 1.21.11 uses last* interpolation fields instead of prev* position fields.
        camera.lastX=camera.getX();camera.lastY=camera.getY();camera.lastZ=camera.getZ();camera.lastYaw=camera.getYaw();camera.lastPitch=camera.getPitch();
        camera.setPosition(camera.getX()+dx*speed,camera.getY()+vertical*speed,camera.getZ()+dz*speed);
    }

    private static boolean down(long window,int key){return GLFW.glfwGetKey(window,key)==GLFW.GLFW_PRESS;}

    public static void syncMouseLook(MinecraftClient client){
        if(!active||camera==null||client==null||client.player==null)return;
        float cameraYaw=camera.getYaw(),cameraPitch=camera.getPitch(),yawDelta=MathHelper.wrapDegrees(cameraYaw-freeYaw),pitchDelta=cameraPitch-freePitch;
        if(Math.abs(yawDelta)>0.00001F||Math.abs(pitchDelta)>0.00001F){freeYaw=MathHelper.wrapDegrees(cameraYaw);freePitch=MathHelper.clamp(cameraPitch,-90,90);}
        else{float playerYawDelta=MathHelper.wrapDegrees(client.player.getYaw()-savedYaw),playerPitchDelta=client.player.getPitch()-savedPitch;if(Math.abs(playerYawDelta)>0.00001F||Math.abs(playerPitchDelta)>0.00001F){freeYaw=MathHelper.wrapDegrees(freeYaw+playerYawDelta);freePitch=MathHelper.clamp(freePitch+playerPitchDelta,-90,90);camera.setYaw(freeYaw);camera.setPitch(freePitch);camera.setHeadYaw(freeYaw);}}
        client.player.setYaw(savedYaw);client.player.setPitch(savedPitch);client.player.setHeadYaw(savedYaw);
    }

    private static void enable(MinecraftClient client){
        if(client.player==null||client.world==null)return;if(active)disable(client);activeWorld=client.world;savedYaw=client.player.getYaw();savedPitch=client.player.getPitch();freeYaw=savedYaw;freePitch=savedPitch;
        UUID id=UUID.nameUUIDFromBytes("star-free-camera".getBytes(StandardCharsets.UTF_8));camera=new OtherClientPlayerEntity(client.world,new GameProfile(id,"StarFreeCam"));
        camera.setPosition(client.player.getX(),client.player.getY(),client.player.getZ());camera.lastX=camera.getX();camera.lastY=camera.getY();camera.lastZ=camera.getZ();camera.setInvisible(true);camera.setNoGravity(true);camera.noClip=true;camera.lastYaw=freeYaw;camera.lastPitch=freePitch;camera.setYaw(freeYaw);camera.setPitch(freePitch);camera.setHeadYaw(freeYaw);client.setCameraEntity(camera);active=true;
    }

    public static void disable(MinecraftClient client){if(!active)return;if(client!=null&&client.player!=null){client.setCameraEntity(client.player);client.player.setYaw(savedYaw);client.player.setPitch(savedPitch);client.player.setHeadYaw(savedYaw);client.player.setVelocity(0,0,0);}camera=null;activeWorld=null;active=false;}
}
