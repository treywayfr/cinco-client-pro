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
     * CJ reference rebuild: GTA: San Andreas default outfit/silhouette.
     * The model intentionally uses rounded/faceted low-poly geometry instead of Minecraft cuboids.
     */
    private static void renderCj(MatrixStack m,VertexConsumerProvider consumers,Pose p,boolean detailed){
        StarCharacterGeometry.Drawer d=StarCharacterGeometry.begin(m,consumers);

        final int skin=0xFF74442E, skinHi=0xFF8D5A3D, skinMid=0xFF6A3A29, skinShade=0xFF4B291E, skinDeep=0xFF2E1A15;
        final int hair=0xFF0B0D0E, brow=0xFF17100F, eyeWhite=0xFFF4EFE8, iris=0xFF39251A, pupil=0xFF080707;
        final int tank=0xFFF3F1EB, tankShade=0xFFDAD8D1, tankHi=0xFFFBFAF5;
        final int denim=0xFF294260, denimHi=0xFF3B5A7C, denimMid=0xFF263B56, denimDark=0xFF18283D;
        final int belt=0xFF2A1C17, beltHi=0xFF5B4337, buckle=0xFFB79250;
        final int shoe=0xFF111216, shoeHi=0xFF292A2E, sole=0xFFDAD9D5, lace=0xFFA8A7A2;

        float amount=clamp(p.limbAmount,0,1);
        float walk=(float)Math.sin(p.limbSwing*.6662f);
        float leftLeg=walk*.72f*amount, rightLeg=-leftLeg;
        float leftArm=-leftLeg*.70f, rightArm=-rightLeg*.70f;
        float leftKnee=Math.max(0f,-walk)*.42f*amount;
        float rightKnee=Math.max(0f,walk)*.42f*amount;

        // Hips / waist. Keep CJ slim through the middle with slightly looser jeans at the top.
        d.taperedPrismY(0,.855f,.978f,0,.210f,.130f,.252f,.150f,detailed?16:12,denimDark);
        d.taperedPrismY(0,.945f,.988f,0,.255f,.158f,.255f,.158f,14,belt);
        d.ellipsoid(0,.967f,-.163f,.046f,.027f,.018f,4,10,buckle);
        for(int side:new int[]{-1,1}){
            float lx=.186f*side;
            d.taperedPrismY(lx,.944f,.990f,-.001f,.012f,.160f,.012f,.160f,8,beltHi);
        }

        // Legs are separate articulated upper/lower meshes so they walk naturally.
        cjLeg(m,d,-.132f,.945f,leftLeg,leftKnee,denim,denimHi,denimMid,denimDark,shoe,shoeHi,sole,lace,detailed);
        cjLeg(m,d,.132f,.945f,rightLeg,rightKnee,denim,denimHi,denimMid,denimDark,shoe,shoeHi,sole,lace,detailed);

        // Human body under the tank: narrow waist, broader shoulders/chest.
        d.taperedPrismY(0,.980f,1.350f,0,.207f,.125f,.285f,.152f,detailed?18:14,skin);
        d.ellipsoid(-.279f,1.352f,0,.107f,.103f,.106f,6,14,skinHi);
        d.ellipsoid(.279f,1.352f,0,.107f,.103f,.106f,6,14,skinHi);

        // White tank top from the reference: tight at waist, wider across chest, curved scoop neck.
        d.taperedPrismY(0,.991f,1.326f,.004f,.202f,.132f,.253f,.144f,detailed?18:14,tank);
        d.taperedPrismY(-.170f,1.300f,1.407f,0,.047f,.138f,.057f,.142f,12,tank);
        d.taperedPrismY(.170f,1.300f,1.407f,0,.047f,.138f,.057f,.142f,12,tank);
        d.surfaceOval(0,1.358f,-.149f,.107f,.066f,.012f,16,skin);
        d.surfaceOval(0,1.012f,-.144f,.213f,.025f,.010f,16,tankShade);
        if(detailed){
            for(int i=-4;i<=4;i++){
                float xx=i*.042f;
                d.surfaceOval(xx,1.158f,-.150f,.0045f,.137f,.004f,8,(i&1)==0?tankShade:tankHi);
            }
            d.surfaceOval(-.170f,1.318f,-.148f,.047f,.061f,.005f,10,tankHi);
            d.surfaceOval(.170f,1.318f,-.148f,.047f,.061f,.005f,10,tankHi);
        }

        // Long articulated arms.
        cjArm(m,d,-.318f,1.390f,leftArm,skin,skinHi,skinMid,skinShade,detailed);
        cjArm(m,d,.318f,1.390f,rightArm,skin,skinHi,skinMid,skinShade,detailed);

        // Neck and clavicle transition.
        d.taperedPrismY(0,1.382f,1.520f,0,.084f,.074f,.074f,.066f,12,skinShade);
        d.ellipsoid(0,1.405f,-.070f,.095f,.048f,.044f,5,12,skinMid);

        // Head follows camera look direction independently from body.
        m.push();
        m.translate(0,1.500f,0);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(clamp(p.headYaw,-62,62)));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(clamp(p.pitch,-36,36)*.66f));

        // GTA-SA-style head silhouette: elongated cranium, defined cheek/jaw, smaller chin.
        d.ellipsoid(0,.215f,.008f,.177f,.213f,.153f,detailed?11:8,detailed?20:14,skin);
        d.ellipsoid(0,.112f,-.019f,.161f,.146f,.151f,detailed?9:7,detailed?18:14,skinMid);
        d.ellipsoid(0,.018f,-.027f,.126f,.080f,.129f,7,14,skinShade);
        d.ellipsoid(-.185f,.184f,-.003f,.029f,.060f,.039f,5,10,skinShade);
        d.ellipsoid(.185f,.184f,-.003f,.029f,.060f,.039f,5,10,skinShade);

        // Caesar cut: close cap with a crisp low front hairline.
        d.ellipsoid(0,.356f,.010f,.174f,.071f,.146f,6,18,hair);
        d.surfaceOval(0,.313f,-.149f,.160f,.026f,.009f,18,hair);
        d.surfaceOval(-.078f,.301f,-.156f,.069f,.012f,.005f,12,StarCharacterGeometry.lighten(hair,.07f));
        d.surfaceOval(.078f,.301f,-.156f,.069f,.012f,.005f,12,StarCharacterGeometry.lighten(hair,.07f));

        // Brow ridge and eyes.
        d.surfaceOval(-.080f,.255f,-.166f,.061f,.014f,.008f,14,brow);
        d.surfaceOval(.080f,.255f,-.166f,.061f,.014f,.008f,14,brow);
        d.surfaceOval(-.073f,.207f,-.178f,.054f,.021f,.008f,16,eyeWhite);
        d.surfaceOval(.073f,.207f,-.178f,.054f,.021f,.008f,16,eyeWhite);
        float eyeShift=clamp(p.headYaw,-30,30)/30f*.011f;
        float eyeLift=-clamp(p.pitch,-25,25)/25f*.004f;
        d.surfaceOval(-.060f+eyeShift,.207f+eyeLift,-.189f,.016f,.017f,.006f,10,iris);
        d.surfaceOval(.060f+eyeShift,.207f+eyeLift,-.189f,.016f,.017f,.006f,10,iris);
        d.surfaceOval(-.060f+eyeShift,.207f+eyeLift,-.195f,.0065f,.008f,.003f,8,pupil);
        d.surfaceOval(.060f+eyeShift,.207f+eyeLift,-.195f,.0065f,.008f,.003f,8,pupil);

        // CJ's wider nose and nostrils.
        d.ellipsoid(0,.145f,-.186f,.040f,.060f,.047f,6,12,skinShade);
        d.ellipsoid(0,.108f,-.205f,.058f,.026f,.030f,5,12,skinHi);
        d.surfaceOval(-.034f,.105f,-.212f,.014f,.007f,.003f,8,skinDeep);
        d.surfaceOval(.034f,.105f,-.212f,.014f,.007f,.003f,8,skinDeep);

        // Lips + subtle mustache/goatee from the default San Andreas look.
        d.surfaceOval(0,.061f,-.195f,.084f,.012f,.008f,14,0xFF6A3933);
        d.surfaceOval(0,.043f,-.192f,.073f,.011f,.007f,14,0xFF4B2927);
        d.surfaceOval(0,.013f,-.178f,.058f,.022f,.007f,12,0xFF251A19);
        d.surfaceOval(0,.081f,-.189f,.022f,.006f,.004f,8,0xFF2C1D1B);

        if(detailed){
            // Cheek planes, under-eye shadows, ear detail and jaw stubble keep the face recognizably CJ-like.
            d.surfaceOval(-.139f,.134f,-.159f,.026f,.049f,.005f,10,skinHi);
            d.surfaceOval(.139f,.134f,-.159f,.026f,.049f,.005f,10,skinHi);
            d.surfaceOval(-.091f,.181f,-.170f,.044f,.010f,.004f,10,StarCharacterGeometry.shade(skin,.90f));
            d.surfaceOval(.091f,.181f,-.170f,.044f,.010f,.004f,10,StarCharacterGeometry.shade(skin,.90f));
            d.surfaceOval(-.100f,.035f,-.151f,.034f,.011f,.004f,8,0xFF4E3028);
            d.surfaceOval(.100f,.035f,-.151f,.034f,.011f,.004f,8,0xFF4E3028);
            d.surfaceOval(-.188f,.184f,-.026f,.010f,.026f,.004f,8,skinDeep);
            d.surfaceOval(.188f,.184f,-.026f,.010f,.026f,.004f,8,skinDeep);
        }
        m.pop();
    }

    private static void cjLeg(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float hip,float knee,
                              int denim,int denimHi,int denimMid,int denimDark,int shoe,int shoeHi,int sole,int lace,boolean detailed){
        m.push();
        m.translate(x,y,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(hip));

        // Baggy upper jeans narrowing toward the knee.
        d.taperedPrismY(0,-.405f,0,.006f,.116f,.120f,.136f,.138f,detailed?16:12,denim);
        if(detailed){
            d.surfaceOval(0,-.215f,-.136f,.009f,.165f,.005f,8,denimHi);
            d.surfaceOval(-.071f,-.124f,-.137f,.041f,.015f,.004f,8,denimHi);
            d.surfaceOval(.071f,-.124f,-.137f,.041f,.015f,.004f,8,denimHi);
        }

        m.translate(0,-.397f,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(knee));
        d.taperedPrismY(0,-.365f,.016f,.004f,.102f,.108f,.119f,.122f,detailed?16:12,denimDark);
        d.surfaceOval(0,-.172f,-.122f,.008f,.145f,.004f,8,denimMid);

        // Rounded black low-top tennis shoe with white/gray sole and lace rows.
        d.ellipsoid(0,-.397f,-.052f,.116f,.066f,.194f,6,detailed?16:12,shoe);
        d.ellipsoid(0,-.442f,-.058f,.122f,.027f,.207f,4,detailed?16:12,sole);
        d.surfaceOval(0,-.375f,-.222f,.090f,.020f,.009f,14,shoeHi);
        if(detailed){
            for(int i=-2;i<=2;i++)d.surfaceOval(i*.033f,-.380f,-.231f,.011f,.005f,.003f,8,lace);
            d.surfaceOval(0,-.412f,.128f,.086f,.012f,.004f,10,shoeHi);
        }
        m.pop();
    }

    private static void cjArm(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float swing,
                              int skin,int skinHi,int skinMid,int skinShade,boolean detailed){
        m.push();
        m.translate(x,y,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(swing));

        d.taperedPrismY(0,-.315f,0,0,.061f,.067f,.081f,.085f,detailed?16:12,skin);
        d.ellipsoid(0,-.315f,0,.070f,.068f,.074f,5,12,skinHi);

        m.translate(0,-.310f,0);
        float elbow=-.045f-Math.max(0,swing)*.10f;
        m.multiply(RotationAxis.POSITIVE_X.rotation(elbow));
        d.taperedPrismY(0,-.300f,.010f,0,.053f,.058f,.065f,.069f,detailed?16:12,skinMid);

        // Human-shaped palm + thumb + subtle finger segmentation.
        d.ellipsoid(0,-.347f,-.010f,.075f,.088f,.071f,6,detailed?16:12,skin);
        d.ellipsoid(-.060f,-.327f,-.027f,.020f,.043f,.028f,4,8,skinShade);
        if(detailed){
            d.surfaceOval(-.036f,-.374f,-.076f,.014f,.037f,.004f,8,skinShade);
            d.surfaceOval(0,-.380f,-.079f,.014f,.040f,.004f,8,skinShade);
            d.surfaceOval(.036f,-.374f,-.076f,.014f,.037f,.004f,8,skinShade);
        }
        m.pop();
    }

    private static void renderTung(MatrixStack m,VertexConsumerProvider consumers,Pose p,boolean detailed){
        StarCharacterGeometry.Drawer d=StarCharacterGeometry.begin(m,consumers);
        final int wood=0xFFB47636,woodLight=0xFFD79C58,woodMid=0xFF98602E,woodDark=0xFF74431F,woodDeep=0xFF4B2915;
        final int white=0xFFF9F6E9,black=0xFF16100E,mouth=0xFF31130F,tongue=0xFFD9574D;
        float amp=clamp(p.limbAmount,0,1);
        float step=(float)Math.sin(p.limbSwing*.6662f);
        float leftLeg=-step*.90f*amp,rightLeg=step*.90f*amp;
        float leftArm=-rightLeg*.74f;
        // Bat hand still animates, just with a slightly reduced swing so the bat does not clip through the body.
        float rightArm=-leftLeg*.48f;

        tungLeg(m,d,-.14f,.58f,leftLeg,woodDark,woodMid,woodLight,detailed);
        tungLeg(m,d,.14f,.58f,rightLeg,woodDark,woodMid,woodLight,detailed);
        tungArm(m,d,-.37f,1.45f,leftArm,woodDark,woodMid,woodLight,detailed,false);
        tungArm(m,d,.37f,1.45f,rightArm,woodDark,woodMid,woodLight,detailed,true);

        // Rounded tapered log body.
        m.push();
        m.translate(0,.55f,0);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(clamp(p.headYaw,-42,42)*.14f));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(clamp(p.pitch,-28,28)*.055f));
        d.taperedPrismY(0,.035f,1.485f,0,.300f,.260f,.342f,.292f,detailed?24:16,wood);
        d.ellipsoid(0,1.475f,0,.337f,.078f,.288f,5,detailed?24:16,woodLight);
        d.ellipsoid(0,.045f,0,.299f,.065f,.257f,5,detailed?22:14,woodDark);

        if(detailed){
            for(int i=-3;i<=3;i++){
                float xx=i*.071f;
                float yy=.62f+(i%3)*.09f;
                d.surfaceOval(xx,yy,-.294f,.010f,.48f,.006f,10,(i&1)==0?woodLight:woodMid);
            }
            d.surfaceOval(-.205f,.815f,-.300f,.050f,.035f,.008f,12,woodDeep);
            d.surfaceOval(-.205f,.815f,-.309f,.022f,.016f,.005f,10,woodLight);
            d.surfaceOval(.175f,1.245f,-.300f,.046f,.032f,.008f,12,woodDeep);
            d.surfaceOval(.020f,.565f,.270f,.052f,.040f,.007f,10,woodDark);
        }

        float eyeY=1.19f;
        d.surfaceOval(-.128f,eyeY,-.307f,.111f,.132f,.020f,18,white);
        d.surfaceOval(.128f,eyeY,-.307f,.111f,.132f,.020f,18,white);
        float pupilX=clamp(p.headYaw,-35,35)/35f*.035f,pupilY=-clamp(p.pitch,-28,28)/28f*.025f;
        d.surfaceOval(-.128f+pupilX,eyeY+pupilY,-.329f,.032f,.042f,.010f,12,black);
        d.surfaceOval(.128f+pupilX,eyeY+pupilY,-.329f,.032f,.042f,.010f,12,black);

        d.surfaceOval(-.130f,1.365f,-.310f,.112f,.022f,.010f,12,woodDeep);
        d.surfaceOval(.130f,1.365f,-.310f,.112f,.022f,.010f,12,woodDeep);
        d.surfaceOval(-.130f,1.397f,-.307f,.075f,.017f,.008f,10,woodDeep);
        d.surfaceOval(.130f,1.397f,-.307f,.075f,.017f,.008f,10,woodDeep);

        d.surfaceOval(0,.965f,-.318f,.185f,.083f,.018f,18,mouth);
        d.surfaceOval(0,.987f,-.334f,.145f,.038f,.010f,16,black);
        d.surfaceOval(0,.930f,-.339f,.106f,.036f,.010f,14,tongue);
        d.surfaceOval(0,1.035f,-.328f,.150f,.014f,.008f,14,black);
        m.pop();
    }

    private static void tungLeg(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float swing,
                                int woodDark,int woodMid,int woodLight,boolean detailed){
        m.push();
        m.translate(x,y,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
        d.taperedPrismY(0,-.48f,0,0,.040f,.044f,.056f,.058f,detailed?12:9,woodDark);
        d.ellipsoid(0,-.505f,0,.050f,.055f,.050f,5,10,woodMid);
        d.ellipsoid(0,-.555f,-.060f,.092f,.045f,.145f,5,detailed?12:9,woodLight);
        if(detailed)d.surfaceOval(0,-.560f,-.180f,.060f,.010f,.005f,8,woodMid);
        m.pop();
    }

    private static void tungArm(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float swing,
                                int woodDark,int woodMid,int woodLight,boolean detailed,boolean batHand){
        m.push();
        m.translate(x,y,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
        d.taperedPrismY(0,-.58f,0,0,.039f,.041f,.054f,.056f,detailed?12:9,woodDark);
        d.ellipsoid(0,-.610f,0,.050f,.052f,.050f,5,10,woodMid);
        d.ellipsoid(0,-.665f,-.008f,.069f,.082f,.064f,5,detailed?12:9,woodLight);
        if(detailed){
            d.surfaceOval(-.025f,-.688f,-.068f,.013f,.032f,.004f,8,woodMid);
            d.surfaceOval(.010f,-.694f,-.069f,.013f,.035f,.004f,8,woodMid);
            d.surfaceOval(.044f,-.682f,-.064f,.012f,.030f,.004f,8,woodMid);
        }
        if(batHand)renderTungBat(m,d,detailed);
        m.pop();
    }

    /** Detailed wooden bat parented to Tung's right hand, so every arm animation carries it correctly. */
    private static void renderTungBat(MatrixStack m,StarCharacterGeometry.Drawer d,boolean detailed){
        final int batDark=0xFF55331C, bat=0xFF9B6333, batLight=0xFFD19A5A, batGrain=0xFF6D4527;
        m.push();
        m.translate(.010f,-.680f,-.010f);
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10f));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(7f));

        // Handle passes through the palm, then widens into the barrel toward the ground.
        d.taperedPrismY(0,-.190f,.045f,0,.027f,.027f,.030f,.030f,detailed?14:10,batDark);
        d.ellipsoid(0,.055f,0,.043f,.030f,.043f,4,10,batDark); // knob
        d.taperedPrismY(0,-.780f,-.175f,0,.080f,.078f,.034f,.034f,detailed?18:12,bat);
        d.ellipsoid(0,-.790f,0,.080f,.050f,.078f,5,detailed?16:12,batLight);

        if(detailed){
            d.surfaceOval(-.020f,-.465f,-.079f,.010f,.235f,.004f,8,batLight);
            d.surfaceOval(.028f,-.515f,-.078f,.008f,.190f,.004f,8,batGrain);
            d.surfaceOval(0,-.315f,-.081f,.035f,.015f,.004f,10,batGrain);
            d.surfaceOval(0,-.650f,-.081f,.041f,.018f,.004f,10,batGrain);
        }
        m.pop();
    }

    private static float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
}
