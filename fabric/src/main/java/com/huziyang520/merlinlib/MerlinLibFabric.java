package com.huziyang520.merlinlib;

import com.huziyang520.merlinlib.command.MerlinCommand;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.impl.PermissionGate;
import com.huziyang520.merlinlib.network.DamageFeedbackPayload;
import com.huziyang520.merlinlib.network.HealthEditPayload;
import com.huziyang520.merlinlib.network.ItemEditPayload;
import com.huziyang520.merlinlib.tools.MerlinCreativeEntries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric entrypoint. Boots the loader independent part through {@link MerlinLib#init(Object)},
 * registers the diagnostic command and adds the creative inventory entries.
 *
 * <p>No client class is referenced here, so a dedicated server can load this class safely.
 */
public class MerlinLibFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register(MerlinCommand::register);

        registerDamageFeedback();

        // 26.3 renamed the item group api to the creative tab api; the output carries the display
        // parameters, whose holder lookup is exactly what the data driven enchantment registry needs.
        CreativeModeTabEvents.modifyOutputEvent(MerlinCreativeEntries.TAB).register(output -> {
            for (ItemStack stack : MerlinCreativeEntries.enchantedBooks(output.getContext().holders())) {
                output.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(MerlinCreativeEntries.TOOLS_TAB).register(output -> {
            for (ItemStack stack : MerlinCreativeEntries.testWeapons()) {
                output.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });

        MerlinLib.init(null);
    }

    /**
     * Registers the damage feedback payload and publishes it from the server damage event.
     *
     * <p>The event amount is the damage before the target's remaining health caps it, which is exactly
     * the "theoretical damage of this swing" the overlay shows. Returning {@code true} leaves vanilla's
     * own damage handling untouched.
     */
    private static void registerDamageFeedback() {

        PayloadTypeRegistry.clientboundPlay().register(DamageFeedbackPayload.TYPE, DamageFeedbackPayload.STREAM_CODEC);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (source.getEntity() instanceof ServerPlayer attacker) {
                ServerPlayNetworking.send(attacker,
                        new DamageFeedbackPayload(amount, false, entity.getId()));
            }
            return true;
        });

        // Health edit: client -> server, validated and clamped here before touching the entity.
        PayloadTypeRegistry.serverboundPlay().register(HealthEditPayload.TYPE, HealthEditPayload.STREAM_CODEC);
        // Item edit: client -> server, applied to the main hand of the requesting player. The server is the
        // only side that can persist it, and the only side that can check permission, so the editor is a
        // remote control: a refused request simply changes nothing.
        PayloadTypeRegistry.serverboundPlay().register(ItemEditPayload.TYPE, ItemEditPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ItemEditPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (!PermissionGate.canOpenEditor(player)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component
                        .translatable("message.merlinlib.no_permission"));
                return;
            }
            ItemStack requested = payload.stack();
            if (!requested.isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, requested);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(HealthEditPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
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
                    attribute.removeModifier(id);
                    if (max > 0) {
                        // Permanent, not transient: the edit belongs to the holder and is meant to survive a
                        // death and respawn. Setting the maximum to zero instead removes it, so a death at
                        // zero health - the one case where the player asked to be reset - comes back with the
                        // default maximum, which is the safety net this switch exists for.
                        attribute.addPermanentModifier(new AttributeModifier(id, max - attribute.getBaseValue(),
                                AttributeModifier.Operation.ADD_VALUE));
                    }
                }
                target.setHealth(Math.min(current, target.getMaxHealth()));
            }
        });
    }
}
