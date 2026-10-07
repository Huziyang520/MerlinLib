package com.huziyang520.merlinlib.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the weapon an arrow-type projectile was fired as.
 *
 * <h2>Why an accessor mixin is needed at all</h2>
 *
 * <p>The projectile hit event has to deliver itself to the enchantments of the <em>weapon</em>, not to
 * the shooter's current equipment: by the time a thrown trident lands, it has left the hand, and an
 * equipment scan finds nothing. The 26.3 line read the weapon from
 * {@code AbstractArrow#getWeaponItem()} - a public accessor that <b>does not exist on 1.20.1</b>.
 *
 * <p>What this version has instead is {@code protected abstract ItemStack getPickupItem()}, which
 * every arrow type implements to say what the player gets back when they walk over it - and for a
 * trident that is a copy of the trident itself, which is exactly the snapshot the event needs.
 *
 * <p>It cannot be reached from the outside three ways, and all three were checked before this class
 * was written:
 * <ul>
 *   <li>{@code @Shadow} on a mixin targeting {@code Projectile} does not work: Mixin resolves a shadow
 *       against the class it targets, and the method is declared on the subclass, not there. javac
 *       accepts it - which is what makes it a trap rather than a compile error.</li>
 *   <li>a cast to {@code AbstractArrow} and a direct call does not compile: the method is
 *       {@code protected} and this package is not {@code net.minecraft.world.entity.projectile}.</li>
 *   <li>a plain interface cannot declare it either, for the same reason.</li>
 * </ul>
 *
 * <p>So the access is generated: {@code @Invoker} makes Mixin implement this interface on
 * {@code AbstractArrow} itself, where the protected method is reachable. This is the mechanism Mixin
 * provides for exactly this case.
 *
 * <p><b>Registered in {@code merlinlib.mixins.json} as {@code AbstractArrowAccessor}.</b> It is
 * deliberately in the {@code mixins} list and not the {@code client} list: a thrown trident is
 * resolved on the server, where the damage and the events live.
 *
 * <p>The {@code merlinlib$} prefix on the method is the project's convention for members it adds to
 * vanilla classes, so a name collision with a future vanilla method is impossible and a stack trace
 * says immediately who added it.
 */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {

    /**
     * @return the item a player picks up from this projectile, which for a thrown trident is a copy of
     *         the trident that was thrown - the weapon snapshot the projectile hit event needs
     */
    @Invoker("getPickupItem")
    ItemStack merlinlib$getPickupItem();
}
