// Keeps merlinlib:unbreakable items at full durability.

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

@Mixin(ItemStack.class)
public abstract class MixinItemStackUnbreakable {
    @Inject(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void merlinlib$refuseDurabilityDamage(int amount, LivingEntity entity, Consumer<LivingEntity> onBroken,
                                                  CallbackInfo callbackInfo) {
        if (amount <= 0) {
            return;
        }
        Enchantment unbreakable = EnchantmentRegistry.unbreakable();
        if (unbreakable == null) {
            return;
        }
        ItemStack stack = (ItemStack) (Object) this;
        if (EnchantmentHelper.getItemEnchantmentLevel(unbreakable, stack) > 0) {
            if (stack.isDamaged()) {
                stack.setDamageValue(0);
            }
            callbackInfo.cancel();
        }
    }
}
