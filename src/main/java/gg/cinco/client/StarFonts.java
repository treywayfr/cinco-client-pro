package gg.cinco.client;

import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Font profiles based on Minecraft's bundled renderers. Minecraft TEN uses the crisp pixel renderer profile. */
public final class StarFonts {
    private static final StyleSpriteSource.Font PIXEL=new StyleSpriteSource.Font(Identifier.of("minecraft","default"));
    private static final StyleSpriteSource.Font CLEAN=new StyleSpriteSource.Font(Identifier.of("minecraft","uniform"));
    private StarFonts(){}
    public static Text text(String value,boolean bold){ClientModule font=CincoClient.findModule("Font Style");int mode=font==null?1:Math.max(1,Math.min(5,(int)Math.round(font.setting)));StyleSpriteSource.Font selected;boolean strong=bold;switch(mode){case 2->selected=CLEAN;case 3->{selected=PIXEL;strong=bold;}case 4->{selected=CLEAN;strong=bold;}case 5->{selected=PIXEL;strong=true;}default->selected=PIXEL;}return Text.literal(value).setStyle(Style.EMPTY.withFont(selected).withBold(strong));}
    public static String name(int mode){return switch(mode){case 2->"Clean Sans";case 3->"Classic Minecraft";case 4->"Compact UI";case 5->"Minecraft TEN";default->"Minecraft";};}
}
