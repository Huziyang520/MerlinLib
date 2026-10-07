package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IPackBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Forge implementation of {@link IPackBridge}.
 *
 * <p>Only the reload listener half is needed on 1.20.1. The runtime generated datapack of the 26.3
 * line has no job here: enchantments, tags and villager trades all live in ordinary jar resources or
 * in code, so nothing is mounted at runtime and there is no {@code KnownPack} to declare.
 *
 * <p>{@code AddReloadListenerEvent} fires on the game bus, and every listener added to it runs on
 * each {@code /reload} as well as on world load.
 */
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
        // Registering the same id twice must be a no-op: /reload may re-run the caller.
        listeners.putIfAbsent(id, listener);
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        for (PreparableReloadListener listener : listeners.values()) {
            event.addListener(listener);
        }
    }
}
