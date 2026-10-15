package gg.cinco.client;

import org.lwjgl.glfw.GLFW;

public final class ClientModule {
    public enum Category {
        COMBAT("COMBAT"), MOVEMENT("MOVEMENT"), RENDER("RENDER"),
        WORLD("WORLD"), UTILITY("UTILITY"), PRACTICE("PRACTICE"), SETTINGS("SETTINGS");
        public final String label;
        Category(String label) { this.label = label; }
    }

    public final String name;
    public final String description;
    public final Category category;

    // Main feature-specific setting, kept stable for the gameplay modules.
    public final String settingLabel;
    public final double min, max, step;
    public double setting;
    public final double defaultSetting;

    // Additional per-module controls, shown in the module detail screen and persisted.
    public final String setting2Label;
    public final double min2, max2, step2;
    public double setting2;
    public final double defaultSetting2;
    public final String setting3Label;
    public final double min3, max3, step3;
    public double setting3;
    public final double defaultSetting3;

    public boolean enabled;
    public int keyCode = GLFW.GLFW_KEY_UNKNOWN;

    public ClientModule(String name, String description, Category category, boolean enabled,
                        String settingLabel, double setting, double min, double max, double step) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = enabled;
        this.settingLabel = settingLabel;
        this.setting = setting;
        this.min = min;
        this.max = max;
        this.step = step;

        // Sensible extra controls for every module; key gameplay options have specific labels/ranges.
        if (name.equals("Color Wheel")) {
            setting2Label = "Saturation"; setting2 = 0.90; min2 = 0.10; max2 = 1.0; step2 = 0.05;
            setting3Label = "Brightness"; setting3 = 1.0; min3 = 0.15; max3 = 1.0; step3 = 0.05;
        } else if (name.equals("Anchor Macro")) {
            setting2Label = "Switch delay"; setting2 = 1; min2 = 0; max2 = 5; step2 = 1;
            setting3Label = "Charge retries"; setting3 = 2; min3 = 1; max3 = 4; step3 = 1;
            keyCode = GLFW.GLFW_KEY_G;
        } else if (name.equals("Key Pearl")) {
            setting2Label = "Restore delay"; setting2 = 2; min2 = 1; max2 = 10; step2 = 1;
            setting3Label = "Restore slot"; setting3 = 1; min3 = 0; max3 = 1; step3 = 1;
            keyCode = GLFW.GLFW_KEY_V;
        } else if (name.equals("FOV Changer")) {
            setting2Label = "Ease ticks"; setting2 = 1; min2 = 1; max2 = 10; step2 = 1;
            setting3Label = "Motion FOV"; setting3 = 0; min3 = 0; max3 = 1; step3 = 1;
        } else if (name.equals("Sky Changer")) {
            setting2Label = "Animation speed"; setting2 = 0.8; min2 = 0.1; max2 = 2.0; step2 = 0.1;
            setting3Label = "Intensity"; setting3 = 0.8; min3 = 0.2; max3 = 1.0; step3 = 0.1;
        } else if (name.equals("Fortnite Style")) {
            setting2Label = "Suit palette"; setting2 = 1; min2 = 1; max2 = 4; step2 = 1;
            setting3Label = "Helmet visor"; setting3 = 1; min3 = 0; max3 = 1; step3 = 1;
        } else if (name.equals("Hover Glow")) {
            setting2Label = "Pulse speed"; setting2 = 1; min2 = 0.5; max2 = 2.5; step2 = 0.25;
            setting3Label = "Glow width"; setting3 = 2; min3 = 1; max3 = 4; step3 = 1;
        } else if (name.equals("Auto Safe Anchor")) {
            setting2Label = "Placement radius"; setting2 = 2.0; min2 = 1.0; max2 = 5.0; step2 = 0.5;
            setting3Label = "Cooldown"; setting3 = 3; min3 = 1; max3 = 10; step3 = 1;
        } else if (name.equals("Auto Totem")) {
            setting2Label = "Cooldown ticks"; setting2 = 10; min2 = 2; max2 = 40; step2 = 2;
            setting3Label = "Health buffer"; setting3 = 2; min3 = 0; max3 = 8; step3 = 1;
        } else if (name.equals("Auto Eat")) {
            setting2Label = "Retry delay"; setting2 = 5; min2 = 1; max2 = 20; step2 = 1;
            setting3Label = "Use golden apples"; setting3 = 0; min3 = 0; max3 = 1; step3 = 1;
        } else if (name.contains("Crystal")) {
            setting2Label = "Action delay"; setting2 = 2; min2 = 1; max2 = 10; step2 = 1;
            setting3Label = "Target mode"; setting3 = 1; min3 = 1; max3 = 3; step3 = 1;
        } else if (category == Category.WORLD) {
            setting2Label = "Scan budget"; setting2 = 1200; min2 = 200; max2 = 2400; step2 = 200;
            setting3Label = "Minimum cluster"; setting3 = 3; min3 = 1; max3 = 12; step3 = 1;
        } else if (category == Category.RENDER) {
            setting2Label = "Scale"; setting2 = 1.0; min2 = 0.5; max2 = 2.0; step2 = 0.25;
            setting3Label = "Opacity"; setting3 = 1.0; min3 = 0.25; max3 = 1.0; step3 = 0.25;
        } else if (category == Category.MOVEMENT) {
            setting2Label = "Assist delay"; setting2 = 1; min2 = 0; max2 = 10; step2 = 1;
            setting3Label = "Mode"; setting3 = 1; min3 = 1; max3 = 3; step3 = 1;
        } else if (category == Category.SETTINGS) {
            setting2Label = "Accent strength"; setting2 = 0.85; min2 = 0.25; max2 = 1.0; step2 = 0.05;
            setting3Label = "Animation amount"; setting3 = 1.0; min3 = 0; max3 = 2; step3 = 0.25;
        } else if (category == Category.PRACTICE) {
            setting2Label = "Practice window"; setting2 = 5; min2 = 1; max2 = 20; step2 = 1;
            setting3Label = "Display mode"; setting3 = 1; min3 = 1; max3 = 3; step3 = 1;
        } else {
            setting2Label = "Interval"; setting2 = 10; min2 = 1; max2 = 60; step2 = 1;
            setting3Label = "Mode"; setting3 = 1; min3 = 1; max3 = 3; step3 = 1;
        }
        if (name.equals("Open GUI")) keyCode = GLFW.GLFW_KEY_RIGHT_SHIFT;
        else if (name.equals("Toggle HUD")) keyCode = GLFW.GLFW_KEY_H;
        this.defaultSetting = this.setting;
        this.defaultSetting2 = this.setting2;
        this.defaultSetting3 = this.setting3;
    }

    public void adjustSetting() { setting = advance(setting, min, max, step); }
    public void adjustSetting2() { setting2 = advance(setting2, min2, max2, step2); }
    public void adjustSetting3() { setting3 = advance(setting3, min3, max3, step3); }

    private double advance(double current, double lower, double upper, double by) {
        if (by <= 0 || upper <= lower) return current;
        double next = current + by;
        if (next > upper + 0.00001) next = lower;
        return clamp(next, lower, upper);
    }

    public void setSettingFromSlider(double fraction) { setting = fromSlider(fraction, min, max, step); }
    public void setSetting2FromSlider(double fraction) { setting2 = fromSlider(fraction, min2, max2, step2); }
    public void setSetting3FromSlider(double fraction) { setting3 = fromSlider(fraction, min3, max3, step3); }

    private double fromSlider(double fraction, double lower, double upper, double by) {
        double f = Math.max(0.0, Math.min(1.0, fraction));
        double raw = lower + (upper - lower) * f;
        if (by > 0) raw = lower + Math.round((raw - lower) / by) * by;
        return clamp(Math.round(raw * 100.0) / 100.0, lower, upper);
    }

    public void resetSettings() {
        setting = defaultSetting;
        setting2 = defaultSetting2;
        setting3 = defaultSetting3;
    }

    private double clamp(double v, double lower, double upper) {
        return Math.max(lower, Math.min(upper, v));
    }
}
