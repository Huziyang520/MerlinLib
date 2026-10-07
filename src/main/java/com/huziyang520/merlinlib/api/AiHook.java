package com.huziyang520.merlinlib.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * A mod's say in what a mob is allowed to do about a living entity.
 *
 * <h2>What it is for</h2>
 *
 * <p>Content like "wear this skull and that mob will not attack you", "creepers keep their distance" or "cats
 * stop running from you" all come down to the same three questions, and the answers are spread across the
 * targeting goal, the navigation and the avoid goals. Answering them once, here, means a mod does not have to
 * find those places again - and a second mod can add its own answers without touching the first one's.
 *
 * <h2>The three questions</h2>
 *
 * <ul>
 *   <li>{@link #allowsTargeting} - the mob is about to take the entity as its attack target. Say no and it
 *       keeps looking. Note that vanilla sets a target for reasons other than aggression as well (being hurt,
 *       for one), so answer this by looking at the mob, not by assuming why it is asking.</li>
 *   <li>{@link #wantsToFlee} - the mob should actively keep away from the entity. The library does the moving:
 *       it steers the mob away and keeps a distance, roughly once a second.</li>
 *   <li>{@link #allowsAvoiding} - the mob's own "run away from this" behaviour. Say no and a mob that normally
 *       flees stays where it is, which is what "cats do not run from you any more" needs.</li>
 * </ul>
 *
 * <p>Every method has a default that changes nothing, so an implementation only writes the answers it has.
 * Hooks are asked in registration order by whichever mod registered one - the first hook that refuses is
 * enough - and they are never asked unless a mob actually does one of these things, so a world without any
 * hook pays nothing at all.
 *
 * <p>Hooks run on the server, inside the mob's own tick or its goal selection. They must be quick and must not
 * change the world in place: a hook decides, the mob acts.
 *
 * <p>1.20.1 carries this interface over unchanged. The three injection points it is asked from
 * ({@code Mob#setTarget}, {@code AvoidEntityGoal} and {@code Cat$CatAvoidEntityGoal}) exist on this version
 * with the same members, which is what the probe in the migration plan confirmed before any of it was written.
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
