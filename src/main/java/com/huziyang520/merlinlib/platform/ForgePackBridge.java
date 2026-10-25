// Forge implementation of IPackBridge.

package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IPackBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ForgePackBridge implements IPackBridge {
    public static final ForgePackBridge INSTANCE = new ForgePackBridge();
    private final Map<ResourceLocation, PreparableReloadListener> listeners = new LinkedHashMap<>();
    private boolean installed;

    private ForgePackBridge() {
    }

    @Override
    public void bootstrap(Object loaderContext) {
        if (installed) {
            return;
        }
        installed = true;
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListener);
    }

    @Override
    public void registerServerReloadListener(ResourceLocation id, PreparableReloadListener listener) {
        listeners.putIfAbsent(id, listener);
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        for (PreparableReloadListener listener : listeners.values()) {
            event.addListener(listener);
        }
    }
}
