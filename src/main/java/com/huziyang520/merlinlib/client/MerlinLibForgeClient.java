package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.network.DamageFeedbackSink;
import com.huziyang520.merlinlib.network.ServerSender;
import com.huziyang520.merlinlib.network.ToolOpenSink;
import com.huziyang520.merlinlib.tools.HealthEditorItem;
import com.huziyang520.merlinlib.tools.hud.CombatFeedback;
import com.huziyang520.merlinlib.tools.macro.MacroStorage;
import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import com.huziyang520.merlinlib.tools.ui.MerlinUi;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client only entrypoint for MerlinLib on 1.20.1.
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MerlinLibForgeClient {

    /**
     * The vanilla controls screen column these bindings appear under.
     *
     * <p>{@code key.categories.misc} is the vanilla key for "Miscellaneous". 1.20.1 takes the category as
     * a string, so there is no category object to register and no translation key of our own to ship.
 */
    private static final String CATEGORY = "key.categories.misc";

    /** Opens the item editor on the held item. */
    private static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.merlinlib.open_editor", InputConstants.Type.KEYSYM, MerlinUi.KEY_LEFT, CATEGORY);

    /** Opens the macro screen. */
    private static final KeyMapping OPEN_MACROS = new KeyMapping(
            "key.merlinlib.macros", InputConstants.Type.KEYSYM, MerlinUi.KEY_RIGHT, CATEGORY);

    private MerlinLibForgeClient() {
    }

    /**
     * Registers the two key bindings with the loader.
     *
     * <p>Called by {@link MerlinClientSetup}, which sits on the <b>mod</b> bus where
     * {@code RegisterKeyMappingsEvent} actually fires. This class is on the game bus and would never see
     * that event - the mistake that originally left the keys out of the controls screen, and the reason
     * the call is forwarded rather than subscribed here. The mappings themselves stay in this class
     * because the tick handler consumes their clicks.
     *
     * @param event the key registration
 */
    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EDITOR);
        event.register(OPEN_MACROS);
    }

    /**
     * Does the per tick client work: hotkey combinations, the macro layer, and the floating numbers'
     * lifetimes.
     *
     * <p>Guarded on {@code Phase.END} because 1.20.1 fires this event twice per tick. Running the hotkey
     * drain on both phases would let one key press fire a macro twice.
     *
     * @param event the client tick
 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        // The library's own two bindings first, then the player's macros.
        while (OPEN_EDITOR.consumeClick()) {
            MerlinScreens.openItemEditorFromHotkey();
        }
        while (OPEN_MACROS.consumeClick()) {
            MerlinScreens.openMacroScreen(null);
        }
        CombatFeedback.instance().tick(minecraft);
        MacroStorage.tick(minecraft);
    }

    /**
     * Draws the combat overlay, after the vanilla HUD.
 *
     * @param event the render pass
 */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        CombatFeedback.instance().render(event.getGuiGraphics(), event.getPartialTick());
    }

    /**
     * Sneak using an entity opens the health editor for it.
     *
     * <p>The client side guard is what keeps this from running twice: the interaction fires on both sides,
     * and the screen only exists on the client.
     *
     * @param event the interaction
 */
    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() && event.getEntity().isShiftKeyDown()
                && event.getTarget() instanceof LivingEntity living) {
            MerlinScreens.openHealthEditor(null, living);
        }
    }

    /**
     * Wires the pieces that need a running client.
     *
     * <h2>Why this is not the mod constructor</h2>
     *
     * <p>Everything here needs the client's classes to be usable and the {@code Minecraft} instance to
     * exist. Doing it from the mod constructor would run it while the game is still assembling its
     * subsystems. It is called from {@link MerlinClientSetup}, which is driven by Forge's
     * {@code FMLClientSetupEvent} - the moment this version provides for exactly this kind of
     * installation.
     *
     * <p>The network sink is deliberately <b>not</b> installed here: {@link ServerSender#send} already
     * reaches {@code SimpleChannel#sendToServer} on its own on this version, so installing a sink as well
     * would be a second path doing the same thing. The damage feedback sink <em>is</em> installed, because
     * that direction genuinely needs a client only destination.
 */
    public static void install() {
        DamageFeedbackSink.setClientSink(payload ->
                CombatFeedback.instance().onServerDamage(payload.damage(), payload.critical(), payload.targetId()));
        // /merlinlib editor, health and macro run on the server and end here, on the side that owns screens.
        ToolOpenSink.setClientSink(MerlinScreens::openFromCommand);

        MacroStorage.load();
        HealthEditorItem.setOpener(target -> MerlinScreens.openHealthEditor(null, target));
        CombatFeedback.announce();
        Constants.LOG.info("[MerlinLib] client side toolkit installed ({} key binding(s))", 2);
    }

    /**
     * @return the GLFW code of the item editor binding, for the controls screen
 */
    public static int editorKeyCode() {
        return OPEN_EDITOR.getKey().getValue();
    }

    /**
     * @return the GLFW code of the macro binding, for the controls screen
 */
    public static int macroKeyCode() {
        return OPEN_MACROS.getKey().getValue();
    }
}
