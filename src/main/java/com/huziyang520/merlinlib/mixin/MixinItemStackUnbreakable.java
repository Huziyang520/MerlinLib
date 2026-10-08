package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.impl.EnchantmentRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Keeps {@code merlinlib:unbreakable} items at full durability.
 *
 * <h2>Why this is a mixin and not part of the enchantment declaration</h2>
 *
 * <p>On the 26.3 line the enchantment carried the numeric component
 * {@code minecraft:item_damage -> minecraft:set -> -32768}, and the game's own data driven effect
 * pipeline applied it as part of computing the item's components. There was nothing to intercept:
 * the rule was data, and the engine honoured it.
 *
 * <p>1.20.1 has no numeric effect system (that arrives in 1.20.5) and no data driven enchantment
 * registry, so an enchantment is a class with overridable methods - and none of those methods is
 * asked about durability. The question is asked by the item, not by the enchantment, which is why
 * the rule has to sit at the item's side of the call.
 *
 * <h2>Why this exact spot</h2>
 *
 * <p>{@code ItemStack#hurtAndBreak(int, LivingEntity, Consumer)} is the single funnel every
 * durability loss goes through: attacking, mining, blocking, armour damage and the direct
 * {@code ItemStack#hurtAndBreak} calls all reach it, and it checks {@code isDamageableItem} itself
 * before doing anything. Injecting at HEAD and cancelling is therefore exactly equivalent to the
 * component the 26.3 line declared: the first time the item would be damaged, the damage is refused,
 * and every later use keeps the item where it is.
 *
 * <p>{@code require = 1} is deliberate and is the project's rule for every mixin: if this method is
 * ever renamed or its descriptor changes, the game must fail loudly at startup rather than quietly
 * lose the behaviour. The descriptor below was read out of the 1.20.1 bytecode with
 * {@code javap -p -s}, not from a patch file.
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStackUnbreakable {

    /**
     * Refuses durability damage for a stack carrying {@code merlinlib:unbreakable}.
     *
     * @param amount     the durability damage that would be applied
     * @param entity     the entity holding the stack
     * @param onBroken   the vanilla break callback, unused here
     * @param callbackInfo the mixin callback, cancelled when the enchantment is present
     */
    @Inject(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void merlinlib$refuseDurabilityDamage(int amount, LivingEntity entity, Consumer<LivingEntity> onBroken,
                                                  CallbackInfo callbackInfo) {
        if (amount <= 0) {
            return;
        }
        Enchantment unbreakable = EnchantmentRegistry.unbreakable();
        if (unbreakable == null) {
            // The enchantment is not in the registry: either the registry event has not run yet, or a
            // config file disabled it. Either way there is nothing to enforce, and creating a
            // dependency on load order here would be a defect.
            return;
        }
        ItemStack stack = (ItemStack) (Object) this;
        if (EnchantmentHelper.getItemEnchantmentLevel(unbreakable, stack) > 0) {
            // "First damage is repaired, nothing is spent afterwards" is a repair, not a blanket
            // refusal. Cancelling alone froze the item at whatever damage it already carried - an item
            // that was worn when the enchantment arrived stayed worn forever, which is the difference
            // the report named ("only locks durability"). Clearing the damage and then cancelling
            // leaves the stack at full: the moment it would be damaged is the moment it is restored.
            if (stack.isDamaged()) {
                stack.setDamageValue(0);
            }
            callbackInfo.cancel();
        }
    }
}
