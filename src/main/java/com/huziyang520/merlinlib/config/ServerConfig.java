package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server side switches. The server decides, the client only displays what it allows, and every toolkit
 * action is validated against these values where it is applied.
 */
public final class ServerConfig {

    /** Template written on first launch; also the documented reference for every option. */
    private static final String DEFAULTS = """
            # MerlinLib server configuration.
            # This file is authoritative; the client only displays what the server allows.
            # The switches below are read as they are used, so a change applies to the next action.

            [security]
            # When true, only operators may use the testing toolkit (editors, health editor, macros).
            restrict_tools_to_operators = false
            # Require command permission (operator) to open the item editor. On by default.
            editor_requires_permission = true
            # Require command permission (operator) to open the health editor. On by default.
            health_editor_requires_permission = true

            [tools]
            # Master switch of the testing screens: with it off, no toolkit screen opens (the item editor, the
            # health editor and the macro screen all refuse). The testing items stay in the creative tab.
            enable_testing_toolkit = true
            # Upper bound for the damage value of the test weapons. Defaults to the integer limit.
            max_test_weapon_damage = 2147483647
            # Upper bound for enchantment levels set through the item editor. Defaults to the integer limit.
            max_enchantment_level = 2147483647
            # When true, an enchantment may be raised above the maximum level its own definition declares.
            # On by default: the ceiling that matters is max_enchantment_level above, and the level codec
            # that used to cap every saved level at 255 was widened by the mod, so the range is real.
            allow_levels_above_max = true
            # When true, any enchantment may be added to any item, for example efficiency on a sword.
            # Off by default: an enchantment that the item cannot normally carry has no effect in game.
            allow_mismatched_enchantments = false
            # When true, the item editor opens for every item, including blocks and other things that are
            # not weapons, tools or equipment. Off by default: the editor is meant for the equipment it can do
            # something useful with, and opening it on a block or a bucket invites edits that do nothing.
            allow_editing_all_items = false
            # When true, the base damage of an ordinary weapon can be edited too, not only of a testing weapon.
            # Off by default: ordinary weapons are what vanilla balance and other mods rely on.
            allow_non_test_item_damage = false
            # When true, the health editor only opens while holding the health editor item.
            health_editor_requires_item = true

            [content]
            # Emergency switch: when true, none of the enchantments defined in config/MerlinLib/*.json
            # are generated. Code api registrations are unaffected.
            disable_generated_enchantments = false

            [notices]
            # Show the join notices business mods registered through MerlinLib (MerlinApi.notices()). On by
            # default; turn it off to silence every join notice at once. Takes effect on the next join.
            # The per mod switches are not here: they are the [mods] table of notices.toml, which the
            # "join notices" screen under /merlinlib notices writes for you.
            enabled = true
            """;

    private boolean restrictToolsToOperators;
    private boolean editorRequiresPermission = true;
    private boolean healthEditorRequiresPermission = true;
    private boolean testingToolkitEnabled = true;
    private int maxTestWeaponDamage = Integer.MAX_VALUE;
    private int maxEnchantmentLevel = Integer.MAX_VALUE;
    private boolean allowLevelsAboveMax = true;
    private boolean allowMismatchedEnchantments;
    private boolean allowEditingAllItems = false;
    private boolean allowNonTestItemDamage;
    private boolean healthEditorRequiresItem = true;
    private boolean disableGeneratedEnchantments;
    private boolean noticesEnabled = true;
    private List<ContentError> errors = List.of();

    /**
     * @return the server switches, read from whichever store is currently installed
 */
    public static ServerConfig load() {
        return load(ConfigManager.serverView());
    }

    /**
     * Reads the switches from one store.
     *
     * @param file the store to read, normally {@link ConfigManager#serverView()}
     * @return the switches
 */
    static ServerConfig load(ConfigReader file) {
        ServerConfig config = new ServerConfig();
        if (file instanceof TomlFile toml && migrate(toml)) {
            file = TomlFile.read(path(), DEFAULTS);
        }

        config.restrictToolsToOperators = file.getBoolean("security.restrict_tools_to_operators", false);
        config.editorRequiresPermission = file.getBoolean("security.editor_requires_permission", true);
        config.healthEditorRequiresPermission = file.getBoolean("security.health_editor_requires_permission", true);
        config.testingToolkitEnabled = file.getBoolean("tools.enable_testing_toolkit", true);
        config.maxTestWeaponDamage = file.getInt("tools.max_test_weapon_damage", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        config.maxEnchantmentLevel = file.getInt("tools.max_enchantment_level", Integer.MAX_VALUE, 1,
                Integer.MAX_VALUE);
        config.allowLevelsAboveMax = file.getBoolean("tools.allow_levels_above_max", true);
        config.allowMismatchedEnchantments = file.getBoolean("tools.allow_mismatched_enchantments", false);
        config.allowEditingAllItems = file.getBoolean("tools.allow_editing_all_items", false);
        config.allowNonTestItemDamage = file.getBoolean("tools.allow_non_test_item_damage", false);
        config.healthEditorRequiresItem = file.getBoolean("tools.health_editor_requires_item", true);
        config.disableGeneratedEnchantments = file.getBoolean("content.disable_generated_enchantments", false);
        config.noticesEnabled = file.getBoolean("notices.enabled", true);
        config.errors = file.errors();
        return config;
    }

    /** @return the file this configuration is read from and written to. */
    static Path path() {
        return Path.of("config", Constants.CONFIG_DIRECTORY, "server.toml");
    }

    /** The layout marker older builds wrote into {@code server.toml}; read once, never written again. */
    private static final String LEGACY_VERSION_KEY = "tools.config_version";

    /**
     * Brings a file written by an older build up to the current defaults, once.
     *
     * <p>Triggered by the legacy layout marker rather than by a number the server half would have to keep
     * writing: the marker is read from the old file, the values it gated are imported, and the marker is
     * then deleted, because the layout of this file is no longer versioned on the server side.
     *
     * <p>A value is only touched while it still holds the old default, so a number chosen by hand stays.
     *
     * @param file the file the configuration was read from
     * @return {@code true} when values were rewritten and the file has to be read again
 */
    private static boolean migrate(TomlFile file) {
        if (!file.values().containsKey(LEGACY_VERSION_KEY)) {
            return false;
        }
        Map<String, String> overrides = new LinkedHashMap<>();
        if (file.getInt("tools.max_enchantment_level", 255, 1, Integer.MAX_VALUE) == 255) {
            overrides.put("tools.max_enchantment_level", Integer.toString(Integer.MAX_VALUE));
        }
        if (file.getInt("tools.max_test_weapon_damage", 100000, 1, Integer.MAX_VALUE) == 100000) {
            overrides.put("tools.max_test_weapon_damage", Integer.toString(Integer.MAX_VALUE));
        }
        if (file.getBoolean("tools.allow_editing_all_items", false)) {
            overrides.put("tools.allow_editing_all_items", "false");
        }
        if (!file.getBoolean("tools.allow_levels_above_max", false)) {
            overrides.put("tools.allow_levels_above_max", "true");
        }
        TomlFile.rewrite(path(), DEFAULTS, overrides);
        TomlFile.remove(path(), java.util.Set.of(LEGACY_VERSION_KEY));
        Constants.LOG.info("[MerlinLib] server.toml migrated: {}", overrides.keySet());
        return !overrides.isEmpty();
    }

    /** @return the commented template, which is also the documentation of the file. */
    static String defaults() {
        return DEFAULTS;
    }

    /** @return whether the toolkit is limited to operators. */
    public boolean restrictToolsToOperators() {
        return this.restrictToolsToOperators;
    }

    /** @return whether opening the item editor requires command permission. */
    public boolean editorRequiresPermission() {
        return this.editorRequiresPermission;
    }

    /** @return whether opening the health editor requires command permission. */
    public boolean healthEditorRequiresPermission() {
        return this.healthEditorRequiresPermission;
    }

    /** @return whether the testing toolkit is enabled at all. */
    public boolean testingToolkitEnabled() {
        return this.testingToolkitEnabled;
    }

    /** @return the upper bound for test weapon damage. */
    public int maxTestWeaponDamage() {
        return this.maxTestWeaponDamage;
    }

    /** @return the upper bound for enchantment levels set through the item editor. */
    public int maxEnchantmentLevel() {
        return this.maxEnchantmentLevel;
    }

    /** @return whether levels may exceed the enchantment's own maximum. */
    public boolean allowLevelsAboveMax() {
        return this.allowLevelsAboveMax;
    }

    /** @return whether enchantments may be added to items that cannot normally carry them. */
    public boolean allowMismatchedEnchantments() {
        return this.allowMismatchedEnchantments;
    }

    /** @return whether the item editor opens for items that are not weapons, tools or equipment. */
    public boolean allowEditingAllItems() {
        return this.allowEditingAllItems;
    }

    /** @return {@code true} when an ordinary weapon's base damage may be edited as well. */
    public boolean allowNonTestItemDamage() {
        return this.allowNonTestItemDamage;
    }

    /** @return whether the health editor only opens while holding the health editor item. */
    public boolean healthEditorRequiresItem() {
        return this.healthEditorRequiresItem;
    }

    /** @return whether the generated enchantment content is switched off. */
    public boolean disableGeneratedEnchantments() {
        return this.disableGeneratedEnchantments;
    }

    /** @return whether join notices registered through the api are shown at all */
    public boolean noticesEnabled() {
        return this.noticesEnabled;
    }

    /** @return problems found while reading the file during the last reload. */
    public List<ContentError> errors() {
        return this.errors;
    }
}
