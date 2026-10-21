package com.huziyang520.merlinlib.platform.services;

import net.minecraft.core.HolderGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;

/**
 * Loader specific bridge for touching loot tables while they load.
 *
 * <h2>Why a bridge rather than one shared hook</h2>
 *
 * <p>The two loaders hand out the table in different shapes: Fabric's {@code LootTableEvents.MODIFY} passes a
 * builder that pools can be added to, while NeoForge's {@code LootTableLoadEvent} passes an already built
 * table, which NeoForge patched with an {@code addPool} method. The shape a caller cares about is the same
 * either way - "here is a table id, put these pools in it" - so that is what this interface offers, and the
 * difference stays behind it.
 *
 * <h2>When the listener runs</h2>
 *
 * <p>Once per table, while the table is being loaded: on world load, and again after {@code /reload}. A
 * listener therefore must not assume it runs exactly once; it must be a pure function of the registered rules.
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
