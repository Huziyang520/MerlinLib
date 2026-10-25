// Fires BuiltInEvents#POST_KILL on the killer when a living entity dies.

package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.api.CombatApi;
import com.huziyang520.merlinlib.api.KillInfo;
import com.huziyang520.merlinlib.event.BuiltInEvents;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MixinLivingEntityDie {
    @Inject(method = "die", at = @At("HEAD"))
    private void merlinlib$postKill(DamageSource source, CallbackInfo info) {
        LivingEntity victim = (LivingEntity) (Object) this;
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity killer = source.getEntity() instanceof LivingEntity living ? living : null;
        CombatApi.INSTANCE.recordKill(new KillInfo(killer, victim, source,
                source.getDirectEntity() instanceof Projectile));
        if (killer == null || !EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.POST_KILL)) {
            return;
        }
        EnchantmentEventDispatcher.dispatch(BuiltInEvents.POST_KILL,
                new BuiltInEvents.PostKillEvent(level, killer, victim, source), killer);
    }
}
