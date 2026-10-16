package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/** Runtime white material used by Star's lit custom character mesh. */
public final class StarCharacterMaterial {
    private static Identifier id;
    private StarCharacterMaterial() {}

    public static Identifier white() {
        if (id != null) return id;
        NativeImage image = new NativeImage(4, 4, false);
        image.fillRect(0, 0, 4, 4, 0xFFFFFFFF);
        id = Identifier.of("cinco_client", "dynamic/character_white");
        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "Star Character Material", image);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
        texture.upload();
        return id;
    }
}
