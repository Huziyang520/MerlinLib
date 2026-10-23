package com.huziyang520.merlinlib.event;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.resources.ResourceLocation;
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
 */
public final class EnchantmentEventDispatcher {

    /** Enchantment id to (event type to callbacks). */
    private static final Map<ResourceLocation, Map<EnchantmentEventType<?>, List<EnchantmentEventCallback<?>>>> CALLBACKS
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
    public static <E extends EnchantmentEvent> void register(Enchantment enchantment,
                                                             EnchantmentEventType<E> type,
                                                             EnchantmentEventCallback<E> callback) {
        ResourceLocation id = idOf(enchantment);
        if (id == null) {
            Constants.LOG.warn("[MerlinLib] a callback for {} was ignored: the enchantment has no registry id, "
                    + "so it cannot be matched by id", type.id());
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
    public static boolean hasEnchantmentCallbacks(Enchantment enchantment) {
        ResourceLocation id = idOf(enchantment);
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
            ResourceLocation id = idOf(context.enchantment());
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
    public static int callbackCount(Enchantment enchantment) {
        ResourceLocation id = idOf(enchantment);
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
    public static List<ResourceLocation> registeredEnchantmentIds() {
        List<ResourceLocation> ids = new ArrayList<>(CALLBACKS.keySet());
        ids.sort(Comparator.comparing(ResourceLocation::toString));
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

    /**
     * Drops every callback.
 */
    public static void clear() {
        CALLBACKS.clear();
        activeTypes = 0L;
    }

    /**
     * @param enchantment the enchantment
     * @return its id, or {@code null} when it is not in the registry
 */
    private static ResourceLocation idOf(Enchantment enchantment) {
        return enchantment == null ? null : net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
    }
}
