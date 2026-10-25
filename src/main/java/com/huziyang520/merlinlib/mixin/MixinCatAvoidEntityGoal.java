// The same veto as MixinAvoidEntityGoal, for the cat's own avoid goal.

package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.ai.AiRouter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.animal.Cat$CatAvoidEntityGoal")
public class MixinCatAvoidEntityGoal {
    @Inject(method = "canUse()Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void merlinlib$vetoCatAvoid(CallbackInfoReturnable<Boolean> info) {
        merlinlib$veto(info);
    }

    @Inject(method = "canContinueToUse()Z", at = @At("HEAD"), cancellable = true, require = 1)
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
