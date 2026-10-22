package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;

import java.util.List;

/**
 * Server side switches. This file is authoritative: the client only ever displays what the server allows, and
 * every toolkit action is validated against these values on the server.
 *
 * <p>The keys, their defaults and their ranges are declared once, in {@link NeoSpecBackend#server()}, and the
 * loader writes the file and builds the configuration screen from that specification. This class only turns
 * the stored values into questions the mod can ask.
 *
 * <p>Single player note: the integrated server shares this process, so a change made in the configuration
 * screen applies at once. On a dedicated server the file lives on that server and is applied with
 * {@code /reload}, which is why the switches are labelled as server owned.
 */
public final class ServerConfig {

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

    /**
     * Reads the current values.
     *
     * <p>Called on every reload, so an edited file or a changed switch is picked up without a restart.
     *
     * @return the values as they stand right now
     */
    public static ServerConfig load() {
        ServerConfig config = new ServerConfig();
        ConfigReader file = ConfigManager.read(ConfigScope.SERVER);

        config.restrictToolsToOperators = file.getBoolean("security.restrict_tools_to_operators", false);
        config.editorRequiresPermission = file.getBoolean("security.editor_requires_permission", true);
        config.healthEditorRequiresPermission = file.getBoolean("security.health_editor_requires_permission", true);
        config.testingToolkitEnabled = file.getBoolean("tools.enable_testing_toolkit", true);
        config.maxTestWeaponDamage = file.getInt("tools.max_test_weapon_damage", Integer.MAX_VALUE, 1,
                Integer.MAX_VALUE);
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
