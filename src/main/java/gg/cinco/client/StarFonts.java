package gg.cinco.client;

import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/** Font profiles with a small render cache for responsive high-density GUIs. */
public final class StarFonts {
    private static final StyleSpriteSource.Font PIXEL=new StyleSpriteSource.Font(Identifier.of("minecraft","default"));
    private static final StyleSpriteSource.Font CLEAN=new StyleSpriteSource.Font(Identifier.of("minecraft","uniform"));
    private static ClientModule fontModule;
    private static int cachedMode=-1;
    private static final int MAX_CACHE=320;
    private static final Map<Key,Text> CACHE=new LinkedHashMap<>(384,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<Key,Text> e){return size()>MAX_CACHE;}};
    private record Key(String value,boolean bold,int mode){}
    private StarFonts(){}

    public static Text text(String value,boolean bold){
        if(fontModule==null)fontModule=CincoClient.findModule("Font Style");
        int mode=fontModule==null?1:Math.max(1,Math.min(5,(int)Math.round(fontModule.setting)));
        if(mode!=cachedMode){CACHE.clear();cachedMode=mode;}
        String safe=value==null?"":value;Key key=new Key(safe,bold,mode);Text hit=CACHE.get(key);if(hit!=null)return hit;
        StyleSpriteSource.Font selected;boolean strong=bold;
        switch(mode){case 2->selected=CLEAN;case 3->{selected=PIXEL;strong=bold;}case 4->{selected=CLEAN;strong=bold;}case 5->{selected=PIXEL;strong=true;}default->selected=PIXEL;}
        Text out=Text.literal(safe).setStyle(Style.EMPTY.withFont(selected).withBold(strong));CACHE.put(key,out);return out;
    }
    public static String name(int mode){return switch(mode){case 2->"Clean Sans";case 3->"Classic Minecraft";case 4->"Compact UI";case 5->"Minecraft TEN";default->"Minecraft";};}
}
