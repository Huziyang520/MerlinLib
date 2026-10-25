// Fires BuiltInEvents#PROJECTILE_HIT when a projectile hits a living entity.

package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.event.BuiltInEvents;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public class MixinProjectileHit {
    @Inject(method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At("RETURN"), require = 1)
    private void merlinlib$projectileHit(EntityHitResult result, CallbackInfo info) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.PROJECTILE_HIT)) {
            return;
        }
        Projectile projectile = (Projectile) (Object) this;
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(result.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (!(projectile.getOwner() instanceof LivingEntity attacker)) {
            return;
        }

        ItemStack weapon = merlinlib$weaponOf(projectile);
        EnchantmentEventDispatcher.dispatchStack(BuiltInEvents.PROJECTILE_HIT,
                new BuiltInEvents.ProjectileHitEvent(level, attacker, target, weapon, false, 1.0F), weapon);
    }

    private static ItemStack merlinlib$weaponOf(Projectile projectile) {
        if (projectile instanceof AbstractArrow arrow) {
            return ((AbstractArrowAccessor) arrow).merlinlib$getPickupItem();
        }
        return ItemStack.EMPTY;
    }
}
