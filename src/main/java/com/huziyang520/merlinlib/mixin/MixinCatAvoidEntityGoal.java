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
 */
@Mixin(targets = "net.minecraft.world.entity.animal.Cat$CatAvoidEntityGoal")
public class MixinCatAvoidEntityGoal {

    /**
     * Refuses to start the cat's avoid goal when a hook says so.
     *
     * @param info the injection callback
 */
    @Inject(method = "canUse()Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void merlinlib$vetoCatAvoid(CallbackInfoReturnable<Boolean> info) {
        merlinlib$veto(info);
    }

    /**
     * Stops the cat's avoid goal when it is already running.
     *
     * @param info the injection callback
 */
    @Inject(method = "canContinueToUse()Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void merlinlib$vetoCatContinuedAvoid(CallbackInfoReturnable<Boolean> info) {
        merlinlib$veto(info);
    }

    /**
     * The shared veto, identical to the one in {@link MixinAvoidEntityGoal}.
     *
     * @param info the injection callback
 */
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
