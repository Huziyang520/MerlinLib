// The enchantment event api, reached through MerlinApi#combat().

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.impl.CombatTracker;
import net.minecraft.world.entity.LivingEntity;

public final class CombatApi {
    public static final CombatApi INSTANCE = new CombatApi();

    private CombatApi() {
    }

    public void recordDamage(DamageSnapshot snapshot) {
        CombatTracker.INSTANCE.recordDamage(snapshot);
    }

    public void recordKill(KillInfo kill) {
        CombatTracker.INSTANCE.recordKill(kill);
    }

    public DamageSnapshot lastDamage(LivingEntity victim) {
        return CombatTracker.INSTANCE.lastDamage(victim);
    }

    public KillInfo lastKill(LivingEntity victim) {
        return CombatTracker.INSTANCE.lastKill(victim);
    }

    public void clear(LivingEntity victim) {
        CombatTracker.INSTANCE.clear(victim);
    }

    public void clear() {
        CombatTracker.INSTANCE.clear();
    }
}
