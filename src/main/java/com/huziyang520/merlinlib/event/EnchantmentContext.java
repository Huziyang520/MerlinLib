// Where an enchantment was found when an event fired.

package com.huziyang520.merlinlib.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public record EnchantmentContext(Enchantment enchantment, int level, ItemStack itemStack,
                                 EquipmentSlot slot) {
    public String enchantmentId() {
        ResourceLocation id = id();
        return id == null ? "unknown" : id.toString();
    }

    public ResourceLocation id() {
        return net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getKey(this.enchantment);
    }
}
