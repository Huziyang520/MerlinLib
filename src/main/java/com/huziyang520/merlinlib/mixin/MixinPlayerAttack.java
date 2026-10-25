// Fires BuiltInEvents#POST_ATTACK after an attack landed.

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

@Mixin(Player.class)
public class MixinPlayerAttack {
    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"), require = 1)
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
