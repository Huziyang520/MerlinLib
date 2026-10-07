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
 *
 * <p>{@code LootTableLoadEvent} hands out the table that Forge patched with
 * {@code addPool(LootPool)} - verified against the real 1.20.1 Forge bytecode, where
 * {@code LootTable.addPool(LootPool)} exists next to {@code getPool(String)}. So one shape covers
 * both of 26.3's loaders and the sink can simply build the pool and add it.
 *
 * <p>The event itself carries no registry access, so the registry access is captured when the
 * server starts. Loot tables are loaded during {@code ServerAboutToStartEvent}, so it is already
 * populated by the time the first table arrives.
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
