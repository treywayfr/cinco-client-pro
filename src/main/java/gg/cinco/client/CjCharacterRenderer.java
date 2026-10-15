package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** Original low-poly CJ-inspired street character; no GTA game assets are bundled. */
public final class CjCharacterRenderer {
    private static Identifier texture;
    private CjCharacterRenderer() {}

    public static void render(AbstractClientPlayerEntity player,float bodyYaw,float tickDelta,MatrixStack matrices,VertexConsumerProvider consumers,int light,int outfit){
        outfit=Math.max(1,Math.min(6,outfit));int skin=0xFF6F432F,skinHi=0xFF865640,skinDark=0xFF4D2D22,hair=0xFF111111,white=0xFFF2F1EB,gold=0xFFD6B35A;
        int top=switch(outfit){case 2->0xFF2E6F43;case 3->0xFF17191C;case 4->0xFF7A2B2F;case 5->0xFF34704B;case 6->0xFF595A60;default->0xFFF1EFE8;};
        int pants=switch(outfit){case 2->0xFF5B563F;case 3->0xFF161D27;case 6->0xFF24262C;default->0xFF24364D;};int topDark=shade(top,.68f),pantsDark=shade(pants,.70f);
        matrices.push();matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f-bodyYaw));if(player.isSneaking())matrices.translate(0,-.08,.08);
        VertexConsumer v=consumers.getBuffer(RenderLayers.entitySolid(texture()));MatrixStack.Entry e=matrices.peek();double speed=player.getVelocity().horizontalLength();float motion=(float)Math.min(1,speed*4.5),phase=(player.age+tickDelta)*.62f,leg=(float)Math.sin(phase)*.22f*motion,arm=-leg*.88f;
        box(v,e,-.25f,.01f,-.22f+leg,-.015f,.065f,.17f+leg,0xFF17191C,light);box(v,e,.015f,.01f,-.22f-leg,.25f,.065f,.17f-leg,0xFF17191C,light);box(v,e,-.235f,.065f,-.21f+leg,-.025f,.145f,.14f+leg,white,light);box(v,e,.025f,.065f,-.21f-leg,.235f,.145f,.14f-leg,white,light);box(v,e,-.22f,.115f,-.225f+leg,-.04f,.135f,-.202f+leg,0xFFD9D8D3,light);box(v,e,.04f,.115f,-.225f-leg,.22f,.135f,-.202f-leg,0xFFD9D8D3,light);
        box(v,e,-.245f,.15f,-.13f+leg,-.025f,.70f,.14f+leg,pantsDark,light);box(v,e,.025f,.15f,-.13f-leg,.245f,.70f,.14f-leg,pantsDark,light);box(v,e,-.255f,.66f,-.145f,-.018f,1f,.15f,pants,light);box(v,e,.018f,.66f,-.145f,.255f,1f,.15f,pants,light);box(v,e,-.245f,.86f,-.162f,.245f,.92f,.162f,0xFF171719,light);box(v,e,-.045f,.855f,-.170f,.045f,.925f,-.160f,gold,light);
        box(v,e,-.23f,.92f,-.14f,.23f,1.30f,.145f,top,light);box(v,e,-.285f,1.28f,-.145f,.285f,1.48f,.15f,top,light);
        if(outfit==1){box(v,e,-.085f,1.18f,-.154f,.085f,1.49f,-.145f,skin,light);box(v,e,-.22f,1.39f,-.154f,-.115f,1.49f,-.145f,skin,light);box(v,e,.115f,1.39f,-.154f,.22f,1.49f,-.145f,skin,light);}else if(outfit==4||outfit==5){box(v,e,-.10f,1f,-.158f,.10f,1.43f,-.148f,white,light);box(v,e,-.245f,1.02f,-.163f,-.11f,1.44f,-.151f,topDark,light);box(v,e,.11f,1.02f,-.163f,.245f,1.44f,-.151f,topDark,light);}
        box(v,e,-.215f,1.02f,-.160f,-.205f,1.43f,-.150f,topDark,light);box(v,e,.205f,1.02f,-.160f,.215f,1.43f,-.150f,topDark,light);box(v,e,-.19f,1.14f,-.166f,-.08f,1.22f,-.157f,shade(top,.82f),light);box(v,e,-.012f,1.31f,-.170f,.012f,1.43f,-.160f,gold,light);box(v,e,-.045f,1.30f,-.171f,.045f,1.318f,-.160f,gold,light);
        box(v,e,-.39f,1.16f,-.095f+arm,-.285f,1.43f,.095f+arm,outfit==1?skin:top,light);box(v,e,.285f,1.16f,-.095f-arm,.39f,1.43f,.095f-arm,outfit==1?skin:top,light);box(v,e,-.415f,.80f,-.085f+arm,-.305f,1.18f,.085f+arm,skin,light);box(v,e,.305f,.80f,-.085f-arm,.415f,1.18f,.085f-arm,skin,light);box(v,e,-.43f,.72f,-.10f+arm,-.29f,.83f,.10f+arm,skinHi,light);box(v,e,.29f,.72f,-.10f-arm,.43f,.83f,.10f-arm,skinHi,light);
        box(v,e,-.09f,1.46f,-.085f,.09f,1.57f,.085f,skinDark,light);box(v,e,-.16f,1.57f,-.145f,.16f,1.72f,.15f,skin,light);box(v,e,-.19f,1.68f,-.165f,.19f,1.86f,.16f,skin,light);box(v,e,-.17f,1.84f,-.145f,.17f,1.94f,.14f,skinDark,light);box(v,e,-.205f,1.70f,-.06f,-.185f,1.80f,.06f,skinDark,light);box(v,e,.185f,1.70f,-.06f,.205f,1.80f,.06f,skinDark,light);
        box(v,e,-.172f,1.855f,-.148f,.172f,1.955f,.13f,hair,light);box(v,e,-.187f,1.76f,-.12f,-.169f,1.88f,.09f,0xFF2C201C,light);box(v,e,.169f,1.76f,-.12f,.187f,1.88f,.09f,0xFF2C201C,light);box(v,e,-.125f,1.785f,-.176f,-.025f,1.805f,-.165f,hair,light);box(v,e,.025f,1.785f,-.176f,.125f,1.805f,-.165f,hair,light);box(v,e,-.112f,1.748f,-.179f,-.025f,1.770f,-.168f,white,light);box(v,e,.025f,1.748f,-.179f,.112f,1.770f,-.168f,white,light);box(v,e,-.070f,1.750f,-.184f,-.045f,1.771f,-.174f,0xFF201716,light);box(v,e,.045f,1.750f,-.184f,.070f,1.771f,-.174f,0xFF201716,light);box(v,e,-.028f,1.665f,-.205f,.030f,1.742f,-.170f,skinDark,light);box(v,e,-.080f,1.620f,-.183f,.080f,1.642f,-.172f,0xFF5D342D,light);box(v,e,-.055f,1.575f,-.176f,.055f,1.615f,-.166f,0xFF2A1A18,light);matrices.pop();
    }

    private static Identifier texture(){if(texture!=null)return texture;NativeImage image=new NativeImage(NativeImage.Format.RGBA,2,2,false);for(int y=0;y<2;y++)for(int x=0;x<2;x++)image.setColor(x,y,0xFFFFFFFF);texture=MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("star_cj_white",new NativeImageBackedTexture(()->"star_cj_white",image));return texture;}
    private static int shade(int color,float f){int a=(color>>>24)&255,r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;r=Math.max(0,Math.min(255,Math.round(r*f)));g=Math.max(0,Math.min(255,Math.round(g*f)));b=Math.max(0,Math.min(255,Math.round(b*f)));return(a<<24)|(r<<16)|(g<<8)|b;}
    private static void box(VertexConsumer v,MatrixStack.Entry e,float x0,float y0,float z0,float x1,float y1,float z1,int color,int light){float[] a={x0,y0,z0},b={x1,y0,z0},c={x1,y1,z0},d={x0,y1,z0},e0={x0,y0,z1},f={x1,y0,z1},g={x1,y1,z1},h={x0,y1,z1};quad(v,e,a,b,c,d,0,0,-1,color,light);quad(v,e,f,e0,h,g,0,0,1,color,light);quad(v,e,e0,a,d,h,-1,0,0,color,light);quad(v,e,b,f,g,c,1,0,0,color,light);quad(v,e,d,c,g,h,0,1,0,color,light);quad(v,e,e0,f,b,a,0,-1,0,color,light);}
    private static void quad(VertexConsumer v,MatrixStack.Entry e,float[] a,float[] b,float[] c,float[] d,float nx,float ny,float nz,int color,int light){vertex(v,e,a,nx,ny,nz,color,light);vertex(v,e,b,nx,ny,nz,color,light);vertex(v,e,c,nx,ny,nz,color,light);vertex(v,e,d,nx,ny,nz,color,light);}
    private static void vertex(VertexConsumer v,MatrixStack.Entry e,float[] p,float nx,float ny,float nz,int color,int light){v.vertex(e.getPositionMatrix(),p[0],p[1],p[2]).color(color).texture(.5F,.5F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e,nx,ny,nz);}
}
