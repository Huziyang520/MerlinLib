package com.huziyang520.merlinlib;

import com.huziyang520.merlinlib.command.MerlinCommand;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.impl.PermissionGate;
import com.huziyang520.merlinlib.network.DamageFeedbackPayload;
import com.huziyang520.merlinlib.network.DamageFeedbackSink;
import com.huziyang520.merlinlib.network.HealthEditPayload;
import com.huziyang520.merlinlib.network.ItemEditPayload;
import com.huziyang520.merlinlib.tools.MerlinCreativeEntries;
import com.huziyang520.merlinlib.tools.ui.MerlinConfigScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * NeoForge entrypoint. Hands the mod event bus to the shared bootstrap, registers the diagnostic
 * command on the game bus and adds the creative inventory entries.
 */
@Mod(Constants.MOD_ID)
public class MerlinLibNeoForge {

    public MerlinLibNeoForge(IEventBus eventBus, ModContainer modContainer) {

        eventBus.addListener(this::onBuildCreativeTab);
        eventBus.addListener(this::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onIncomingDamage);
        // The mods list screen picks this up and shows the config button for MerlinLib. The factory
        // lambda only references the client side screen class inside its body, so a dedicated server
        // never loads it. The named local resolves the registerExtensionPoint overload ambiguity:
        // a bare lambda matches both Supplier<T> and the functional interface itself.
        IConfigScreenFactory factory = (mod, parent) -> new MerlinConfigScreen(parent);
        modContainer.<IConfigScreenFactory>registerExtensionPoint(IConfigScreenFactory.class, () -> factory);
        MerlinLib.init(eventBus);
    }

    /**
     * Registers the damage feedback payload. The handler delegates to the common sink, which is a
     * no-op on a dedicated server, so this class stays free of client references.
     *
     * @param event the payload registration event
     */
    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {

        // Codec only: the clientbound handler is registered on the client via
        // RegisterClientPayloadHandlersEvent — registering it here too throws
        // "Duplicate clientbound handler registration" on 26.3.
        event.registrar("1").playToClient(DamageFeedbackPayload.TYPE, DamageFeedbackPayload.STREAM_CODEC);
        // Item edit: client -> server, applied to the main hand of the requesting player. The server is the
        // only side that can persist it and the only side that can check permission.
        event.registrar("1").playToServer(ItemEditPayload.TYPE, ItemEditPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                        return;
                    }
                    if (!PermissionGate.canOpenEditor(serverPlayer)) {
                        serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component
                                .translatable("message.merlinlib.no_permission"));
                        return;
                    }
                    if (!payload.stack().isEmpty()) {
                        serverPlayer.setItemInHand(InteractionHand.MAIN_HAND, payload.stack());
                    }
                });

        event.registrar("1").playToServer(HealthEditPayload.TYPE, HealthEditPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        applyHealthEdit(serverPlayer, payload);
                    }
                });
    }

    /**
     * Applies a validated health edit on the server: the screen is only a remote control.
     *
     * @param player  the requesting player
     * @param payload the requested edit
     */
    private static void applyHealthEdit(ServerPlayer player, HealthEditPayload payload) {

        if (!PermissionGate.canOpenHealthEditor(player)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component
                    .translatable("message.merlinlib.no_permission"));
            return;
        }
        if (player.level().getEntity(payload.targetId()) instanceof LivingEntity target) {
            int max = Math.max(0, payload.max());
            int current = Math.max(0, Math.min(max, payload.current()));
            AttributeInstance attribute = target.getAttribute(Attributes.MAX_HEALTH);
            if (attribute != null) {
                Identifier id = Identifier.fromNamespaceAndPath(
                        com.huziyang520.merlinlib.Constants.MOD_ID, "health_editor");
                AttributeModifier existing = attribute.getModifier(id);
                double base = attribute.getBaseValue();
                if (existing != null) {
                    attribute.removeModifier(id);
                }
                if (max > 0) {
                    // Permanent, not transient: the edit belongs to the holder and is meant to survive a death
                    // and respawn. Setting the maximum to zero instead removes it, so a death at zero health -
                    // the one case where the player asked to be reset - comes back with the default maximum.
                    attribute.addPermanentModifier(new AttributeModifier(id, max - base,
                            AttributeModifier.Operation.ADD_VALUE));
                }
            }
            target.setHealth(Math.min(current, target.getMaxHealth()));
        }
    }

    /**
     * Publishes the resolved damage of every player dealt hit to the attacking client.
     *
     * <p>The event amount is the damage before the target's remaining health caps it, which is exactly
     * the "theoretical damage of this swing" the overlay shows.
     *
     * @param event the incoming damage event
     */
    private void onIncomingDamage(LivingIncomingDamageEvent event) {

        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            attacker.connection.send(new DamageFeedbackPayload(
                    event.getAmount(), false, event.getEntity().getId()));
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {

        MerlinCommand.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    /**
     * Adds one enchanted book per MerlinLib enchantment to the vanilla ingredients tab.
     *
     * <p>The stacks are built from the parameters' registry access, which is only populated once the
     * data driven enchantment registry has been loaded; outside of that window the list is simply empty.
     */
    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {

        if (MerlinCreativeEntries.TAB.equals(event.getTabKey())) {
            for (ItemStack stack : MerlinCreativeEntries.enchantedBooks(event.getParameters().holders())) {
                // NeoForge rejects a stack that the tab already holds with
                // "Itemstack ... already exists in the tab's list", and a tab is rebuilt whenever the
                // creative screen is opened, so the same book arrives more than once per session.
                if (event.getParentEntries().contains(stack) || event.getSearchEntries().contains(stack)) {
                    continue;
                }
                event.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            return;
        }
        if (MerlinCreativeEntries.TOOLS_TAB.equals(event.getTabKey())) {
            for (ItemStack stack : MerlinCreativeEntries.testWeapons()) {
                if (event.getParentEntries().contains(stack) || event.getSearchEntries().contains(stack)) {
                    continue;
                }
                event.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
    }
}
