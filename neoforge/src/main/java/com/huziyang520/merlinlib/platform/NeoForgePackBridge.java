package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.datapack.GeneratedPackSource;
import com.huziyang520.merlinlib.platform.services.IPackBridge;
import com.huziyang520.merlinlib.reload.ContentReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/**
 * NeoForge implementation of {@link IPackBridge}.
 *
 * <p>Two different buses are involved and mixing them up results in a listener that never fires:
 * {@link AddPackFindersEvent} is a mod bus event, while {@link AddServerReloadListenersEvent} lives on
 * the game bus.
 */
public class NeoForgePackBridge implements IPackBridge {

    @Override
    public void bootstrap(Object loaderContext) {
        if (loaderContext instanceof IEventBus modBus) {
            modBus.addListener(this::onAddPackFinders);
        }
        NeoForge.EVENT_BUS.addListener(this::onAddServerReloadListeners);
    }

    private void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            event.addRepositorySource(GeneratedPackSource.INSTANCE);
        }
    }

    private void onAddServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(ContentReloadListener.ID, ContentReloadListener.instance());
    }

    @Override
    public void registerServerReloadListener(Identifier id, PreparableReloadListener listener) {
        // Handled by the bootstrap listener above; the event is the only supported registration point.
    }
}
