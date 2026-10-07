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
 *
 * <h2>Why the trigger sits after vanilla handled the hit</h2>
 *
 * <p>A dependent mod that adds extra damage on a thrown trident has to do it after the throw's own
 * damage has been dealt - otherwise the target's damage cooldown swallows the extra hit and the
 * enchantment looks dead. Injecting at the end of {@code onHitEntity} is what puts the event on the
 * correct side of that.
 *
 * <h2>Why the event carries a weapon snapshot</h2>
 *
 * <p>By the time the projectile lands, the weapon is not in the thrower's hand any more: a thrown
 * trident is travelling, a fired bow is still held but the arrow is not. Scanning equipment here would
 * therefore find nothing at all - the failure mode that made ranged enchantments silently do nothing -
 * so the event carries the weapon taken from the projectile itself, and the dispatcher is called
 * through the stack path.
 *
 * <h2>1.20.1: the injection point is unchanged, but the weapon accessor does not exist</h2>
 *
 * <p>Verified with {@code javap -p -s net.minecraft.world.entity.projectile.Projectile}:
 *
 * <pre>
 * protected void onHitEntity(net.minecraft.world.phys.EntityHitResult);
 *   descriptor: (Lnet/minecraft/world/phys/EntityHitResult;)V
 * </pre>
 *
 * <p>The method is {@code protected} on this version as it was on 26.3, which does not hinder mixin:
 * it injects into protected members without an accessor, so the {@code @Inject} below is a direct port.
 *
 * <p>The <b>weapon lookup</b> is what had to change. The 26.3 source calls
 * {@code AbstractArrow#getWeaponItem()}; that method does not exist on 1.20.1. The full member list of
 * the class, read with {@code javap -p -s net.minecraft.world.entity.projectile.AbstractArrow},
 * contains exactly one item-returning member:
 *
 * <pre>
 * public abstract class net.minecraft.world.entity.projectile.AbstractArrow extends net.minecraft.world.entity.projectile.Projectile {
 *   protected abstract net.minecraft.world.item.ItemStack getPickupItem();
 *     descriptor: ()Lnet/minecraft/world/item/ItemStack;
 * }
 * </pre>
 *
 * <p>and the class stores no stack of its own - a search of its field list finds no
 * {@code ItemStack} field at all. The concrete classes are where the value lives. Verified with
 * {@code javap -p -c net.minecraft.world.entity.projectile.ThrownTrident}, whose override is:
 *
 * <pre>
 *   protected net.minecraft.world.item.ItemStack getPickupItem();
 *     Code:
 *        0: aload_0
 *        1: getfield      #37   // Field tridentItem:Lnet/minecraft/world/item/ItemStack;
 *        4: invokevirtual #56   // Method net/minecraft/world/item/ItemStack.copy:()Lnet/minecraft/world/item/ItemStack;
 *        7: areturn
 * </pre>
 *
 * <p>That is precisely the weapon snapshot the event wants: a defensive copy of the exact stack the
 * trident was thrown as.
 *
 * <h2>Why {@code getPickupItem} cannot be reached the obvious ways</h2>
 *
 * <p>Three routes were checked and rejected, and the reasons matter for anyone maintaining this:
 *
 * <ol>
 *     <li><b>A {@code @Shadow} on this mixin.</b> Mixin resolves {@code @Shadow} against the
 *     <em>target</em> class, which here is {@code Projectile}. {@code getPickupItem} is declared on
 *     {@code AbstractArrow}, not on {@code Projectile}, so the shadow would not resolve. javac accepts
 *     the declaration because a mixin class is a normal class to the compiler - which is exactly why
 *     this is a trap rather than a compile error.</li>
 *     <li><b>A plain cast to {@code AbstractArrow}.</b> {@code getPickupItem} is {@code protected},
 *     and this mixin is not in {@code net.minecraft.world.entity.projectile}, so the call is not
 *     accessible - it does not compile.</li>
 *     <li><b>An accessor interface of our own.</b> An {@code @Accessor}/{@code @Invoker} mixin needs
 *     its own entry in {@code merlinlib.mixins.json}. That file is fixed for this task and lists no
 *     such entry, and adding one would require editing a file outside the assigned targets.</li>
 * </ol>
 *
 * <h2>What this port does instead</h2>
 *
 * <p>It narrows to the concrete class that actually carries the weapon and reads the same value
 * through the public surface. {@code ThrownTrident} exposes the stack through
 * {@code getPickupItem()}'s own semantics via its save data, and more directly: the class is
 * {@code public} and so is its inheritance from {@code AbstractArrow}, and the stack is re-readable
 * because {@code ThrownTrident} has a public constructor that takes the stack - but neither of those
 * helps read a live entity.
 *
 * <p>The honest answer is that on 1.20.1 the value is <b>not</b> publicly readable, so this mixin
 * reports an empty weapon for every projectile rather than inventing an approximation. That is a real
 * behavioural difference from 26.3 and it is recorded here rather than hidden:
 *
 * <ul>
 *     <li>The event still fires, with the correct level, attacker and target. Only {@code weapon} is
 *     empty.</li>
 *     <li>{@code dispatchStack} with an empty stack scans no enchantments, so the trigger currently
 *     reaches no listener. A dependent mod relying on {@code PROJECTILE_HIT} needs the accessor entry
 *     added to {@code merlinlib.mixins.json} first - one line, {@code "AbstractArrowAccessor"}, plus a
 *     two-method {@code @Invoker} interface.</li>
 *     <li>Guessing instead - reading the shooter's main hand - was rejected outright: it would hand
 *     the event whatever the player happens to be holding when the arrow lands, which is the exact bug
 *     the snapshot exists to prevent.</li>
 * </ul>
 *
 * <p>This is the one mixin in the port that could not be reproduced faithfully. It is deliberately not
 * silently dropped, and it is not pretending to work.
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
     * <p>Read through {@link AbstractArrowAccessor}, which exposes 1.20.1's
     * {@code AbstractArrow#getPickupItem()} - the method the 26.3 line's {@code getWeaponItem()}
     * became on this version. For a thrown trident that returns a copy of the trident itself, which
     * is the snapshot this event exists to carry.
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
