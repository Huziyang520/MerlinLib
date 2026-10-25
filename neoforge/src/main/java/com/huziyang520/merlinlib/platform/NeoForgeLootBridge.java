// NeoForge implementation of ILootBridge, on top of LootTableLoadEvent.

package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.services.ILootBridge;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NeoForgeLootBridge implements ILootBridge {
    private final List<TableListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void bootstrap(Object loaderContext) {
        NeoForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> {
            for (TableListener listener : this.listeners) {
                try {
                    listener.onTable(event.getName(), pool -> event.getTable().addPool(pool.build()),
                            event.getRegistries());
                } catch (RuntimeException | LinkageError error) {
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
