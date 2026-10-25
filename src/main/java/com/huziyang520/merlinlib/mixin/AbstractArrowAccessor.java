// Exposes the weapon an arrow-type projectile was fired as.

package com.huziyang520.merlinlib.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Invoker("getPickupItem")
    ItemStack merlinlib$getPickupItem();
}
