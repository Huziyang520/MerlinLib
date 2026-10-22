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
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
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
        NeoForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onUseEntity);
        DamageFeedbackSink.setClientSink(payload ->
                CombatFeedback.instance().onServerDamage(payload.damage(), payload.critical(), payload.targetId()));
        ServerSender.setSink(payload ->
                net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                        (net.minecraft.network.protocol.common.custom.CustomPacketPayload) payload));
        MacroStorage.load();
        com.huziyang520.merlinlib.tools.HealthEditorItem.setOpener(target ->
                MerlinScreens.openHealthEditor(null, target));
    }

    /**
     * Whether the stored bindings were already checked against the old, wrong defaults.
     *
     * <p>Done on the first client tick rather than in the constructor: the bindings are created when this class
     * is loaded, but the values the player has in {@code options.txt} are applied later, when the game builds
     * its options - so a repair in the constructor would be overwritten.
     */
    private static boolean hotkeysRepaired;

    /**
     * Moves the two bindings off the keys an earlier build of this line saved by mistake.
     *
     * <p>That build took its key codes from the 26.3 line, which numbers keys by USB HID usage ids, while
     * 26.1 and 26.2 still use the GLFW codes. The bindings were therefore created with 80 and 79 - the P and O
     * keys - and the game wrote that choice into {@code options.txt}. Correcting the default alone does nothing
     * for a profile that already has {@code key_key.merlinlib.open_editor:key.keyboard.p} in it, which is
     * exactly the "the two hot keys are still missing" report. Only the two mistaken values are moved; any
     * other binding the player chose is left alone.
     */
    private void repairHotkeyDefaults() {
        boolean changed = false;
        if ("key.keyboard.p".equals(OPEN_EDITOR.getKey().getName())) {
            OPEN_EDITOR.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM
                    .getOrCreate(MerlinUi.KEY_LEFT));
            changed = true;
        }
        if ("key.keyboard.o".equals(OPEN_MACROS.getKey().getName())) {
            OPEN_MACROS.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM
                    .getOrCreate(MerlinUi.KEY_RIGHT));
            changed = true;
        }
        if (changed) {
            Constants.LOG.info("[MerlinLib] the two hot keys were moved from the P/O keys to the arrow keys, "
                    + "the defaults this build ships");
        }
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {

        event.register(OPEN_EDITOR);
        event.register(OPEN_MACROS);
    }

    /**
     * Registers the client side entry points of the toolkit screens.
     *
     * <p>A screen can only be opened where the screen exists, so these are client commands rather than server
     * ones: {@code /merlinlib editor} would otherwise be a server command that can do nothing on the client.
     * The root literal is the same one the server command uses, and the children do not collide, so both trees
     * are usable at once - the server answers {@code list}/{@code info}/{@code book}, the client answers the
     * three screens.
     *
     * @param event the client command registration event
     */
    private void onRegisterClientCommands(RegisterClientCommandsEvent event) {

        event.getDispatcher().register(Commands.literal("merlinlib")
                .then(Commands.literal("editor").executes(context -> {
                    MerlinScreens.openItemEditor();
                    return 1;
                }))
                .then(Commands.literal("health").executes(context -> {
                    MerlinScreens.openOwnHealthEditor();
                    return 1;
                }))
                .then(Commands.literal("macro").executes(context -> {
                    MerlinScreens.openMacroScreen(null);
                    return 1;
                }))
                .then(Commands.literal("notices").executes(context -> {
                    MerlinScreens.openNoticesScreen(null);
                    return 1;
                })));
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

        if (!hotkeysRepaired) {
            hotkeysRepaired = true;
            repairHotkeyDefaults();
        }
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
