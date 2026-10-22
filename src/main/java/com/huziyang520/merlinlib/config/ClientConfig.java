package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.util.MerlinColor;

import java.nio.file.Path;
import java.util.List;

/**
 * Client only switches: display options and interaction tuning. Nothing here is authoritative, so a
 * client may edit it freely and the change is visible immediately.
 *
 * <p>Every option here is read at the moment it is used, never cached in a field of a screen or of the
 * renderer. That is deliberate: a switch in this file that only takes effect after a restart would be
 * a lie, and the values are offered to the player through a configuration screen.
 *
 * <p>The file layout is carried over from the 26.3 line unchanged, including the layout version and
 * its one-off migration: the two lines are the same product on a different Minecraft version and a
 * player moving between them should not have their switches rewritten.
 */
public final class ClientConfig {

    /**
     * Template written on first launch; also the documented reference for every option.
     *
     * <p>Written as concatenated string literals rather than a text block: the 26.3 source used a text
     * block (Java 15+), and while Java 17 would accept it, this project's sources are plain 17 without
     * any preview or newer syntax so a later lowering stays mechanical. {@code \n} is the only escape
     * used, and this text is never routed through {@code processResources}, so the {@code expand} trap
     * does not apply.
     */
    private static final String DEFAULTS =
            "# MerlinLib client configuration.\n"
            + "# Everything here only affects this client and takes effect right after the game reads the file.\n"
            + "# Content definitions (enchantments, effects, potions) live in the .json files next to this one.\n"
            + "# On 1.20.1 those are read when the game starts; the 26.3 line could also re-read them with\n"
            + "# /reload, which is not possible here because enchantments are a plain registry on this version.\n"
            + "#\n"
            + "# This file is only read when the Jupiter configuration library is absent. With Jupiter installed\n"
            + "# the same switches live in its own configuration screen: this file is imported once, then renamed\n"
            + "# to client.toml.imported.\n"
            + "\n"
            + "[general]\n"
            + "# Join notices in chat. On by default: this is what tells a player that something about the mod\n"
            + "# changed. Takes effect on the next join.\n"
            + "notice_enabled = true\n"
            + "# File layout version. Written by the mod, read only to bring a file from an older build up to the\n"
            + "# current defaults; do not set it back by hand.\n"
            + "config_version = 4\n"
            + "\n"
            + "[hud]\n"
            + "# Show the damage of the weapon you just used near the crosshair. Off by default.\n"
            + "# This is the weapon's listed damage, not the amount of health actually removed.\n"
            + "crosshair_damage = false\n"
            + "# How long the crosshair damage number stays on screen, in ticks (20 ticks = 1 second).\n"
            + "crosshair_damage_ticks = 20\n"
            + "# Draw a very large maximum health as one heart plus the numbers. On by default: a mob with a\n"
            + "# thousand health would otherwise draw five hundred hearts across the whole window.\n"
            + "collapse_enabled = true\n"
            + "# The maximum health from which the row folds; 99 means a ceiling of 100 or more folds.\n"
            + "collapse_threshold = 99\n"
            + "# Move the armour bar to sit just above the health bar that is really drawn. On by default.\n"
            + "armour_auto_dodge = true\n"
            + "\n"
            + "[floating_text]\n"
            + "# Show damage numbers flying off a mob when it is hit. Off by default: it is a testing aid, so it is\n"
            + "# something a player turns on rather than something dropped on them.\n"
            + "enabled = false\n"
            + "# How long a damage number stays visible after a mob is hit, in ticks.\n"
            + "duration_ticks = 20\n"
            + "# Base scale of a damage number. Higher means larger text.\n"
            + "base_scale = 1.0\n"
            + "# ARGB colours for a normal hit and a critical hit.\n"
            + "normal_color = 0xFFFFFFFF\n"
            + "critical_color = 0xFFFFAA00\n"
            + "# Upper bound on simultaneous damage numbers; the oldest one is dropped first.\n"
            + "max_concurrent = 32\n"
            + "\n"
            + "[particles]\n"
            + "# Cap how many particles this client draws. Some paths of the game ask for one particle per point\n"
            + "# of damage, so a single hit with a very large number can freeze the client while it builds them\n"
            + "# all - the frame never finishes because it is still making particles. When enabled, at most\n"
            + "# `limit` particles are drawn per 1/20 second; the rest are dropped before they exist. On by\n"
            + "# default.\n"
            + "limit_enabled = true\n"
            + "# How many particles may be drawn per 1/20 second. 512 is far above what normal play produces\n"
            + "# (rain, explosions, a busy farm) and still keeps a damage number in the thousands from freezing\n"
            + "# the client. Lower it if a very large hit still stutters.\n"
            + "limit = 512\n"
            + "\n"
            + "[editor]\n"
            + "# Allow the editor hotkey to open the item editor. Off by default, so the library can be\n"
            + "# installed without claiming a key. Toggle it in game with /merlinlib editorhotkey on.\n"
            + "hotkey_enabled = false\n"
            + "# Value change when a - or + button is clicked while holding shift.\n"
            + "shift_step = 10\n"
            + "# Value change per mouse wheel notch over a number field.\n"
            + "scroll_step = 1\n"
            + "# Value change per mouse wheel notch while holding ctrl.\n"
            + "scroll_step_fast = 10\n"
            + "\n"
            + "[macros]\n"
            + "# Enable the macro toolkit: the macro screen and running macros from their bound keys.\n"
            + "enabled = true\n";

    /** The file layout this build writes; see the migration in {@link #load()}. */
    static final int CONFIG_VERSION = 4;

    /** Whether a very large maximum health is drawn as one heart plus the numbers. */
    public boolean hudCollapse() {
        return hudCollapse;
    }

    /** The maximum health from which the row folds; 99 means a ceiling of 100 or more folds. */
    public int hudCollapseThreshold() {
        return hudCollapseThreshold;
    }

    /** Whether the armour row keeps following the heart rows once the row is folded. */
    public boolean hudArmourDodge() {
        return hudArmourDodge;
    }

    private boolean crosshairDamage;

    /** Whether a very large maximum health is drawn as one heart plus the numbers. */
    private boolean hudCollapse = true;

    /** The maximum health from which the row folds; 99 means a ceiling of 100 or more folds. */
    private int hudCollapseThreshold = 99;

    /**
     * Whether the armour bar is moved to sit just above the health bar that is really drawn.
     *
     * <p>On: the row the HUD is built from is folded to a single row, so the armour bar ends up right
     * above the heart that is really drawn instead of keeping the height it would need for every heart
     * a large maximum health would ask for. Off: nothing is done, and the armour bar stays where vanilla
     * puts it - which, with a large maximum health, is far above the folded health bar.
     */
    private boolean hudArmourDodge = true;
    private int crosshairDamageTicks = 20;
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
    /** Whether MerlinLib's own join notices are offered at all. */
    private boolean noticeEnabled = true;
    private List<ContentError> errors = List.of();

    public static ClientConfig load() {
        ClientConfig config = new ClientConfig();
        ConfigReader file = ConfigManager.read(path(), DEFAULTS);

        // A file written by an older build is brought up to the new defaults once. The floating damage numbers
        // used to be on by default and a file always wins over the template, so without this a file from before
        // would keep showing them no matter what the default says. Only a switch that still holds the old value
        // is moved, and the version marker in the file makes it a one-off - switching them back on later is a
        // hand picked value from that point on and is never touched again.
        //
        // Layouts 0 to 3 kept the marker in [gui]; it moved to [general] with layout 4, so the old location is
        // read as the fallback and a file that was already current is not migrated twice. The [gui] animation
        // switches themselves are simply not read any more: a file that still carries them keeps them, and
        // they do nothing.
        int layout = file.getInt("general.config_version",
                file.getInt("gui.config_version", 0, 0, Integer.MAX_VALUE), 0, Integer.MAX_VALUE);
        if (layout < CONFIG_VERSION) {
            java.util.Map<String, String> overrides = new java.util.LinkedHashMap<>();
            overrides.put("general.config_version", Integer.toString(CONFIG_VERSION));
            if (layout < 3) {
                if (!file.getBoolean("hud.armour_auto_dodge", true)) {
                    overrides.put("hud.armour_auto_dodge", "true");
                }
                if (file.getBoolean("floating_text.enabled", true)) {
                    overrides.put("floating_text.enabled", "false");
                }
            }
            ConfigManager.write(path(), DEFAULTS, overrides);
            Constants.LOG.info("[MerlinLib] client.toml migrated from layout {} to {}: {}",
                    layout, CONFIG_VERSION, overrides.keySet());
            file = ConfigManager.read(path(), DEFAULTS);
        }

        config.crosshairDamage = file.getBoolean("hud.crosshair_damage", false);
        config.hudCollapse = file.getBoolean("hud.collapse_enabled", true);
        config.hudCollapseThreshold = file.getInt("hud.collapse_threshold", 99, 1, 1000000);
        config.hudArmourDodge = file.getBoolean("hud.armour_auto_dodge", true);
        config.crosshairDamageTicks = file.getInt("hud.crosshair_damage_ticks", 20, 1, 400);
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
        config.noticeEnabled = file.getBoolean("general.notice_enabled", true);
        config.errors = file.errors();
        return config;
    }

    /** @return the file this configuration is read from and written to. */
    static Path path() {
        return Path.of("config", Constants.CONFIG_DIRECTORY, "client.toml");
    }

    /** @return the commented template, which is also the documentation of the file. */
    static String defaults() {
        return DEFAULTS;
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

    /** @return whether the number of drawn particles is capped. */
    public boolean particleLimitEnabled() {
        return this.particleLimitEnabled;
    }

    /** @return how many particles may be drawn per 1/20 second. */
    public int particleLimit() {
        return this.particleLimit;
    }

    /** @return whether MerlinLib offers its own join notices at all */
    public boolean noticeEnabled() {
        return this.noticeEnabled;
    }

    /** @return problems found while reading the file during the last reload. */
    public List<ContentError> errors() {
        return this.errors;
    }
}
