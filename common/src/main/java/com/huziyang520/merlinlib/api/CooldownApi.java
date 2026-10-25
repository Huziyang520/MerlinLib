// Per entity cooldowns and tick budgets, reached through MerlinApi#cooldowns().

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.impl.CooldownStore;
import net.minecraft.world.entity.Entity;

public final class CooldownApi {
    public static final CooldownApi INSTANCE = new CooldownApi();

    private CooldownApi() {
    }

    public boolean ready(Entity entity, String name, int ticks) {
        return CooldownStore.INSTANCE.ready(entity, name, ticks);
    }

    public int remaining(Entity entity, String name) {
        return CooldownStore.INSTANCE.remaining(entity, name);
    }

    public void trigger(Entity entity, String name, int ticks) {
        CooldownStore.INSTANCE.trigger(entity, name, ticks);
    }

    public void reset(Entity entity, String name) {
        CooldownStore.INSTANCE.reset(entity, name);
    }
}
