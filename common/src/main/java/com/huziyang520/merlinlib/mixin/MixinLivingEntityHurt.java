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
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Fires {@link BuiltInEvents#MODIFY_DAMAGE} before and {@link BuiltInEvents#POST_HURT} after damage is
 * applied.
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntityHurt {

    /**
     * Lets the attacker's enchantments change the damage before it is applied.
     *
     * <p>The rewrite is done on the call into {@code actuallyHurt} rather than on the method's own parameter:
     * that call already carries the level, the source and the amount, so the event gets all three without
     * this class having to remember anything between injections - and a nested hit cannot make it read another
     * hit's damage source. Vanilla reaches that call from two branches (a normal hit and a hit large enough to
     * punch through invulnerability), and both are covered.
     *
     * @param args the arguments of the call, the damage being index 2
     */
    @ModifyArgs(method = "hurtServer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void merlinlib$modifyDamage(Args args) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.MODIFY_DAMAGE)) {
            return;
        }
        ServerLevel level = args.get(0);
        DamageSource source = args.get(1);
        float amount = args.get(2);
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            // Damage with no living attacker - falling, fire, a command - has nobody whose equipment the
            // event could belong to, so it is left exactly as vanilla computed it.
            return;
        }
        MutableFloat changed = new MutableFloat(amount);
        EnchantmentEventDispatcher.dispatch(BuiltInEvents.MODIFY_DAMAGE,
                new BuiltInEvents.ModifyDamageEvent(level, attacker, (LivingEntity) (Object) this, source, amount,
                        changed), attacker);
        args.set(2, changed.floatValue());
    }

    /**
     * Tells the victim's enchantments what it actually took.
     *
     * @param level  the level
     * @param source the damage source
     * @param amount the damage that was applied
     * @param info   the injection callback
     */
    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void merlinlib$postHurt(ServerLevel level, DamageSource source, float amount, CallbackInfo info) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.POST_HURT)) {
            return;
        }
        LivingEntity victim = (LivingEntity) (Object) this;
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        EnchantmentEventDispatcher.dispatch(BuiltInEvents.POST_HURT,
                new BuiltInEvents.PostHurtEvent(level, victim, attacker, source, amount, 0.0F, false), victim);
    }
}
