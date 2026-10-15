package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/** Small 1.21.11 bridge for generated local textures. */
public final class StarTextureCompat {
    private StarTextureCompat() {}

    public static Identifier register(String path, NativeImage image) {
        Identifier id = Identifier.of("cinco_client", path);
        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> id.toString(), image);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
        return id;
    }
}
