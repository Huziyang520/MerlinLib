package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.network.DamageFeedbackPayload;
import com.huziyang520.merlinlib.network.DamageFeedbackSink;
import com.huziyang520.merlinlib.network.ServerSender;
import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.tools.hud.CombatFeedback;
import com.huziyang520.merlinlib.tools.macro.MacroStorage;
import com.huziyang520.merlinlib.tools.ui.HealthEditorScreen;
import com.huziyang520.merlinlib.tools.ui.MacroScreen;
import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import com.huziyang520.merlinlib.tools.ui.MerlinUi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Fabric client entrypoint: the single key binding and the editor opening.
 *
 * <p>Kept apart from the main entrypoint so a dedicated server never loads a class that mentions
 * {@code net.minecraft.client}.
 */
public class MerlinLibFabricClient implements ClientModInitializer {

    /** The vanilla controls screen column dedicated to MerlinLib. */
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "main"));

    /**
     * One binding only. 26.3 renamed the registration api ({@code fabric-key-mapping-api-v1}), but the
     * binding itself is an ordinary vanilla {@link KeyMapping}, so it shows up in the vanilla controls
     * screen and can be rebound to anything, including a {@code Ctrl} combination, by the player.
     */
    private static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.merlinlib.open_editor", MerlinUi.KEY_LEFT, CATEGORY);

    /** Opens the macro screen. */
    private static final KeyMapping OPEN_MACROS = new KeyMapping(
            "key.merlinlib.macros", MerlinUi.KEY_RIGHT, CATEGORY);

    @Override
    public void onInitializeClient() {

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

        // Drawn last so the numbers sit above the vanilla HUD, right next to the crosshair.
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(com.huziyang520.merlinlib.Constants.MOD_ID, "combat_overlay"),
                CombatFeedback.instance()::render);
        CombatFeedback.announce();

        // The crosshair number comes from the server; the local swing only colours the popup.
        DamageFeedbackSink.setClientSink(payload ->
                CombatFeedback.instance().onServerDamage(payload.damage(), payload.critical(), payload.targetId()));
        ClientPlayNetworking.registerGlobalReceiver(DamageFeedbackPayload.TYPE, (payload, context) ->
                DamageFeedbackSink.handle(payload));
        ServerSender.setSink(payload ->
                ClientPlayNetworking.send((CustomPacketPayload) payload));
        com.huziyang520.merlinlib.tools.HealthEditorItem.setOpener(target ->
                MerlinScreens.openHealthEditor(null, target));
        MacroStorage.load();

        // Sneak using an entity opens the health editor for it.
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            // No switch is consulted here: openHealthEditor answers "may this open, and is the item held"
            // itself. Checking healthEditorRequiresItem here as well made the sneak click work only while
            // the server did NOT require the item, which is the opposite of what the switch means.
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
