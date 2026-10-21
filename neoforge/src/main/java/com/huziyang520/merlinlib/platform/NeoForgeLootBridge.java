package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.services.ILootBridge;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NeoForge implementation of {@link ILootBridge}, on top of {@code LootTableLoadEvent}.
 *
 * <p>The event hands out the built table rather than a builder, so the pool is added through the
 * {@code LootTable#addPool} method NeoForge adds to it. That method exists precisely for this: the table's own
 * pool list is private and immutable in vanilla, so a mod that wanted to add one would otherwise have to
 * rebuild a table it cannot fully read.
 */
public class NeoForgeLootBridge implements ILootBridge {

    private final List<TableListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void bootstrap(Object loaderContext) {
        NeoForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> {
            for (TableListener listener : this.listeners) {
                try {
                    // The table takes a built pool, and addPool is the method NeoForge adds for exactly this.
                    listener.onTable(event.getName(), pool -> event.getTable().addPool(pool.build()),
                            event.getRegistries());
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
