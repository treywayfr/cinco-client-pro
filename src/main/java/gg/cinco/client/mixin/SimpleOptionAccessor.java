package gg.cinco.client.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Allows Fullbright to use gamma values beyond vanilla's 0..1 validated slider range. */
@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor {
    @Accessor("value")
    void cinco$setRawValue(Object value);
}
