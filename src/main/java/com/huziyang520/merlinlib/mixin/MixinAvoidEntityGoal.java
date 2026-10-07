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
 * {@code canContinueToUse} whether it may keep going. Stopping only the first would leave a cat that
 * was already running.
 *
 * <h2>1.20.1 evidence and what changed from 26.3</h2>
 *
 * <p>Nothing changed: this is a direct port, both injection points included. Verified with
 * {@code javap -p -s net.minecraft.world.entity.ai.goal.AvoidEntityGoal}:
 *
 * <pre>
 * public boolean canUse();
 *   descriptor: ()Z
 * public boolean canContinueToUse();
 *   descriptor: ()Z
 * </pre>
 *
 * <p>Both return {@code boolean}, so both injections keep the
 * {@code CallbackInfoReturnable<Boolean>} shape and cancel by setting the return value to
 * {@code false}, exactly as on 26.3. The fields the two handlers read are reached through
 * {@link AvoidEntityGoalAccessor}, which is a separate interface mixin applied to this same class;
 * see that file for the field descriptors.
 */
@Mixin(AvoidEntityGoal.class)
public class MixinAvoidEntityGoal {

    /**
     * Refuses to start the goal when a hook says the mob should not avoid that entity.
     *
     * @param info the injection callback
     */
    @Inject(method = "canUse()Z", at = @At("HEAD"), cancellable = true, require = 1)
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
    @Inject(method = "canContinueToUse()Z", at = @At("HEAD"), cancellable = true, require = 1)
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
