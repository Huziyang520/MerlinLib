// The loot injection api, reached through MerlinApi#loot().

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.loot.LootInjector;

public final class LootApi {
    public static final LootApi INSTANCE = new LootApi();

    private LootApi() {
    }

    public LootApi register(LootInjectionBuilder builder) {
        return register(builder.build());
    }

    public LootApi register(LootInjection injection) {
        LootInjector.add(injection);
        return this;
    }

    public int size() {
        return LootInjector.size();
    }
}
