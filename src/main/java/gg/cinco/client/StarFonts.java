package gg.cinco.client;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Lightweight font profiles using Minecraft's bundled renderers, so no external font assets are required. */
public final class StarFonts {
    private static final Identifier PIXEL = Identifier.of("minecraft", "default");
    private static final Identifier CLEAN = Identifier.of("minecraft", "uniform");

    private StarFonts() {}

    public static Text text(String value, boolean bold) {
        ClientModule font = CincoClient.findModule("Font Style");
        int mode = font == null ? 1 : Math.max(1, Math.min(4, (int) Math.round(font.setting)));

        Identifier id;
        boolean strong = bold;
        switch (mode) {
            case 2 -> id = CLEAN; // clean UI everywhere
            case 3 -> { id = PIXEL; strong = bold; } // classic Minecraft
            case 4 -> { id = CLEAN; strong = bold; } // compact/uniform UI
            default -> id = bold ? CLEAN : PIXEL; // Nebula Luxe pairing: modern titles + pixel secondary text
        }
        return Text.literal(value).setStyle(Style.EMPTY.withFont(id).withBold(strong));
    }

    public static String name(int mode) {
        return switch (mode) {
            case 2 -> "Clean Sans";
            case 3 -> "Classic Minecraft";
            case 4 -> "Compact UI";
            default -> "Nebula Duo";
        };
    }
}
