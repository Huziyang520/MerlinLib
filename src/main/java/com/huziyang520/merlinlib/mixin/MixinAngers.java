// Asks the AI hooks about anger, and about target choice, in the piglin family.

package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.ai.AiRouter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.piglin.PiglinBruteAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

public final class MixinAngers {
    private MixinAngers() {
    }

    @Mixin(PiglinAi.class)
    public static class PiglinAnger {
        @Inject(method = "setAngerTarget(Lnet/minecraft/world/entity/monster/piglin/AbstractPiglin;"
                        + "Lnet/minecraft/world/entity/LivingEntity;)V",
                at = @At("HEAD"), cancellable = true, require = 1)
        private static void merlinlib$vetoAnger(AbstractPiglin piglin, LivingEntity target,
                                                CallbackInfo info) {
            if (!AiRouter.hasHooks()) {
                return;
            }
            if (AiRouter.vetoesTargeting(piglin, target)) {
                info.cancel();
            }
        }

        @Inject(method = "findNearestValidAttackTarget(Lnet/minecraft/world/entity/monster/piglin/Piglin;)"
                        + "Ljava/util/Optional;",
                at = @At("RETURN"), cancellable = true, require = 1)
        private static void merlinlib$vetoChosenTarget(Piglin piglin,
                                                       CallbackInfoReturnable<Optional<LivingEntity>> info) {
            if (!AiRouter.hasHooks()) {
                return;
            }
            Optional<LivingEntity> target = info.getReturnValue();
            if (target == null || target.isEmpty()) {
                return;
            }
            if (AiRouter.vetoesTargeting(piglin, target.get())) {
                info.setReturnValue(Optional.empty());
            }
        }
    }

    @Mixin(PiglinBruteAi.class)
    public static class BruteAnger {
        @Inject(method = "setAngerTarget(Lnet/minecraft/world/entity/monster/piglin/PiglinBrute;"
                        + "Lnet/minecraft/world/entity/LivingEntity;)V",
                at = @At("HEAD"), cancellable = true, require = 1)
        private static void merlinlib$vetoAnger(PiglinBrute brute, LivingEntity target,
                                                CallbackInfo info) {
            if (!AiRouter.hasHooks()) {
                return;
            }
            if (AiRouter.vetoesTargeting(brute, target)) {
                info.cancel();
            }
        }

        @Inject(method = "findNearestValidAttackTarget(Lnet/minecraft/world/entity/monster/piglin/"
                        + "AbstractPiglin;)Ljava/util/Optional;",
                at = @At("RETURN"), cancellable = true, require = 1)
        private static void merlinlib$vetoChosenTarget(AbstractPiglin brute,
                                                       CallbackInfoReturnable<Optional<LivingEntity>> info) {
            if (!AiRouter.hasHooks()) {
                return;
            }
            Optional<LivingEntity> target = info.getReturnValue();
            if (target == null || target.isEmpty()) {
                return;
            }
            if (AiRouter.vetoesTargeting(brute, target.get())) {
                info.setReturnValue(Optional.empty());
            }
        }
    }
}
