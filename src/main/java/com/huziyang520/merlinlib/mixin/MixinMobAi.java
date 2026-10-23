package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.ai.AiRouter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public class MixinMobAi {

    /** Ticks between two steering attempts for one mob. */
    private static final int STEER_INTERVAL = 20;

    /** Counts down to the next steering attempt for this mob. */
    @Unique
    private int merlinlib$steerCooldown;

    /**
     * Refuses a target some hook does not want this mob to take.
     *
     * @param target the entity the mob is about to target
     * @param info   the injection callback, cancelled to refuse
     */
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void merlinlib$vetoTarget(LivingEntity target, CallbackInfo info) {
        if (target == null || !AiRouter.hasHooks()) {
            return;
        }
        if (AiRouter.vetoesTargeting((Mob) (Object) this, target)) {
            info.cancel();
        }
    }

    /**
     * Keeps a forbidden target from standing, and steers the mob away where a hook asks for it.
     *
     * @param info the injection callback
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void merlinlib$maintainAi(CallbackInfo info) {
        if (!AiRouter.hasHooks()) {
            return;
        }
        Mob self = (Mob) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        LivingEntity current = self.getTarget();
        if (current != null && AiRouter.vetoesTargeting(self, current)) {
            self.setTarget(null);
        }
        if (this.merlinlib$steerCooldown > 0) {
            this.merlinlib$steerCooldown--;
            return;
        }
        this.merlinlib$steerCooldown = STEER_INTERVAL;
        if (AiRouter.hasFleeHooks()) {
            AiRouter.steerAway(self);
        }
    }
}
