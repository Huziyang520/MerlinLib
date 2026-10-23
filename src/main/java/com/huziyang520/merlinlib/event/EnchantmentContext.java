package com.huziyang520.merlinlib.event;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Where an enchantment was found when an event fired.
 */
public record EnchantmentContext(Holder<Enchantment> enchantment, int level, ItemStack itemStack,
                                 EquipmentSlot slot) {

    /**
     * @return the full id of the enchantment, e.g. {@code practical_enchantments:venom}, or {@code unknown}
     *         when the holder is not from a registry
     */
    public String enchantmentId() {
        return this.enchantment.unwrapKey()
                .map(key -> key.location().toString())
                .orElse("unknown");
    }
}
