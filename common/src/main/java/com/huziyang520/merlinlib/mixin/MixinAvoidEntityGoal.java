package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.ai.AiRouter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a mod stop a mob from fleeing something - "cats do not run from you any more".
 *
 * <p>Both ends of the goal are covered: {@code canUse} decides whether it may start, and
 * {@code canContinueToUse} whether it may keep going. Stopping only the first would leave a cat that was
 * already running.
 */
@Mixin(AvoidEntityGoal.class)
public class MixinAvoidEntityGoal {

    /**
     * Refuses to start the goal when a hook says the mob should not avoid that entity.
     *
     * @param info the injection callback
     */
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void merlinlib$vetoAvoid(CallbackInfoReturnable<Boolean> info) {
        if (!AiRouter.hasHooks()) {
            return;
        }
        AvoidEntityGoalAccessor self = (AvoidEntityGoalAccessor) this;
        PathfinderMob mob = self.merlinlib$mob();
        LivingEntity avoid = self.merlinlib$toAvoid();
        if (mob != null && avoid != null && AiRouter.vetoesAvoiding(mob, avoid)) {
            info.setReturnValue(false);
        }
    }

    /**
     * Stops the goal that is already running, for the same reason.
     *
     * @param info the injection callback
     */
    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void merlinlib$vetoContinuedAvoid(CallbackInfoReturnable<Boolean> info) {
        if (!AiRouter.hasHooks()) {
            return;
        }
        AvoidEntityGoalAccessor self = (AvoidEntityGoalAccessor) this;
        PathfinderMob mob = self.merlinlib$mob();
        LivingEntity avoid = self.merlinlib$toAvoid();
        if (mob != null && avoid != null && AiRouter.vetoesAvoiding(mob, avoid)) {
            info.setReturnValue(false);
        }
    }
}
