package gg.cinco.client;

public final class ClientModule {
    public enum Category {
        COMBAT("COMBAT"), MOVEMENT("MOVEMENT"), RENDER("RENDER"), WORLD("WORLD"), UTILITY("UTILITY"), PRACTICE("PRACTICE");
        public final String label;
        Category(String label) { this.label = label; }
    }

    public final String name;
    public final String description;
    public final Category category;
    public final String settingLabel;
    public final double min;
    public final double max;
    public final double step;
    public boolean enabled;
    public double setting;

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
    }

    public void adjustSetting() {
        if (step <= 0 || max <= min) return;
        setting += step;
        if (setting > max + 0.00001) setting = min;
        setting = Math.max(min, Math.min(max, Math.round(setting * 100.0) / 100.0));
    }

    public void setSettingFromSlider(double fraction) {
        double f = Math.max(0.0, Math.min(1.0, fraction));
        double raw = min + (max - min) * f;
        if (step > 0) raw = min + Math.round((raw - min) / step) * step;
        setting = Math.max(min, Math.min(max, Math.round(raw * 100.0) / 100.0));
    }
}
