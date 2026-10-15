package gg.cinco.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Smooth textureless CJ-inspired preset shaped from the user's front/side/back references. */
public final class CjReferenceRenderer {
    private CjReferenceRenderer() {}

    public static void render(AbstractClientPlayerEntity player,float bodyYaw,float tickDelta,MatrixStack matrices,VertexConsumerProvider consumers,int light,int outfit,boolean detailed){
        outfit=Math.max(1,Math.min(6,outfit));
        int skin=0xFF6A402E,skinHi=0xFF83523B,skinDark=0xFF3F261D,hair=0xFF0E0E0F,white=0xFFF3F2EE,sole=0xFFE6E6E3,belt=0xFF291B16,buckle=0xFFC9A34E;
        int denim=switch(outfit){case 2->0xFF344635;case 3->0xFF202833;case 6->0xFF292A30;default->0xFF263A55;};
        int top=switch(outfit){case 2->0xFF2F7044;case 3->0xFF17191C;case 4->0xFF7B2D32;case 5->0xFF37714D;case 6->0xFF5B5C62;default->white;};
        matrices.push();matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f-bodyYaw));if(player.isSneaking())matrices.translate(0,-.07,.08);
        StarCharacterGeometry.Drawer d=StarCharacterGeometry.begin(matrices,consumers);
        double speed=player.getVelocity().horizontalLength();float motion=(float)Math.min(1,speed*4.8),phase=(player.age+tickDelta)*.58f,leg=(float)Math.sin(phase)*.08f*motion,arm=-leg*1.15f;

        // Black sneakers with bright soles and long tapered-looking jeans.
        d.box(-.25f,.00f,-.24f+leg,-.02f,.045f,.17f+leg,sole);d.box(.02f,.00f,-.24f-leg,.25f,.045f,.17f-leg,sole);
        d.box(-.235f,.04f,-.225f+leg,-.035f,.135f,.145f+leg,0xFF111216);d.box(.035f,.04f,-.225f-leg,.235f,.135f,.145f-leg,0xFF111216);
        d.box(-.235f,.13f,-.12f+leg,-.025f,.58f,.13f+leg,StarCharacterGeometry.shade(denim,.74f));d.box(.025f,.13f,-.12f-leg,.235f,.58f,.13f-leg,StarCharacterGeometry.shade(denim,.74f));
        d.box(-.25f,.56f,-.14f+leg*.5f,-.015f,.98f,.15f+leg*.5f,denim);d.box(.015f,.56f,-.14f-leg*.5f,.25f,.98f,.15f-leg*.5f,denim);
        d.box(-.25f,.92f,-.155f,.25f,.985f,.155f,belt);d.box(-.045f,.91f,-.169f,.045f,.99f,-.154f,buckle);

        // Athletic torso and classic tank / outfit top.
        d.box(-.22f,.98f,-.13f,.22f,1.29f,.14f,top);d.box(-.29f,1.28f,-.145f,.29f,1.47f,.15f,outfit==1?skin:top);
        if(outfit==1){d.box(-.225f,1.01f,-.153f,.225f,1.37f,-.137f,white);d.box(-.22f,1.34f,-.155f,-.135f,1.48f,-.14f,white);d.box(.135f,1.34f,-.155f,.22f,1.48f,-.14f,white);d.box(-.075f,1.30f,-.158f,.075f,1.46f,-.145f,skin);}
        else if(outfit==4||outfit==5){d.box(-.10f,1.02f,-.158f,.10f,1.43f,-.145f,white);}

        // Slim articulated-looking arms.
        arm(d,-1,arm,skin,skinHi,outfit==1?skin:top);arm(d,1,-arm,skin,skinHi,outfit==1?skin:top);
        d.box(-.085f,1.45f,-.08f,.085f,1.58f,.08f,skinDark);

        // Layered head/jaw proportions.
        d.box(-.145f,1.56f,-.135f,.145f,1.68f,.14f,skinDark);
        d.box(-.19f,1.66f,-.16f,.19f,1.86f,.16f,skin);
        d.box(-.17f,1.84f,-.145f,.17f,1.94f,.14f,skinDark);
        d.box(-.172f,1.885f,-.148f,.172f,1.97f,.13f,hair);
        d.box(-.205f,1.70f,-.055f,-.18f,1.80f,.06f,skinDark);d.box(.18f,1.70f,-.055f,.205f,1.80f,.06f,skinDark);
        d.box(-.125f,1.795f,-.172f,-.025f,1.812f,-.158f,hair);d.box(.025f,1.795f,-.172f,.125f,1.812f,-.158f,hair);
        d.box(-.112f,1.753f,-.178f,-.025f,1.773f,-.164f,0xFFE7E2DB);d.box(.025f,1.753f,-.178f,.112f,1.773f,-.164f,0xFFE7E2DB);
        d.box(-.070f,1.754f,-.185f,-.045f,1.774f,-.172f,0xFF171112);d.box(.045f,1.754f,-.185f,.070f,1.774f,-.172f,0xFF171112);
        d.box(-.030f,1.67f,-.200f,.032f,1.744f,-.166f,skinDark);d.box(-.078f,1.628f,-.185f,.078f,1.646f,-.170f,0xFF7C463E);
        if(detailed){d.box(-.054f,1.582f,-.178f,.054f,1.625f,-.165f,0xFF231817);d.box(-.012f,.70f,-.151f,.012f,.96f,-.141f,StarCharacterGeometry.lighten(denim,.12f));d.box(-.19f,.82f,-.155f,-.08f,.845f,-.143f,StarCharacterGeometry.lighten(denim,.18f));d.box(.08f,.82f,-.155f,.19f,.845f,-.143f,StarCharacterGeometry.lighten(denim,.18f));}
        matrices.pop();
    }

    private static void arm(StarCharacterGeometry.Drawer d,int side,float swing,int skin,int hi,int sleeve){float s=side;d.box((side<0?-.40f:.29f),1.15f,-.085f+swing,(side<0?-.29f:.40f),1.43f,.085f+swing,sleeve);d.box((side<0?-.42f:.31f),.79f,-.075f+swing*1.2f,(side<0?-.31f:.42f),1.17f,.075f+swing*1.2f,skin);d.box((side<0?-.435f:.295f),.67f,-.09f+swing*1.25f,(side<0?-.295f:.435f),.82f,.09f+swing*1.25f,hi);}
}
