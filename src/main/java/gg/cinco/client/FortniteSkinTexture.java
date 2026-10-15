package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Generates a local 64x64 Minecraft skin texture with a battle-royale-inspired tactical suit.
 * This changes only the local player's rendered skin, not their actual Mojang skin or other players.
 */
public final class FortniteSkinTexture {
    private static Identifier textureId;
    private static int previousOutfit = -1;
    private static int previousPalette = -1;
    private static int previousVisor = -1;

    private FortniteSkinTexture() {}

    public static Identifier getTexture() {
        ClientModule styleModule = CincoClient.findModule("Fortnite Style");
        int outfit = styleModule == null ? 1 : (int) Math.round(styleModule.setting);
        int palette = styleModule == null ? 1 : (int) Math.round(styleModule.setting2);
        int visor = styleModule == null ? 1 : (int) Math.round(styleModule.setting3);
        if (textureId != null && outfit == previousOutfit && palette == previousPalette && visor == previousVisor) {
            return textureId;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (textureId != null) client.getTextureManager().destroyTexture(textureId);

        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 64, 64, false);
        fill(image, 0, 0, 64, 64, 0x00000000);

        int[][] palettes = {
                {0xFF172944, 0xFF2D5F94, 0xFFCE8A32, 0xFF111722},
                {0xFF351D25, 0xFF9A303E, 0xFFFFB347, 0xFF191316},
                {0xFF103738, 0xFF1C8A82, 0xFFE6C56C, 0xFF101D22},
                {0xFF29203F, 0xFF7250A4, 0xFF65D7F0, 0xFF171421}
        };
        int[] p = palettes[Math.floorMod(palette - 1 + outfit - 1, palettes.length)];
        int dark = p[0], main = p[1], trim = p[2], shadow = p[3];
        int skin = 0xFFD6A17D, hair = 0xFF211815, white = 0xFFEAF4FF;

        // Head base: helmet, face, sides and rear.
        rect(image, 8, 0, 8, 8, shadow);       // head top
        rect(image, 16, 0, 8, 8, dark);        // head underside
        rect(image, 0, 8, 8, 8, dark);         // right side
        rect(image, 8, 8, 8, 8, skin);         // face
        rect(image, 16, 8, 8, 8, skin);        // left side
        rect(image, 24, 8, 8, 8, hair);        // back of head
        rect(image, 8, 8, 8, 2, hair);         // hairline
        rect(image, 9, 11, 2, 1, white);       // eyes
        rect(image, 13, 11, 2, 1, white);
        rect(image, 9, 12, 2, 1, trim);
        rect(image, 13, 12, 2, 1, trim);
        rect(image, 11, 14, 2, 1, 0xFF985E4E); // mouth
        // Helmet overlay. A clear visor stripe makes the face look more like a hero skin.
        rect(image, 40, 0, 8, 8, main);
        rect(image, 48, 0, 8, 8, shadow);
        rect(image, 32, 8, 8, 8, dark);
        rect(image, 40, 8, 8, 8, main);
        rect(image, 48, 8, 8, 8, dark);
        rect(image, 56, 8, 8, 8, shadow);
        rect(image, 40, 10, 8, 3, visor == 1 ? trim : shadow);
        rect(image, 42, 10, 4, 1, white);

        // Torso base: armored front, back, side panels and belt.
        rect(image, 20, 16, 8, 4, dark);
        rect(image, 28, 16, 8, 4, shadow);
        rect(image, 16, 20, 4, 12, shadow);
        rect(image, 20, 20, 8, 12, main);
        rect(image, 28, 20, 4, 12, dark);
        rect(image, 32, 20, 8, 12, shadow);
        rect(image, 20, 20, 8, 3, trim);
        rect(image, 21, 24, 2, 5, dark);
        rect(image, 25, 24, 2, 5, dark);
        rect(image, 20, 29, 8, 3, shadow);
        rect(image, 22, 29, 4, 1, trim);
        // Jacket / shoulder-pad overlay.
        rect(image, 20, 36, 8, 4, main);
        rect(image, 28, 36, 8, 4, dark);
        rect(image, 16, 40, 4, 8, shadow);
        rect(image, 20, 40, 8, 8, main);
        rect(image, 28, 40, 4, 8, dark);
        rect(image, 32, 40, 8, 8, shadow);
        rect(image, 20, 40, 8, 2, trim);
        rect(image, 23, 43, 2, 3, trim);

        // Right arm base and sleeve.
        rect(image, 44, 16, 4, 4, main);
        rect(image, 48, 16, 4, 4, dark);
        rect(image, 40, 20, 4, 12, shadow);
        rect(image, 44, 20, 4, 12, main);
        rect(image, 48, 20, 4, 12, dark);
        rect(image, 52, 20, 4, 12, shadow);
        rect(image, 44, 20, 4, 3, trim);
        // Left arm base.
        rect(image, 36, 48, 4, 4, main);
        rect(image, 40, 48, 4, 4, dark);
        rect(image, 32, 52, 4, 12, shadow);
        rect(image, 36, 52, 4, 12, main);
        rect(image, 40, 52, 4, 12, dark);
        rect(image, 44, 52, 4, 12, shadow);
        rect(image, 36, 52, 4, 3, trim);
        // Boots / tactical trousers for both legs.
        rect(image, 4, 16, 4, 4, main);
        rect(image, 8, 16, 4, 4, shadow);
        rect(image, 0, 20, 4, 12, dark);
        rect(image, 4, 20, 4, 12, main);
        rect(image, 8, 20, 4, 12, shadow);
        rect(image, 12, 20, 4, 12, dark);
        rect(image, 4, 27, 4, 5, shadow);
        rect(image, 4, 30, 4, 2, trim);
        rect(image, 20, 48, 4, 4, main);
        rect(image, 24, 48, 4, 4, shadow);
        rect(image, 16, 52, 4, 12, dark);
        rect(image, 20, 52, 4, 12, main);
        rect(image, 24, 52, 4, 12, shadow);
        rect(image, 28, 52, 4, 12, dark);
        rect(image, 20, 59, 4, 5, shadow);
        rect(image, 20, 62, 4, 2, trim);
        // Armor details on outer layers and small bright accents.
        rect(image, 44, 32, 4, 4, dark);
        rect(image, 48, 32, 4, 4, shadow);
        rect(image, 40, 36, 4, 8, shadow);
        rect(image, 44, 36, 4, 8, main);
        rect(image, 48, 36, 4, 8, dark);
        rect(image, 52, 36, 4, 8, shadow);
        rect(image, 36, 48, 4, 1, trim);
        rect(image, 20, 48, 4, 1, trim);
        rect(image, 24, 40, 2, 2, trim);

        textureId = client.getTextureManager().registerDynamicTexture("cinco_fortnite_style", new NativeImageBackedTexture(image));
        previousOutfit = outfit;
        previousPalette = palette;
        previousVisor = visor;
        return textureId;
    }

    private static void rect(NativeImage image, int x, int y, int width, int height, int argb) {
        if (width <= 0 || height <= 0) return;
        int color = toAbgr(argb);
        int xEnd = Math.min(64, x + width), yEnd = Math.min(64, y + height);
        for (int yy = Math.max(0, y); yy < yEnd; yy++) {
            for (int xx = Math.max(0, x); xx < xEnd; xx++) image.setColor(xx, yy, color);
        }
    }

    private static void fill(NativeImage image, int x, int y, int width, int height, int argb) {
        rect(image, x, y, width, height, argb);
    }

    private static int toAbgr(int argb) {
        int a = (argb >>> 24) & 255;
        int r = (argb >>> 16) & 255;
        int g = (argb >>> 8) & 255;
        int b = argb & 255;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
