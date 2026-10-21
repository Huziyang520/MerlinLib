package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.config.ConfigManager;
import net.minecraft.world.entity.player.Player;

/**
 * Server authoritative gate for every testing toolkit action.
 *
 * <h2>26.3 versus 1.21.1</h2>
 *
 * <p>26.3 asked {@code player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)}; the
 * {@code Player#permissions()} accessor and the {@code Permissions} class do not exist on 1.21.1.
 * The equivalent here is {@link Player#hasPermissions(int)} - the very call vanilla's
 * {@code CommandSourceStack#hasPermission} resolves to - with level 2, which is the "gamemaster"
 * level that gates {@code /gamemode}, {@code /give} and every other cheat command.
 */
public final class PermissionGate {

    /** Vanilla's {@code Commands.LEVEL_GAMEMASTERS}. */
    private static final int OPERATOR_LEVEL = 2;

    private PermissionGate() {
    }

    /**
     * @param player the player to check, may be {@code null}
     * @return {@code true} when the player counts as operator level 2 or higher
     */
    public static boolean isOperator(Player player) {
        return player != null && player.hasPermissions(OPERATOR_LEVEL);
    }

    /**
     * Decides whether a player may use the testing toolkit right now.
     *
     * @param player the player requesting access
     * @return {@code false} when the toolkit is switched off, or when operator only mode is on and the
     *         player is not an operator
     */
    public static boolean canUseTools(Player player) {
        if (!ConfigManager.server().testingToolkitEnabled()) {
            return false;
        }
        if (ConfigManager.server().restrictToolsToOperators()) {
            return isOperator(player);
        }
        return true;
    }

    /**
     * Whether the item editor may be opened right now.
     *
     * <p>Two separate gates, both off by default on the server side: the toolkit itself, and the
     * requirement that the player may run commands. Keeping them apart means an operator can allow the
     * toolkit for everyone while still keeping the editors behind permission.
     *
     * @param player the player requesting access
     * @return {@code true} when the editor may open
     */
    public static boolean canOpenEditor(Player player) {
        if (!canUseTools(player)) {
            return false;
        }
        return !ConfigManager.server().editorRequiresPermission() || isOperator(player);
    }

    /**
     * Whether the health editor may be opened right now.
     *
     * @param player the player requesting access
     * @return {@code true} when the health editor may open
     */
    public static boolean canOpenHealthEditor(Player player) {
        if (!canUseTools(player)) {
            return false;
        }
        return !ConfigManager.server().healthEditorRequiresPermission() || isOperator(player);
    }

    /** @return {@code true} when the toolkit is enabled server side at all. */
    public static boolean toolkitEnabled() {
        return ConfigManager.server().testingToolkitEnabled();
    }
}
