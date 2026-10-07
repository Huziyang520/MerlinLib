package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.event.BuiltInEvents;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link BuiltInEvents#POST_KILL} when the entity that dies is a player.
 *
 * <p>{@code ServerPlayer.die} replaces the base implementation instead of extending it, so without
 * this hook a kill event would fire for every victim except a player. Since the override does not call
 * the base method, adding this one does not produce a second event for the same death - the pair
 * together produces exactly one.
 *
 * <h2>1.20.1 evidence and what changed from 26.3</h2>
 *
 * <p>Nothing changed: this is a direct port. {@code ServerPlayer} on this version inherits its
 * {@code die} from the {@code Player} override, which is what makes the base-class hook miss players
 * in the first place. Verified with {@code javap -p -s net.minecraft.world.entity.player.Player}:
 *
 * <pre>
 * public void die(net.minecraft.world.damagesource.DamageSource);
 *   descriptor: (Lnet/minecraft/world/damagesource/DamageSource;)V
 * </pre>
 *
 * <p>Because {@code die} is declared on {@code Player} and not redeclared on {@code ServerPlayer},
 * the mixin's method resolution walks up to that declaration; the descriptor it finds is the one
 * above, which is identical to the 26.3 descriptor. Targeting {@code @Mixin(ServerPlayer.class)} is
 * still the correct choice: it is what makes the hook apply to players only, rather than to every
 * {@code Player} instance on either side.
 */
@Mixin(ServerPlayer.class)
public class MixinServerPlayerDie {

    /**
     * Dispatches the event when a player dies.
     *
     * @param source the damage source that killed them
     * @param info   the injection callback
     */
    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At("HEAD"), require = 1)
    private void merlinlib$postKill(DamageSource source, CallbackInfo info) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.POST_KILL)) {
            return;
        }
        ServerPlayer victim = (ServerPlayer) (Object) this;
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
