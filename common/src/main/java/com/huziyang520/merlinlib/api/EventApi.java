// The enchantment event api, reached through MerlinApi#events().

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.event.EnchantmentEvent;
import com.huziyang520.merlinlib.event.EnchantmentEventCallback;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import com.huziyang520.merlinlib.event.EnchantmentEventType;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

public final class EventApi {
    public static final EventApi INSTANCE = new EventApi();

    private EventApi() {
    }

    public <E extends EnchantmentEvent> void register(Holder<Enchantment> enchantment,
                                                      EnchantmentEventType<E> type,
                                                      EnchantmentEventCallback<E> callback) {
        EnchantmentEventDispatcher.register(enchantment, type, callback);
    }

    public <E extends EnchantmentEvent> void register(Holder<Enchantment> enchantment,
                                                      EnchantmentEventType<E> type,
                                                      int priority,
                                                      EnchantmentEventCallback<E> callback) {
        EnchantmentEventDispatcher.register(enchantment, type, callback, priority);
    }

    public boolean hasCallbacks(EnchantmentEventType<?> type) {
        return EnchantmentEventDispatcher.hasCallbacks(type);
    }

    public boolean hasCallbacks(Holder<Enchantment> enchantment) {
        return EnchantmentEventDispatcher.hasEnchantmentCallbacks(enchantment);
    }

    public int registeredEnchantments() {
        return EnchantmentEventDispatcher.registeredEnchantments();
    }
}
