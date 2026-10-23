package com.huziyang520.merlinlib.event;

/**
 * The one callback shape used for every event type.
 */
@FunctionalInterface
public interface EnchantmentEventCallback<E extends EnchantmentEvent> {

    /**
     * Called once for every enchantment found on the scanned equipment.
     *
     * @param event   the event that happened
     * @param context the enchantment the call is for
 */
    void onEvent(E event, EnchantmentContext context);
}
