package com.huziyang520.merlinlib.platform.services;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;

/**
 * Loader specific bridge for touching loot tables while they load.
 *
 * <h2>Why a bridge rather than one shared hook</h2>
 *
 * <p>On 26.3 the two loaders handed out the table in different shapes (Fabric a builder, NeoForge
 * an already built table). On 1.20.1 there is one shape: Forge's {@code LootTableLoadEvent} hands
 * out a built table that Forge patched with {@code addPool(LootPool)} - probed against the real
 * 1.20.1 Forge bytecode, the method is there. The interface is kept so the loot injector does not
 * talk to Forge directly.
 *
 * <h2>When the listener runs</h2>
 *
 * <p>Once per table, while the table is being loaded: on world load, and again after
 * {@code /reload}. A listener therefore must not assume it runs exactly once; it must be a pure
 * function of the registered rules.
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
