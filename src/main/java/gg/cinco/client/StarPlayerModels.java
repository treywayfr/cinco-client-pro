package gg.cinco.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** High-detail articulated local character models used both in-world and by Locker previews. */
public final class StarPlayerModels {
    public record Pose(float limbSwing,float limbAmount,float headYaw,float pitch,float age) {}

    private StarPlayerModels() {}

    public static void render(int character, MatrixStack m, VertexConsumerProvider consumers, Pose pose, boolean detailed) {
        if (character==2) renderTung(m,consumers,pose,detailed);
        else renderCj(m,consumers,pose,detailed);
    }

    private static void renderCj(MatrixStack m,VertexConsumerProvider consumers,Pose p,boolean detailed){
        StarCharacterGeometry.Drawer d=StarCharacterGeometry.begin(m,consumers);
        final int skin=0xFF6A402E,skinHi=0xFF87563E,skinDark=0xFF3D241B,hair=0xFF101011;
        final int white=0xFFF3F1EA,denim=0xFF263B59,denimDark=0xFF182A42,shoe=0xFF111216,sole=0xFFE6E6E2,belt=0xFF281A15,buckle=0xFFC69E48;
        float amp=Math.min(1f,Math.max(0f,p.limbAmount));
        float rightLeg=(float)Math.sin(p.limbSwing*.6662f)*.92f*amp;
        float leftLeg=-rightLeg;
        float rightArm=-rightLeg*.82f,leftArm=-leftLeg*.82f;

        // Hips / belt core.
        d.box(-.255f,.88f,-.145f,.255f,1.00f,.155f,denimDark);
        d.box(-.265f,.955f,-.165f,.265f,1.015f,.165f,belt);
        d.box(-.045f,.958f,-.178f,.045f,1.018f,-.162f,buckle);
        for(int x=-1;x<=1;x+=2)d.box(x*.205f-.018f,.958f,-.174f,x*.205f+.018f,1.016f,-.162f,0xFF544036);

        // Articulated legs from hip pivots.
        leg(m,d,-.135f,.96f,leftLeg,denim,denimDark,shoe,sole,detailed);
        leg(m,d,.135f,.96f,rightLeg,denim,denimDark,shoe,sole,detailed);

        // Athletic torso: skin shoulder volume + layered white tank.
        d.box(-.295f,1.22f,-.145f,.295f,1.47f,.15f,skin);
        d.box(-.235f,1.00f,-.142f,.235f,1.38f,.145f,white);
        d.box(-.205f,1.34f,-.151f,.205f,1.49f,.145f,white);
        d.box(-.078f,1.31f,-.164f,.078f,1.49f,-.143f,skin);
        d.box(-.235f,1.01f,-.159f,.235f,1.075f,-.143f,StarCharacterGeometry.shade(white,.88f));
        if(detailed){
            d.box(-.020f,1.05f,-.158f,.020f,1.30f,-.146f,0xFFE7E4DD);
            d.box(-.205f,1.10f,-.160f,-.090f,1.14f,-.146f,0xFFF8F7F2);
            d.box(.090f,1.10f,-.160f,.205f,1.14f,-.146f,0xFFF8F7F2);
            d.box(-.245f,1.40f,-.12f,-.195f,1.465f,.12f,skinHi);
            d.box(.195f,1.40f,-.12f,.245f,1.465f,.12f,skinHi);
        }

        arm(m,d,-.33f,1.42f,leftArm,skin,skinHi,detailed);
        arm(m,d,.33f,1.42f,rightArm,skin,skinHi,detailed);

        // Neck.
        d.box(-.085f,1.45f,-.078f,.085f,1.61f,.082f,skinDark);

        // Head pivot follows player look independently from body.
        m.push();
        m.translate(0,1.64,0);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(clamp(p.headYaw,-65,65)));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(clamp(p.pitch,-38,38)*.72f));
        // Jaw / skull / close cropped hair.
        d.box(-.145f,-.04f,-.145f,.145f,.105f,.13f,skinDark);
        d.box(-.192f,.06f,-.165f,.192f,.285f,.16f,skin);
        d.box(-.172f,.27f,-.148f,.172f,.385f,.145f,skinDark);
        d.box(-.176f,.326f,-.152f,.176f,.415f,.135f,hair);
        // ears
        d.box(-.212f,.12f,-.06f,-.187f,.235f,.065f,skinDark); d.box(.187f,.12f,-.06f,.212f,.235f,.065f,skinDark);
        // brows / eyes
        d.box(-.135f,.222f,-.177f,-.024f,.241f,-.161f,hair); d.box(.024f,.222f,-.177f,.135f,.241f,-.161f,hair);
        d.box(-.122f,.168f,-.184f,-.024f,.197f,-.166f,0xFFF1ECE3); d.box(.024f,.168f,-.184f,.122f,.197f,-.166f,0xFFF1ECE3);
        d.box(-.074f,.169f,-.194f,-.045f,.198f,-.179f,0xFF171112); d.box(.045f,.169f,-.194f,.074f,.198f,-.179f,0xFF171112);
        // nose, mouth, lower lip/goatee
        d.box(-.032f,.082f,-.205f,.034f,.164f,-.166f,skinDark);
        d.box(-.082f,.024f,-.190f,.082f,.044f,-.171f,0xFF6F3C36);
        d.box(-.055f,-.020f,-.183f,.055f,.020f,-.166f,0xFF231716);
        if(detailed){
            d.box(-.148f,.105f,-.174f,-.121f,.155f,-.161f,skinHi);d.box(.121f,.105f,-.174f,.148f,.155f,-.161f,skinHi);
            d.box(-.010f,.35f,-.157f,.010f,.397f,-.145f,0xFF282124);
            d.box(-.105f,.300f,-.155f,-.030f,.318f,-.144f,StarCharacterGeometry.lighten(hair,.08f));
            d.box(.030f,.300f,-.155f,.105f,.318f,-.144f,StarCharacterGeometry.lighten(hair,.08f));
        }
        m.pop();
    }

    private static void leg(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float swing,int denim,int denimDark,int shoe,int sole,boolean detailed){
        m.push();m.translate(x,y,0);m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
        d.box(-.112f,-.43f,-.125f,.112f,0,.13f,denim);
        d.box(-.104f,-.78f,-.112f,.104f,-.41f,.118f,denimDark);
        if(detailed){d.box(-.012f,-.72f,-.121f,.012f,-.46f,-.111f,StarCharacterGeometry.lighten(denim,.18f));d.box(-.095f,-.56f,-.122f,.095f,-.535f,-.111f,StarCharacterGeometry.lighten(denim,.12f));}
        d.box(-.108f,-.865f,-.215f,.108f,-.765f,.145f,shoe);
        d.box(-.118f,-.895f,-.235f,.118f,-.855f,.165f,sole);
        if(detailed){for(int i=0;i<3;i++)d.box(-.073f+i*.05f,-.842f,-.221f,-.045f+i*.05f,-.825f,-.211f,0xFF898989);}
        m.pop();
    }

    private static void arm(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float swing,int skin,int skinHi,boolean detailed){
        m.push();m.translate(x,y,0);m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
        d.box(-.072f,-.31f,-.082f,.072f,0,.082f,skin);
        d.box(-.066f,-.62f,-.073f,.066f,-.29f,.073f,skinHi);
        d.box(-.080f,-.735f,-.090f,.080f,-.61f,.090f,skin);
        if(detailed){d.box(-.052f,-.722f,-.098f,-.020f,-.655f,-.088f,StarCharacterGeometry.lighten(skin,.10f));d.box(.020f,-.722f,-.098f,.052f,-.655f,-.088f,StarCharacterGeometry.lighten(skin,.10f));}
        m.pop();
    }

    private static void renderTung(MatrixStack m,VertexConsumerProvider consumers,Pose p,boolean detailed){
        StarCharacterGeometry.Drawer d=StarCharacterGeometry.begin(m,consumers);
        final int wood=0xFFB47636,woodLight=0xFFD59A56,woodDark=0xFF74431F,woodDeep=0xFF4B2915;
        final int white=0xFFF9F6E9,black=0xFF16100E,mouth=0xFF31130F,tongue=0xFFD9574D;
        float amp=Math.min(1f,Math.max(0f,p.limbAmount));
        float rs=(float)Math.sin(p.limbSwing*.6662f)*1.05f*amp,ls=-rs;

        // Legs first so they disappear naturally into the trunk.
        thinLimb(m,d,-.14f,.58f,ls,.54f,.055f,woodDark,true); thinLimb(m,d,.14f,.58f,rs,.54f,.055f,woodDark,true);
        // arms swing opposite legs
        thinArm(m,d,-.37f,1.45f,-ls*.82f,.68f,.052f,woodDark); thinArm(m,d,.37f,1.45f,-rs*.82f,.68f,.052f,woodDark);

        // Tall rounded trunk twists subtly toward look direction.
        m.push();m.translate(0,.55f,0);m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(clamp(p.headYaw,-45,45)*.28f));m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(clamp(p.pitch,-30,30)*.10f));
        d.prismY(0,0,1.52f,0,.34f,.29f,detailed?16:12,wood);
        d.discY(0,1.525f,0,.33f,.28f,detailed?20:14,woodLight);
        d.discY(0,.005f,0,.32f,.27f,detailed?20:14,woodDark);

        // wood grain ribs on front and sides
        if(detailed){
            for(int i=-3;i<=3;i++){
                float xx=i*.072f;float yy=.12f+(i+3)*.055f;
                d.box(xx-.009f,yy,-.303f,xx+.009f,1.42f-yy*.16f,-.292f,(i&1)==0?woodLight:woodDark);
            }
            d.box(-.305f,.25f,-.075f,-.292f,1.20f,.025f,woodDark);d.box(.292f,.42f,.045f,.305f,1.36f,.13f,woodLight);
            // knots
            d.box(-.24f,.78f,-.309f,-.17f,.84f,-.296f,woodDeep);d.box(-.225f,.795f,-.314f,-.185f,.825f,-.304f,woodLight);
            d.box(.15f,1.24f,-.309f,.22f,1.30f,-.296f,woodDeep);
            d.box(-.05f,.56f,.292f,.04f,.64f,.304f,woodDark);
        }

        // Cartoon face on front (-Z); pupils shift with look direction.
        float eyeY=1.19f;
        d.box(-.235f,eyeY-.12f,-.330f,-.025f,eyeY+.13f,-.292f,white);
        d.box(.025f,eyeY-.12f,-.330f,.235f,eyeY+.13f,-.292f,white);
        float pupilX=clamp(p.headYaw,-35,35)/35f*.035f,pupilY=-clamp(p.pitch,-28,28)/28f*.025f;
        d.box(-.145f+pupilX,eyeY-.015f+pupilY,-.344f,-.095f+pupilX,eyeY+.055f+pupilY,-.326f,black);
        d.box(.095f+pupilX,eyeY-.015f+pupilY,-.344f,.145f+pupilX,eyeY+.055f+pupilY,-.326f,black);
        // thick arched brows approximated by layered segments
        d.box(-.245f,1.345f,-.334f,-.035f,1.385f,-.306f,woodDeep);d.box(.035f,1.345f,-.334f,.245f,1.385f,-.306f,woodDeep);
        d.box(-.205f,1.382f,-.333f,-.065f,1.412f,-.305f,woodDeep);d.box(.065f,1.382f,-.333f,.205f,1.412f,-.305f,woodDeep);
        // smile / open mouth / tongue
        d.box(-.185f,.89f,-.338f,.185f,1.035f,-.298f,mouth);
        d.box(-.145f,.895f,-.347f,.145f,.925f,-.329f,0xFF0A0707);
        d.box(-.105f,.885f,-.349f,.105f,.945f,-.337f,tongue);
        d.box(-.155f,1.018f,-.346f,.155f,1.047f,-.326f,black);
        m.pop();
    }

    private static void thinLimb(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float swing,float len,float r,int color,boolean foot){
        m.push();m.translate(x,y,0);m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
        d.prismY(0,-len,0,0,r,r,10,color);
        d.prismY(0,-len-.08f,-len+.01f,-.025f,r*1.65f,r*2.0f,10,StarCharacterGeometry.lighten(color,.10f));
        if(foot)d.box(-r*1.8f,-len-.105f,-.14f,r*1.8f,-len-.055f,.08f,StarCharacterGeometry.lighten(color,.18f));
        m.pop();
    }

    private static void thinArm(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float swing,float len,float r,int color){
        m.push();m.translate(x,y,0);m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
        d.prismY(0,-len,0,0,r,r,10,color);
        d.prismY(0,-len-.11f,-len+.01f,0,r*1.55f,r*1.55f,10,StarCharacterGeometry.lighten(color,.12f));
        m.pop();
    }

    private static float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
}
