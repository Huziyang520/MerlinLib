package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.network.DamageFeedbackSink;
import com.huziyang520.merlinlib.network.ServerSender;
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
 *
 * <h2>What this class is, next to the 26.3 line's client entrypoint</h2>
 *
 * <p>26.3 had {@code MerlinLibNeoForgeClient} (and a Fabric twin). This is those two collapsed into one
 * Forge class. It is annotated {@code Dist.CLIENT}, which is the 1.20.1 way of saying what 26.3 said with
 * {@code @Mod(dist = Dist.CLIENT)}: on a dedicated server this class is never loaded, so the client-only
 * types it touches - {@code Minecraft}, {@code GuiGraphics}, {@code KeyMapping} - cannot break server
 * startup. That distinction is load bearing rather than cosmetic: a server that loads one of those classes
 * dies in the class initialiser.
 *
 * <h2>The four things this class does that nothing else can</h2>
 *
 * <ol>
 *   <li><b>Installs the damage feedback sink.</b> The server sends a payload; something has to turn it
 *       into a floating number. That something is a client-only object, so the server side cannot do it
 *       and the library leaves a hole for it here.</li>
 *   <li><b>Points the network layer at the client's sender.</b> {@link ServerSender#send} has to reach
 *       {@code SimpleChannel#sendToServer}, which only exists on a client.</li>
 *   <li><b>Registers the two key bindings</b> so they appear in the vanilla controls screen and can be
 *       rebound by the player.</li>
 *   <li><b>Drives the per tick work</b> - the hotkey layer and the floating numbers' lifetimes - and
 *       draws the overlay.</li>
 * </ol>
 *
 * <h2>1.20.1: what replaced what</h2>
 *
 * <ul>
 *   <li>{@code RegisterGuiLayersEvent} / {@code registerAboveAll} is a 1.21 API and has no 1.20.1
 *       equivalent. The overlay is drawn from {@link RenderGuiEvent.Post} instead, which fires after the
 *       vanilla HUD has been drawn - the same position in the frame that {@code registerAboveAll} asked
 *       for, reached a different way.</li>
 *   <li>{@code ClientTickEvent.Post} is also 1.21; 1.20.1 has a single {@code TickEvent.ClientTickEvent}
 *       with a {@code phase} field, so the handler checks for {@code Phase.END} to get the same
 *       "after the tick" moment.</li>
 *   <li>{@code KeyMapping.Category.register(...)} does not exist; the category is the plain string
 *       {@code "key.categories.misc"} that the 1.20.1 constructor takes.</li>
 *   <li>{@code KeyMapping(String, int, Category)} does not exist; the 1.20.1 constructor is
 *       {@code KeyMapping(String translationKey, InputConstants.Type type, int keyCode, String category)}.</li>
 *   <li>{@code ClientPacketDistributor.sendToServer} does not exist; the channel does it, which is why
 *       {@link ServerSender#setSink} is left unset here - see the note in the constructor.</li>
 * </ul>
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
     * <p>{@code RenderGuiEvent.Post} is 1.20.1's "the HUD has been drawn, add to it" moment. The 26.3 line
     * registered a GUI layer {@code aboveAll}; this is the same position in the frame reached through the
     * event this version has.
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