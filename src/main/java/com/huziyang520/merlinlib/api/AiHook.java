// A mod's say in what a mob is allowed to do about a living entity.

package com.huziyang520.merlinlib.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public interface AiHook {
    default boolean allowsTargeting(Mob mob, LivingEntity target) {
        return true;
    }

    default boolean wantsToFlee(Mob mob, LivingEntity entity) {
        return false;
    }

    default boolean allowsAvoiding(Mob mob, LivingEntity entity) {
        return true;
    }
}
