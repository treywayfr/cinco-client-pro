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

    /**
     * Default/early-game GTA: San Andreas inspired CJ: thin build, white ribbed tank,
     * loose blue jeans, black tennis shoes, Caesar haircut and light facial hair.
     * Geometry is Star-original and deliberately faceted to preserve the PS2-era silhouette.
     */
    private static void renderCj(MatrixStack m,VertexConsumerProvider consumers,Pose p,boolean detailed){
        StarCharacterGeometry.Drawer d=StarCharacterGeometry.begin(m,consumers);
        final int skin=0xFF70432F,skinLight=0xFF85533B,skinShade=0xFF4A2B21,skinDeep=0xFF332019;
        final int hair=0xFF101012,brow=0xFF161214,eyeWhite=0xFFF2EEE8,iris=0xFF3A261B;
        final int white=0xFFF1F0EA,whiteShade=0xFFD8D7D2;
        final int denim=0xFF2B4160,denimHi=0xFF385473,denimDark=0xFF1B2C43;
        final int shoe=0xFF111216,shoeHi=0xFF25262A,sole=0xFFE5E3DC,belt=0xFF2A1B17,buckle=0xFFB99554;

        float amount=clamp(p.limbAmount,0,1);
        float walk=(float)Math.sin(p.limbSwing*.6662f);
        float leftLeg=walk*.78f*amount,rightLeg=-leftLeg;
        float leftArm=-leftLeg*.78f,rightArm=-rightLeg*.78f;
        float leftKnee=Math.max(0f,-walk)*.48f*amount;
        float rightKnee=Math.max(0f,walk)*.48f*amount;

        // Narrow hips and low waist: CJ's default build is thin, not gym-bulked.
        d.prismY(0,.865f,.975f,0,.245f,.145f,10,denimDark);
        d.box(-.255f,.935f,-.162f,.255f,.985f,.162f,belt);
        d.box(-.043f,.936f,-.176f,.043f,.987f,-.160f,buckle);
        for(int side:new int[]{-1,1}){
            float x=.185f*side;
            d.box(x-.015f,.938f,-.171f,x+.015f,.985f,-.158f,0xFF5C4438);
        }

        // Articulated two-joint legs; loose jeans taper toward the shoes.
        cjLeg(m,d,-.13f,.94f,leftLeg,leftKnee,denim,denimHi,denimDark,shoe,shoeHi,sole,detailed);
        cjLeg(m,d,.13f,.94f,rightLeg,rightKnee,denim,denimHi,denimDark,shoe,shoeHi,sole,detailed);

        // Underlying slim torso and exposed shoulders.
        d.prismY(0,.97f,1.385f,0,.245f,.145f,10,skin);
        d.prismY(0,1.285f,1.415f,0,.292f,.152f,10,skinLight);

        // Classic white ribbed tank: narrow waist, broad chest, deep front neck opening.
        d.prismY(0,.985f,1.335f,.006f,.224f,.142f,10,white);
        d.box(-.214f,1.315f,-.151f,-.122f,1.405f,.139f,white);
        d.box(.122f,1.315f,-.151f,.214f,1.405f,.139f,white);
        d.box(-.118f,1.307f,-.162f,.118f,1.407f,-.142f,skin);
        d.box(-.222f,.990f,-.154f,.222f,1.035f,-.142f,whiteShade);
        if(detailed){
            for(int i=-4;i<=4;i++){
                float x=i*.043f;
                d.box(x-.005f,1.035f,-.158f,x+.005f,1.298f,-.148f,(i&1)==0?whiteShade:0xFFF9F8F3);
            }
            d.box(-.205f,1.085f,-.160f,-.085f,1.108f,-.148f,0xFFF9F8F3);
            d.box(.085f,1.085f,-.160f,.205f,1.108f,-.148f,0xFFF9F8F3);
        }

        // Shoulder caps and long, slim articulated arms.
        d.ellipsoid(-.292f,1.368f,0,.105f,.095f,.105f,5,10,skinLight);
        d.ellipsoid(.292f,1.368f,0,.105f,.095f,.105f,5,10,skinLight);
        cjArm(m,d,-.315f,1.385f,leftArm,skin,skinLight,skinShade,detailed);
        cjArm(m,d,.315f,1.385f,rightArm,skin,skinLight,skinShade,detailed);

        // Neck.
        d.prismY(0,1.385f,1.525f,0,.080f,.073f,10,skinShade);
        d.box(-.070f,1.405f,-.080f,.070f,1.515f,-.064f,skin);

        // Head follows the look direction independently from the body.
        m.push();
        m.translate(0,1.505f,0);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(clamp(p.headYaw,-62,62)));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(clamp(p.pitch,-36,36)*.68f));

        // GTA-SA-like low-poly head: rounded cranium + narrower jaw/chin.
        d.ellipsoid(0,.205f,.005f,.182f,.205f,.158f,detailed?8:6,detailed?14:10,skin);
        d.ellipsoid(0,.075f,-.012f,.158f,.125f,.151f,6,12,skinShade);
        d.box(-.128f,-.010f,-.144f,.128f,.075f,.122f,skinShade);
        d.ellipsoid(0,.012f,-.020f,.122f,.070f,.124f,5,10,skinShade);

        // Caesar cut: low, close-cropped cap with a flat front hairline.
        d.ellipsoid(0,.350f,.012f,.176f,.072f,.148f,5,14,hair);
        d.box(-.169f,.298f,-.151f,.169f,.350f,-.128f,hair);
        d.box(-.148f,.286f,-.160f,-.015f,.309f,-.146f,StarCharacterGeometry.lighten(hair,.08f));
        d.box(.015f,.286f,-.160f,.148f,.309f,-.146f,StarCharacterGeometry.lighten(hair,.08f));

        // Ears sit slightly low and close to the skull.
        d.ellipsoid(-.188f,.180f,-.005f,.030f,.060f,.040f,4,8,skinShade);
        d.ellipsoid(.188f,.180f,-.005f,.030f,.060f,.040f,4,8,skinShade);
        if(detailed){
            d.box(-.197f,.172f,-.034f,-.185f,.205f,-.020f,skinDeep);
            d.box(.185f,.172f,-.034f,.197f,.205f,-.020f,skinDeep);
        }

        // Brows and slightly hooded eyes. Brown irises match CJ's in-game appearance.
        d.box(-.136f,.245f,-.171f,-.025f,.268f,-.153f,brow);
        d.box(.025f,.245f,-.171f,.136f,.268f,-.153f,brow);
        d.box(-.124f,.194f,-.177f,-.025f,.222f,-.160f,eyeWhite);
        d.box(.025f,.194f,-.177f,.124f,.222f,-.160f,eyeWhite);
        float eyeShift=clamp(p.headYaw,-30,30)/30f*.012f;
        d.box(-.074f+eyeShift,.195f,-.188f,-.043f+eyeShift,.223f,-.174f,iris);
        d.box(.043f+eyeShift,.195f,-.188f,.074f+eyeShift,.223f,-.174f,iris);
        d.box(-.064f+eyeShift,.201f,-.194f,-.052f+eyeShift,.217f,-.184f,0xFF090708);
        d.box(.052f+eyeShift,.201f,-.194f,.064f+eyeShift,.217f,-.184f,0xFF090708);

        // Broad, low nose and nostrils.
        d.box(-.034f,.105f,-.205f,.036f,.187f,-.157f,skinShade);
        d.box(-.062f,.092f,-.199f,.062f,.120f,-.166f,skinLight);
        d.box(-.052f,.094f,-.207f,-.020f,.111f,-.195f,skinDeep);
        d.box(.020f,.094f,-.207f,.052f,.111f,-.195f,skinDeep);

        // Thin lips and subtle San Andreas facial hair/goatee.
        d.box(-.085f,.050f,-.191f,.085f,.067f,-.174f,0xFF6A3933);
        d.box(-.075f,.030f,-.188f,.075f,.048f,-.173f,0xFF4D2928);
        d.box(-.060f,-.005f,-.177f,.060f,.026f,-.160f,0xFF251A19);
        d.box(-.020f,.073f,-.187f,.020f,.086f,-.175f,0xFF2C1D1B);
        if(detailed){
            // cheek planes and faint stubble accents keep the face from reading as a Minecraft cube.
            d.box(-.153f,.100f,-.158f,-.126f,.162f,-.145f,skinLight);
            d.box(.126f,.100f,-.158f,.153f,.162f,-.145f,skinLight);
            d.box(-.126f,.020f,-.151f,-.070f,.042f,-.140f,0xFF4E3028);
            d.box(.070f,.020f,-.151f,.126f,.042f,-.140f,0xFF4E3028);
        }
        m.pop();
    }

    private static void cjLeg(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float hip,float knee,
                              int denim,int denimHi,int denimDark,int shoe,int shoeHi,int sole,boolean detailed){
        m.push();
        m.translate(x,y,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(hip));
        d.prismY(0,-.405f,0,.005f,.128f,.132f,8,denim);
        if(detailed){
            d.box(-.012f,-.385f,-.142f,.012f,-.055f,-.132f,denimHi);
            d.box(-.112f,-.145f,-.141f,-.040f,-.118f,-.131f,denimHi);
            d.box(.040f,-.145f,-.141f,.112f,-.118f,-.131f,denimHi);
        }
        m.translate(0,-.395f,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(knee));
        d.prismY(0,-.365f,.015f,.003f,.116f,.120f,8,denimDark);
        if(detailed)d.box(-.010f,-.335f,-.130f,.010f,-.035f,-.120f,denimHi);
        // Black low tennis shoe and bright sole lip.
        d.box(-.112f,-.440f,-.205f,.112f,-.345f,.142f,shoe);
        d.box(-.117f,-.455f,-.225f,.117f,-.420f,.160f,sole);
        d.box(-.095f,-.405f,-.215f,.095f,-.355f,-.175f,shoeHi);
        if(detailed){
            for(int i=0;i<4;i++){
                float lx=-.070f+i*.045f;
                d.box(lx,-.392f,-.221f,lx+.027f,-.379f,-.211f,0xFF9B9A96);
            }
            d.box(-.100f,-.444f,.120f,.100f,-.420f,.156f,0xFF1A1B1F);
        }
        m.pop();
    }

    private static void cjArm(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float swing,
                              int skin,int skinLight,int skinShade,boolean detailed){
        m.push();
        m.translate(x,y,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
        d.prismY(0,-.315f,0,0,.070f,.076f,9,skin);
        d.ellipsoid(0,-.315f,0,.074f,.070f,.078f,4,8,skinLight);
        m.translate(0,-.310f,0);
        float elbow=-.055f-Math.max(0,swing)*.12f;
        m.multiply(RotationAxis.POSITIVE_X.rotation(elbow));
        d.prismY(0,-.310f,.010f,0,.062f,.068f,9,skinLight);
        d.ellipsoid(0,-.350f,-.006f,.078f,.085f,.078f,5,9,skin);
        if(detailed){
            // blocky finger separations, still faceted like the original game model.
            for(int i=0;i<4;i++){
                float fx=-.056f+i*.037f;
                d.box(fx,-.405f,-.073f,fx+.025f,-.338f,-.057f,skinShade);
            }
            d.box(-.074f,-.328f,-.078f,-.052f,-.250f,-.065f,skinShade);
        }
        m.pop();
    }

    private static void renderTung(MatrixStack m,VertexConsumerProvider consumers,Pose p,boolean detailed){
        StarCharacterGeometry.Drawer d=StarCharacterGeometry.begin(m,consumers);
        final int wood=0xFFB47636,woodLight=0xFFD59A56,woodDark=0xFF74431F,woodDeep=0xFF4B2915;
        final int white=0xFFF9F6E9,black=0xFF16100E,mouth=0xFF31130F,tongue=0xFFD9574D;
        float amp=Math.min(1f,Math.max(0f,p.limbAmount));
        float rs=(float)Math.sin(p.limbSwing*.6662f)*.96f*amp,ls=-rs;

        // Legs first so they disappear naturally into the trunk.
        thinLimb(m,d,-.14f,.58f,ls,.54f,.055f,woodDark,true);
        thinLimb(m,d,.14f,.58f,rs,.54f,.055f,woodDark,true);
        // Arms swing opposite legs.
        thinArm(m,d,-.37f,1.45f,-ls*.76f,.68f,.052f,woodDark);
        thinArm(m,d,.37f,1.45f,-rs*.76f,.68f,.052f,woodDark);

        // Tall rounded trunk: look motion is deliberately subtle to avoid F5 clipping/jitter.
        m.push();
        m.translate(0,.55f,0);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(clamp(p.headYaw,-42,42)*.18f));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(clamp(p.pitch,-28,28)*.07f));
        d.prismY(0,0,1.52f,0,.34f,.29f,detailed?16:12,wood);
        d.discY(0,1.525f,0,.33f,.28f,detailed?20:14,woodLight);
        d.discY(0,.005f,0,.32f,.27f,detailed?20:14,woodDark);

        if(detailed){
            for(int i=-3;i<=3;i++){
                float xx=i*.072f,yy=.12f+(i+3)*.055f;
                d.box(xx-.009f,yy,-.303f,xx+.009f,1.42f-yy*.16f,-.292f,(i&1)==0?woodLight:woodDark);
            }
            d.box(-.305f,.25f,-.075f,-.292f,1.20f,.025f,woodDark);
            d.box(.292f,.42f,.045f,.305f,1.36f,.13f,woodLight);
            d.box(-.24f,.78f,-.309f,-.17f,.84f,-.296f,woodDeep);
            d.box(-.225f,.795f,-.314f,-.185f,.825f,-.304f,woodLight);
            d.box(.15f,1.24f,-.309f,.22f,1.30f,-.296f,woodDeep);
            d.box(-.05f,.56f,.292f,.04f,.64f,.304f,woodDark);
        }

        float eyeY=1.19f;
        d.box(-.235f,eyeY-.12f,-.330f,-.025f,eyeY+.13f,-.292f,white);
        d.box(.025f,eyeY-.12f,-.330f,.235f,eyeY+.13f,-.292f,white);
        float pupilX=clamp(p.headYaw,-35,35)/35f*.035f,pupilY=-clamp(p.pitch,-28,28)/28f*.025f;
        d.box(-.145f+pupilX,eyeY-.015f+pupilY,-.344f,-.095f+pupilX,eyeY+.055f+pupilY,-.326f,black);
        d.box(.095f+pupilX,eyeY-.015f+pupilY,-.344f,.145f+pupilX,eyeY+.055f+pupilY,-.326f,black);
        d.box(-.245f,1.345f,-.334f,-.035f,1.385f,-.306f,woodDeep);
        d.box(.035f,1.345f,-.334f,.245f,1.385f,-.306f,woodDeep);
        d.box(-.205f,1.382f,-.333f,-.065f,1.412f,-.305f,woodDeep);
        d.box(.065f,1.382f,-.333f,.205f,1.412f,-.305f,woodDeep);
        d.box(-.185f,.89f,-.338f,.185f,1.035f,-.298f,mouth);
        d.box(-.145f,.895f,-.347f,.145f,.925f,-.329f,0xFF0A0707);
        d.box(-.105f,.885f,-.349f,.105f,.945f,-.337f,tongue);
        d.box(-.155f,1.018f,-.346f,.155f,1.047f,-.326f,black);
        m.pop();
    }

    private static void thinLimb(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float swing,float len,float r,int color,boolean foot){
        m.push();
        m.translate(x,y,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
        d.prismY(0,-len,0,0,r,r,10,color);
        d.prismY(0,-len-.08f,-len+.01f,-.025f,r*1.65f,r*2.0f,10,StarCharacterGeometry.lighten(color,.10f));
        if(foot)d.box(-r*1.8f,-len-.105f,-.14f,r*1.8f,-len-.055f,.08f,StarCharacterGeometry.lighten(color,.18f));
        m.pop();
    }

    private static void thinArm(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float swing,float len,float r,int color){
        m.push();
        m.translate(x,y,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
        d.prismY(0,-len,0,0,r,r,10,color);
        d.prismY(0,-len-.11f,-len+.01f,0,r*1.55f,r*1.55f,10,StarCharacterGeometry.lighten(color,.12f));
        m.pop();
    }

    private static float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
}
