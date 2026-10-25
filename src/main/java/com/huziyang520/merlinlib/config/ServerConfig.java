// Server side switches.

package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;
import java.util.List;

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
    private List<ContentError> errors = List.of();

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
        config.errors = file.errors();
        return config;
    }

    public boolean restrictToolsToOperators() {
        return this.restrictToolsToOperators;
    }

    public boolean editorRequiresPermission() {
        return this.editorRequiresPermission;
    }

    public boolean healthEditorRequiresPermission() {
        return this.healthEditorRequiresPermission;
    }

    public boolean testingToolkitEnabled() {
        return this.testingToolkitEnabled;
    }

    public int maxTestWeaponDamage() {
        return this.maxTestWeaponDamage;
    }

    public int maxEnchantmentLevel() {
        return this.maxEnchantmentLevel;
    }

    public boolean allowLevelsAboveMax() {
        return this.allowLevelsAboveMax;
    }

    public boolean allowMismatchedEnchantments() {
        return this.allowMismatchedEnchantments;
    }

    public boolean allowEditingAllItems() {
        return this.allowEditingAllItems;
    }

    public boolean allowNonTestItemDamage() {
        return this.allowNonTestItemDamage;
    }

    public boolean healthEditorRequiresItem() {
        return this.healthEditorRequiresItem;
    }

    public boolean disableGeneratedEnchantments() {
        return this.disableGeneratedEnchantments;
    }

    public boolean noticesEnabled() {
        return this.noticesEnabled;
    }

    public List<ContentError> errors() {
        return this.errors;
    }
}
