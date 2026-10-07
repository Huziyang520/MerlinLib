package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server side switches. This file is authoritative: the client only ever displays what the server
 * allows, and every toolkit action is validated against these values on the server.
 *
 * <p>Single player note: the integrated server shares this process, so editing the file from the
 * configuration screen takes effect immediately. On a dedicated server the file must be edited there.
 * The 26.3 line applied it with {@code /reload}; on 1.20.1 the toolkit switches are read at the moment
 * they are used, so they are live as soon as the file is written, while the content files beside it
 * need a restart (see {@link ConfigDirectory#fingerprint()}).
 */
public final class ServerConfig {

    /**
     * Template written on first launch; also the documented reference for every option.
     *
     * <p>Concatenated literals rather than a text block, for the reason given on
     * {@link ClientConfig}'s template. The wording of the two lines that mention {@code /reload} is
     * adjusted: content is no longer re-registered by that command on 1.20.1.
     */
    private static final String DEFAULTS =
            "# MerlinLib server configuration.\n"
            + "# This file is authoritative; the client only displays what the server allows.\n"
            + "# The switches below are read as they are used, so a change applies to the next action.\n"
            + "# The .json content files next to this one are read at startup: on 1.20.1 an enchantment is a\n"
            + "# plain game registry entry, so editing them takes effect after a restart (the 26.3 line could\n"
            + "# re-read them with /reload).\n"
            + "\n"
            + "[security]\n"
            + "# When true, only operators may use the testing toolkit (editors, health editor, macros).\n"
            + "restrict_tools_to_operators = false\n"
            + "# Require command permission (operator) to open the item editor. On by default.\n"
            + "editor_requires_permission = true\n"
            + "# Require command permission (operator) to open the health editor. On by default.\n"
            + "health_editor_requires_permission = true\n"
            + "\n"
            + "[tools]\n"
            + "# Master switch of the testing screens: with it off, no toolkit screen opens (the item editor, the\n"
            + "# health editor and the macro screen all refuse). The testing items stay in the creative tab.\n"
            + "enable_testing_toolkit = true\n"
            + "# Upper bound for the damage value of the test weapons. Defaults to the integer limit.\n"
            + "max_test_weapon_damage = 2147483647\n"
            + "# Upper bound for enchantment levels set through the item editor. Defaults to the integer limit.\n"
            + "max_enchantment_level = 2147483647\n"
            + "# When true, an enchantment may be raised above the maximum level its own definition declares.\n"
            + "# On by default: the ceiling that matters is max_enchantment_level above, and the level stored in\n"
            + "# the item's NBT is a short, which holds the whole range this option can reach.\n"
            + "allow_levels_above_max = true\n"
            + "# When true, any enchantment may be added to any item, for example efficiency on a sword.\n"
            + "# Off by default: an enchantment that the item cannot normally carry has no effect in game.\n"
            + "allow_mismatched_enchantments = false\n"
            + "# When true, the item editor opens for every item, including blocks and other things that are\n"
            + "# not weapons, tools or equipment. Off by default: the editor is meant for the equipment it can do\n"
            + "# something useful with, and opening it on a block or a bucket invites edits that do nothing.\n"
            + "allow_editing_all_items = false\n"
            + "# When true, the base damage of an ordinary weapon can be edited too, not only of a testing weapon.\n"
            + "# Off by default: ordinary weapons are what vanilla balance and other mods rely on.\n"
            + "allow_non_test_item_damage = false\n"
            + "# When true, the health editor only opens while holding the health editor item.\n"
            + "health_editor_requires_item = true\n"
            + "# File layout version. Written by the mod, read only to bring a file from an older build up to\n"
            + "# the current defaults. Leave it alone; setting it back only repeats the migration.\n"
            + "config_version = 4\n"
            + "\n"
            + "[content]\n"
            + "# Emergency switch: when true, none of the enchantments defined in config/MerlinLib/*.json\n"
            + "# are registered. Code api registrations are unaffected.\n"
            + "disable_generated_enchantments = false\n"
            + "\n"
            + "[notices]\n"
            + "# Show the join notices business mods registered through MerlinLib (MerlinApi.notices()). On by\n"
            + "# default; turn it off to silence every join notice at once. Takes effect on the next join.\n"
            + "enabled = true\n"
            + "# Per mod switches, written by \"edit each mod's join notices\" on the general tab as\n"
            + "# \"<modid>=true;<modid>=false>\". A mod that is not listed uses the default it declared when it\n"
            + "# registered. Editing this line by hand works too; the screen is just easier.\n"
            + "per_mod = \"\"\n";

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
    private String noticePerMod = "";
    private List<ContentError> errors = List.of();

    public static ServerConfig load() {
        ServerConfig config = new ServerConfig();
        TomlFile file = TomlFile.read(path(), DEFAULTS);
        if (migrate(file)) {
            // The values below are read from the file, so the file has to be parsed again after a migration
            // rewrote it; otherwise this launch would still see the old numbers.
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
        config.noticePerMod = file.getString("notices.per_mod", "");
        config.errors = file.errors();
        return config;
    }

    /** @return the file this configuration is read from and written to. */
    static Path path() {
        return Path.of("config", Constants.CONFIG_DIRECTORY, "server.toml");
    }

    /** The file layout this build writes; see {@link #migrate(TomlFile)}. */
    private static final int CONFIG_VERSION = 4;

    /**
     * Brings a file written by an older build up to the current defaults, once.
     *
     * <p>Three options changed meaning while the toolkit was renamed from a weapon editor to an item
     * editor: the level and damage ceilings were 255 and 100000 and are now the integer limit, and editing
     * every item used to be off. A file always wins over the template, so a file created by the old build
     * would keep the old, narrower behaviour forever and the switches would look broken.
     *
     * <p>A value is only touched while it still holds the old default: any other number was chosen by hand
     * and is left exactly as it is. The version marker makes this a one-off, so setting a value back to 255
     * on purpose stays honoured on the next launch.
     *
     * @param file the file the configuration was read from
     * @return {@code true} when the file was rewritten and has to be read again
     */
    private static boolean migrate(TomlFile file) {
        if (file.getInt("tools.config_version", 0, 0, Integer.MAX_VALUE) >= CONFIG_VERSION) {
            return false;
        }
        Map<String, String> overrides = new LinkedHashMap<>();
        overrides.put("tools.config_version", Integer.toString(CONFIG_VERSION));
        if (file.getInt("tools.max_enchantment_level", 255, 1, Integer.MAX_VALUE) == 255) {
            overrides.put("tools.max_enchantment_level", Integer.toString(Integer.MAX_VALUE));
        }
        if (file.getInt("tools.max_test_weapon_damage", 100000, 1, Integer.MAX_VALUE) == 100000) {
            overrides.put("tools.max_test_weapon_damage", Integer.toString(Integer.MAX_VALUE));
        }
        // Editing every item was on by default until this build, and the migration before this one wrote it into
        // every file it touched as true - which is exactly why a file kept ignoring the new default. It is moved
        // back to the default once, so the switch finally reads "off" the way the template documents it.
        if (file.getBoolean("tools.allow_editing_all_items", false)) {
            overrides.put("tools.allow_editing_all_items", "false");
        }
        if (!file.getBoolean("tools.allow_levels_above_max", false)) {
            overrides.put("tools.allow_levels_above_max", "true");
        }
        TomlFile.rewrite(path(), DEFAULTS, overrides);
        Constants.LOG.info("[MerlinLib] server.toml migrated to layout {}: {}", CONFIG_VERSION, overrides.keySet());
        return true;
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

    /** @return the per mod overrides, as written in the configuration */
    public String noticePerMod() {
        return this.noticePerMod;
    }

    /** @return problems found while reading the file during the last reload. */
    public List<ContentError> errors() {
        return this.errors;
    }
}
