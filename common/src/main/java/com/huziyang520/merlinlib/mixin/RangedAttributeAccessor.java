package com.huziyang520.merlinlib.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Access to the upper bound of a ranged attribute, which vanilla keeps in a final field.
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
