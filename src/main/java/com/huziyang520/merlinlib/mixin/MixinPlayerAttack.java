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
 * <p>The event means "the attack has happened", so it has to run after vanilla finished the hit: damage
 * applied, durability spent, sweeping resolved. The tail of the one method that performs the attack is that
 * moment on both loaders, which is also why no loader event is used here - Fabric and NeoForge describe the
 * attack differently, and neither describes it after the fact.
 *
 * <h2>What is filled in and what is not</h2>
 *
 * <p>The attacker, the target and the charge are known. Whether the hit was critical and whether it swept are
 * decided inside private helpers of this same method, and the event is not the place to guess them: they are
 * reported as {@code false}, and a dependent mod that needs them should ask for the trigger to be widened
 * rather than trusting a value. Nothing in the library or in its current dependents reads them.
 */
@Mixin(Player.class)
public class MixinPlayerAttack {

    /**
     * Dispatches the event when the attack is over.
     *
     * @param target the entity that was attacked
     * @param info   the injection callback
     */
    @Inject(method = "attack", at = @At("TAIL"))
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
