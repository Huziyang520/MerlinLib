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

/**
 * Asks the AI hooks about anger, and about target choice, in the piglin family.
 */
public final class MixinAngers {

    private MixinAngers() {
    }

    /** Piglin anger and target choice, both resolved by {@code PiglinAi}. */
    @Mixin(PiglinAi.class)
    public static class PiglinAnger {

        /**
         * Refuses to record anger a hook does not want.
         *
         * @param piglin the piglin about to become angry
         * @param target who it is about to be angry at
         * @param info   the injection callback, cancelled to refuse
 */
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

        /**
         * Drops a chosen attack target a hook refuses.
         *
         * @param piglin the piglin choosing
         * @param info   the injection callback holding the choice
 */
        @Inject(method = "findNearestValidAttackTarget(Lnet/minecraft/world/entity/monster/piglin/Piglin;)"
                        + "Ljava/util/Optional;",
                at = @At("RETURN"), cancellable = true, require = 1)
        private static void merlinlib$vetoChosenTarget(Piglin piglin,
                                                       CallbackInfoReturnable<Optional<LivingEntity>> info) {
            // Written out rather than shared through a helper on the holder class: the body of this
            // method is copied into PiglinAi itself, which lives in another package, so a private or
            // package-private helper would be an access violation once the game is running.
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

    /** Piglin brute anger and target choice, resolved by its own {@code PiglinBruteAi}. */
    @Mixin(PiglinBruteAi.class)
    public static class BruteAnger {

        /**
         * Refuses to record anger a hook does not want.
         *
         * @param brute  the brute about to become angry
         * @param target who it is about to be angry at
         * @param info   the injection callback, cancelled to refuse
 */
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

        /**
         * Drops a chosen attack target a hook refuses.
         *
         * @param brute the brute choosing
         * @param info  the injection callback holding the choice
 */
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
