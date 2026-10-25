// Fires BuiltInEvents#POST_KILL when the entity that dies is a player.

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

@Mixin(ServerPlayer.class)
public class MixinServerPlayerDie {
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
