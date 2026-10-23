package com.huziyang520.merlinlib.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the weapon an arrow-type projectile was fired as.
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
