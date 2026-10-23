package com.huziyang520.merlinlib.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * A mod's say in what a mob is allowed to do about a living entity.
 */
public interface AiHook {

    /**
     * Whether the mob may take this living entity as its attack target.
     *
     * @param mob    the mob that is choosing a target
     * @param target the entity it is considering
     * @return false to refuse the target, which is the default of "no opinion" being true
 */
    default boolean allowsTargeting(Mob mob, LivingEntity target) {
        return true;
    }

    /**
     * Whether the mob should actively move away from this living entity.
     *
     * @param mob    the mob
     * @param entity the entity to keep away from
     * @return true to make the mob flee, false to leave the decision to the mob
 */
    default boolean wantsToFlee(Mob mob, LivingEntity entity) {
        return false;
    }

    /**
     * Whether the mob's own avoidance behaviour may apply to this living entity.
     *
     * @param mob    the mob
     * @param entity the entity it would avoid
     * @return false to stop the mob from fleeing, true to leave it alone
 */
    default boolean allowsAvoiding(Mob mob, LivingEntity entity) {
        return true;
    }
}
