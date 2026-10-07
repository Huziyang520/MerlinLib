package com.huziyang520.merlinlib.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Access to the upper bound of a ranged attribute, which vanilla keeps in a final field.
 *
 * <h2>Why this is needed</h2>
 *
 * <p>{@code minecraft:max_health} is a {@link RangedAttribute} whose bound is 1024, and
 * {@code AttributeInstance} clamps every value it computes to that bound. A health editor that
 * advertises "up to the integer limit" therefore cannot deliver it by setting modifiers alone - the
 * bound itself has to be raised, and there is no public setter for it.
 *
 * <p>This is an interface mixin, so the accessor is a method on the attribute's own class and can be
 * called by casting the attribute to this interface. It is applied once at startup by the shared
 * bootstrap.
 *
 * <h2>1.20.1 evidence and what changed from 26.3</h2>
 *
 * <p>Nothing changed: this is a direct port. The field was verified against the 1.20.1 bytecode with
 * {@code javap -p -s net.minecraft.world.entity.ai.attributes.RangedAttribute}:
 *
 * <pre>
 * public class net.minecraft.world.entity.ai.attributes.RangedAttribute extends ...Attribute {
 *   private final double minValue;
 *     descriptor: D
 *   private final double maxValue;
 *     descriptor: D
 *   public double getMaxValue();
 *     descriptor: ()D
 * }
 * </pre>
 *
 * <p>{@code maxValue} is {@code private final double}, so a plain {@code @Accessor} read would work
 * but a write needs {@code @Mutable} to strip the {@code final} flag - which is exactly what is
 * carried over from the 26.3 line.
 *
 * <h2>This file is an accessor, not an injector</h2>
 *
 * <p>{@code merlinlib.mixins.json} sets {@code injectors.defaultRequire = 1}. That switch only
 * applies to {@code @Inject}, {@code @Redirect}, {@code @ModifyArg}/{@code @ModifyArgs},
 * {@code @ModifyVariable} and {@code @ModifyConstant}. An {@code @Accessor} or {@code @Invoker}
 * carries its own {@code required} notion: a missing field or method makes the mixin fail to apply,
 * and because the config declares {@code required: true} that failure is still fatal at startup, so
 * the project's fail-loudly guarantee holds without any annotation change here.
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
