package gg.cinco.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Separate persistent colors for player accessories and world cosmetic animations. */
public final class CosmeticColorStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("star-cosmetics.json");

    private static final Profile ACCESSORY = new Profile(276.0, 0.72, 1.0);
    private static final Profile EFFECT = new Profile(276.0, 0.78, 1.0);

    private CosmeticColorStore() {}

    private static final class Data {
        Profile accessory = new Profile(276.0, 0.72, 1.0);
        Profile effect = new Profile(276.0, 0.78, 1.0);
    }

    public static final class Profile {
        public double hue;
        public double saturation;
        public double brightness;
        public Profile() {}
        public Profile(double hue, double saturation, double brightness) {
            this.hue = hue;
            this.saturation = saturation;
            this.brightness = brightness;
        }
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            Data d = GSON.fromJson(Files.readString(FILE), Data.class);
            if (d == null) return;
            if (d.accessory != null) copySanitized(d.accessory, ACCESSORY);
            if (d.effect != null) copySanitized(d.effect, EFFECT);
        } catch (Exception ignored) {
            // Cosmetics must never prevent the client from starting.
        }
    }

    public static void save() {
        try {
            Data d = new Data();
            d.accessory = new Profile(ACCESSORY.hue, ACCESSORY.saturation, ACCESSORY.brightness);
            d.effect = new Profile(EFFECT.hue, EFFECT.saturation, EFFECT.brightness);
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(d));
        } catch (IOException ignored) {}
    }

    public static Profile accessory() { return ACCESSORY; }
    public static Profile effect() { return EFFECT; }

    public static int accessoryColor(int alpha) { return color(ACCESSORY, alpha); }
    public static int effectColor(int alpha) { return color(EFFECT, alpha); }

    public static int color(Profile p, int alpha) {
        double[] rgb = hsv(p.hue / 360.0, p.saturation, p.brightness);
        int r = (int)Math.round(rgb[0] * 255.0);
        int g = (int)Math.round(rgb[1] * 255.0);
        int b = (int)Math.round(rgb[2] * 255.0);
        return (clamp255(alpha) << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lighten(int color, double amount, int alpha) {
        int r = (color >>> 16) & 255, g = (color >>> 8) & 255, b = color & 255;
        r = (int)Math.round(r + (255 - r) * amount);
        g = (int)Math.round(g + (255 - g) * amount);
        b = (int)Math.round(b + (255 - b) * amount);
        return (clamp255(alpha) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    public static int darken(int color, double amount, int alpha) {
        int r = (color >>> 16) & 255, g = (color >>> 8) & 255, b = color & 255;
        double k = Math.max(0.0, 1.0 - amount);
        return (clamp255(alpha) << 24) | ((int)(r*k) << 16) | ((int)(g*k) << 8) | (int)(b*k);
    }

    private static void copySanitized(Profile from, Profile to) {
        to.hue = ((from.hue % 360.0) + 360.0) % 360.0;
        to.saturation = clamp01(from.saturation);
        to.brightness = clamp01(from.brightness);
    }

    private static double[] hsv(double hue, double saturation, double value) {
        double h = ((hue % 1.0) + 1.0) % 1.0;
        double s = clamp01(saturation), v = clamp01(value);
        double chroma = v * s;
        double section = h * 6.0;
        double x = chroma * (1.0 - Math.abs(section % 2.0 - 1.0));
        double r=0,g=0,b=0;
        if(section<1){r=chroma;g=x;} else if(section<2){r=x;g=chroma;} else if(section<3){g=chroma;b=x;}
        else if(section<4){g=x;b=chroma;} else if(section<5){r=x;b=chroma;} else {r=chroma;b=x;}
        double m=v-chroma;
        return new double[]{r+m,g+m,b+m};
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }
    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
