// Fires BuiltInEvents#POST_KILL on the killer when a living entity dies.

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

@Mixin(LivingEntity.class)
public class MixinLivingEntityDie {
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
