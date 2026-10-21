package com.huziyang520.merlinlib.event;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Delivers an event to the enchantments that care about it.
 *
 * <h2>How a dispatch is kept cheap</h2>
 *
 * <p>A dispatch first looks at a mask of the types that have at least one callback. When nobody listens to
 * attacks, an attack costs one bit test and nothing else - no scan of the equipment, no allocation. The mask
 * is read through a volatile field, so a registration during gameplay is visible to the next trigger without
 * locking the hot path.
 *
 * <h2>How one bad callback is kept harmless</h2>
 *
 * <p>Callbacks belong to other people's mods. A callback that throws is logged with its enchantment and the
 * remaining callbacks - and the caller's own logic - still run. A library that lets one listener take down an
 * attack calculation is worse than no library.
 *
 * <h2>What the key is</h2>
 *
 * <p>Callbacks are registered against a {@link Holder}, but stored against the enchantment's id, because
 * holders are recreated by every datapack reload while ids are stable. The context that reaches a callback
 * still carries the live holder, so a callback always sees the current definition.
 */
public final class EnchantmentEventDispatcher {

    /** Enchantment id to (event type to callbacks). */
    private static final Map<Identifier, Map<EnchantmentEventType<?>, List<EnchantmentEventCallback<?>>>> CALLBACKS
            = new ConcurrentHashMap<>();

    /** The types that have at least one callback; bit i is {@link EnchantmentEventType#bit()}. */
    private static volatile long activeTypes;

    private EnchantmentEventDispatcher() {
    }

    /**
     * Registers a callback for one enchantment and one event type.
     *
     * <p>Registering twice for the same pair is allowed and both callbacks run, in registration order.
     *
     * @param enchantment the enchantment to listen for
     * @param type        the event type
     * @param callback    the callback
     * @param <E>         the event type's event class
     */
    public static <E extends EnchantmentEvent> void register(Holder<Enchantment> enchantment,
                                                             EnchantmentEventType<E> type,
                                                             EnchantmentEventCallback<E> callback) {
        Identifier id = idOf(enchantment);
        if (id == null) {
            Constants.LOG.warn("[MerlinLib] a callback for {} was ignored: the enchantment has no registry id, "
                    + "so it cannot be matched by id after a datapack reload", type.id());
            return;
        }
        CALLBACKS.computeIfAbsent(id, key -> new ConcurrentHashMap<>())
                .computeIfAbsent(type, key -> new CopyOnWriteArrayList<>())
                .add(callback);
        activeTypes |= type.mask();
    }

    /**
     * @param type the event type
     * @return {@code true} when any enchantment listens to it; a trigger uses this to do nothing at all
     */
    public static boolean hasCallbacks(EnchantmentEventType<?> type) {
        return (activeTypes & type.mask()) != 0L;
    }

    /**
     * @param enchantment the enchantment
     * @return {@code true} when that enchantment listens to anything
     */
    public static boolean hasEnchantmentCallbacks(Holder<Enchantment> enchantment) {
        Identifier id = idOf(enchantment);
        Map<EnchantmentEventType<?>, List<EnchantmentEventCallback<?>>> byType = id == null ? null : CALLBACKS.get(id);
        return byType != null && !byType.isEmpty();
    }

    /**
     * Delivers an event to the enchantments worn by an entity.
     *
     * @param type   the event type
     * @param event  the event
     * @param entity the entity to scan, usually {@link EnchantmentEvent#getEntity()}
     * @param <E>    the event class
     */
    public static <E extends EnchantmentEvent> void dispatch(EnchantmentEventType<E> type, E event,
                                                             LivingEntity entity) {
        if (event == null || !hasCallbacks(type)) {
            return;
        }
        dispatchTo(type, event, EnchantmentScanner.scan(entity, type));
    }

    /**
     * Delivers an event to the enchantments of one item stack.
     *
     * <p>The path for the events whose weapon has already left the hand: a thrown trident, a fired bow, a
     * crossbow bolt. The caller passes the weapon it took a snapshot of before the projectile flew.
     *
     * @param type  the event type
     * @param event the event
     * @param stack the weapon snapshot to scan
     * @param <E>   the event class
     */
    public static <E extends EnchantmentEvent> void dispatchStack(EnchantmentEventType<E> type, E event,
                                                                  ItemStack stack) {
        if (event == null || !hasCallbacks(type)) {
            return;
        }
        dispatchTo(type, event, EnchantmentScanner.scanStack(stack));
    }

    /**
     * Runs the callbacks of every context found.
     *
     * @param type     the event type
     * @param event    the event
     * @param contexts the enchantments to deliver to
     * @param <E>      the event class
     */
    private static <E extends EnchantmentEvent> void dispatchTo(EnchantmentEventType<E> type, E event,
                                                                List<EnchantmentContext> contexts) {
        for (EnchantmentContext context : contexts) {
            Identifier id = idOf(context.enchantment());
            if (id == null) {
                continue;
            }
            Map<EnchantmentEventType<?>, List<EnchantmentEventCallback<?>>> byType = CALLBACKS.get(id);
            if (byType == null) {
                continue;
            }
            List<EnchantmentEventCallback<?>> callbacks = byType.get(type);
            if (callbacks == null) {
                continue;
            }
            for (EnchantmentEventCallback<?> raw : callbacks) {
                try {
                    @SuppressWarnings("unchecked")
                    EnchantmentEventCallback<E> callback = (EnchantmentEventCallback<E>) raw;
                    callback.onEvent(event, context);
                } catch (RuntimeException | LinkageError error) {
                    // One listener from one mod must not break the others, the caller, or the server.
                    Constants.LOG.error("[MerlinLib] the {} callback of {} failed; the event continues",
                            type.id(), id, error);
                }
            }
        }
    }

    /**
     * @param enchantment the enchantment
     * @return how many callbacks it has registered, across all event types
     */
    public static int callbackCount(Holder<Enchantment> enchantment) {
        Identifier id = idOf(enchantment);
        Map<EnchantmentEventType<?>, List<EnchantmentEventCallback<?>>> byType = id == null ? null : CALLBACKS.get(id);
        if (byType == null) {
            return 0;
        }
        int count = 0;
        for (List<EnchantmentEventCallback<?>> callbacks : byType.values()) {
            count += callbacks.size();
        }
        return count;
    }

    /**
     * @param type the event type
     * @return how many callbacks are registered for it, across all enchantments
     */
    public static int callbackCount(EnchantmentEventType<?> type) {
        int count = 0;
        for (Map<EnchantmentEventType<?>, List<EnchantmentEventCallback<?>>> byType : CALLBACKS.values()) {
            List<EnchantmentEventCallback<?>> callbacks = byType.get(type);
            count += callbacks == null ? 0 : callbacks.size();
        }
        return count;
    }

    /** @return how many enchantments have at least one callback, for diagnostics */
    public static int registeredEnchantments() {
        return CALLBACKS.size();
    }

    /** @return the ids that have at least one callback, sorted, for diagnostics */
    public static List<Identifier> registeredEnchantmentIds() {
        List<Identifier> ids = new ArrayList<>(CALLBACKS.keySet());
        ids.sort(Comparator.comparing(Identifier::toString));
        return ids;
    }

    /** @return the event types that have at least one callback, for diagnostics */
    public static List<EnchantmentEventType<?>> activeTypes() {
        List<EnchantmentEventType<?>> types = new ArrayList<>();
        for (Map<EnchantmentEventType<?>, List<EnchantmentEventCallback<?>>> byType : CALLBACKS.values()) {
            for (EnchantmentEventType<?> type : byType.keySet()) {
                if (!types.contains(type)) {
                    types.add(type);
                }
            }
        }
        types.sort(Comparator.comparing(type -> type.id().toString()));
        return types;
    }

    /** Drops every callback; used by the reload path when a pack removes an enchantment. */
    public static void clear() {
        CALLBACKS.clear();
        activeTypes = 0L;
    }

    /**
     * @param enchantment the enchantment
     * @return its id, or {@code null} when it is not from a registry
     */
    private static Identifier idOf(Holder<Enchantment> enchantment) {
        return enchantment == null ? null : enchantment.unwrapKey().map(key -> key.identifier()).orElse(null);
    }
}
