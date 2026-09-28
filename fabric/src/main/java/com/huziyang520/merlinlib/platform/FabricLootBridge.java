package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.services.ILootBridge;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fabric implementation of {@link ILootBridge}, on top of {@code LootTableEvents.MODIFY}.
 *
 * <p>Fabric hands out the table's builder, so a pool is simply added to it. Listeners are kept in a list
 * because MerlinLib's own rules are installed during initialisation and a dependent mod may add more later.
 */
public class FabricLootBridge implements ILootBridge {

    private final List<TableListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void bootstrap(Object loaderContext) {
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            for (TableListener listener : this.listeners) {
                try {
                    // The event's builder takes a pool builder directly, so the sink is the same shape here.
                    listener.onTable(key.identifier(), builder::withPool, registries);
                } catch (RuntimeException | LinkageError error) {
                    // One broken rule must not stop the table from loading, nor the other rules from running.
                    Constants.LOG.error("[MerlinLib] a loot table listener failed for {}", key.identifier(), error);
                }
            }
        });
    }

    @Override
    public void onTableLoad(TableListener listener) {
        this.listeners.add(listener);
    }
}
