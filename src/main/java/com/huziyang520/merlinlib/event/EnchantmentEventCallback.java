// The one callback shape used for every event type.

package com.huziyang520.merlinlib.event;

@FunctionalInterface
public interface EnchantmentEventCallback<E extends EnchantmentEvent> {
    void onEvent(E event, EnchantmentContext context);
}
