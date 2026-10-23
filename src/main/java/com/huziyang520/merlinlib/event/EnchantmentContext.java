package com.huziyang520.merlinlib.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Where an enchantment was found when an event fired.
 */
public record EnchantmentContext(Enchantment enchantment, int level, ItemStack itemStack,
                                 EquipmentSlot slot) {

    /**
     * @return the full id of the enchantment, e.g. {@code practical_enchantments:venom}, or {@code unknown}
     *         when it is not in the registry
 */
    public String enchantmentId() {
        ResourceLocation id = id();
        return id == null ? "unknown" : id.toString();
    }

    /**
     * @return the registry id of the enchantment, or {@code null} when it is not registered
 */
    public ResourceLocation id() {
        return net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getKey(this.enchantment);
    }
}
