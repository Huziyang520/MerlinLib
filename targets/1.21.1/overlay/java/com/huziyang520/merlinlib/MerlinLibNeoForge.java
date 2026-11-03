// NeoForge entrypoint.

package com.huziyang520.merlinlib;

import com.huziyang520.merlinlib.command.MerlinCommand;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.ConfigScope;
import com.huziyang520.merlinlib.config.NeoConfigs;
import com.huziyang520.merlinlib.config.NoticeSettings;
import com.huziyang520.merlinlib.impl.PermissionGate;
import com.huziyang520.merlinlib.network.DamageFeedbackPayload;
import com.huziyang520.merlinlib.network.DamageFeedbackSink;
import com.huziyang520.merlinlib.network.HealthEditPayload;
import com.huziyang520.merlinlib.network.ItemEditPayload;
import com.huziyang520.merlinlib.tools.MerlinCreativeEntries;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import com.huziyang520.merlinlib.tools.HealthCeiling;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Constants.MOD_ID)
public class MerlinLibNeoForge {
    public MerlinLibNeoForge(IEventBus eventBus, ModContainer modContainer) {
        NoticeSettings.init();
        NeoConfigs.register(modContainer);
        ConfigManager.setBackend(ConfigScope.CLIENT, NeoConfigs.clientBackend());
        ConfigManager.setBackend(ConfigScope.SERVER, NeoConfigs.commonBackend());
        eventBus.addListener(this::onConfigLoading);
        eventBus.addListener(this::onConfigReloading);
        eventBus.addListener(this::onBuildCreativeTab);
        eventBus.addListener(this::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(this::onPlayerClone);
        MerlinLib.init(eventBus);
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        ConfigManager.onConfigLoaded(event.getConfig());
    }

    private void onConfigReloading(ModConfigEvent.Reloading event) {
        ConfigManager.reload();
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(DamageFeedbackPayload.TYPE, DamageFeedbackPayload.STREAM_CODEC,
                (payload, context) -> DamageFeedbackSink.handle(payload));

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

    private static void applyHealthEdit(ServerPlayer player, HealthEditPayload payload) {
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
    }

    private void onPlayerClone(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
        HealthCeiling.copyTo(event.getOriginal(), event.getEntity());
    }

    private void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            attacker.connection.send(new DamageFeedbackPayload(
                    event.getAmount(), false, event.getEntity().getId()));
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        MerlinCommand.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (MerlinCreativeEntries.TAB.equals(event.getTabKey())) {
            for (ItemStack stack : MerlinCreativeEntries.enchantedBooks(event.getParameters().holders())) {
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
