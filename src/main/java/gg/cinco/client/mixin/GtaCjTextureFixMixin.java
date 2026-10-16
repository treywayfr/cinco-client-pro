package gg.cinco.client.mixin;

import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Sanitizes the generated GTA CJ atlas before Minecraft uploads it.
 *
 * The source atlas is built from many tiny per-triangle texture cells. Any transparent or partially
 * transparent source texel becomes a literal cutout hole because CJ is rendered on an entity cutout
 * layer. CJ's body/clothing texture is supposed to be opaque, so fill those pixels from the nearest
 * usable texel inside the SAME 18x18 triangle cell and then force alpha to 255. Keeping the search
 * inside each source cell prevents colors from a neighboring triangle/material bleeding into it.
 */
@Mixin(targets = "gg.cinco.client.GtaCjImportedModel", remap = false)
public abstract class GtaCjTextureFixMixin {
    private static final int STAR_CJ_ATLAS_CELL = 18;

    @ModifyVariable(
            method = "lambda$bootstrap$1",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private static NativeImage star$fillCjAtlasHoles(NativeImage image) {
        if (image == null) return null;

        int width = image.getWidth();
        int height = image.getHeight();
        int[] original = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                original[y * width + x] = image.getColorArgb(x, y);
            }
        }

        for (int y = 0; y < height; y++) {
            int cellY0 = (y / STAR_CJ_ATLAS_CELL) * STAR_CJ_ATLAS_CELL;
            int cellY1 = Math.min(height - 1, cellY0 + STAR_CJ_ATLAS_CELL - 1);
            for (int x = 0; x < width; x++) {
                int color = original[y * width + x];
                int alpha = color >>> 24;
                if (alpha >= 250) {
                    image.setColorArgb(x, y, color | 0xFF000000);
                    continue;
                }

                int cellX0 = (x / STAR_CJ_ATLAS_CELL) * STAR_CJ_ATLAS_CELL;
                int cellX1 = Math.min(width - 1, cellX0 + STAR_CJ_ATLAS_CELL - 1);
                int replacement = -1;

                // Small nearest-neighbour inpaint. The converter gives every triangle a generous
                // gutter, so a 5px radius is enough to close pinholes/alpha fringe without smearing.
                search:
                for (int radius = 1; radius <= 5; radius++) {
                    int minX = Math.max(cellX0, x - radius);
                    int maxX = Math.min(cellX1, x + radius);
                    int minY = Math.max(cellY0, y - radius);
                    int maxY = Math.min(cellY1, y + radius);
                    for (int yy = minY; yy <= maxY; yy++) {
                        for (int xx = minX; xx <= maxX; xx++) {
                            if (Math.abs(xx - x) != radius && Math.abs(yy - y) != radius) continue;
                            int candidate = original[yy * width + xx];
                            if ((candidate >>> 24) >= 250) {
                                replacement = candidate;
                                break search;
                            }
                        }
                    }
                }

                if (replacement < 0) replacement = color;
                image.setColorArgb(x, y, replacement | 0xFF000000);
            }
        }
        return image;
    }
}
