package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.event.EnchantmentEvent;
import com.huziyang520.merlinlib.event.EnchantmentEventCallback;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import com.huziyang520.merlinlib.event.EnchantmentEventType;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The enchantment event api, reached through {@link MerlinApi#events()}.
 *
 * <p>A dependent mod registers one callback per enchantment and event type. The library then takes care of the
 * rest: scanning the equipment (or a weapon snapshot), matching by enchantment id, passing the level and the
 * item along, and keeping one broken listener from affecting the others.
 *
 * <pre>{@code
 * MerlinApi.events().register(venomHolder, BuiltInEvents.POST_ATTACK, (event, context) -> {
 *     event.target().addEffect(new MobEffectInstance(MobEffects.POISON, 100, context.level() - 1));
 * });
 * }</pre>
 *
 * <p>Registration may happen from a mod constructor: nothing is scanned until an event actually fires, and an
 * event whose type has no callbacks costs a single bit test.
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
    public <E extends EnchantmentEvent> void register(Holder<Enchantment> enchantment,
                                                      EnchantmentEventType<E> type,
                                                      EnchantmentEventCallback<E> callback) {
        EnchantmentEventDispatcher.register(enchantment, type, callback);
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
    public boolean hasCallbacks(Holder<Enchantment> enchantment) {
        return EnchantmentEventDispatcher.hasEnchantmentCallbacks(enchantment);
    }

    /** @return how many enchantments have registered a callback, for diagnostics */
    public int registeredEnchantments() {
        return EnchantmentEventDispatcher.registeredEnchantments();
    }
}
