// Combat helpers reached through MerlinApi#combat().

package com.huziyang520.merlinlib.api;

import net.minecraft.world.entity.LivingEntity;

public final class CombatApi {
    public static final CombatApi INSTANCE = new CombatApi();

    private CombatApi() {
    }

    public void knockback(LivingEntity target, double strength, double dx, double dz) {
        if (target == null || target.level().isClientSide()) {
            return;
        }
        target.knockback(strength, dx, dz);
    }
}
