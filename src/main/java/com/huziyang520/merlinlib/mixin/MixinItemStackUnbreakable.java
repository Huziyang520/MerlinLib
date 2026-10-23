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
