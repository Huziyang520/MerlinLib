package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.event.EnchantmentEvent;
import com.huziyang520.merlinlib.event.EnchantmentEventCallback;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import com.huziyang520.merlinlib.event.EnchantmentEventType;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The enchantment event api, reached through {@link MerlinApi#events()}.
 *
 * <p>A dependent mod registers one callback per enchantment and event type. The library then takes care of the
 * rest: scanning the equipment (or a weapon snapshot), matching by enchantment id, passing the level and the
 * item along, and keeping one broken listener from affecting the others.
 *
 * <pre>{@code
 * MerlinApi.events().register(MerlinApi.enchantments().get(venomId), BuiltInEvents.POST_ATTACK, (event, context) -> {
 *     event.target().addEffect(new MobEffectInstance(MobEffects.POISON, 100, context.level() - 1));
 * });
 * }</pre>
 *
 * <h2>The one signature that changed from the 26.3 line</h2>
 *
 * <p>The 26.3 line took a {@code Holder<Enchantment>}. On 1.20.1 an enchantment is a plain registry
 * object reached through a {@code RegistryObject<Enchantment>}, and the id stable enough to key
 * callbacks by is the registry key rather than a holder's key, because this registry is populated
 * once and never rebuilt. The method below takes the {@link Enchantment} itself, and the dispatcher
 * reads its id from {@code BuiltInRegistries}; there is an overload taking a {@code RegistryObject}
 * for callers that hold one, which is the shape a 26.3 port will have after a mechanical translation.
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
