package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.impl.PermissionGate;
import com.huziyang520.merlinlib.network.OpenToolPayload;
import com.huziyang520.merlinlib.tools.HealthEditorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

/**
 * Client side entry points for the MerlinLib screens, and the one place that decides whether a screen may
 * open on this side.
 */
public final class MerlinScreens {

    private MerlinScreens() {
    }

    /**
     * Whether the permission switches let this player use an editor, as far as the client can tell.
 *
     * <p>In single player the integrated server is in this very process, so the question can be answered
     * properly by asking it about its own player object. That is what makes the "requires command
     * permission" switches visible the moment they are flipped, instead of the player opening an editor
     * whose work is dropped when it is saved. On a dedicated server the client has no permission set to
     * ask about, so the screen opens and the server keeps the last word.
 *
     * @param player the local player
     * @param health {@code true} for the health editor, {@code false} for the item editor
     * @return {@code false} only when the switches deny it and the answer is known
 */
    private static boolean mayOpenEditorLocally(Player player, boolean health) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return true;
        }
        ServerPlayer authoritative = server.getPlayerList().getPlayer(player.getUUID());
        if (authoritative == null) {
            return true;
        }
        boolean allowed = health
                ? PermissionGate.canOpenHealthEditor(authoritative)
                : PermissionGate.canOpenEditor(authoritative);
        if (!allowed) {
            String which = health ? "health editor" : "item editor";
            Constants.LOG.info("[MerlinLib] the {} was refused locally: this server requires command permission "
                    + "for it (server.toml: security.*_requires_permission = true)", which);
            player.displayClientMessage(Component.translatable("message.merlinlib.no_permission"), true);
        }
        return allowed;
    }

    /**
     * Whether a toolkit screen may open on this side.
     *
     * <p>The only switch that is checked is the toolkit master switch, which is also what keeps the items
     * out of the creative inventory. Permission is not checked here, for the reason in the class comment.
     *
     * @param player the local player, may be {@code null}
     * @return {@code true} when a screen may open
 */
    public static boolean mayUseToolsLocally(Player player) {
        if (player == null) {
            return false;
        }
        if (!ConfigManager.server().testingToolkitEnabled()) {
            Constants.LOG.info("[MerlinLib] {} was requested but the testing toolkit is switched off "
                    + "(server.toml: tools.enable_testing_toolkit = false)", "a toolkit screen");
            return false;
        }
        return true;
    }

    /**
     * Whether the item editor may open for this item.
     *
     * <p>Any item may be renamed and given enchantments while the toolkit is on. The one question this
     * answers is whether an item that is not a weapon, a tool or a piece of equipment may be opened too,
     * because an enchantment on such an item has no effect in game: that is
     * {@code tools.allow_editing_all_items}, off by default.
     *
     * @param player the local player
     * @param stack  the item in hand
     * @return {@code true} when the editor may open
 */
    public static boolean mayEdit(Player player, ItemStack stack) {
        if (!mayUseToolsLocally(player)) {
            return false;
        }
        if (ConfigManager.server().allowEditingAllItems() || isEquippableToolOrWeapon(stack)) {
            return true;
        }
        Constants.LOG.info("[MerlinLib] {} is not a weapon, tool or piece of equipment, so the item editor "
                + "refused it (server.toml: tools.allow_editing_all_items = false)", stack.getItem());
        return false;
    }

    /**
     * Whether an item is something the editor opens by default: a weapon, a tool or equipment.
 *
     * <p>The test is deliberately broader than a list of item ids, so modded weapons, tools and armour are
     * covered too.
 *
     * @param stack the item
     * @return {@code true} when the item is normally editable
 */
    private static boolean isEquippableToolOrWeapon(ItemStack stack) {
        return !stack.getAttributeModifiers(EquipmentSlot.MAINHAND).isEmpty()
                || stack.getItem() instanceof DiggerItem
                || stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof ProjectileWeaponItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof ShieldItem
                || stack.getItem() instanceof ArmorItem
                || stack.isDamageableItem();
    }

    /**
     * Opens the item editor for the item in the main hand, when it is allowed.
 */
    public static void openItemEditor() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            Constants.LOG.info("[MerlinLib] the item editor was requested with an empty hand");
            return;
        }
        if (!mayEdit(player, held)) {
            return;
        }
        if (!mayOpenEditorLocally(player, false)) {
            return;
        }
        minecraft.setScreen(new ItemEditorScreen(null, held));
    }

    /**
     * Opens the item editor from the key binding.
     *
     * <p>The binding ships switched off ({@code editor.hotkey_enabled}), so the library can be installed
     * without claiming a key. It can be switched on with {@code /merlinlib editorhotkey on}, or in the
     * configuration file (Jupiter's screen, or {@code client.toml} when Jupiter is absent).
 */
    public static void openItemEditorFromHotkey() {
        if (!ConfigManager.client().editorHotkeyEnabled()) {
            return;
        }
        openItemEditor();
    }

    /**
     * Opens one of the toolkit screens because the server asked for it.
     *
     * <p>Called from the network sink when {@code /merlinlib editor}, {@code /merlinlib health} or
     * {@code /merlinlib macro} ran. The command has already checked the server's switches and the player's
     * permission; everything below checks the client's own switches again, because the two sides may
     * disagree about them and the client is where the screen actually opens.
     *
     * @param tool the screen to open, one of {@link OpenToolPayload}'s constants
 */
    public static void openFromCommand(int tool) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        switch (tool) {
            case OpenToolPayload.EDITOR -> {
                if (player.getMainHandItem().isEmpty()) {
                    // The command is normally typed with both hands free, so silence here would read as
                    // "the command did nothing". The message is the same one the toolkit uses elsewhere.
                    player.displayClientMessage(Component.translatable("message.merlinlib.tools.empty_hand"), true);
                    return;
                }
                openItemEditor();
            }
            case OpenToolPayload.HEALTH -> openHealthEditorForSelf();
            case OpenToolPayload.MACRO -> openMacroScreen(null);
            case OpenToolPayload.NOTICES -> openNoticesScreen(null);
            default -> Constants.LOG.warn("[MerlinLib] the client was asked to open unknown screen {}", tool);
        }
    }

    /**
     * Opens the health editor for the player who asked for it.
     *
     * <p>Reached only from {@code /merlinlib health}. The "hold the health editor item" requirement is
     * deliberately not applied: that requirement exists so that a sneak right click is unambiguous, and a
     * command is already about as unambiguous as a request gets. The permission switch still applies, on
     * both sides.
 */
    private static void openHealthEditorForSelf() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !mayUseToolsLocally(player)) {
            return;
        }
        if (!mayOpenEditorLocally(player, true)) {
            return;
        }
        minecraft.setScreen(new HealthEditorScreen(null, player));
    }

    /**
     * Opens the macro screen.
     *
     * <p>Two switches are consulted: the toolkit master switch, which takes every MerlinLib screen out of
     * reach at once, and the macro switch itself. Permissions are not consulted, for the reason in the
     * class comment.
     *
     * @param parent the screen to return to, or {@code null} to resume the game
 */
    public static void openMacroScreen(Screen parent) {
        Player player = Minecraft.getInstance().player;
        if (!mayUseToolsLocally(player)) {
            return;
        }
        if (!ConfigManager.client().macrosEnabled()) {
            Constants.LOG.info("[MerlinLib] the macro screen was requested but macros are switched off "
                    + "(client.toml: macros.enabled = false)");
            return;
        }
        Minecraft.getInstance().setScreen(new MacroScreen(parent));
    }

    /**
     * Opens the join notice switches.
     *
     * <p>Two entry points want this screen: {@code /merlinlib notices}, and the description row both
     * configuration front ends carry in their notices category. The toolkit master switch is deliberately not
     * consulted - this page edits a configuration file and touches no item - while the command that asks for
     * it has already been through {@code PermissionGate} on the server.
     *
     * @param parent the screen to return to, or {@code null} to resume the game
 */
    public static void openNoticesScreen(Screen parent) {
        Minecraft.getInstance().setScreen(new NoticesScreen(parent));
    }

    /**
     * Opens the health editor for one entity, when it is allowed.
     *
     * <p>Whether the health editor item has to be held is answered here, from the main hand, rather than
     * passed in by the caller. A caller-supplied flag was wrong for exactly one path - sneak using an entity
     * passed "not holding the item" because the item was not *used* on it - so the switch refused the very
     * interaction it was meant to allow.
     *
     * <p>The return value exists for the one caller that has to know: the sneak right click hook, which
     * swallows the click only when a screen really opened.
     *
     * @param parent the screen to return to
     * @param target the entity to edit
     * @return {@code true} when the screen was opened
 */
    public static boolean openHealthEditor(Screen parent, LivingEntity target) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || target == null) {
            return false;
        }
        if (!mayUseToolsLocally(player)) {
            return false;
        }
        boolean holdingItem = player.getMainHandItem().getItem() instanceof HealthEditorItem;
        if (target == player && !holdingItem) {
            // Editing yourself is a feature of the item and nothing else, so it needs the item whatever the
            // "requires item" switch says. Without this the switch's off position turned a bare-handed sneak
            // right click into a self edit, which also made the item look pointless.
            //
            // Both refusals are silent towards the player on purpose: the requirement is already stated in the
            // item's own tooltip, and repeating it in the middle of the screen on every stray right click is
            // noise. The log lines stay, because "why does nothing happen" is a question the log can answer
            // and the screen cannot.
            Constants.LOG.info("[MerlinLib] a self edit was refused: the health editor item must be held to "
                    + "edit yourself");
            return false;
        }
        if (ConfigManager.server().healthEditorRequiresItem() && !holdingItem) {
            Constants.LOG.info("[MerlinLib] the health editor needs the health editor item held "
                    + "(server.toml: tools.health_editor_requires_item = true)");
            return false;
        }
        // Permission is asked last on purpose: a bare-handed sneak right click is not a permission problem, and
        // reporting it as one both misleads the player and breaks the "refuse silently" rule (spec §7).
        if (!mayOpenEditorLocally(player, true)) {
            return false;
        }
        minecraft.setScreen(new HealthEditorScreen(parent, target));
        return true;
    }
}
