package com.huziyang520.merlinlib.platform.services;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;

/**
 * Loader specific bridge for touching loot tables while they load.
 */
public interface ILootBridge extends ILoaderBridge {

    /**
     * Adds a listener that may add pools to every loading table.
     *
     * @param listener the listener, never {@code null}
 */
    void onTableLoad(TableListener listener);

    /** Called once per loading table. */
    @FunctionalInterface
    interface TableListener {

        /**
         * @param tableId    the full id of the table being loaded
         * @param sink       where to put pools that should end up in it
         * @param registries the registries to resolve, for example, enchantment ids against
 */
        void onTable(ResourceLocation tableId, PoolSink sink, HolderLookup.Provider registries);
    }

    /** Where a listener puts its pools. */
    @FunctionalInterface
    interface PoolSink {

        /**
         * @param pool the pool to add to the table
 */
        void add(LootPool.Builder pool);
    }
}
