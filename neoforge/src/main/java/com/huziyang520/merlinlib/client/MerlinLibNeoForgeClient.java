package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.network.DamageFeedbackSink;
import com.huziyang520.merlinlib.network.ServerSender;
import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.tools.hud.CombatFeedback;
import com.huziyang520.merlinlib.tools.macro.MacroStorage;
import com.huziyang520.merlinlib.tools.ui.HealthEditorScreen;
import com.huziyang520.merlinlib.tools.ui.MacroScreen;
import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import com.huziyang520.merlinlib.tools.ui.MerlinUi;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * NeoForge client only entrypoint, declared with {@code dist = CLIENT} so a dedicated server never
 * even loads this class and the client types it uses cannot break server startup.
 */
@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class MerlinLibNeoForgeClient {

    /** The vanilla controls screen column dedicated to MerlinLib. */
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "main"));

    /**
     * One binding only, registered like any vanilla key binding, so it appears in the vanilla controls
     * screen and can be rebound to anything by the player.
     */
    private static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.merlinlib.open_editor", MerlinUi.KEY_LEFT, CATEGORY);

    /** Opens the macro screen. */
    private static final KeyMapping OPEN_MACROS = new KeyMapping(
            "key.merlinlib.macros", MerlinUi.KEY_RIGHT, CATEGORY);

    public MerlinLibNeoForgeClient(IEventBus modBus) {

        modBus.addListener(this::onRegisterKeyMappings);
        modBus.addListener(this::onRegisterGuiLayers);
        modBus.addListener(this::onRegisterClientPayloads);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onUseEntity);
        DamageFeedbackSink.setClientSink(payload ->
                CombatFeedback.instance().onServerDamage(payload.damage(), payload.critical(), payload.targetId()));
        ServerSender.setSink(payload ->
                net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                        (net.minecraft.network.protocol.common.custom.CustomPacketPayload) payload));
        MacroStorage.load();
        com.huziyang520.merlinlib.tools.HealthEditorItem.setOpener(target ->
                MerlinScreens.openHealthEditor(null, target, true));
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {

        event.register(OPEN_EDITOR);
        event.register(OPEN_MACROS);
    }

    /**
     * Registers the client side handler of the damage feedback payload.
     *
     * @param event the client payload registration event
     */
    private void onRegisterClientPayloads(
            net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent event) {

        event.register(com.huziyang520.merlinlib.network.DamageFeedbackPayload.TYPE,
                (payload, context) -> DamageFeedbackSink.handle(payload));
    }

    /**
     * Registers the combat overlay as the topmost GUI layer, so the numbers sit above the vanilla HUD.
     *
     * @param event the layer registration event
     */
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

    /**
     * Sneak using an entity opens the health editor for it.
     *
     * @param event the interaction event
     */
    private void onUseEntity(PlayerInteractEvent.EntityInteract event) {

        if (event.getLevel().isClientSide() && event.getEntity().isShiftKeyDown()
                && !com.huziyang520.merlinlib.config.ConfigManager.server().healthEditorRequiresItem()
                && event.getTarget() instanceof net.minecraft.world.entity.LivingEntity living) {
            MerlinScreens.openHealthEditor(null, living, false);
        }
    }
}
