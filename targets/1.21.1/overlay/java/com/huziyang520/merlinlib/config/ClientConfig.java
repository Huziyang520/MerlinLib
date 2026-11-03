// Client only switches: display options and interaction tuning, read at the moment they are used so a change is visible ...

package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.util.MerlinColor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ClientConfig {
    private static final int CONFIG_VERSION = 3;
    private boolean crosshairDamage;
    private int crosshairDamageTicks = 20;
    private boolean hudCollapse = true;
    private int hudCollapseThreshold = 99;
    private boolean hudArmourDodge = true;
    private boolean floatingTextEnabled = false;
    private int floatingTextDurationTicks = 20;
    private double floatingTextBaseScale = 1.0D;
    private int floatingTextNormalColor = MerlinColor.WHITE;
    private int floatingTextCriticalColor = MerlinColor.ORANGE;
    private int floatingTextMaxConcurrent = 32;
    private boolean editorHotkeyEnabled;
    private int editorShiftStep = 10;
    private int editorScrollStep = 1;
    private int editorScrollStepFast = 10;
    private boolean macrosEnabled = true;
    private boolean particleLimitEnabled = true;
    private int particleLimit = 512;
    private List<ContentError> errors = List.of();

    public static ClientConfig load() {
        ClientConfig config = new ClientConfig();
        ConfigReader file = ConfigManager.read(ConfigScope.CLIENT);
        ensureLayoutVersion();
        config.crosshairDamage = file.getBoolean("hud.crosshair_damage", false);
        config.crosshairDamageTicks = file.getInt("hud.crosshair_damage_ticks", 20, 1, 400);
        config.hudCollapse = file.getBoolean("hud.collapse_enabled", true);
        config.hudCollapseThreshold = file.getInt("hud.collapse_threshold", 99, 1, 1000000);
        config.hudArmourDodge = file.getBoolean("hud.armour_auto_dodge", true);
        config.floatingTextEnabled = file.getBoolean("floating_text.enabled", false);
        config.floatingTextDurationTicks = file.getInt("floating_text.duration_ticks", 20, 1, 400);
        config.floatingTextBaseScale = file.getDouble("floating_text.base_scale", 1.0D, 0.1D, 8.0D);
        config.floatingTextNormalColor = file.getColor("floating_text.normal_color", MerlinColor.WHITE);
        config.floatingTextCriticalColor = file.getColor("floating_text.critical_color", MerlinColor.ORANGE);
        config.floatingTextMaxConcurrent = file.getInt("floating_text.max_concurrent", 32, 1, 512);
        config.editorHotkeyEnabled = file.getBoolean("editor.hotkey_enabled", false);
        config.editorShiftStep = file.getInt("editor.shift_step", 10, 1, 100000);
        config.editorScrollStep = file.getInt("editor.scroll_step", 1, 1, 1000);
        config.editorScrollStepFast = file.getInt("editor.scroll_step_fast", 10, 1, 100000);
        config.macrosEnabled = file.getBoolean("macros.enabled", true);
        config.particleLimitEnabled = file.getBoolean("particles.limit_enabled", true);
        config.particleLimit = file.getInt("particles.limit", 512, 1, 1000000);
        config.errors = file.errors();
        return config;
    }

    private static void ensureLayoutVersion() {
        Path file = path();
        if (!Files.isRegularFile(file)) {
            return;
        }

        if (TomlFile.read(file, "").getInt("general.config_version", 0, 0, Integer.MAX_VALUE) == CONFIG_VERSION) {
            return;
        }
        try {
            TomlFile.rewrite(file, "", Map.of("general.config_version", Integer.toString(CONFIG_VERSION)));
        } catch (RuntimeException exception) {
        }
    }

    static Path path() {
        return Path.of("config", NeoConfigs.CLIENT_FILE);
    }

    public boolean crosshairDamage() {
        return this.crosshairDamage;
    }

    public int crosshairDamageTicks() {
        return this.crosshairDamageTicks;
    }

    public boolean floatingTextEnabled() {
        return this.floatingTextEnabled;
    }

    public int floatingTextDurationTicks() {
        return this.floatingTextDurationTicks;
    }

    public double floatingTextBaseScale() {
        return this.floatingTextBaseScale;
    }

    public int floatingTextNormalColor() {
        return this.floatingTextNormalColor;
    }

    public int floatingTextCriticalColor() {
        return this.floatingTextCriticalColor;
    }

    public int floatingTextMaxConcurrent() {
        return this.floatingTextMaxConcurrent;
    }

    public boolean editorHotkeyEnabled() {
        return this.editorHotkeyEnabled;
    }

    public int editorShiftStep() {
        return this.editorShiftStep;
    }

    public int editorScrollStep() {
        return this.editorScrollStep;
    }

    public int editorScrollStepFast() {
        return this.editorScrollStepFast;
    }

    public boolean macrosEnabled() {
        return this.macrosEnabled;
    }

    public boolean hudCollapse() {
        return this.hudCollapse;
    }

    public int hudCollapseThreshold() {
        return this.hudCollapseThreshold;
    }

    public boolean hudArmourDodge() {
        return this.hudArmourDodge;
    }

    public boolean particleLimitEnabled() {
        return this.particleLimitEnabled;
    }

    public int particleLimit() {
        return this.particleLimit;
    }

    public List<ContentError> errors() {
        return this.errors;
    }
}
