// NeoForge client only entrypoint, declared with dist = CLIENT so a dedicated server never even loads this class and the ...

package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.network.DamageFeedbackSink;
import com.huziyang520.merlinlib.network.ServerSender;
import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.tools.hud.CombatFeedback;
import com.huziyang520.merlinlib.tools.macro.MacroStorage;
import com.huziyang520.merlinlib.tools.ui.HealthEditorScreen;
import com.huziyang520.merlinlib.tools.ui.MacroScreen;
import com.huziyang520.merlinlib.tools.ui.ConfigScreens;
import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import com.huziyang520.merlinlib.tools.ui.MerlinUi;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class MerlinLibNeoForgeClient {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "main"));

    private static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.merlinlib.open_editor", MerlinUi.KEY_LEFT, CATEGORY);

    private static final KeyMapping OPEN_MACROS = new KeyMapping(
            "key.merlinlib.macros", MerlinUi.KEY_RIGHT, CATEGORY);

    public MerlinLibNeoForgeClient(IEventBus modBus) {
        modBus.addListener(this::onRegisterKeyMappings);
        modBus.addListener(this::onRegisterGuiLayers);
        modBus.addListener(this::onRegisterClientPayloads);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
        NeoForge.EVENT_BUS.addListener(this::onUseEntity);
        DamageFeedbackSink.setClientSink(payload ->
                CombatFeedback.instance().onServerDamage(payload.damage(), payload.critical(), payload.targetId()));
        ServerSender.setSink(payload ->
                net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                        (net.minecraft.network.protocol.common.custom.CustomPacketPayload) payload));
        MacroStorage.load();
        com.huziyang520.merlinlib.tools.HealthEditorItem.setOpener(target ->
                MerlinScreens.openHealthEditor(null, target));
        ModContainer container = ModList.get().getModContainerById(Constants.MOD_ID).orElse(null);
        if (container != null) {
            container.registerExtensionPoint(IConfigScreenFactory.class,
                    (IConfigScreenFactory) (mod, parent) ->
                            new ConfigurationScreen(mod, parent, MerlinConfigSectionScreen::new));
            ConfigScreens.install(parent ->
                    new ConfigurationScreen(container, parent, MerlinConfigSectionScreen::new));
        }
    }

    private void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(Constants.MOD_ID)
                .then(Commands.literal("editor")
                        .executes(context -> {
                            MerlinScreens.openItemEditor();
                            return 1;
                        }))
                .then(Commands.literal("health")
                        .executes(context -> {
                            MerlinScreens.openHealthEditorSelf();
                            return 1;
                        }))
                .then(Commands.literal("macro")
                        .executes(context -> {
                            MerlinScreens.openMacroScreen(null);
                            return 1;
                        }))
                .then(Commands.literal("config")
                        .executes(context -> {
                            MerlinScreens.openConfigScreen(null);
                            return 1;
                        }))
                .then(Commands.literal("notices")
                        .executes(context -> {
                            MerlinScreens.openNoticesScreen(null);
                            return 1;
                        })));
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EDITOR);
        event.register(OPEN_MACROS);
    }

    private void onRegisterClientPayloads(
            net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent event) {
        event.register(com.huziyang520.merlinlib.network.DamageFeedbackPayload.TYPE,
                (payload, context) -> DamageFeedbackSink.handle(payload));
    }

    private void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(com.huziyang520.merlinlib.Constants.MOD_ID, "combat_overlay"),
                CombatFeedback.instance()::render);
        CombatFeedback.announce();
    }

    private void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_EDITOR.consumeClick()) {
            MerlinScreens.openItemEditorFromHotkey();
        }
        while (OPEN_MACROS.consumeClick()) {
            if (net.minecraft.client.Minecraft.getInstance().player != null) {
                MerlinScreens.openMacroScreen(null);
            }
        }
        if (net.minecraft.client.Minecraft.getInstance().player != null) {
            CombatFeedback.instance().tick(net.minecraft.client.Minecraft.getInstance());
            MacroStorage.tick(net.minecraft.client.Minecraft.getInstance());
        }
    }

    private void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() && event.getEntity().isShiftKeyDown()
                && event.getTarget() instanceof net.minecraft.world.entity.LivingEntity living) {
            MerlinScreens.openHealthEditor(null, living);
        }
    }
}
