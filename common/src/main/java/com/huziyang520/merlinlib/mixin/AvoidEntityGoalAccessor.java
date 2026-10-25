// Reads the mob and the entity an avoid goal is about.

package com.huziyang520.merlinlib.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AvoidEntityGoal.class)
public interface AvoidEntityGoalAccessor {
    @Accessor("mob")
    PathfinderMob merlinlib$mob();

    @Accessor("toAvoid")
    LivingEntity merlinlib$toAvoid();
}
