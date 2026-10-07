package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.config.ConfigManager;
import net.minecraft.world.entity.player.Player;

/**
 * Server authoritative gate for every testing toolkit action.
 *
 * <h2>What changed from the 26.3 line</h2>
 *
 * <p>26.3 asked {@code player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)}. The
 * {@code Player#permissions()} accessor and the {@code Permissions} class are 1.21 additions, and
 * this version has neither - the permission set is still the one the server assembles per player from
 * their game profile and their operator entry.
 *
 * <p>The 1.20.1 equivalent is {@link Player#hasPermissions(int)}, which is the same question vanilla's
 * own command requirement asks: {@code Commands#hasPermission(int)} is literally
 * {@code source -> source.hasPermission(level)}, and it resolves through
 * {@code CommandSourceStack#hasPermission} to the same call made below. Level 2 is "gamemaster" - it
 * is the level that gates {@code /gamemode}, {@code /give} and every other cheat command - so this is
 * the operator check, not an approximation of one.
 *
 * <p>The reason this matters for the toolkit specifically: it is the check that decides whether a
 * player may edit the damage of the weapon they are holding. It has to be the server's answer, never
 * the client's, and it has to agree with what the command system would say about the same player.
 */
public final class PermissionGate {

    /**
     * The permission level that means "operator" on this version.
     *
     * <p>Vanilla's {@code Commands.LEVEL_GAMEMASTERS}. Spelled out rather than referenced because the
     * constant is a private field of {@code Commands}; the number is documented and has not changed
     * since permission levels were introduced.
     */
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
