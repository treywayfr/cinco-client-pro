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

    /** Default GTA: San Andreas CJ silhouette: slim build, white tank, loose jeans, black shoes. */
    private static void renderCj(MatrixStack m,VertexConsumerProvider consumers,Pose p,boolean detailed){
        StarCharacterGeometry.Drawer d=StarCharacterGeometry.begin(m,consumers);
        final int skin=0xFF70432F,skinLight=0xFF87563D,skinShade=0xFF4B2C21,skinDeep=0xFF322018;
        final int hair=0xFF0F1011,brow=0xFF171214,eyeWhite=0xFFF0ECE7,iris=0xFF3B281C,pupil=0xFF090708;
        final int white=0xFFF2F0E9,whiteShade=0xFFD8D6CF;
        final int denim=0xFF2C4160,denimHi=0xFF3C5878,denimDark=0xFF1A2A40;
        final int shoe=0xFF111216,shoeHi=0xFF28292D,sole=0xFFD8D7D2,belt=0xFF2A1B17,buckle=0xFFB99554;

        float amount=clamp(p.limbAmount,0,1);
        float walk=(float)Math.sin(p.limbSwing*.6662f);
        float leftLeg=walk*.76f*amount,rightLeg=-leftLeg;
        float leftArm=-leftLeg*.74f,rightArm=-rightLeg*.74f;
        float leftKnee=Math.max(0f,-walk)*.46f*amount;
        float rightKnee=Math.max(0f,walk)*.46f*amount;

        // Hips and belt. Only the buckle/loops remain hard-edged details.
        d.taperedPrismY(0,.865f,.975f,0,.225f,.138f,.248f,.148f,detailed?14:10,denimDark);
        d.taperedPrismY(0,.942f,.985f,0,.255f,.159f,.255f,.159f,12,belt);
        d.box(-.042f,.944f,-.174f,.042f,.986f,-.158f,buckle);
        for(int side:new int[]{-1,1}){float x=.185f*side;d.box(x-.014f,.944f,-.171f,x+.014f,.984f,-.158f,0xFF5B4337);}

        cjLeg(m,d,-.13f,.94f,leftLeg,leftKnee,denim,denimHi,denimDark,shoe,shoeHi,sole,detailed);
        cjLeg(m,d,.13f,.94f,rightLeg,rightKnee,denim,denimHi,denimDark,shoe,shoeHi,sole,detailed);

        // Human torso uses a tapered mesh rather than a Steve-like box.
        d.taperedPrismY(0,.97f,1.385f,0,.215f,.132f,.286f,.150f,detailed?16:12,skin);
        d.ellipsoid(-.276f,1.355f,0,.105f,.105f,.105f,6,12,skinLight);
        d.ellipsoid(.276f,1.355f,0,.105f,.105f,.105f,6,12,skinLight);

        // Tank top follows the torso taper and keeps the characteristic low scoop neck.
        d.taperedPrismY(0,.985f,1.325f,.003f,.206f,.137f,.253f,.145f,detailed?16:12,white);
        d.taperedPrismY(-.167f,1.305f,1.405f,0,.047f,.140f,.055f,.142f,10,white);
        d.taperedPrismY(.167f,1.305f,1.405f,0,.047f,.140f,.055f,.142f,10,white);
        d.surfaceOval(0,1.356f,-.148f,.105f,.067f,.012f,14,skin);
        d.surfaceOval(0,1.010f,-.145f,.215f,.026f,.010f,14,whiteShade);
        if(detailed){
            for(int i=-4;i<=4;i++){
                float xx=i*.043f;
                d.surfaceOval(xx,1.16f,-.149f,.006f,.132f,.006f,8,(i&1)==0?whiteShade:0xFFF9F7F1);
            }
        }

        cjArm(m,d,-.315f,1.385f,leftArm,skin,skinLight,skinShade,detailed);
        cjArm(m,d,.315f,1.385f,rightArm,skin,skinLight,skinShade,detailed);

        d.taperedPrismY(0,1.385f,1.525f,0,.083f,.075f,.076f,.069f,12,skinShade);

        // Head follows look direction and is built from layered ellipsoids instead of a cuboid skull.
        m.push();
        m.translate(0,1.505f,0);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(clamp(p.headYaw,-62,62)));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(clamp(p.pitch,-36,36)*.68f));

        d.ellipsoid(0,.205f,.006f,.184f,.207f,.159f,detailed?10:7,detailed?18:12,skin);
        d.ellipsoid(0,.078f,-.018f,.157f,.133f,.151f,detailed?8:6,detailed?16:12,skinShade);
        d.ellipsoid(0,.010f,-.026f,.121f,.073f,.126f,6,12,skinShade);
        d.ellipsoid(-.188f,.181f,-.002f,.030f,.061f,.040f,5,10,skinShade);
        d.ellipsoid(.188f,.181f,-.002f,.030f,.061f,.040f,5,10,skinShade);

        // Caesar haircut: rounded cap plus a deliberately straighter front hairline.
        d.ellipsoid(0,.350f,.012f,.177f,.074f,.149f,6,16,hair);
        d.surfaceOval(0,.309f,-.151f,.164f,.028f,.011f,16,hair);
        if(detailed){
            d.surfaceOval(-.072f,.297f,-.158f,.068f,.014f,.006f,10,StarCharacterGeometry.lighten(hair,.08f));
            d.surfaceOval(.072f,.297f,-.158f,.068f,.014f,.006f,10,StarCharacterGeometry.lighten(hair,.08f));
        }

        // Rounded facial features keep CJ recognizable without Minecraft-pixel anatomy.
        d.surfaceOval(-.080f,.252f,-.166f,.060f,.015f,.009f,12,brow);
        d.surfaceOval(.080f,.252f,-.166f,.060f,.015f,.009f,12,brow);
        d.surfaceOval(-.073f,.207f,-.177f,.055f,.022f,.009f,14,eyeWhite);
        d.surfaceOval(.073f,.207f,-.177f,.055f,.022f,.009f,14,eyeWhite);
        float eyeShift=clamp(p.headYaw,-30,30)/30f*.012f;
        d.surfaceOval(-.061f+eyeShift,.207f,-.188f,.017f,.018f,.007f,10,iris);
        d.surfaceOval(.061f+eyeShift,.207f,-.188f,.017f,.018f,.007f,10,iris);
        d.surfaceOval(-.061f+eyeShift,.207f,-.195f,.007f,.009f,.004f,8,pupil);
        d.surfaceOval(.061f+eyeShift,.207f,-.195f,.007f,.009f,.004f,8,pupil);

        d.ellipsoid(0,.139f,-.184f,.043f,.061f,.050f,5,10,skinShade);
        d.ellipsoid(0,.106f,-.205f,.059f,.026f,.031f,5,10,skinLight);
        d.surfaceOval(-.034f,.105f,-.211f,.015f,.008f,.004f,8,skinDeep);
        d.surfaceOval(.034f,.105f,-.211f,.015f,.008f,.004f,8,skinDeep);

        d.surfaceOval(0,.059f,-.194f,.086f,.013f,.009f,12,0xFF6A3933);
        d.surfaceOval(0,.040f,-.191f,.075f,.012f,.008f,12,0xFF4D2928);
        d.surfaceOval(0,.010f,-.178f,.060f,.023f,.008f,12,0xFF251A19);
        d.surfaceOval(0,.078f,-.188f,.022f,.007f,.005f,8,0xFF2C1D1B);

        if(detailed){
            d.surfaceOval(-.139f,.128f,-.158f,.027f,.050f,.006f,10,skinLight);
            d.surfaceOval(.139f,.128f,-.158f,.027f,.050f,.006f,10,skinLight);
            d.surfaceOval(-.098f,.034f,-.151f,.034f,.012f,.005f,8,0xFF4E3028);
            d.surfaceOval(.098f,.034f,-.151f,.034f,.012f,.005f,8,0xFF4E3028);
        }
        m.pop();
    }

    private static void cjLeg(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float hip,float knee,
                              int denim,int denimHi,int denimDark,int shoe,int shoeHi,int sole,boolean detailed){
        m.push();
        m.translate(x,y,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(hip));
        d.taperedPrismY(0,-.405f,0,.005f,.118f,.122f,.132f,.135f,detailed?14:10,denim);
        if(detailed){
            d.surfaceOval(0,-.220f,-.134f,.011f,.160f,.006f,8,denimHi);
            d.surfaceOval(-.070f,-.125f,-.136f,.040f,.016f,.005f,8,denimHi);
            d.surfaceOval(.070f,-.125f,-.136f,.040f,.016f,.005f,8,denimHi);
        }
        m.translate(0,-.395f,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(knee));
        d.taperedPrismY(0,-.365f,.015f,.003f,.105f,.110f,.119f,.122f,detailed?14:10,denimDark);
        if(detailed)d.surfaceOval(0,-.175f,-.122f,.009f,.145f,.005f,8,denimHi);

        // Rounded low tennis shoe rather than a rectangular foot block.
        d.ellipsoid(0,-.395f,-.045f,.116f,.067f,.192f,6,detailed?14:10,shoe);
        d.ellipsoid(0,-.440f,-.055f,.121f,.026f,.205f,4,detailed?14:10,sole);
        d.surfaceOval(0,-.377f,-.220f,.089f,.022f,.010f,12,shoeHi);
        if(detailed){
            for(int i=-2;i<=2;i++)d.surfaceOval(i*.034f,-.382f,-.229f,.012f,.006f,.004f,8,0xFF9B9A96);
        }
        m.pop();
    }

    private static void cjArm(MatrixStack m,StarCharacterGeometry.Drawer d,float x,float y,float swing,
                              int skin,int skinLight,int skinShade,boolean detailed){
        m.push();
        m.translate(x,y,0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
        d.taperedPrismY(0,-.315f,0,0,.061f,.068f,.079f,.084f,detailed?14:10,skin);
        d.ellipsoid(0,-.315f,0,.071f,.067f,.075f,5,10,skinLight);
        m.translate(0,-.310f,0);
        float elbow=-.055f-Math.max(0,swing)*.12f;
        m.multiply(RotationAxis.POSITIVE_X.rotation(elbow));
        d.taperedPrismY(0,-.300f,.010f,0,.055f,.061f,.066f,.070f,detailed?14:10,skinLight);
        d.ellipsoid(0,-.347f,-.010f,.077f,.090f,.073f,6,detailed?14:10,skin);
        if(detailed){
            d.surfaceOval(-.036f,-.374f,-.078f,.015f,.038f,.005f,8,skinShade);
            d.surfaceOval(0,-.380f,-.081f,.015f,.041f,.005f,8,skinShade);
            d.surfaceOval(.036f,-.374f,-.078f,.015f,.038f,.005f,8,skinShade);
            d.ellipsoid(-.063f,-.330f,-.030f,.021f,.045f,.030f,4,8,skinShade);
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
        float leftArm=-rightLeg*.74f,rightArm=-leftLeg*.74f;

        tungLeg(m,d,-.14f,.58f,leftLeg,woodDark,woodMid,woodLight,detailed);
        tungLeg(m,d,.14f,.58f,rightLeg,woodDark,woodMid,woodLight,detailed);
        tungArm(m,d,-.37f,1.45f,leftArm,woodDark,woodMid,woodLight,detailed);
        tungArm(m,d,.37f,1.45f,rightArm,woodDark,woodMid,woodLight,detailed);

        // Rounded tapered log body, closer to the reference model than a straight Minecraft cylinder.
        m.push();
        m.translate(0,.55f,0);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(clamp(p.headYaw,-42,42)*.14f));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(clamp(p.pitch,-28,28)*.055f));
        d.taperedPrismY(0,.035f,1.485f,0,.300f,.260f,.342f,.292f,detailed?24:16,wood);
        d.ellipsoid(0,1.475f,0,.337f,.078f,.288f,5,detailed?24:16,woodLight);
        d.ellipsoid(0,.045f,0,.299f,.065f,.257f,5,detailed?22:14,woodDark);

        // Wood grain is laid onto the curved front as thin rounded streaks/knots, not square strips.
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
                                int woodDark,int woodMid,int woodLight,boolean detailed){
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
        m.pop();
    }

    private static float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
}
