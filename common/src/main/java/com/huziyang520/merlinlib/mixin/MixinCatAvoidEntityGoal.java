package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.ai.AiRouter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The same veto as {@link MixinAvoidEntityGoal}, for the cat's own avoid goal.
 *
 * <p>The cat does not use {@code AvoidEntityGoal} directly: it has a nested goal of its own, and a nested goal
 * that overrides the two decision methods never reaches the parent's version of them. One file per target is
 * the price of mixin not being able to inject into "a method of that shape somewhere in this hierarchy".
 *
 * <p>Targeted by name rather than by class literal: the nested type is not reachable from here.
 */
@Mixin(targets = "net.minecraft.world.entity.animal.feline.Cat$CatAvoidEntityGoal")
public class MixinCatAvoidEntityGoal {

    /**
     * Refuses to start the cat's avoid goal when a hook says so.
     *
     * @param info the injection callback
     */
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void merlinlib$vetoCatAvoid(CallbackInfoReturnable<Boolean> info) {
        merlinlib$veto(info);
    }

    /**
     * Stops the cat's avoid goal when it is already running.
     *
     * @param info the injection callback
     */
    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void merlinlib$vetoCatContinuedAvoid(CallbackInfoReturnable<Boolean> info) {
        merlinlib$veto(info);
    }

    private void merlinlib$veto(CallbackInfoReturnable<Boolean> info) {
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
