package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.Constants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Drives the client side installation at the moment Forge provides for it.
 *
 * <h2>Why this is separate from {@link MerlinLibForgeClient}</h2>
 *
 * <p>{@link MerlinLibForgeClient} holds the client's behaviour and its event handlers; this class holds the
 * one line that starts it. They are separate because they subscribe to different buses:
 * {@code FMLClientSetupEvent} is a <b>mod</b> bus event, while the tick, render and interaction handlers in
 * {@link MerlinLibForgeClient} are <b>game</b> bus events. Putting both in one class under one
 * {@code @Mod.EventBusSubscriber} is the exact mistake that silently loses half the handlers - the bus is
 * named explicitly on both classes so it cannot recur.
 *
 * <p>{@code value = Dist.CLIENT} keeps this class, and therefore the client types it reaches through
 * {@link MerlinLibForgeClient}, off a dedicated server entirely.
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MerlinClientSetup {

    private MerlinClientSetup() {
    }

    /**
     * Installs the client side toolkit.
     *
     * <p>Fired once, after the client's own subsystems exist and before any world is loaded, which is the
     * first moment the things {@link MerlinLibForgeClient#install()} touches can safely be used.
     *
     * @param event the client setup
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // enqueueWork rather than doing it inline: this event is fired on the mod loading thread, and the
        // install touches the key binding registry and the macro file. Queuing it onto the client's own
        // thread is what Forge asks for here, and it removes any question about which thread the file read
        // happens on.
        event.enqueueWork(MerlinLibForgeClient::install);
    }
}