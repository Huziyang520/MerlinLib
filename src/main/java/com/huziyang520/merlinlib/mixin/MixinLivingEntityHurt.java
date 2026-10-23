package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.event.BuiltInEvents;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link BuiltInEvents#MODIFY_DAMAGE} before and {@link BuiltInEvents#POST_HURT} after damage is
 * applied.
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntityHurt {

    /**
     * Lets the attacker's enchantments change the damage before it is applied.
     *
     * <p>The parameter is modified at the entry of {@code hurt}, which is the single point upstream of
     * both {@code actuallyHurt} call sites shown in the class Javadoc. Modifying the parameter rather
     * than an outgoing call means no state has to be carried between injections, so a nested hit can
     * never read another hit's damage source.
     *
     * @param amount the damage as requested, before armour and effects
     * @param source the damage source, used to find the attacker and the level
     * @return the damage to actually apply
 */
    @ModifyVariable(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"), argsOnly = true, require = 1)
    private float merlinlib$modifyDamage(float amount, DamageSource source) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.MODIFY_DAMAGE)) {
            return amount;
        }
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            // Damage with no living attacker - falling, fire, a command - has nobody whose equipment
            // the event could belong to, so it is left exactly as vanilla computed it.
            return amount;
        }
        LivingEntity victim = (LivingEntity) (Object) this;
        if (!(victim.level() instanceof ServerLevel level)) {
            return amount;
        }
        MutableFloat changed = new MutableFloat(amount);
        EnchantmentEventDispatcher.dispatch(BuiltInEvents.MODIFY_DAMAGE,
                new BuiltInEvents.ModifyDamageEvent(level, attacker, victim, source, amount, changed),
                attacker);
        return changed.floatValue();
    }

    /**
     * Tells the victim's enchantments what it actually took.
     *
     * <p>Injected at the return of {@code actuallyHurt}, so the amount is the one that reached the
     * health bar rather than the one that was requested.
     *
     * @param source the damage source
     * @param amount the damage that was applied
     * @param info   the injection callback
 */
    @Inject(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At("RETURN"), require = 1)
    private void merlinlib$postHurt(DamageSource source, float amount, CallbackInfo info) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.POST_HURT)) {
            return;
        }
        LivingEntity victim = (LivingEntity) (Object) this;
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        EnchantmentEventDispatcher.dispatch(BuiltInEvents.POST_HURT,
                new BuiltInEvents.PostHurtEvent(level, victim, attacker, source, amount, 0.0F, false),
                victim);
    }
}
