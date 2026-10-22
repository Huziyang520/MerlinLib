package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.util.MerlinColor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client only switches: display options and interaction tuning, plus the general switches. Nothing here is
 * authoritative, so a client may edit it freely and the change is visible immediately.
 *
 * <p>Every option here is read at the moment it is used, never cached in a field of a screen or of the
 * renderer. That is deliberate: a switch in this file that only takes effect after a restart would be a lie.
 */
public final class ClientConfig {

    /** Template written on first launch; also the documented reference for every option. */
    private static final String DEFAULTS = """
            # MerlinLib client configuration.
            # Everything here only affects this client and takes effect right after the game reads the file.
            # Content definitions (enchantments, effects, potions) live in the .json files next to this one
            # and are applied on the server side with /reload.

            [general]
            # Show the join notices MerlinLib sends, including the one about the configuration screen. On by
            # default. Takes effect on the next join.
            notice_enabled = true
            # File layout version. Written by the mod, read only to bring a file from an older build up to the
            # current defaults; do not set it back by hand.
            config_version = 3

            [hud]
            # Show the damage of the weapon you just used near the crosshair. Off by default.
            # This is the weapon's listed damage, not the amount of health actually removed.
            crosshair_damage = false
            # How long the crosshair damage number stays on screen, in ticks (20 ticks = 1 second).
            crosshair_damage_ticks = 20
            # Fold a very large health bar into one heart plus the current and maximum numbers, instead of one
            # heart per two points of health. On by default: without it a maximum health in the thousands asks
            # for thousands of hearts and the frame never finishes.
            collapse_enabled = true
            # Health above which the bar folds. 99 keeps a normal player bar untouched.
            collapse_threshold = 99
            # Move the armour row up so it sits on the folded health row. Off leaves it where vanilla puts it,
            # which is where it would be for the unfolded bar.
            armour_auto_dodge = true

            [floating_text]
            # Show damage numbers flying off a mob when it is hit. Off by default: it is a testing aid, so it is
            # something a player turns on rather than something dropped on them.
            enabled = false
            # How long a damage number stays visible after a mob is hit, in ticks.
            duration_ticks = 20
            # Base scale of a damage number. Higher means larger text.
            base_scale = 1.0
            # ARGB colours for a normal hit and a critical hit.
            normal_color = 0xFFFFFFFF
            critical_color = 0xFFFFAA00
            # Upper bound on simultaneous damage numbers; the oldest one is dropped first.
            max_concurrent = 32

            [particles]
            # Cap how many particles this client draws. Some paths of the game ask for one particle per point
            # of damage, so a single hit with a very large number can freeze the client while it builds them
            # all - the frame never finishes because it is still making particles. When enabled, at most
            # `limit` particles are drawn per 1/20 second; the rest are dropped before they exist. On by
            # default.
            limit_enabled = true
            # How many particles may be drawn per 1/20 second. 512 is far above what normal play produces
            # (rain, explosions, a busy farm) and still keeps a damage number in the thousands from freezing
            # the client. Lower it if a very large hit still stutters.
            limit = 512

            [editor]
            # Allow the editor hotkey to open the item editor. Off by default, so the library can be
            # installed without claiming a key. Toggle it in game with /merlinlib editorhotkey on.
            hotkey_enabled = false
            # Value change when a - or + button is clicked while holding shift.
            shift_step = 10
            # Value change per mouse wheel notch over a number field.
            scroll_step = 1
            # Value change per mouse wheel notch while holding ctrl.
            scroll_step_fast = 10

            [macros]
            # Enable the macro toolkit: the macro screen and running macros from their bound keys.
            enabled = true
            """;

    /** The file layout this build writes; see the migration in {@link #load()}. */
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
    private boolean noticeEnabled = true;
    private List<ContentError> errors = List.of();

    public static ClientConfig load() {
        ClientConfig config = new ClientConfig();
        migrateFile();
        ConfigReader file = ConfigManager.read(path(), DEFAULTS);

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
        config.noticeEnabled = file.getBoolean("general.notice_enabled", true);
        config.errors = file.errors();
        return config;
    }

    /**
     * Brings a {@code client.toml} written by an older build up to the current layout, once.
     *
     * <p>Three things changed, and a file always wins over the template, so without this a file would keep the
     * old shape forever:
     * <ul>
     *     <li>the whole {@code [gui]} section is gone - the self drawn configuration screens and their
     *     animation switches were removed, and a section nothing reads is only misleading documentation;</li>
     *     <li>{@code config_version} moved from {@code [gui]} to {@code [general]}, which is where the general
     *     switches live;</li>
     *     <li>{@code general.notice_enabled} is new and defaults to on.</li>
     * </ul>
     *
     * <p>The version marker makes it a one-off: a value chosen afterwards is never touched again, and running
     * the migration a second time changes nothing. It works on the TOML file only - when a loader installed its
     * own store there is no file layout to bring forward, and that store owns its values.
     */
    private static void migrateFile() {
        if (ConfigManager.backend() != null || !Files.exists(path())) {
            // Nothing to migrate: a loader store owns the values, or the file does not exist yet and is about
            // to be written from the current template.
            return;
        }
        TomlFile file = TomlFile.read(path(), DEFAULTS);
        int version = Math.max(file.getInt("general.config_version", 0, 0, Integer.MAX_VALUE),
                file.getInt("gui.config_version", 0, 0, Integer.MAX_VALUE));
        if (version >= CONFIG_VERSION) {
            return;
        }

        TomlFile.removeSection(path(), "gui");

        Map<String, String> overrides = new LinkedHashMap<>();
        overrides.put("general.config_version", Integer.toString(CONFIG_VERSION));
        if (file.getString("general.notice_enabled", null) == null) {
            overrides.put("general.notice_enabled", "true");
        }
        // Layout 2 turned the flying damage numbers off by default, because a file from before that kept the
        // old default of on forever. A version 1 file still needs that move; a later one has been through it.
        if (version < 2 && file.getBoolean("floating_text.enabled", true)) {
            overrides.put("floating_text.enabled", "false");
        }
        TomlFile.rewrite(path(), DEFAULTS, overrides);
        // [general] is new to this file and rewrite() appends an unknown section at the end; the documented
        // order puts it first, so it is moved there rather than rewriting the file and losing its comments.
        TomlFile.moveSectionToTop(path(), "general");
        Constants.LOG.info("[MerlinLib] client.toml migrated to layout {}: {}", CONFIG_VERSION, overrides.keySet());
    }

    /** @return the file this configuration is read from and written to. */
    public static Path path() {
        return Path.of("config", Constants.CONFIG_DIRECTORY, "client.toml");
    }

    /** @return the commented template, which is also the documentation of the file. */
    public static String defaults() {
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

    /** @return whether MerlinLib's join notices are shown at all */
    public boolean noticeEnabled() {
        return this.noticeEnabled;
    }

    /** @return whether a very large health bar is drawn as one heart plus the numbers */
    public boolean hudCollapse() {
        return this.hudCollapse;
    }

    /** @return the health above which the bar folds */
    public int hudCollapseThreshold() {
        return this.hudCollapseThreshold;
    }

    /** @return whether the armour row moves up onto the folded health row */
    public boolean hudArmourDodge() {
        return this.hudArmourDodge;
    }

    /** @return problems found while reading the file during the last reload. */
    public List<ContentError> errors() {
        return this.errors;
    }
}
