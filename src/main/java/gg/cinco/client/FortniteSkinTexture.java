package gg.cinco.client;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

/** Generated local tactical skin texture retained for compatibility with older Player Changer paths. */
public final class FortniteSkinTexture {
    private static Identifier textureId;
    private FortniteSkinTexture() {}

    public static Identifier getTexture() {
        if (textureId != null) return textureId;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 64, 64, false);
        fill(image,0,0,64,64,0x00000000);
        int dark=0xFF17151C, main=0xFF4B2670, accent=0xFFA968FF, skin=0xFF875438, white=0xFFEAF4FF;
        // Head / face.
        rect(image,8,0,8,8,dark); rect(image,16,0,8,8,dark); rect(image,0,8,8,8,dark);
        rect(image,8,8,8,8,skin); rect(image,16,8,8,8,skin); rect(image,24,8,8,8,dark);
        rect(image,9,11,2,1,white); rect(image,13,11,2,1,white);
        // Torso / jacket.
        rect(image,20,16,8,4,dark); rect(image,28,16,8,4,dark); rect(image,16,20,4,12,dark);
        rect(image,20,20,8,12,main); rect(image,28,20,4,12,dark); rect(image,32,20,8,12,dark);
        rect(image,22,29,4,1,accent);
        // Arms.
        rect(image,44,16,4,4,main); rect(image,48,16,4,4,dark); rect(image,40,20,4,12,dark);
        rect(image,44,20,4,12,main); rect(image,48,20,4,12,dark); rect(image,52,20,4,12,dark);
        rect(image,36,48,4,4,main); rect(image,40,48,4,4,dark); rect(image,32,52,4,12,dark);
        rect(image,36,52,4,12,main); rect(image,40,52,4,12,dark); rect(image,44,52,4,12,dark);
        // Legs.
        rect(image,4,16,4,4,main); rect(image,8,16,4,4,dark); rect(image,0,20,4,12,dark);
        rect(image,4,20,4,12,main); rect(image,8,20,4,12,dark); rect(image,12,20,4,12,dark);
        rect(image,20,48,4,4,main); rect(image,24,48,4,4,dark); rect(image,16,52,4,12,dark);
        rect(image,20,52,4,12,main); rect(image,24,52,4,12,dark); rect(image,28,52,4,12,dark);
        textureId = StarTextureCompat.register("generated_tactical_skin", image);
        return textureId;
    }

    private static void rect(NativeImage image,int x,int y,int w,int h,int argb){int c=toAbgr(argb);for(int yy=Math.max(0,y);yy<Math.min(64,y+h);yy++)for(int xx=Math.max(0,x);xx<Math.min(64,x+w);xx++)image.setColor(xx,yy,c);}
    private static void fill(NativeImage image,int x,int y,int w,int h,int argb){rect(image,x,y,w,h,argb);}
    private static int toAbgr(int argb){int a=(argb>>>24)&255,r=(argb>>>16)&255,g=(argb>>>8)&255,b=argb&255;return(a<<24)|(b<<16)|(g<<8)|r;}
}
