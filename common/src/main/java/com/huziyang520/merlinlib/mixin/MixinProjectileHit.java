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
 *
 * <h2>Why the trigger sits after vanilla handled the hit</h2>
 *
 * <p>A dependent mod that adds extra damage on a thrown trident has to do it after the throw's own damage has
 * been dealt - otherwise the target's damage cooldown swallows the extra hit and the enchantment looks dead.
 * Injecting at the end of {@code onHitEntity} is what puts the event on the correct side of that.
 *
 * <h2>Why the event carries a weapon snapshot</h2>
 *
 * <p>By the time the projectile lands, the weapon is not in the thrower's hand any more: a thrown trident is
 * travelling, a fired bow is still held but the arrow is not. Scanning equipment here would therefore find
 * nothing at all - the failure mode that made ranged enchantments silently do nothing - so the event carries
 * the weapon taken from the projectile itself, and the dispatcher is called through the stack path.
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
