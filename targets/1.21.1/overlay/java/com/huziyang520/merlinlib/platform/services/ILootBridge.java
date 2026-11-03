// Loader specific bridge for touching loot tables while they load.

package com.huziyang520.merlinlib.platform.services;

import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;

public interface ILootBridge extends ILoaderBridge {
    void onTableLoad(TableListener listener);

    @FunctionalInterface
    interface TableListener {
        void onTable(ResourceLocation tableId, PoolSink sink, HolderGetter.Provider registries);
    }

    @FunctionalInterface
    interface PoolSink {
        void add(LootPool.Builder pool);
    }
}
