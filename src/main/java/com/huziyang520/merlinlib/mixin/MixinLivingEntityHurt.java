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
 *
 * <h2>Why these two methods</h2>
 *
 * <p>On 1.20.1 the gate every hit passes through is {@code LivingEntity.hurt}, and it receives the
 * damage before armour, enchantments and effects have had their say - exactly the number a "change
 * the damage" event has to be handed. {@code actuallyHurt} is the method that finally subtracts
 * health, and its argument is what really went through; a dependent mod that scales an effect off the
 * damage the victim took needs that number, not the requested one.
 *
 * <h2>What the events do not carry</h2>
 *
 * <p>Shield blocking is decided inside {@code hurt}: a blocked hit returns before {@code actuallyHurt}
 * and therefore never produces a hurt event, and the blocked amount is not visible here either. The
 * two fields are reported as {@code false} and {@code 0} for that reason.
 *
 * <h2>1.20.1: this is the port that had to change the most, and why</h2>
 *
 * <p>The 26.3 line hooks {@code hurtServer(ServerLevel, DamageSource, float)} and redirects the call
 * into {@code actuallyHurt(ServerLevel, DamageSource, float)}. <b>Neither method exists on
 * 1.20.1.</b> Verified with
 * {@code javap -p -s net.minecraft.world.entity.LivingEntity}, which lists no {@code hurtServer} at
 * all and gives the real pair:
 *
 * <pre>
 * public boolean hurt(net.minecraft.world.damagesource.DamageSource, float);
 *   descriptor: (Lnet/minecraft/world/damagesource/DamageSource;F)Z
 * protected void actuallyHurt(net.minecraft.world.damagesource.DamageSource, float);
 *   descriptor: (Lnet/minecraft/world/damagesource/DamageSource;F)V
 * </pre>
 *
 * <p>Two differences follow from that, and both are deliberate rather than incidental:
 *
 * <ol>
 *     <li>The level is no longer an argument of the modified call. {@code actuallyHurt} on this
 *     version takes only the source and the amount, so there is nothing to read the {@code ServerLevel}
 *     from at the call site. The damage event still promises a level, so it is resolved from the
 *     victim instead - the source's entity and the victim are always in the same level for a landed
 *     hit, and the guard below refuses the dispatch when that is not a {@code ServerLevel}.</li>
 *     <li>The redirect target moved from {@code hurtServer} to {@code hurt}. The event's meaning is
 *     unchanged: it is still "the damage as requested, before armour and effects", which is precisely
 *     what the entry of {@code hurt} holds. It is now an injection at the method's own parameter
 *     rather than at an outgoing call, because on this version the amount is the method argument
 *     itself.</li>
 * </ol>
 *
 * <p>The old design's reason for redirecting a <em>call</em> instead of the parameter was to avoid
 * having to remember state between two injections and to avoid a nested hit reading another hit's
 * damage source. Both of those are satisfied here by the {@code @ModifyVariable} form below, which
 * receives the source and the victim as its own arguments and holds no state at all.
 *
 * <h2>The exact injection point, read out of the bytecode</h2>
 *
 * <p>{@code javap -p -c net.minecraft.world.entity.LivingEntity} shows {@code hurt} calling
 * {@code actuallyHurt} from <b>two</b> places, exactly as the 26.3 comment describes for its own
 * version. The first is the "hit large enough to punch through invulnerability" branch, the second
 * the ordinary hit:
 *
 * <pre>
 *      281: aload_0
 *      282: aload_1
 *      283: fload_2
 *      284: aload_0
 *      285: getfield      #1974   // Field lastHurt:F
 *      288: fsub
 *      289: invokevirtual #1978   // Method actuallyHurt:(Lnet/minecraft/world/damagesource/DamageSource;F)V
 *      ...
 *      314: aload_0
 *      315: aload_1
 *      316: fload_2
 *      317: invokevirtual #1978   // Method actuallyHurt:(Lnet/minecraft/world/damagesource/DamageSource;F)V
 * </pre>
 *
 * <p>Reaching <em>both</em> is the reason the {@code @Inject} below names the method by its full
 * descriptor and sits at {@code HEAD}: a parameter modification at the entry of {@code hurt} is
 * upstream of both branches, whereas an injection at either individual call site would cover only one
 * of them. The bytecode above is what proves that both existing call sites are on the far side of
 * this hook.
 *
 * <p>{@code require = 1} is on both injectors. If {@code hurt} or {@code actuallyHurt} is ever renamed
 * or its descriptor changes, startup fails loudly rather than silently dropping damage modification.
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
