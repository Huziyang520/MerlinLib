// Server authoritative gate for every testing toolkit action.

package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.config.ConfigManager;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;

public final class PermissionGate {
    private PermissionGate() {
    }

    public static boolean isOperator(Player player) {
        return player != null && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static boolean canUseTools(Player player) {
        if (!ConfigManager.server().testingToolkitEnabled()) {
            return false;
        }
        if (ConfigManager.server().restrictToolsToOperators()) {
            return isOperator(player);
        }
        return true;
    }

    public static boolean canOpenEditor(Player player) {
        if (!canUseTools(player)) {
            return false;
        }
        return !ConfigManager.server().editorRequiresPermission() || isOperator(player);
    }

    public static boolean canOpenHealthEditor(Player player) {
        if (!canUseTools(player)) {
            return false;
        }
        return !ConfigManager.server().healthEditorRequiresPermission() || isOperator(player);
    }

    public static boolean toolkitEnabled() {
        return ConfigManager.server().testingToolkitEnabled();
    }
}
