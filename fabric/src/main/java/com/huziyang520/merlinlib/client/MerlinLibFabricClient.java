// Fabric client entrypoint: the single key binding and the editor opening.

package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.network.DamageFeedbackPayload;
import com.huziyang520.merlinlib.network.DamageFeedbackSink;
import com.huziyang520.merlinlib.network.ServerSender;
import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.FabricConfig;
import com.huziyang520.merlinlib.config.MezzConfigBridge;
import com.huziyang520.merlinlib.tools.hud.CombatFeedback;
import com.huziyang520.merlinlib.tools.macro.MacroStorage;
import com.huziyang520.merlinlib.tools.ui.HealthEditorScreen;
import com.huziyang520.merlinlib.tools.ui.MacroScreen;
import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import com.huziyang520.merlinlib.tools.ui.MerlinUi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class MerlinLibFabricClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "main"));

    private static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.merlinlib.open_editor", MerlinUi.KEY_LEFT, CATEGORY);

    private static final KeyMapping OPEN_MACROS = new KeyMapping(
            "key.merlinlib.macros", MerlinUi.KEY_RIGHT, CATEGORY);

    @Override
    public void onInitializeClient() {
        MezzConfigBridge.install();
        KeyMappingHelper.registerKeyMapping(OPEN_EDITOR);
        KeyMappingHelper.registerKeyMapping(OPEN_MACROS);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_EDITOR.consumeClick()) {
                MerlinScreens.openItemEditorFromHotkey();
            }
            while (OPEN_MACROS.consumeClick()) {
                MerlinScreens.openMacroScreen(null);
            }
            CombatFeedback.instance().tick(client);
            MacroStorage.tick(client);
        });

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(com.huziyang520.merlinlib.Constants.MOD_ID, "combat_overlay"),
                CombatFeedback.instance()::render);
        CombatFeedback.announce();

        DamageFeedbackSink.setClientSink(payload ->
                CombatFeedback.instance().onServerDamage(payload.damage(), payload.critical(), payload.targetId()));
        ClientPlayNetworking.registerGlobalReceiver(DamageFeedbackPayload.TYPE, (payload, context) ->
                DamageFeedbackSink.handle(payload));
        ServerSender.setSink(payload ->
                ClientPlayNetworking.send((CustomPacketPayload) payload));
        com.huziyang520.merlinlib.tools.HealthEditorItem.setOpener(target ->
                MerlinScreens.openHealthEditor(null, target));
        MacroStorage.load();
        FabricConfig.install();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
                ClientCommands.literal(Constants.MOD_ID)
                        .then(ClientCommands.literal("editor")
                                .executes(context -> {
                                    MerlinScreens.openItemEditor();
                                    return 1;
                                }))
                        .then(ClientCommands.literal("health")
                                .executes(context -> {
                                    MerlinScreens.openHealthEditorSelf();
                                    return 1;
                                }))
                        .then(ClientCommands.literal("macro")
                                .executes(context -> {
                                    MerlinScreens.openMacroScreen(null);
                                    return 1;
                                }))
                        .then(ClientCommands.literal("config")
                                .executes(context -> {
                                    if (MerlinScreens.openConfigScreen(null)) {
                                        return 1;
                                    }
                                    context.getSource().sendError(
                                            Component.translatable("message.merlinlib.config.needs_screen"));
                                    return 0;
                                }))
                        .then(ClientCommands.literal("notices")
                                .executes(context -> {
                                    MerlinScreens.openNoticesScreen(null);
                                    return 1;
                                }))));

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide() && player.isShiftKeyDown()
                    && entity instanceof net.minecraft.world.entity.LivingEntity living
                    && MerlinScreens.openHealthEditor(null, living)) {
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) {
                CombatFeedback.instance().onLocalSwing(player, entity, CombatFeedback.isCritical(player));
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
    }
}
