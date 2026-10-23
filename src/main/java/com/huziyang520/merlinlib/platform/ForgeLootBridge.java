package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.services.ILootBridge;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge implementation of {@link ILootBridge}.
 */
public final class ForgeLootBridge implements ILootBridge {

    public static final ForgeLootBridge INSTANCE = new ForgeLootBridge();

    private final List<TableListener> listeners = new ArrayList<>();

    private RegistryAccess registryAccess;
    private boolean installed;

    private ForgeLootBridge() {
    }

    @Override
    public void bootstrap(Object loaderContext) {
        if (installed) {
            return;
        }
        installed = true;
        MinecraftForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(this::onLootTableLoad);
    }

    @Override
    public void onTableLoad(TableListener listener) {
        listeners.add(listener);
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        registryAccess = event.getServer().registryAccess();
    }

    private void onLootTableLoad(LootTableLoadEvent event) {
        if (listeners.isEmpty()) {
            return;
        }
        HolderLookup.Provider registries = registryAccess;
        PoolSink sink = pool -> event.getTable().addPool(pool.build());
        for (TableListener listener : listeners) {
            try {
                listener.onTable(event.getName(), sink, registries);
            } catch (RuntimeException failure) {
                // One bad rule must not take the rest of the table down with it.
                Constants.LOG.error("[MerlinLib] loot rule failed for table {}", event.getName(), failure);
            }
        }
    }
}
