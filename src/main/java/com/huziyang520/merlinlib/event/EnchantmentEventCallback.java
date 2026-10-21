package com.huziyang520.merlinlib.event;

/**
 * The one callback shape used for every event type.
 *
 * <p>The event carries what happened, the {@link EnchantmentContext} carries where it came from: which
 * enchantment, at which level, on which item, in which slot. A callback therefore never has to search for the
 * enchantment it belongs to.
 *
 * @param <E> the concrete event type
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
