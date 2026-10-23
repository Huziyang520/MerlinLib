package com.huziyang520.merlinlib.platform.services;

import net.minecraft.core.HolderGetter;
import net.minecraft.resources.Identifier;
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
        void onTable(Identifier tableId, PoolSink sink, HolderGetter.Provider registries);
    }

    /** Where a listener puts its pools. */
    @FunctionalInterface
    interface PoolSink {

        /**
         * @param pool the pool to add to the table, as a builder: Fabric's table builder takes a pool builder,
         *             NeoForge's table takes a built pool, and a builder can produce either while a built pool
         *             can produce only itself
         */
        void add(LootPool.Builder pool);
    }
}
