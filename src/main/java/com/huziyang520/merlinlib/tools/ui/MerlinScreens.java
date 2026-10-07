package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.impl.PermissionGate;
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
 *
 * <h2>Silence is deliberate</h2>
 *
 * <p>A refused open says nothing on screen. The reason is written to the log instead: a chat message that
 * repeats "no permission" while the player is already experimenting only adds noise, and the player asked
 * for exactly this. The log line names the switch and its current value, which is what actually helps.
 *
 * <h2>Why the client cannot ask for permissions</h2>
 *
 * <p>The operator question can only be answered on the server: on a client the local player carries no
 * permission set, so asking there locks out everybody including the operator. This class therefore never
 * checks permissions, and the server refuses the write where it happens, which is the only place that knows.
 *
 * <p>This class is only referenced from loader client code, so a dedicated server never loads it and the
 * client-only types it uses ({@code Minecraft}, {@code Screen}) can never break server startup.
 *
 * <h2>What changed / 1.20.1 note</h2>
 *
 * <p>Three API moves, none of which change what the class decides:
 *
 * <ul>
 *   <li>{@code Minecraft#setScreenAndShow(Screen)} is a 1.21 method; this version's is
 *       {@code Minecraft#setScreen(Screen)}. Every open below uses it;</li>
 *   <li>{@link #mayOpenEditorLocally} reports the refusal with
 *       {@code Player#displayClientMessage(Component, boolean)} and passes {@code actionBar = true}, which
 *       is exactly what 26.3's {@code sendOverlayMessage} meant - the message above the hotbar, not in chat.
 *       The method name changed; the behaviour did not, and the "refuse silently towards chat" rule above
 *       still holds;</li>
 *   <li>{@link #isEquippableToolOrWeapon} could ask about item <em>components</em> on 26.3
 *       ({@code DataComponents.ATTRIBUTE_MODIFIERS}, {@code TOOL}, {@code WEAPON},
 *       {@code KINETIC_WEAPON}, {@code PIERCING_WEAPON}). 1.20.1 has no data component system - components
 *       arrive in 1.20.5 - so the same question is asked of the item's <em>class</em> and of the vanilla
 *       attribute map instead. See that method for what replaced what.</li>
 * </ul>
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
     * <p><b>What changed / 1.20.1 note:</b> the refusal is sent with
     * {@code displayClientMessage(message, true)}. The Boolean is "show it above the hotbar rather than in
     * chat", which is the 1.20.1 spelling of the 26.3 {@code sendOverlayMessage}.
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
     * <p><b>What changed / 1.20.1 note:</b> 26.3 read the item's components, which is the cleanest possible
     * test:
     *
     * <table border="1">
     *   <caption>one 26.3 component test, replaced</caption>
     *   <tr><th>26.3 test</th><th>1.20.1 replacement</th></tr>
     *   <tr><td>{@code DataComponents.ATTRIBUTE_MODIFIERS}</td>
     *       <td>{@code stack.getAttributeModifiers(EquipmentSlot.MAINHAND)} is non-empty - the same
     *       question, asked of the live attribute map instead of a stored component. That map is built from
     *       the item's own defaults, so a modded sword answers yes without being named here;</td></tr>
     *   <tr><td>{@code DataComponents.TOOL}</td>
     *       <td>{@link DiggerItem} - the class that carries a mining tier, speed and set of blocks;</td></tr>
     *   <tr><td>{@code DataComponents.WEAPON}</td>
     *       <td>{@link SwordItem} and {@link ProjectileWeaponItem} (which covers {@link BowItem} and
     *       {@link CrossbowItem}); {@link TridentItem} and {@link ShieldItem} are added on their own, since
     *       they are combat items that carry none of those supertypes;</td></tr>
     *   <tr><td>{@code DataComponents.KINETIC_WEAPON} / {@code PIERCING_WEAPON}</td>
     *       <td>nothing. Those two components are about the post-1.21 combat model - kinetic (mace style)
     *       and piercing weapons - and this version has no item that would carry them, so there is nothing
     *       to test. A modded trident-like weapon is still covered by {@link TridentItem} or by the
     *       attribute test above;</td></tr>
     *   <tr><td>bulk equipment</td>
     *       <td>{@link ArmorItem} and {@code stack.isDamageableItem()}, the latter standing in for "this
     *       item has durability", which is how the 26.3 code accepted any damageable item.</td></tr>
     * </table>
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
     * without claiming a key. It can be switched on from the configuration screen or with
     * {@code /merlinlib editorhotkey on}.
     */
    public static void openItemEditorFromHotkey() {
        if (!ConfigManager.client().editorHotkeyEnabled()) {
            return;
        }
        openItemEditor();
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
            // item's own tooltip and on the config screen, and repeating it in the middle of the screen on
            // every stray right click is noise. The log lines stay, because "why does nothing happen" is a
            // question the log can answer and the screen cannot.
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
