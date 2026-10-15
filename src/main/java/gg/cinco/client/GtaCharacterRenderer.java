package gg.cinco.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Textureless procedural local character presets for Minecraft 1.21.11. */
public final class GtaCharacterRenderer {
    private GtaCharacterRenderer() {}

    public static void render(AbstractClientPlayerEntity player,float bodyYaw,float tickDelta,MatrixStack matrices,VertexConsumerProvider consumers,int light){
        ClientModule module=CincoClient.findModule("Player Changer");if(module==null)module=CincoClient.findModule("GTA Skin Changer");if(module==null||!module.enabled)return;
        int character=Math.max(1,Math.min(4,(int)Math.round(module.setting))),palette=Math.max(1,Math.min(6,(int)Math.round(module.setting2)));
        matrices.push();matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f-bodyYaw));if(player.isSneaking())matrices.translate(0,-.08,.08);
        StarCharacterGeometry.Drawer d=StarCharacterGeometry.begin(matrices,consumers);
        double speed=player.getVelocity().horizontalLength();float move=(float)Math.min(1,speed*4.5),phase=(player.age+tickDelta)*.62f,leg=(float)Math.sin(phase)*.11f*move,arm=-leg*.85f;
        switch(character){case 2->spiky(d,leg,arm);case 3->anime(d,leg,arm,palette);case 4->wood(d,leg,arm);default->franklin(d,leg,arm,palette);}matrices.pop();
    }

    private static void franklin(StarCharacterGeometry.Drawer d,float leg,float arm,int palette){
        int skin=0xFF835136,hair=0xFF151211,white=0xFFF4F1E9,shirt=switch(palette){case 2->0xFF202326;case 3->0xFF214B72;case 4->0xFF742E2A;case 5->0xFF171A1F;case 6->0xFFE3D7C4;default->0xFF2C784A;},pants=0xFF27364A;
        legs(d,leg,pants,0xFF17191C);torso(d,shirt);arms(d,arm,skin,shirt);head(d,skin,hair);
        d.box(-.10f,1.17f,-.205f,.10f,1.40f,-.185f,white);d.box(-.025f,.87f,-.17f,.025f,.93f,-.155f,0xFFD6AD52);
    }
    private static void spiky(StarCharacterGeometry.Drawer d,float leg,float arm){int yellow=0xFFFFD735,red=0xFFF05B3D,blue=0xFF315EBC;legs(d,leg,blue,0xFFFFFFFF);torso(d,red);arms(d,arm,yellow,red);head(d,yellow,0xFFE6B61E);for(int i=0;i<7;i++){float x=-.20f+i*.065f;d.box(x,1.90f,-.08f,x+.045f,2.05f+(i%2)*.05f,.06f,yellow);}}
    private static void anime(StarCharacterGeometry.Drawer d,float leg,float arm,int palette){int skin=0xFFF0C6B2,hair=switch(palette){case 2->0xFF18191D;case 3->0xFF6A347F;case 4->0xFFB66175;default->0xFF2C2548;},top=0xFFF3EEF8,skirt=0xFF1B2032;legs(d,leg,0xFF323648,0xFF2A2230);torso(d,top);arms(d,arm,skin,top);head(d,skin,hair);d.box(-.28f,.82f,-.16f,.28f,1.02f,.16f,skirt);d.box(-.24f,1.57f,.10f,-.18f,1.94f,.17f,hair);d.box(.18f,1.57f,.10f,.24f,1.94f,.17f,hair);}
    private static void wood(StarCharacterGeometry.Drawer d,float leg,float arm){int wood=0xFF9A6236,light=0xFFC18149,dark=0xFF5F351D;legs(d,leg,dark,0xFF3E2415);torso(d,wood);arms(d,arm,wood,wood);head(d,light,dark);d.box(-.13f,1.67f,-.205f,-.04f,1.74f,-.19f,0xFFEBD9B9);d.box(.04f,1.67f,-.205f,.13f,1.74f,-.19f,0xFFEBD9B9);}

    private static void legs(StarCharacterGeometry.Drawer d,float leg,int pants,int shoe){d.box(-.24f,.10f,-.13f+leg,-.02f,.92f,.14f+leg,pants);d.box(.02f,.10f,-.13f-leg,.24f,.92f,.14f-leg,pants);d.box(-.25f,.01f,-.23f+leg,-.01f,.12f,.17f+leg,shoe);d.box(.01f,.01f,-.23f-leg,.25f,.12f,.17f-leg,shoe);}
    private static void torso(StarCharacterGeometry.Drawer d,int color){d.box(-.25f,.91f,-.15f,.25f,1.46f,.15f,color);d.box(-.30f,1.28f,-.14f,.30f,1.47f,.14f,StarCharacterGeometry.lighten(color,.06f));}
    private static void arms(StarCharacterGeometry.Drawer d,float swing,int skin,int sleeve){d.box(-.40f,1.14f,-.08f+swing,-.29f,1.42f,.08f+swing,sleeve);d.box(.29f,1.14f,-.08f-swing,.40f,1.42f,.08f-swing,sleeve);d.box(-.42f,.76f,-.075f+swing,-.31f,1.16f,.075f+swing,skin);d.box(.31f,.76f,-.075f-swing,.42f,1.16f,.075f-swing,skin);}
    private static void head(StarCharacterGeometry.Drawer d,int skin,int hair){d.box(-.18f,1.55f,-.16f,.18f,1.90f,.16f,skin);d.box(-.18f,1.84f,-.16f,.18f,1.96f,.14f,hair);d.box(-.115f,1.74f,-.175f,-.025f,1.77f,-.162f,0xFFF2F2EA);d.box(.025f,1.74f,-.175f,.115f,1.77f,-.162f,0xFFF2F2EA);d.box(-.06f,1.74f,-.183f,-.04f,1.77f,-.172f,0xFF171112);d.box(.04f,1.74f,-.183f,.06f,1.77f,-.172f,0xFF171112);}
}
