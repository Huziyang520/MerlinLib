package com.huziyang520.merlinlib.event;

import net.minecraft.resources.ResourceLocation;
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
 * <h2>The one shape change from the 26.3 line</h2>
 *
 * <p>The enchantment is a plain {@link Enchantment} rather than a {@code Holder<Enchantment>}. On 1.20.1
 * the enchantment registry is a static registry that is populated once and never rebuilt, so there is
 * no reload that could invalidate a reference, and no holder indirection is needed to stay correct. A
 * port from 26.3 therefore replaces {@code Holder<Enchantment>} with {@code RegistryObject<Enchantment>}
 * at the registration site and with {@link Enchantment} here.
 *
 * @param enchantment the enchantment, the one the callback was registered for
 * @param level       its level on that item, at least one
 * @param itemStack   the item carrying it
 * @param slot        the slot the item is worn in, or {@code null} when the item is not worn - a weapon
 *                    snapshot taken from a projectile, for example
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
