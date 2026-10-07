package com.huziyang520.merlinlib.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the mob and the entity an avoid goal is about.
 *
 * <p>The goal keeps both as fields and offers no getters, and a mixin cannot reach another class's
 * fields by casting alone. The accessor interface is added to {@link AvoidEntityGoal} itself, so
 * subclasses - the cat's own avoid goal among them - carry it as well.
 *
 * <h2>1.20.1 evidence and what changed from 26.3</h2>
 *
 * <p>Both fields kept their names and types, but their access modifiers differ from the 26.3 line
 * in a way that matters for the Javadoc above rather than for the code:
 * {@code javap -p -s net.minecraft.world.entity.ai.goal.AvoidEntityGoal} shows
 *
 * <pre>
 * public class net.minecraft.world.entity.ai.goal.AvoidEntityGoal&lt;T extends LivingEntity&gt; extends Goal {
 *   protected final net.minecraft.world.entity.PathfinderMob mob;
 *     descriptor: Lnet/minecraft/world/entity/PathfinderMob;
 *   protected T toAvoid;
 *     descriptor: Lnet/minecraft/world/entity/LivingEntity;
 *   public boolean canUse();
 *     descriptor: ()Z
 *   public boolean canContinueToUse();
 *     descriptor: ()Z
 * }
 * </pre>
 *
 * <p>{@code mob} is {@code final}, which is why the 26.3 source already declares it read-only and
 * only {@code toAvoid} is a plain field. Note that {@code toAvoid} carries the erased descriptor
 * {@code Lnet/minecraft/world/entity/LivingEntity;} because {@code T} is bounded by
 * {@code LivingEntity}; the accessor return type below is therefore {@code LivingEntity} and needs
 * no cast at the call sites. Both fields are non-{@code private} here, but a mixin still cannot
 * reach them from outside the class without an accessor, so this interface remains necessary.
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
