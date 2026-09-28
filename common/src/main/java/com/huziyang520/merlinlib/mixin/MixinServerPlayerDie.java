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
 * <p>{@code ServerPlayer.die} replaces the base implementation instead of extending it, so without this hook a
 * kill event would fire for every victim except a player. Since the override does not call the base method,
 * adding this one does not produce a second event for the same death - the pair together produces exactly one.
 */
@Mixin(ServerPlayer.class)
public class MixinServerPlayerDie {

    /**
     * Dispatches the event when a player dies.
     *
     * @param source the damage source that killed them
     * @param info   the injection callback
     */
    @Inject(method = "die", at = @At("HEAD"))
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
