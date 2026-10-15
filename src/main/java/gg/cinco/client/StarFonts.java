package gg.cinco.client;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Lightweight font profiles using Minecraft's bundled renderers, so no external font assets are required. */
public final class StarFonts {
    private StarFonts() {}

    public static Text text(String value, boolean bold) {
        ClientModule font = CincoClient.findModule("Font Style");
        int mode = font == null ? 1 : Math.max(1, Math.min(4, (int) Math.round(font.setting)));
        Identifier id = mode == 2 || mode == 4
                ? Identifier.of("minecraft", "uniform")
                : Identifier.of("minecraft", "default");
        boolean strong = bold || mode == 3;
        return Text.literal(value).setStyle(Style.EMPTY.withFont(id).withBold(strong));
    }

    public static String name(int mode) {
        return switch (mode) {
            case 2 -> "Clean Sans";
            case 3 -> "Classic Bold";
            case 4 -> "Compact UI";
            default -> "Minecraft Ten";
        };
    }
}
