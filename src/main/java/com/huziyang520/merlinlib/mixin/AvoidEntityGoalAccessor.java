package com.huziyang520.merlinlib.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the mob and the entity an avoid goal is about.
 */
@Mixin(AvoidEntityGoal.class)
public interface AvoidEntityGoalAccessor {

    /** @return the mob that owns this goal */
    @Accessor("mob")
    PathfinderMob merlinlib$mob();

    /** @return the entity the mob would avoid, or {@code null} when it has not picked one yet */
    @Accessor("toAvoid")
    LivingEntity merlinlib$toAvoid();
}
