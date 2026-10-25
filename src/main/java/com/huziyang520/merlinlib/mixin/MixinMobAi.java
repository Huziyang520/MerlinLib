// Asks the mods' AI hooks the two questions that live on the mob itself: may it target that entity, and should it keep ...

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
    private static final int STEER_INTERVAL = 20;

    @Unique
    private int merlinlib$steerCooldown;

    @Inject(method = "setTarget(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void merlinlib$vetoTarget(LivingEntity target, CallbackInfo info) {
        if (target == null || !AiRouter.hasHooks()) {
            return;
        }
        if (AiRouter.vetoesTargeting((Mob) (Object) this, target)) {
            info.cancel();
        }
    }

    @Inject(method = "tick()V", at = @At("HEAD"), require = 1)
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
