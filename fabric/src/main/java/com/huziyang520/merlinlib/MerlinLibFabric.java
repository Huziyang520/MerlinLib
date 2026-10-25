// Fabric entrypoint.

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
import com.huziyang520.merlinlib.tools.HealthCeiling;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
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

public class MerlinLibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(MerlinCommand::register);
        registerDamageFeedback();

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                HealthCeiling.copyTo(oldPlayer, newPlayer));

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

    private static void registerDamageFeedback() {
        PayloadTypeRegistry.clientboundPlay().register(DamageFeedbackPayload.TYPE, DamageFeedbackPayload.STREAM_CODEC);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (source.getEntity() instanceof ServerPlayer attacker) {
                ServerPlayNetworking.send(attacker,
                        new DamageFeedbackPayload(amount, false, entity.getId()));
            }
            return true;
        });
        PayloadTypeRegistry.serverboundPlay().register(HealthEditPayload.TYPE, HealthEditPayload.STREAM_CODEC);
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
                HealthCeiling.apply(target, max);
                target.setHealth(Math.min(current, target.getMaxHealth()));
            }
        });
    }
}
