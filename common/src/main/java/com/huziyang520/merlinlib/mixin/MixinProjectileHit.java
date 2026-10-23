package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.event.BuiltInEvents;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link BuiltInEvents#PROJECTILE_HIT} when a projectile hits a living entity.
 */
@Mixin(Projectile.class)
public class MixinProjectileHit {

    /**
     * Dispatches the event to the weapon's enchantments.
     *
     * @param result the hit result vanilla just processed
     * @param info   the injection callback
     */
    @Inject(method = "onHitEntity", at = @At("RETURN"))
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
        // The weapon as it was fired: for a trident the thrown stack itself, for an arrow the bow that shot it.
        ItemStack weapon = projectile instanceof AbstractArrow arrow ? arrow.getWeaponItem() : ItemStack.EMPTY;
        EnchantmentEventDispatcher.dispatchStack(BuiltInEvents.PROJECTILE_HIT,
                new BuiltInEvents.ProjectileHitEvent(level, attacker, target, weapon, false, 1.0F), weapon);
    }
}
