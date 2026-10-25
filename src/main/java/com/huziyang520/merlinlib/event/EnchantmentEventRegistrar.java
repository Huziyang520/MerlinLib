// The handle a dependent mod registers its callbacks through.

package com.huziyang520.merlinlib.event;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

public final class EnchantmentEventRegistrar {
    public static final EnchantmentEventRegistrar INSTANCE = new EnchantmentEventRegistrar();

    private EnchantmentEventRegistrar() {
    }

    public <E extends EnchantmentEvent> EnchantmentEventRegistrar register(Holder<Enchantment> enchantment,
                                                                          EnchantmentEventType<E> type,
                                                                          EnchantmentEventCallback<E> callback) {
        EnchantmentEventDispatcher.register(enchantment, type, callback);
        return this;
    }

    public int registeredCount() {
        return EnchantmentEventDispatcher.registeredEnchantments();
    }
}
