package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Runtime-generated 64x64 player skins so Star's presets stay self-contained. */
public final class StarPlayerSkins {
    private static final Map<Integer, Identifier> IDS = new HashMap<>();

    private StarPlayerSkins() {}

    public static Identifier texture(int character, int outfit) {
        int key = Math.max(1, Math.min(5, character)) * 10 + Math.max(1, Math.min(6, outfit));
        Identifier cached = IDS.get(key);
        if (cached != null) return cached;

        NativeImage image = new NativeImage(64, 64, false);
        clear(image);
        paint(character, outfit, image);

        Identifier id = Identifier.of("cinco_client", "dynamic/player_" + key);
        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "Star Player " + key, image);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
        texture.upload();
        IDS.put(key, id);
        return id;
    }

    private static void clear(NativeImage image) {
        image.fillRect(0, 0, 64, 64, 0x00000000);
    }

    private static void paint(int character, int outfit, NativeImage image) {
        switch (Math.max(1, Math.min(5, character))) {
            case 2 -> bart(image, outfit);
            case 3 -> street(image, outfit);
            case 4 -> anime(image, outfit);
            case 5 -> cj(image, outfit);
            default -> peter(image, outfit);
        }
    }

    private static void peter(NativeImage i, int outfit) {
        int skin = 0xFFF2C7A5, hair = 0xFF4A2F22, white = 0xFFF7F6F0;
        int green = switch(outfit){case 2->0xFF426A35;case 3->0xFF264D63;case 4->0xFF6A3532;case 5->0xFF27292D;case 6->0xFF8A734D;default->0xFF4E7B3A;};
        base(i, skin, white, green, hair, 0xFF2F2A25);
        face(i, skin, 0xFF161616, 0xFFFFFFFF, 0xFF8B4D3E);
        rect(i, 9, 10, 3, 2, 0xFF161616); rect(i, 13, 10, 3, 2, 0xFF161616); rect(i, 12, 10, 1, 1, 0xFF161616);
        rect(i, 10, 14, 5, 1, 0xFF8A6A58);
    }

    private static void bart(NativeImage i, int outfit) {
        int yellow = 0xFFFFD536, orange = switch(outfit){case 2->0xFFEA7A25;case 3->0xFFDC5535;case 4->0xFFF0A02D;default->0xFFF06B2B;};
        int blue = 0xFF315FAE;
        base(i, yellow, orange, blue, yellow, 0xFFFFFFFF);
        face(i, yellow, 0xFF111111, 0xFFFFFFFF, 0xFF6B3A28);
        for (int x = 8; x < 16; x += 2) rect(i, x, 8, 1, 2 + ((x / 2) & 1), 0xFFFFE35A);
    }

    private static void street(NativeImage i, int outfit) {
        int skin = 0xFFB87652, hair = 0xFF171719;
        int hoodie = switch(outfit){case 2->0xFF2F6048;case 3->0xFF243B68;case 4->0xFF6A2F46;case 5->0xFF171A20;case 6->0xFFC7B89A;default->0xFF2B2C35;};
        base(i, skin, hoodie, 0xFF252B37, hair, 0xFF111318);
        face(i, skin, 0xFF141414, 0xFFF1E9DF, 0xFF6D392F);
        rect(i, 20, 21, 8, 2, shade(hoodie, .82f));
        rect(i, 23, 26, 2, 4, 0xFFC9A24C);
    }

    private static void anime(NativeImage i, int outfit) {
        int skin = 0xFFF0C3B2;
        int hair = switch(outfit){case 2->0xFF16171A;case 3->0xFF6C3A83;case 4->0xFFB65D78;case 5->0xFF365A86;default->0xFF2C254A;};
        int top = 0xFFF1ECF6, dark = 0xFF24283C;
        base(i, skin, top, dark, hair, 0xFF1A1B26);
        face(i, skin, 0xFF17151A, 0xFFF8F8FA, 0xFFC27679);
        rect(i, 9, 9, 6, 2, hair); rect(i, 8, 8, 2, 6, hair); rect(i, 15, 8, 1, 6, hair);
        rect(i, 9, 12, 2, 1, 0xFF6045A0); rect(i, 13, 12, 2, 1, 0xFF6045A0);
    }

    private static void cj(NativeImage i, int outfit) {
        int skin = 0xFF6B412E, hair = 0xFF101012, white = 0xFFF1F0EB;
        int top = switch(outfit){case 2->0xFF2F7044;case 3->0xFF17191C;case 4->0xFF7A2E31;case 5->0xFF3B754F;case 6->0xFF5C5E64;default->white;};
        int denim = switch(outfit){case 2->0xFF344638;case 3->0xFF202833;case 6->0xFF2B2C32;default->0xFF263A55;};
        base(i, skin, top, denim, hair, 0xFF101216);
        face(i, skin, 0xFF111112, 0xFFE7E1DA, 0xFF704238);
        rect(i, 8, 8, 8, 2, hair); rect(i, 9, 9, 6, 1, hair);
        if(outfit == 1){ rect(i, 22, 20, 4, 2, skin); rect(i, 23, 20, 2, 5, skin); }
        rect(i, 20, 31, 8, 1, 0xFF2A1C16);
    }

    private static void base(NativeImage i, int skin, int shirt, int pants, int hair, int shoes) {
        rect(i, 8, 8, 8, 8, skin); rect(i, 0, 8, 8, 8, skin); rect(i, 16, 8, 8, 8, skin); rect(i, 24, 8, 8, 8, skin);
        rect(i, 8, 0, 8, 8, hair); rect(i, 16, 0, 8, 8, skin);
        rect(i, 8, 8, 8, 2, hair); rect(i, 0, 8, 8, 2, hair); rect(i, 16, 8, 8, 2, hair); rect(i, 24, 8, 8, 2, hair);
        rect(i, 20, 20, 8, 12, shirt); rect(i, 16, 20, 4, 12, shade(shirt,.86f)); rect(i, 28, 20, 4, 12, shade(shirt,.92f)); rect(i, 32, 20, 8, 12, shade(shirt,.80f));
        rect(i, 20, 16, 8, 4, lighten(shirt,.08f)); rect(i, 28, 16, 8, 4, shade(shirt,.72f));
        rect(i, 44, 20, 4, 12, skin); rect(i, 40, 20, 4, 12, skin); rect(i, 48, 20, 4, 12, skin); rect(i, 52, 20, 4, 12, skin);
        rect(i, 44, 16, 4, 4, skin); rect(i, 48, 16, 4, 4, skin); rect(i, 44, 20, 4, 4, shirt);
        rect(i, 36, 52, 4, 12, skin); rect(i, 32, 52, 4, 12, skin); rect(i, 40, 52, 4, 12, skin); rect(i, 44, 52, 4, 12, skin);
        rect(i, 36, 48, 4, 4, skin); rect(i, 40, 48, 4, 4, skin); rect(i, 36, 52, 4, 4, shirt);
        rect(i, 4, 20, 4, 12, pants); rect(i, 0, 20, 4, 12, shade(pants,.86f)); rect(i, 8, 20, 4, 12, shade(pants,.92f)); rect(i, 12, 20, 4, 12, shade(pants,.80f));
        rect(i, 4, 16, 4, 4, pants); rect(i, 8, 16, 4, 4, pants); rect(i, 4, 29, 4, 3, shoes);
        rect(i, 20, 52, 4, 12, pants); rect(i, 16, 52, 4, 12, shade(pants,.86f)); rect(i, 24, 52, 4, 12, shade(pants,.92f)); rect(i, 28, 52, 4, 12, shade(pants,.80f));
        rect(i, 20, 48, 4, 4, pants); rect(i, 24, 48, 4, 4, pants); rect(i, 20, 61, 4, 3, shoes);
    }

    private static void face(NativeImage i, int skin, int eye, int white, int mouth) {
        rect(i, 8, 8, 8, 8, skin);
        rect(i, 9, 11, 2, 2, white); rect(i, 13, 11, 2, 2, white);
        rect(i, 10, 12, 1, 1, eye); rect(i, 13, 12, 1, 1, eye);
        rect(i, 11, 13, 2, 1, shade(skin,.82f));
        rect(i, 10, 15, 4, 1, mouth);
    }

    private static void rect(NativeImage i, int x, int y, int w, int h, int argb) {
        i.fillRect(x, y, w, h, argbToAbgr(argb));
    }

    private static int argbToAbgr(int argb) {
        int a=(argb>>>24)&255,r=(argb>>>16)&255,g=(argb>>>8)&255,b=argb&255;
        return (a<<24)|(b<<16)|(g<<8)|r;
    }

    private static int shade(int color, double k) {
        int a=(color>>>24)&255,r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;
        return (a<<24)|((int)(r*k)<<16)|((int)(g*k)<<8)|(int)(b*k);
    }

    private static int lighten(int color, double k) {
        int a=(color>>>24)&255,r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;
        r=(int)(r+(255-r)*k);g=(int)(g+(255-g)*k);b=(int)(b+(255-b)*k);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }
}
