// Where an enchantment was found when an event fired.

package com.huziyang520.merlinlib.event;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public record EnchantmentContext(Holder<Enchantment> enchantment, int level, ItemStack itemStack,
                                 EquipmentSlot slot) {
    public String enchantmentId() {
        return this.enchantment.unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("unknown");
    }
}
