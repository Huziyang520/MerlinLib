package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.loot.LootInjector;

/**
 * The loot injection api, reached through {@link MerlinApi#loot()}.
 *
 * <p>A dependent mod registers a rule per table it wants its content to appear in; the library then adds the
 * matching entries to those tables on whichever loader it is running on - Fabric's
 * {@code LootTableEvents.MODIFY}, NeoForge's {@code LootTableLoadEvent} - so the same rule produces the same
 * loot on both.
 *
 * <pre>{@code
 * MerlinApi.loot().register(LootInjectionBuilder.create()
 *         .toTables(LootTables.SHIPWRECK_TREASURE)
 *         .asBook()
 *         .withEnchantments("practical_enchantments:venom")
 *         .chance(0.16F)
 *         .weight(3)
 *         .quality(1));
 * }</pre>
 *
 * <p>Registration is safe from a mod constructor. Rules are read when a table loads, so a rule registered
 * before the first world loads applies to it.
 */
public final class LootApi {

    /** Single instance, handed out by {@link MerlinApi#loot()}. */
    public static final LootApi INSTANCE = new LootApi();

    private LootApi() {
    }

    /**
     * Registers a rule.
     *
     * @param builder the rule, built immediately
     * @return this api, for chaining
     */
    public LootApi register(LootInjectionBuilder builder) {
        return register(builder.build());
    }

    /**
     * Registers a rule.
     *
     * @param injection the rule
     * @return this api, for chaining
     */
    public LootApi register(LootInjection injection) {
        LootInjector.add(injection);
        return this;
    }

    /** @return how many rules are registered, for diagnostics */
    public int size() {
        return LootInjector.size();
    }
}
