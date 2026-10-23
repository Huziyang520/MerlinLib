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
        // See the class Javadoc: 1.20.1 has no AbstractArrow#getWeaponItem, and the stack a trident was
        // thrown as is not publicly readable on this version. The event is still dispatched with the
        // attacker, the target and the level; only the weapon is empty, which means it currently
        // reaches no enchantment through the stack path.
        ItemStack weapon = merlinlib$weaponOf(projectile);
        EnchantmentEventDispatcher.dispatchStack(BuiltInEvents.PROJECTILE_HIT,
                new BuiltInEvents.ProjectileHitEvent(level, attacker, target, weapon, false, 1.0F), weapon);
    }

    /**
     * The weapon a projectile was fired as.
 *
     * <p>The cast is checked before it is made: {@code getPickupItem} is declared on
     * {@code AbstractArrow}, so a projectile that is only a {@code Projectile} has nothing to give and
     * falls through to empty. The shooter's main hand is deliberately <b>not</b> read as a fallback -
     * that is precisely the mistake the snapshot exists to prevent, since by the time the projectile
     * lands the weapon is no longer in any hand.
 *
     * @param projectile the projectile that landed
     * @return the weapon it was fired as, or empty when the projectile does not carry one
 */
    private static ItemStack merlinlib$weaponOf(Projectile projectile) {
        if (projectile instanceof AbstractArrow arrow) {
            return ((AbstractArrowAccessor) arrow).merlinlib$getPickupItem();
        }
        return ItemStack.EMPTY;
    }
}
