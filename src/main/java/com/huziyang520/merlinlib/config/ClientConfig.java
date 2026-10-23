package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.util.MerlinColor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Client only switches: display options and interaction tuning, read at the moment they are used so a
 * change is visible immediately.
 */
public final class ClientConfig {

    /** The file layout this build writes. Kept out of the specification: the mod owns this key alone. */
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

    /**
     * Reads the current values.
     *
     * <p>Called on every reload, so an edited file or a changed switch is picked up without a restart.
     *
     * @return the values as they stand right now
     */
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

    /**
     * Keeps the layout marker in the file after the loader has rewritten it from its specification.
     *
     * <p>{@code general.config_version} is not declared in the specification, so a save from the
     * configuration screen writes a file without it. The marker still has to be there, because the next
     * build reads it to learn which layout a file was written with; this restores it on every reload, which
     * a save always triggers, so the key is back in the file by the time anybody looks at it.
     */
    private static void ensureLayoutVersion() {
        Path file = path();
        if (!Files.isRegularFile(file)) {
            return;
        }
        // Only a file that lost the key is touched: writing it on every reload would change its timestamp,
        // which the loader answers with another reload, and the two would keep each other going.
        if (TomlFile.read(file, "").getInt("general.config_version", 0, 0, Integer.MAX_VALUE) == CONFIG_VERSION) {
            return;
        }
        try {
            TomlFile.rewrite(file, "", Map.of("general.config_version", Integer.toString(CONFIG_VERSION)));
        } catch (RuntimeException exception) {
            // A read only file must not break startup: the marker simply stays as it was.
        }
    }

    /** @return the file the loader writes this half to */
    static Path path() {
        return Path.of("config", NeoConfigs.CLIENT_FILE);
    }

    /** @return whether the crosshair damage display is on. */
    public boolean crosshairDamage() {
        return this.crosshairDamage;
    }

    /** @return how long the crosshair damage number stays visible, in ticks. */
    public int crosshairDamageTicks() {
        return this.crosshairDamageTicks;
    }

    /** @return whether damage numbers fly off hit mobs. */
    public boolean floatingTextEnabled() {
        return this.floatingTextEnabled;
    }

    /** @return how long a damage number stays visible, in ticks. */
    public int floatingTextDurationTicks() {
        return this.floatingTextDurationTicks;
    }

    /** @return the base scale of a damage number. */
    public double floatingTextBaseScale() {
        return this.floatingTextBaseScale;
    }

    /** @return the ARGB colour of a normal damage number. */
    public int floatingTextNormalColor() {
        return this.floatingTextNormalColor;
    }

    /** @return the ARGB colour of a critical damage number. */
    public int floatingTextCriticalColor() {
        return this.floatingTextCriticalColor;
    }

    /** @return the upper bound on simultaneous damage numbers. */
    public int floatingTextMaxConcurrent() {
        return this.floatingTextMaxConcurrent;
    }

    /** @return whether the editor hotkey may open the item editor. */
    public boolean editorHotkeyEnabled() {
        return this.editorHotkeyEnabled;
    }

    /** @return the value change of a shift click on a - or + button. */
    public int editorShiftStep() {
        return this.editorShiftStep;
    }

    /** @return the value change per wheel notch over a number field. */
    public int editorScrollStep() {
        return this.editorScrollStep;
    }

    /** @return the value change per wheel notch while holding ctrl. */
    public int editorScrollStepFast() {
        return this.editorScrollStepFast;
    }

    /** @return whether the macro toolkit is enabled. */
    public boolean macrosEnabled() {
        return this.macrosEnabled;
    }

    /** @return whether a very large health bar is folded to one heart plus the numbers. */
    public boolean hudCollapse() {
        return this.hudCollapse;
    }

    /** @return the health above which the bar folds. */
    public int hudCollapseThreshold() {
        return this.hudCollapseThreshold;
    }

    /** @return whether the armour bar follows the folded health row. */
    public boolean hudArmourDodge() {
        return this.hudArmourDodge;
    }

    /** @return whether the number of drawn particles is capped. */
    public boolean particleLimitEnabled() {
        return this.particleLimitEnabled;
    }

    /** @return how many particles may be drawn per 1/20 second. */
    public int particleLimit() {
        return this.particleLimit;
    }

    /** @return problems found while reading the file during the last reload. */
    public List<ContentError> errors() {
        return this.errors;
    }
}
