// NeoForge implementation of IPackBridge.

package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.datapack.GeneratedPackSource;
import com.huziyang520.merlinlib.platform.services.IPackBridge;
import com.huziyang520.merlinlib.reload.ContentReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public class NeoForgePackBridge implements IPackBridge {
    @Override
    public void bootstrap(Object loaderContext) {
        if (loaderContext instanceof IEventBus modBus) {
            modBus.addListener(this::onAddPackFinders);
        }
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
    }

    private void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            event.addRepositorySource(GeneratedPackSource.INSTANCE);
        }
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(ContentReloadListener.instance());
    }

    @Override
    public void registerServerReloadListener(ResourceLocation id, PreparableReloadListener listener) {
    }
}
