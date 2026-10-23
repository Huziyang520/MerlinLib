package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.event.EnchantmentEvent;
import com.huziyang520.merlinlib.event.EnchantmentEventCallback;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import com.huziyang520.merlinlib.event.EnchantmentEventType;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The enchantment event api, reached through {@link MerlinApi#events()}.
 */
public final class EventApi {

    /** Single instance, handed out by {@link MerlinApi#events()}. */
    public static final EventApi INSTANCE = new EventApi();

    private EventApi() {
    }

    /**
     * Registers a callback for one enchantment and one event type.
     *
     * @param enchantment the enchantment to listen for
     * @param type        the event type, one of the constants in
     *                    {@link com.huziyang520.merlinlib.event.BuiltInEvents}
     * @param callback    the callback; it receives the event and the enchantment it was found on
     * @param <E>         the event class
 */
    public <E extends EnchantmentEvent> void register(Enchantment enchantment,
                                                      EnchantmentEventType<E> type,
                                                      EnchantmentEventCallback<E> callback) {
        EnchantmentEventDispatcher.register(enchantment, type, callback);
    }

    /**
     * Registers a callback for an enchantment held as a Forge {@code RegistryObject}.
     *
     * @param enchantment the registry object, whose value is read at registration time
     * @param type        the event type
     * @param callback    the callback
     * @param <E>         the event class
 */
    public <E extends EnchantmentEvent> void register(net.minecraftforge.registries.RegistryObject<Enchantment> enchantment,
                                                      EnchantmentEventType<E> type,
                                                      EnchantmentEventCallback<E> callback) {
        if (enchantment == null || !enchantment.isPresent()) {
            throw new IllegalArgumentException("cannot register a callback for an enchantment that is not in the registry yet");
        }
        register(enchantment.get(), type, callback);
    }

    /**
     * @param type the event type
     * @return {@code true} when at least one enchantment listens to it
 */
    public boolean hasCallbacks(EnchantmentEventType<?> type) {
        return EnchantmentEventDispatcher.hasCallbacks(type);
    }

    /**
     * @param enchantment the enchantment
     * @return {@code true} when it has at least one callback
 */
    public boolean hasCallbacks(Enchantment enchantment) {
        return EnchantmentEventDispatcher.hasEnchantmentCallbacks(enchantment);
    }

    /** @return how many enchantments have registered a callback, for diagnostics */
    public int registeredEnchantments() {
        return EnchantmentEventDispatcher.registeredEnchantments();
    }
}
