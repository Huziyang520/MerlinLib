package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.loot.LootInjector;

/**
 * The loot injection api, reached through {@link MerlinApi#loot()}.
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
