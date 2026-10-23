package com.huziyang520.merlinlib.event;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The handle a dependent mod registers its callbacks through.
 */
public final class EnchantmentEventRegistrar {

    /** The shared instance. */
    public static final EnchantmentEventRegistrar INSTANCE = new EnchantmentEventRegistrar();

    private EnchantmentEventRegistrar() {
    }

    /**
     * Registers a callback for one enchantment and one event type.
     *
     * @param enchantment the enchantment to listen for
     * @param type        the event type, one of the constants in {@link BuiltInEvents}
     * @param callback    the callback, which receives the event and where the enchantment was found
     * @param <E>         the event class
     * @return this registrar, so several registrations can be chained
     */
    public <E extends EnchantmentEvent> EnchantmentEventRegistrar register(Holder<Enchantment> enchantment,
                                                                          EnchantmentEventType<E> type,
                                                                          EnchantmentEventCallback<E> callback) {
        EnchantmentEventDispatcher.register(enchantment, type, callback);
        return this;
    }

    /** @return how many enchantments currently have at least one callback, for diagnostics */
    public int registeredCount() {
        return EnchantmentEventDispatcher.registeredEnchantments();
    }
}
