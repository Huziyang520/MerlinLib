package com.huziyang520.merlinlib.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Access to the upper bound of a ranged attribute, which vanilla keeps in a final field.
 *
 * <p>Why this is needed: {@code minecraft:max_health} is a {@link RangedAttribute} whose bound is 1024, and
 * {@code AttributeInstance} clamps every value it computes to that bound. A health editor that advertises
 * "up to the integer limit" therefore cannot deliver it by setting modifiers alone - the bound itself has to
 * be raised, and there is no public setter for it.
 *
 * <p>This is an interface mixin, so the accessor is a method on the attribute's own class and can be called
 * by casting the attribute to this interface. It is applied once at startup by the shared bootstrap.
 */
@Mixin(RangedAttribute.class)
public interface RangedAttributeAccessor {

    /**
     * Sets the upper bound of the attribute.
     *
     * @param value the new upper bound
     */
    @Mutable
    @Accessor("maxValue")
    void merlinlib$setMaxValue(double value);
}
