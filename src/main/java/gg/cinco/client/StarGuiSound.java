package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

public final class StarGuiSound {
    private StarGuiSound() {}

    public static void click(float pitch) {
        ClientModule module = CincoClient.findModule("GUI Sounds");
        MinecraftClient client = MinecraftClient.getInstance();
        if (module == null || !module.enabled || client == null || client.getSoundManager() == null) return;
        client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK,
                Math.max(0.65f, Math.min(1.5f, pitch))));
    }
}
