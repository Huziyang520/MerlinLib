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
import net.minecraft.resources.ResourceLocation;
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

    /** The vanilla controls screen column dedicated to MerlinLib. 1.21.1 keys it by a plain string. */
    private static final String CATEGORY = "key.categories.merlinlib";

    /**
     * One binding only, registered like any vanilla key binding, so it appears in the vanilla controls
     * screen and can be rebound to anything by the player.
     */
    private static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.merlinlib.open_editor", MerlinUi.KEY_LEFT, CATEGORY);

    /** Opens the macro screen. */
    private static final KeyMapping OPEN_MACROS = new KeyMapping(
            "key.merlinlib.macros", MerlinUi.KEY_RIGHT, CATEGORY);

    public MerlinLibNeoForgeClient(IEventBus modBus, net.neoforged.fml.ModContainer modContainer) {

        // The mod list's "Config" button. Registered from the CLIENT entrypoint on purpose: a lambda
        // implementing IConfigScreenFactory names Screen in its signature, so creating one on a
        // dedicated server trips RuntimeDistCleaner before the screen is ever opened.
        net.neoforged.neoforge.client.gui.IConfigScreenFactory factory =
                (mod, parent) -> new com.huziyang520.merlinlib.tools.ui.MerlinConfigScreen(parent);
        modContainer.<net.neoforged.neoforge.client.gui.IConfigScreenFactory>registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, () -> factory);

        modBus.addListener(this::onRegisterKeyMappings);
        modBus.addListener(this::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onUseEntity);
        DamageFeedbackSink.setClientSink(payload ->
                CombatFeedback.instance().onServerDamage(payload.damage(), payload.critical(), payload.targetId()));
        // 1.21.1 sends through the common PacketDistributor; the client-only distributor class is a
        // later addition.
        ServerSender.setSink(payload ->
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        (net.minecraft.network.protocol.common.custom.CustomPacketPayload) payload));
        MacroStorage.load();
        com.huziyang520.merlinlib.tools.HealthEditorItem.setOpener(target ->
                MerlinScreens.openHealthEditor(null, target));
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {

        event.register(OPEN_EDITOR);
        event.register(OPEN_MACROS);
    }

    /**
     * Registers the combat overlay as the topmost GUI layer, so the numbers sit above the vanilla HUD.
     *
     * @param event the layer registration event
     */
    private void onRegisterGuiLayers(RegisterGuiLayersEvent event) {

        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(com.huziyang520.merlinlib.Constants.MOD_ID, "combat_overlay"),
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

        // openHealthEditor decides everything, including whether the health editor item has to be held: the
        // check that used to sit here demanded the opposite of what that switch means.
        if (event.getLevel().isClientSide() && event.getEntity().isShiftKeyDown()
                && event.getTarget() instanceof net.minecraft.world.entity.LivingEntity living) {
            MerlinScreens.openHealthEditor(null, living);
        }
    }
}
