// Fabric implementation of ILootBridge, on top of LootTableEvents.MODIFY.

package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.services.ILootBridge;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FabricLootBridge implements ILootBridge {
    private final List<TableListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void bootstrap(Object loaderContext) {
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            for (TableListener listener : this.listeners) {
                try {
                    listener.onTable(key.identifier(), builder::withPool, registries);
                } catch (RuntimeException | LinkageError error) {
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
