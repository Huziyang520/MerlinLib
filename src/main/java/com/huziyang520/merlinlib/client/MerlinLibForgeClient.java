// Client only entrypoint for MerlinLib on 1.20.1.

package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.MezzConfigBridge;
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

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MerlinLibForgeClient {
    private static final String CATEGORY = "key.categories.misc";

    private static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.merlinlib.open_editor", InputConstants.Type.KEYSYM, MerlinUi.KEY_LEFT, CATEGORY);

    private static final KeyMapping OPEN_MACROS = new KeyMapping(
            "key.merlinlib.macros", InputConstants.Type.KEYSYM, MerlinUi.KEY_RIGHT, CATEGORY);

    private MerlinLibForgeClient() {
    }

    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EDITOR);
        event.register(OPEN_MACROS);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        while (OPEN_EDITOR.consumeClick()) {
            MerlinScreens.openItemEditorFromHotkey();
        }
        while (OPEN_MACROS.consumeClick()) {
            MerlinScreens.openMacroScreen(null);
        }
        CombatFeedback.instance().tick(minecraft);
        MacroStorage.tick(minecraft);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        CombatFeedback.instance().render(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() && event.getEntity().isShiftKeyDown()
                && event.getTarget() instanceof LivingEntity living) {
            MerlinScreens.openHealthEditor(null, living);
        }
    }

    public static void install() {
        MezzConfigBridge.install();
        DamageFeedbackSink.setClientSink(payload ->
                CombatFeedback.instance().onServerDamage(payload.damage(), payload.critical(), payload.targetId()));

        ToolOpenSink.setClientSink(MerlinScreens::openFromCommand);
        MacroStorage.load();
        HealthEditorItem.setOpener(target -> MerlinScreens.openHealthEditor(null, target));
        CombatFeedback.announce();
        Constants.LOG.info("[MerlinLib] client side toolkit installed ({} key binding(s))", 2);
    }

    public static int editorKeyCode() {
        return OPEN_EDITOR.getKey().getValue();
    }

    public static int macroKeyCode() {
        return OPEN_MACROS.getKey().getValue();
    }
}
