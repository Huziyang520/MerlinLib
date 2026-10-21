package com.huziyang520.merlinlib.event;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Where an enchantment was found when an event fired.
 *
 * <p>This is what makes a callback able to do its job without looking anything up: the enchantment, its level
 * and the exact item it sits on are all here. The item matters on its own - two different swords may carry the
 * same enchantment at different levels, and an effect that changes the item (durability, damage) has to touch
 * the one that actually has it.
 *
 * @param enchantment the enchantment holder, the one the callback was registered for
 * @param level       its level on that item, at least one
 * @param itemStack   the item carrying it
 * @param slot        the slot the item is worn in, or {@code null} when the item is not worn - a weapon
 *                    snapshot taken from a projectile, for example
 */
public record EnchantmentContext(Holder<Enchantment> enchantment, int level, ItemStack itemStack,
                                 EquipmentSlot slot) {

    /**
     * @return the full id of the enchantment, e.g. {@code practical_enchantments:venom}, or {@code unknown}
     *         when the holder is not from a registry
     */
    public String enchantmentId() {
        return this.enchantment.unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("unknown");
    }
}
