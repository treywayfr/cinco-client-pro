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

/** Extra close-range clothing layers for the additional Franklin outfit presets. */
public final class GtaExpandedOutfitOverlay {
    private static Identifier texture;
    private GtaExpandedOutfitOverlay() {}

    public static void renderFranklin(AbstractClientPlayerEntity player,float bodyYaw,MatrixStack matrices,VertexConsumerProvider consumers,int light,int outfit){
        if(outfit<5)return;int outer=outfit==5?0xFF171A1F:0xFFE3D7C4,inner=outfit==5?0xFFB7B9BC:0xFF242326,trim=outfit==5?0xFF6A6D73:0xFF937F62;
        matrices.push();matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F-bodyYaw));if(player.isSneaking())matrices.translate(0,-.08,.08);
        VertexConsumer v=consumers.getBuffer(RenderLayers.entitySolid(texture()));MatrixStack.Entry e=matrices.peek();
        box(v,e,-.228f,.93f,-.178f,.228f,1.455f,.176f,outer,light);box(v,e,-.108f,1f,-.186f,.108f,1.415f,-.176f,inner,light);box(v,e,-.222f,1.08f,-.190f,-.112f,1.435f,-.179f,trim,light);box(v,e,.112f,1.08f,-.190f,.222f,1.435f,-.179f,trim,light);box(v,e,-.335f,1.15f,-.118f,-.285f,1.39f,.118f,outer,light);box(v,e,.285f,1.15f,-.118f,.335f,1.39f,.118f,outer,light);box(v,e,-.205f,1.20f,-.194f,-.105f,1.235f,-.183f,shade(outer,.72f),light);box(v,e,-.010f,.96f,-.188f,.010f,1.43f,-.180f,shade(trim,1.12f),light);matrices.pop();
    }

    private static Identifier texture(){if(texture!=null)return texture;NativeImage image=new NativeImage(NativeImage.Format.RGBA,2,2,false);for(int y=0;y<2;y++)for(int x=0;x<2;x++)image.setColor(x,y,0xFFFFFFFF);texture=MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("star_franklin_outfit_white",new NativeImageBackedTexture(()->"star_franklin_outfit_white",image));return texture;}
    private static int shade(int color,float f){int a=(color>>>24)&255,r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;r=Math.max(0,Math.min(255,Math.round(r*f)));g=Math.max(0,Math.min(255,Math.round(g*f)));b=Math.max(0,Math.min(255,Math.round(b*f)));return(a<<24)|(r<<16)|(g<<8)|b;}
    private static void box(VertexConsumer v,MatrixStack.Entry e,float x0,float y0,float z0,float x1,float y1,float z1,int color,int light){float[] a={x0,y0,z0},b={x1,y0,z0},c={x1,y1,z0},d={x0,y1,z0},e0={x0,y0,z1},f={x1,y0,z1},g={x1,y1,z1},h={x0,y1,z1};quad(v,e,a,b,c,d,0,0,-1,color,light);quad(v,e,f,e0,h,g,0,0,1,color,light);quad(v,e,e0,a,d,h,-1,0,0,color,light);quad(v,e,b,f,g,c,1,0,0,color,light);quad(v,e,d,c,g,h,0,1,0,color,light);quad(v,e,e0,f,b,a,0,-1,0,color,light);}
    private static void quad(VertexConsumer v,MatrixStack.Entry e,float[] a,float[] b,float[] c,float[] d,float nx,float ny,float nz,int color,int light){vertex(v,e,a,nx,ny,nz,color,light);vertex(v,e,b,nx,ny,nz,color,light);vertex(v,e,c,nx,ny,nz,color,light);vertex(v,e,d,nx,ny,nz,color,light);}
    private static void vertex(VertexConsumer v,MatrixStack.Entry e,float[] p,float nx,float ny,float nz,int color,int light){v.vertex(e.getPositionMatrix(),p[0],p[1],p[2]).color(color).texture(.5F,.5F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e,nx,ny,nz);}
}
