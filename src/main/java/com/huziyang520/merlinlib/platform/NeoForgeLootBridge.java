package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.services.ILootBridge;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NeoForge implementation of {@link ILootBridge}, on top of {@code LootTableLoadEvent}.
 */
public class NeoForgeLootBridge implements ILootBridge {

    private final List<TableListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void bootstrap(Object loaderContext) {
        NeoForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> {
            for (TableListener listener : this.listeners) {
                try {
                    // The table takes a built pool, and addPool is the method NeoForge adds for exactly this.
                    // 1.21.1 hands out HolderLookup.Provider here; the bridge speaks the narrower
                    // HolderGetter.Provider, and asGetterLookup() is the conversion vanilla provides.
                    listener.onTable(event.getName(), pool -> event.getTable().addPool(pool.build()),
                            event.getRegistries().asGetterLookup());
                } catch (RuntimeException | LinkageError error) {
                    // One broken rule must not stop the table from loading, nor the other rules from running.
                    Constants.LOG.error("[MerlinLib] a loot table listener failed for {}", event.getName(), error);
                }
            }
        });
    }

    @Override
    public void onTableLoad(TableListener listener) {
        this.listeners.add(listener);
    }
}
