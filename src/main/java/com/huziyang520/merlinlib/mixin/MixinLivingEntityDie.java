package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.event.BuiltInEvents;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link BuiltInEvents#POST_KILL} on the killer when a living entity dies.
 *
 * <h2>Why there is a second mixin for players</h2>
 *
 * <p>{@code ServerPlayer.die} overrides {@code LivingEntity.die} without calling it, so a hook on the
 * base method never sees a player die. That is the classic silent failure of this trigger: everything
 * works in a test against a zombie and nothing works the moment the victim is a player.
 * {@link MixinServerPlayerDie} covers that case, and because the override does not call the base
 * method, one death produces exactly one event.
 *
 * <h2>Why the killer, not the victim</h2>
 *
 * <p>The event is delivered to the equipment of {@code getEntity()}, and the enchantments that care
 * about a kill - beheading, frenzy, incinerate - sit on the weapon. The victim is on the event under
 * its own name. A death with no killer (falling, starvation) therefore produces no event at all.
 *
 * <h2>1.20.1 evidence and what changed from 26.3</h2>
 *
 * <p>Nothing changed: this is a direct port. Verified with
 * {@code javap -p -s net.minecraft.world.entity.LivingEntity}:
 *
 * <pre>
 * public void die(net.minecraft.world.damagesource.DamageSource);
 *   descriptor: (Lnet/minecraft/world/damagesource/DamageSource;)V
 * </pre>
 *
 * <p>The descriptor matches the 26.3 line, so the injection and the callback signature carry over
 * untouched.
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntityDie {

    /**
     * Dispatches the event when a non player living entity dies.
     *
     * @param source the damage source that killed it
     * @param info   the injection callback
     */
    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At("HEAD"), require = 1)
    private void merlinlib$postKill(DamageSource source, CallbackInfo info) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.POST_KILL)) {
            return;
        }
        LivingEntity victim = (LivingEntity) (Object) this;
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(source.getEntity() instanceof LivingEntity killer)) {
            return;
        }
        EnchantmentEventDispatcher.dispatch(BuiltInEvents.POST_KILL,
                new BuiltInEvents.PostKillEvent(level, killer, victim, source), killer);
    }
}
