package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.ai.AiRouter;
import net.minecraft.server.level.ServerLevel;
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
 *
 * <p>This is the anger half of the hook set. {@code MixinMobAi} covers every ordinary mob, whose
 * hostility ends up in {@code Mob#target}; the piglin family is not one of those mobs and needs the
 * two funnels below instead.</p>
 *
 * <h2>Why {@code Mob#setTarget} is not enough</h2>
 *
 * <p>Piglins and piglin brutes are Brain mobs: their hostility lives in the Brain's memories
 * ({@code ANGRY_AT} / {@code ATTACK_TARGET}), not in the {@code target} field. Anger is recorded by
 * {@code PiglinAi#setAngerTarget} and its brute twin {@code PiglinBruteAi#setAngerTarget}, and
 * <b>neither class ever calls {@code Mob#setTarget}</b>, so the hook that refuses targeting never
 * runs for these two mobs. A piglin-head disguise used to do nothing against them for exactly that
 * reason while working against zombies and skeletons, whose goals do go through {@code setTarget}.</p>
 *
 * <p>{@code setAngerTarget} is the single funnel every anger path ends in - {@code maybeRetaliate}
 * (hurt by someone), the shared anger broadcast, and {@code angerNearbyPiglins} (mining gold,
 * opening chests, breaking a piglin's gold) all set their target through it - so refusing there
 * covers all of them at once.</p>
 *
 * <h2>Why refusing anger is not enough either</h2>
 *
 * <p>Not becoming angry is not the same as not attacking: {@code findNearestValidAttackTarget} ends
 * in a branch which answers with {@code NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD} - a player who
 * wears no gold armour is a legitimate target on sight, angry or not - and the brute twin ends in
 * {@code NEAREST_VISIBLE_NEMESIS} the same way. A disguise that only closed the anger path left the
 * mob attacking its wearer for that other reason.</p>
 *
 * <p>{@code findNearestValidAttackTarget} is the single funnel the attack target is acquired
 * through: the fight activity starts attacking from it, and {@code isNearestValidAttackTarget} -
 * which {@code StopAttackingIfTargetInvalid} and {@code EraseMemoryIf} use to drop a target that is
 * no longer valid - calls the same method. Refusing there therefore both withholds a new target and
 * releases one already held, which is what makes putting the head on take effect immediately instead
 * of after the current fight ends.</p>
 *
 * <h2>Why the result is inspected rather than the call refused outright</h2>
 *
 * <p>The same method also answers for targets no hook has an opinion about - the nemesis memories,
 * and for piglins the hoglin hunt. A blanket refusal at the head of the method would take those away
 * from every mob in the world, so the injection runs at the return and only drops a result the hooks
 * actually refuse. {@code @At("RETURN")} matches <em>every</em> return of the method (a single one
 * would need {@code ordinal}); with no hook registered the path costs one {@code isEmpty} call.</p>
 *
 * <p>Both halves keep the semantics of the targeting path: a mob the player has already hurt returns
 * {@code true} from {@code allowsTargeting} and is allowed to come back for more, which is the
 * "active hostility is exempt" half of the disguise's contract.</p>
 *
 * <p>{@code require = 1} on all four injections: if a method is renamed, the game must fail loudly
 * rather than quietly hand the disguise back to the mob.</p>
 *
 * <h2>Signature differences from the 1.20.1 line</h2>
 *
 * <p>This line's {@code PiglinAi#setAngerTarget} and both {@code findNearestValidAttackTarget}
 * methods take the {@code ServerLevel} they run in as their first parameter; the descriptors below
 * carry it. Both were read off this line's own merged jar with {@code javap -p -s} rather than
 * carried over from the older line.</p>
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
         * @param level  the level the piglin is in
         * @param piglin the piglin about to become angry
         * @param target who it is about to be angry at
         * @param info   the injection callback, cancelled to refuse
         */
        @Inject(method = "setAngerTarget(Lnet/minecraft/server/level/ServerLevel;"
                        + "Lnet/minecraft/world/entity/monster/piglin/AbstractPiglin;"
                        + "Lnet/minecraft/world/entity/LivingEntity;)V",
                at = @At("HEAD"), cancellable = true, require = 1)
        private static void merlinlib$vetoAnger(ServerLevel level, AbstractPiglin piglin,
                                                LivingEntity target, CallbackInfo info) {
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
         * @param level  the level the piglin is in
         * @param piglin the piglin choosing
         * @param info   the injection callback holding the choice
         */
        @Inject(method = "findNearestValidAttackTarget(Lnet/minecraft/server/level/ServerLevel;"
                        + "Lnet/minecraft/world/entity/monster/piglin/Piglin;)Ljava/util/Optional;",
                at = @At("RETURN"), cancellable = true, require = 1)
        private static void merlinlib$vetoChosenTarget(ServerLevel level, Piglin piglin,
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
         * @param level the level the brute is in
         * @param brute the brute choosing
         * @param info  the injection callback holding the choice
         */
        @Inject(method = "findNearestValidAttackTarget(Lnet/minecraft/server/level/ServerLevel;"
                        + "Lnet/minecraft/world/entity/monster/piglin/AbstractPiglin;)"
                        + "Ljava/util/Optional;",
                at = @At("RETURN"), cancellable = true, require = 1)
        private static void merlinlib$vetoChosenTarget(ServerLevel level, AbstractPiglin brute,
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
