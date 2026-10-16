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
     * CJ always uses the lit high-detail path so he never falls back to a Minecraft-like low-detail body.
     */
    private static void renderCj(MatrixStack m,VertexConsumerProvider consumers,Pose p,boolean detailed){
        detailed=true;
        StarCharacterGeometry.Drawer d=StarCharacterGeometry.beginLit(m,consumers);

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

        // Hips / waist: slim center with the looser low-rise jeans CJ wears in San Andreas.
        d.taperedPrismY(0,.855f,.978f,0,.210f,.130f,.252f,.150f,20,denimDark);
        d.taperedPrismY(0,.945f,.988f,0,.255f,.158f,.255f,.158f,18,belt);
        d.ellipsoid(0,.967f,-.163f,.046f,.027f,.018f,5,14,buckle);
        for(int side:new int[]{-1,1}){
            float lx=.186f*side;
            d.taperedPrismY(lx,.944f,.990f,-.001f,.012f,.160f,.012f,.160f,10,beltHi);
        }

        cjLeg(m,d,-.132f,.945f,leftLeg,leftKnee,denim,denimHi,denimMid,denimDark,shoe,shoeHi,sole,lace,true);
        cjLeg(m,d,.132f,.945f,rightLeg,rightKnee,denim,denimHi,denimMid,denimDark,shoe,shoeHi,sole,lace,true);

        // GTA body proportions: noticeably wider clavicles/shoulders than waist, without a Steve rectangle.
        d.taperedPrismY(0,.980f,1.350f,0,.205f,.124f,.302f,.154f,24,skin);
        d.ellipsoid(-.294f,1.352f,0,.112f,.106f,.110f,8,20,skinHi);
        d.ellipsoid(.294f,1.352f,0,.112f,.106f,.110f,8,20,skinHi);
        d.ellipsoid(-.175f,1.315f,-.015f,.135f,.080f,.110f,7,18,skinMid);
        d.ellipsoid(.175f,1.315f,-.015f,.135f,.080f,.110f,7,18,skinMid);

        // Default white ribbed tank top from GTA:SA.
        d.taperedPrismY(0,.991f,1.326f,.004f,.201f,.132f,.264f,.145f,24,tank);
        d.taperedPrismY(-.176f,1.300f,1.407f,0,.046f,.138f,.058f,.142f,14,tank);
        d.taperedPrismY(.176f,1.300f,1.407f,0,.046f,.138f,.058f,.142f,14,tank);
        d.surfaceOval(0,1.358f,-.149f,.109f,.067f,.012f,20,skin);
        d.surfaceOval(0,1.012f,-.144f,.213f,.025f,.010f,18,tankShade);
        for(int i=-5;i<=5;i++){
            float xx=i*.035f;
            d.surfaceOval(xx,1.158f,-.150f,.0038f,.137f,.0035f,10,(i&1)==0?tankShade:tankHi);
        }
        d.surfaceOval(-.176f,1.318f,-.148f,.047f,.061f,.005f,12,tankHi);
        d.surfaceOval(.176f,1.318f,-.148f,.047f,.061f,.005f,12,tankHi);

        cjArm(m,d,-.330f,1.390f,leftArm,skin,skinHi,skinMid,skinShade,true);
        cjArm(m,d,.330f,1.390f,rightArm,skin,skinHi,skinMid,skinShade,true);

        d.taperedPrismY(0,1.382f,1.520f,0,.084f,.074f,.074f,.066f,16,skinShade);
        d.ellipsoid(0,1.405f,-.070f,.100f,.050f,.046f,6,16,skinMid);

        // Head follows camera look direction independently from body.
        m.push();
        m.translate(0,1.500f,0);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(clamp(p.headYaw,-62,62)));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(clamp(p.pitch,-36,36)*.66f));

        // San Andreas face silhouette: narrower temples, wider cheek plane, defined jaw/chin.
        d.ellipsoid(0,.222f,.010f,.172f,.211f,.151f,14,30,skin);
        d.ellipsoid(0,.132f,-.018f,.164f,.145f,.151f,12,28,skinMid);
        d.ellipsoid(0,.040f,-.026f,.132f,.090f,.132f,10,24,skinShade);
        d.ellipsoid(-.143f,.115f,-.048f,.050f,.078f,.112f,8,18,skinMid);
        d.ellipsoid(.143f,.115f,-.048f,.050f,.078f,.112f,8,18,skinMid);
        d.ellipsoid(-.183f,.185f,-.003f,.028f,.059f,.039f,6,14,skinShade);
        d.ellipsoid(.183f,.185f,-.003f,.028f,.059f,.039f,6,14,skinShade);

        // Caesar haircut / close crop.
        d.ellipsoid(0,.357f,.010f,.173f,.071f,.145f,8,26,hair);
        d.surfaceOval(0,.314f,-.149f,.158f,.024f,.008f,24,hair);
        d.surfaceOval(-.078f,.302f,-.156f,.069f,.011f,.004f,16,StarCharacterGeometry.lighten(hair,.07f));
        d.surfaceOval(.078f,.302f,-.156f,.069f,.011f,.004f,16,StarCharacterGeometry.lighten(hair,.07f));

        // Brows and slightly narrow brown eyes.
        d.surfaceOval(-.079f,.255f,-.166f,.060f,.013f,.007f,18,brow);
        d.surfaceOval(.079f,.255f,-.166f,.060f,.013f,.007f,18,brow);
        d.surfaceOval(-.072f,.207f,-.179f,.053f,.020f,.007f,20,eyeWhite);
        d.surfaceOval(.072f,.207f,-.179f,.053f,.020f,.007f,20,eyeWhite);
        float eyeShift=clamp(p.headYaw,-30,30)/30f*.010f;
        float eyeLift=-clamp(p.pitch,-25,25)/25f*.004f;
        d.surfaceOval(-.059f+eyeShift,.207f+eyeLift,-.190f,.015f,.016f,.006f,14,iris);
        d.surfaceOval(.059f+eyeShift,.207f+eyeLift,-.190f,.015f,.016f,.006f,14,iris);
        d.surfaceOval(-.059f+eyeShift,.207f+eyeLift,-.196f,.006f,.0075f,.003f,10,pupil);
        d.surfaceOval(.059f+eyeShift,.207f+eyeLift,-.196f,.006f,.0075f,.003f,10,pupil);

        // Wider nose bridge/tip and nostrils characteristic of CJ's model.
        d.ellipsoid(0,.151f,-.187f,.041f,.061f,.046f,8,18,skinShade);
        d.ellipsoid(0,.108f,-.207f,.060f,.027f,.031f,7,18,skinHi);
        d.surfaceOval(-.034f,.105f,-.214f,.014f,.007f,.003f,10,skinDeep);
        d.surfaceOval(.034f,.105f,-.214f,.014f,.007f,.003f,10,skinDeep);

        // Mouth and light mustache/goatee.
        d.surfaceOval(0,.062f,-.197f,.083f,.012f,.007f,18,0xFF6A3933);
        d.surfaceOval(0,.043f,-.194f,.072f,.011f,.006f,18,0xFF4B2927);
        d.surfaceOval(0,.014f,-.180f,.057f,.021f,.006f,16,0xFF251A19);
        d.surfaceOval(0,.082f,-.191f,.022f,.006f,.003f,12,0xFF2C1D1B);

        // Extra facial planes/stubble so the face reads like a GTA low-poly head from front and profile.
        d.surfaceOval(-.137f,.137f,-.160f,.028f,.050f,.005f,14,skinHi);
        d.surfaceOval(.137f,.137f,-.160f,.028f,.050f,.005f,14,skinHi);
        d.surfaceOval(-.090f,.181f,-.171f,.043f,.009f,.004f,14,StarCharacterGeometry.shade(skin,.90f));
        d.surfaceOval(.090f,.181f,-.171f,.043f,.009f,.004f,14,StarCharacterGeometry.shade(skin,.90f));
        d.surfaceOval(-.099f,.036f,-.153f,.034f,.011f,.004f,12,0xFF4E3028);
        d.surfaceOval(.099f,.036f,-.153f,.034f,.011f,.004f,12,0xFF4E3028);
        d.surfaceOval(-.186f,.184f,-.026f,.010f,.026f,.004f,10,skinDeep);
        d.surfaceOval(.186f,.184f,-.026f,.010f,.026f,.004f,10,skinDeep);
        m.pop();
    }

    private static void cjLeg(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float hip,float knee,
                              int denim,int denimHi,int denimMid,int denimDark,int shoe,int shoeHi,int sole,int lace,boolean detailed){
        m.push();
        m.translate(x,y,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(hip));

        d.taperedPrismY(0,-.405f,0,.006f,.116f,.120f,.136f,.138f,20,denim);
        d.surfaceOval(0,-.215f,-.136f,.009f,.165f,.005f,10,denimHi);
        d.surfaceOval(-.071f,-.124f,-.137f,.041f,.015f,.004f,10,denimHi);
        d.surfaceOval(.071f,-.124f,-.137f,.041f,.015f,.004f,10,denimHi);

        m.translate(0,-.397f,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(knee));
        d.taperedPrismY(0,-.365f,.016f,.004f,.102f,.108f,.119f,.122f,20,denimDark);
        d.surfaceOval(0,-.172f,-.122f,.008f,.145f,.004f,10,denimMid);

        d.ellipsoid(0,-.397f,-.052f,.116f,.066f,.194f,8,22,shoe);
        d.ellipsoid(0,-.442f,-.058f,.122f,.027f,.207f,5,22,sole);
        d.surfaceOval(0,-.375f,-.222f,.090f,.020f,.009f,18,shoeHi);
        for(int i=-2;i<=2;i++)d.surfaceOval(i*.033f,-.380f,-.231f,.011f,.005f,.003f,10,lace);
        d.surfaceOval(0,-.412f,.128f,.086f,.012f,.004f,12,shoeHi);
        m.pop();
    }

    private static void cjArm(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float swing,
                              int skin,int skinHi,int skinMid,int skinShade,boolean detailed){
        m.push();
        m.translate(x,y,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(swing));

        d.taperedPrismY(0,-.320f,0,0,.064f,.070f,.086f,.090f,20,skin);
        d.ellipsoid(0,-.318f,0,.074f,.071f,.078f,7,18,skinHi);

        m.translate(0,-.314f,0);
        float elbow=-.045f-Math.max(0,swing)*.10f;
        m.multiply(RotationAxis.POSITIVE_X.rotation(elbow));
        d.taperedPrismY(0,-.302f,.010f,0,.055f,.060f,.068f,.071f,20,skinMid);

        d.ellipsoid(0,-.350f,-.010f,.077f,.090f,.073f,8,20,skin);
        d.ellipsoid(-.061f,-.329f,-.027f,.020f,.043f,.028f,5,12,skinShade);
        d.surfaceOval(-.036f,-.376f,-.076f,.014f,.037f,.004f,10,skinShade);
        d.surfaceOval(0,-.382f,-.079f,.014f,.040f,.004f,10,skinShade);
        d.surfaceOval(.036f,-.376f,-.076f,.014f,.037f,.004f,10,skinShade);
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
        float rightArm=-leftLeg*.48f;

        tungLeg(m,d,-.14f,.58f,leftLeg,woodDark,woodMid,woodLight,detailed);
        tungLeg(m,d,.14f,.58f,rightLeg,woodDark,woodMid,woodLight,detailed);
        tungArm(m,d,-.37f,1.45f,leftArm,woodDark,woodMid,woodLight,detailed,false);
        tungArm(m,d,.37f,1.45f,rightArm,woodDark,woodMid,woodLight,detailed,true);

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

    /** Bat is parented to Tung's right hand and projects forward from the palm, not down at the ground. */
    private static void renderTungBat(MatrixStack m,StarCharacterGeometry.Drawer d,boolean detailed){
        final int batDark=0xFF55331C, bat=0xFF9B6333, batLight=0xFFD19A5A, batGrain=0xFF6D4527;
        m.push();
        m.translate(.010f,-.680f,-.028f);
        // Local -Y is the bat length. Rotating +82 degrees around X points the barrel forward (-Z).
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(82f));
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-8f));
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-6f));

        d.taperedPrismY(0,-.170f,.055f,0,.027f,.027f,.030f,.030f,detailed?14:10,batDark);
        d.ellipsoid(0,.062f,0,.043f,.030f,.043f,5,12,batDark);
        d.taperedPrismY(0,-.790f,-.160f,0,.082f,.079f,.034f,.034f,detailed?20:14,bat);
        d.ellipsoid(0,-.802f,0,.082f,.052f,.080f,6,detailed?18:14,batLight);

        if(detailed){
            d.surfaceOval(-.020f,-.465f,-.081f,.010f,.235f,.004f,10,batLight);
            d.surfaceOval(.028f,-.515f,-.080f,.008f,.190f,.004f,10,batGrain);
            d.surfaceOval(0,-.315f,-.083f,.035f,.015f,.004f,12,batGrain);
            d.surfaceOval(0,-.650f,-.083f,.041f,.018f,.004f,12,batGrain);
        }
        m.pop();
    }

    private static float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
}
