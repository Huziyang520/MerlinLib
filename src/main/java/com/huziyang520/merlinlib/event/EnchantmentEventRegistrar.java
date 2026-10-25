// The handle a dependent mod registers its callbacks through.

package com.huziyang520.merlinlib.event;

import net.minecraft.world.item.enchantment.Enchantment;

public final class EnchantmentEventRegistrar {
    public static final EnchantmentEventRegistrar INSTANCE = new EnchantmentEventRegistrar();

    private EnchantmentEventRegistrar() {
    }

    public <E extends EnchantmentEvent> EnchantmentEventRegistrar register(Enchantment enchantment,
                                                                          EnchantmentEventType<E> type,
                                                                          EnchantmentEventCallback<E> callback) {
        EnchantmentEventDispatcher.register(enchantment, type, callback);
        return this;
    }

    public <E extends EnchantmentEvent> EnchantmentEventRegistrar register(
            net.minecraftforge.registries.RegistryObject<Enchantment> enchantment,
            EnchantmentEventType<E> type,
            EnchantmentEventCallback<E> callback) {
        if (enchantment == null || !enchantment.isPresent()) {
            throw new IllegalArgumentException("cannot register a callback for an enchantment that is not in the registry yet");
        }
        return register(enchantment.get(), type, callback);
    }

    public int registeredCount() {
        return EnchantmentEventDispatcher.registeredEnchantments();
    }
}
