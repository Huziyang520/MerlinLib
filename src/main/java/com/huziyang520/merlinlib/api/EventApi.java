// The enchantment event api, reached through MerlinApi#events().

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.event.EnchantmentEvent;
import com.huziyang520.merlinlib.event.EnchantmentEventCallback;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import com.huziyang520.merlinlib.event.EnchantmentEventType;
import net.minecraft.world.item.enchantment.Enchantment;

public final class EventApi {
    public static final EventApi INSTANCE = new EventApi();

    private EventApi() {
    }

    public <E extends EnchantmentEvent> void register(Enchantment enchantment,
                                                      EnchantmentEventType<E> type,
                                                      EnchantmentEventCallback<E> callback) {
        EnchantmentEventDispatcher.register(enchantment, type, callback);
    }

    public <E extends EnchantmentEvent> void register(net.minecraftforge.registries.RegistryObject<Enchantment> enchantment,
                                                      EnchantmentEventType<E> type,
                                                      EnchantmentEventCallback<E> callback) {
        if (enchantment == null || !enchantment.isPresent()) {
            throw new IllegalArgumentException("cannot register a callback for an enchantment that is not in the registry yet");
        }
        register(enchantment.get(), type, callback);
    }

    public boolean hasCallbacks(EnchantmentEventType<?> type) {
        return EnchantmentEventDispatcher.hasCallbacks(type);
    }

    public boolean hasCallbacks(Enchantment enchantment) {
        return EnchantmentEventDispatcher.hasEnchantmentCallbacks(enchantment);
    }

    public int registeredEnchantments() {
        return EnchantmentEventDispatcher.registeredEnchantments();
    }
}
