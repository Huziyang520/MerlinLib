// Client side entry points for the MerlinLib screens, and the one place that decides whether a screen may open on this ...

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

public final class MerlinScreens {
    private MerlinScreens() {
    }

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

    public static void openItemEditorFromHotkey() {
        if (!ConfigManager.client().editorHotkeyEnabled()) {
            return;
        }
        openItemEditor();
    }

    public static void openFromCommand(int tool) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        switch (tool) {
            case OpenToolPayload.EDITOR -> {
                if (player.getMainHandItem().isEmpty()) {
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

    public static void openNoticesScreen(Screen parent) {
        Minecraft.getInstance().setScreen(new NoticesScreen(parent));
    }

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
            Constants.LOG.info("[MerlinLib] a self edit was refused: the health editor item must be held to "
                    + "edit yourself");
            return false;
        }
        if (ConfigManager.server().healthEditorRequiresItem() && !holdingItem) {
            Constants.LOG.info("[MerlinLib] the health editor needs the health editor item held "
                    + "(server.toml: tools.health_editor_requires_item = true)");
            return false;
        }

        if (!mayOpenEditorLocally(player, true)) {
            return false;
        }
        minecraft.setScreen(new HealthEditorScreen(parent, target));
        return true;
    }
}
