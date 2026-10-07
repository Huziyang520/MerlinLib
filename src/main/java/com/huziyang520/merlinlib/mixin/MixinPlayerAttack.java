package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.event.BuiltInEvents;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link BuiltInEvents#POST_ATTACK} after an attack landed.
 *
 * <h2>Why the tail of {@code Player.attack}</h2>
 *
 * <p>The event means "the attack has happened", so it has to run after vanilla finished the hit:
 * damage applied, durability spent, sweeping resolved. The tail of the one method that performs the
 * attack is that moment, which is also why no loader event is used here - a loader event describes
 * the attack differently on each loader, and neither describes it after the fact.
 *
 * <h2>What is filled in and what is not</h2>
 *
 * <p>The attacker, the target and the charge are known. Whether the hit was critical and whether it
 * swept are decided inside private helpers of this same method, and the event is not the place to
 * guess them: they are reported as {@code false}, and a dependent mod that needs them should ask for
 * the trigger to be widened rather than trusting a value.
 *
 * <h2>1.20.1 evidence and what changed from 26.3</h2>
 *
 * <p>Nothing changed: this is a direct port, including the injection point. Verified with
 * {@code javap -p -s net.minecraft.world.entity.player.Player}:
 *
 * <pre>
 * public void attack(net.minecraft.world.entity.Entity);
 *   descriptor: (Lnet/minecraft/world/entity/Entity;)V
 * </pre>
 *
 * <p>The parameters and the return type are identical to the 26.3 line, so the callback keeps the
 * {@code void} shape and needs no {@code CallbackInfoReturnable}. {@code getAttackStrengthScale}
 * also survives unchanged ({@code (F)F}), which is why the charge can be reported at all.
 *
 * <p>This is the first of the mixins ported under the project's rule that injection points are read
 * out of real bytecode rather than copied from a patch file or from the 26.3 method names. The
 * {@code method = "attack"} form is kept name-only for the same reason it is name-only upstream: the
 * name is unambiguous in this class, and the descriptor is asserted below by {@code require = 1}
 * failing loudly if it is ever not.
 */
@Mixin(Player.class)
public class MixinPlayerAttack {

    /**
     * Dispatches the event when the attack is over.
     *
     * @param target the entity that was attacked
     * @param info   the injection callback
     */
    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"), require = 1)
    private void merlinlib$postAttack(Entity target, CallbackInfo info) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.POST_ATTACK)) {
            return;
        }
        Player attacker = (Player) (Object) this;
        if (!(attacker.level() instanceof ServerLevel level) || !(target instanceof LivingEntity victim)) {
            return;
        }
        EnchantmentEventDispatcher.dispatch(BuiltInEvents.POST_ATTACK,
                new BuiltInEvents.PostAttackEvent(level, attacker, victim,
                        attacker.getAttackStrengthScale(0.5F), false, false),
                attacker);
    }
}
