package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.network.DamageFeedbackSink;
import com.huziyang520.merlinlib.network.ServerSender;
import com.huziyang520.merlinlib.tools.hud.CombatFeedback;
import com.huziyang520.merlinlib.tools.macro.MacroStorage;
import com.huziyang520.merlinlib.tools.ui.ConfigScreens;
import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import com.huziyang520.merlinlib.tools.ui.MerlinUi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
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

    public MerlinLibNeoForgeClient(IEventBus modBus, ModContainer modContainer) {

        // The mod list's "Config" button, answered by the screen the loader already has. Handing the button to
        // ConfigurationScreen instead of a screen of our own is the whole point of declaring the switches in a
        // specification: the layout, the tooltips, the reset button and the undo history all come from the
        // loader. The section screen is MerlinLib's own subclass, so the join notice page has a row on it.
        //
        // Registered from the CLIENT entrypoint on purpose: a lambda implementing IConfigScreenFactory names
        // Screen in its signature, so creating one on a dedicated server trips RuntimeDistCleaner before the
        // screen is ever opened.
        IConfigScreenFactory factory =
                (mod, parent) -> new ConfigurationScreen(mod, parent, MerlinConfigSectionScreen::new);
        modContainer.<IConfigScreenFactory>registerExtensionPoint(IConfigScreenFactory.class, () -> factory);
        ConfigScreens.install(parent ->
                new ConfigurationScreen(modContainer, parent, MerlinConfigSectionScreen::new));

        modBus.addListener(this::onRegisterKeyMappings);
        modBus.addListener(this::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
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
     * Adds the {@code /merlinlib} sub commands that open a toolkit screen.
     *
     * <p>These are client commands because a screen can only be opened on the client: the server has no idea
     * what a screen is, and an operator command that asks the server to show one is a network round trip for
     * something that was already decided locally. NeoForge merges a client command tree into the server's for
     * suggestions, and an argument this tree does not know - {@code /merlinlib list}, {@code /merlinlib book} -
     * falls through to the server, so both trees live side by side under the same name.
     *
     * @param event the client command registration event
     */
    private void onRegisterClientCommands(RegisterClientCommandsEvent event) {

        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("merlinlib")
                .then(Commands.literal("editor")
                        .executes(MerlinLibNeoForgeClient::openEditor))
                .then(Commands.literal("health")
                        .executes(MerlinLibNeoForgeClient::openHealth))
                .then(Commands.literal("macro")
                        .executes(MerlinLibNeoForgeClient::openMacros))
                .then(Commands.literal("config")
                        .executes(MerlinLibNeoForgeClient::openConfig))
                .then(Commands.literal("notices")
                        .executes(MerlinLibNeoForgeClient::openNotices)));
    }

    /**
     * @param context the command context, unused
     * @return 1, the standard success code
     */
    private static int openEditor(CommandContext<CommandSourceStack> context) {

        MerlinScreens.openItemEditorFromCommand();
        return 1;
    }

    /**
     * @param context the command context, unused
     * @return 1, the standard success code
     */
    private static int openHealth(CommandContext<CommandSourceStack> context) {

        MerlinScreens.openHealthEditorFromCommand();
        return 1;
    }

    /**
     * @param context the command context, unused
     * @return 1, the standard success code
     */
    private static int openMacros(CommandContext<CommandSourceStack> context) {

        MerlinScreens.openMacroScreen(null);
        return 1;
    }

    /**
     * Opens the loader's configuration screen, the same one the mod list opens.
     *
     * @param context the command context, unused
     * @return 1, the standard success code
     */
    private static int openConfig(CommandContext<CommandSourceStack> context) {

        MerlinScreens.openConfigScreen(null);
        return 1;
    }

    /**
     * Opens the join notice page: the master switch and one switch per business mod.
     *
     * <p>Registered here with the other screen commands for the same reason - only the client can open a
     * screen - and with the same requirements: none. The switch it writes lives in this side's own
     * configuration, and a player who may not change it simply does not get the permission to run the command
     * on a server that cares.
     *
     * @param context the command context, unused
     * @return 1, the standard success code
     */
    private static int openNotices(CommandContext<CommandSourceStack> context) {

        MerlinScreens.openNoticesScreen(null);
        return 1;
    }

    /**
     * Registers the combat overlay as the topmost GUI layer, so the numbers sit above the vanilla HUD.
     *
     * @param event the layer registration event
     */
    private void onRegisterGuiLayers(RegisterGuiLayersEvent event) {

        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "combat_overlay"),
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
