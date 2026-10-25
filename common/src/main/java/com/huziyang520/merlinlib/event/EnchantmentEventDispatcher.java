// Delivers an event to the enchantments that care about it.

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

public final class EnchantmentEventDispatcher {
    private static final Map<Identifier, Map<EnchantmentEventType<?>, List<Entry>>> CALLBACKS
            = new ConcurrentHashMap<>();

    private static volatile long activeTypes;

    private EnchantmentEventDispatcher() {
    }

    private record Entry(int priority, EnchantmentEventCallback<?> callback) {
    }

    public static <E extends EnchantmentEvent> void register(Holder<Enchantment> enchantment,
                                                             EnchantmentEventType<E> type,
                                                             EnchantmentEventCallback<E> callback) {
        register(enchantment, type, callback, 0);
    }

    public static <E extends EnchantmentEvent> void register(Holder<Enchantment> enchantment,
                                                             EnchantmentEventType<E> type,
                                                             EnchantmentEventCallback<E> callback,
                                                             int priority) {
        Identifier id = idOf(enchantment);
        if (id == null) {
            Constants.LOG.warn("[MerlinLib] a callback for {} was ignored: the enchantment has no registry id, "
                    + "so it cannot be matched by id after a datapack reload", type.id());
            return;
        }
        List<Entry> entries = CALLBACKS.computeIfAbsent(id, key -> new ConcurrentHashMap<>())
                .computeIfAbsent(type, key -> new CopyOnWriteArrayList<>());
        insert(entries, new Entry(priority, callback));
        activeTypes |= type.mask();
    }

    private static void insert(List<Entry> entries, Entry entry) {
        synchronized (entries) {
            int index = 0;
            while (index < entries.size() && entries.get(index).priority() <= entry.priority()) {
                index++;
            }
            entries.add(index, entry);
        }
    }

    public static boolean hasCallbacks(EnchantmentEventType<?> type) {
        return (activeTypes & type.mask()) != 0L;
    }

    public static boolean hasEnchantmentCallbacks(Holder<Enchantment> enchantment) {
        Identifier id = idOf(enchantment);
        Map<EnchantmentEventType<?>, List<Entry>> byType = id == null ? null : CALLBACKS.get(id);
        return byType != null && !byType.isEmpty();
    }

    public static <E extends EnchantmentEvent> void dispatch(EnchantmentEventType<E> type, E event,
                                                             LivingEntity entity) {
        if (event == null || !hasCallbacks(type)) {
            return;
        }
        dispatchTo(type, event, EnchantmentScanner.scan(entity, type));
    }

    public static <E extends EnchantmentEvent> void dispatchStack(EnchantmentEventType<E> type, E event,
                                                                  ItemStack stack) {
        if (event == null || !hasCallbacks(type)) {
            return;
        }
        dispatchTo(type, event, EnchantmentScanner.scanStack(stack));
    }

    private static <E extends EnchantmentEvent> void dispatchTo(EnchantmentEventType<E> type, E event,
                                                                List<EnchantmentContext> contexts) {
        for (EnchantmentContext context : contexts) {
            Identifier id = idOf(context.enchantment());
            if (id == null) {
                continue;
            }
            Map<EnchantmentEventType<?>, List<Entry>> byType = CALLBACKS.get(id);
            if (byType == null) {
                continue;
            }
            List<Entry> entries = byType.get(type);
            if (entries == null) {
                continue;
            }
            for (Entry entry : entries) {
                try {
                    @SuppressWarnings("unchecked")
                    EnchantmentEventCallback<E> callback = (EnchantmentEventCallback<E>) entry.callback();
                    callback.onEvent(event, context);
                } catch (RuntimeException | LinkageError error) {
                    Constants.LOG.error("[MerlinLib] the {} callback of {} failed; the event continues",
                            type.id(), id, error);
                }
                if (event.isCanceled()) {
                    return;
                }
            }
        }
    }

    public static int callbackCount(Holder<Enchantment> enchantment) {
        Identifier id = idOf(enchantment);
        Map<EnchantmentEventType<?>, List<Entry>> byType = id == null ? null : CALLBACKS.get(id);
        if (byType == null) {
            return 0;
        }
        int count = 0;
        for (List<Entry> entries : byType.values()) {
            count += entries.size();
        }
        return count;
    }

    public static int callbackCount(EnchantmentEventType<?> type) {
        int count = 0;
        for (Map<EnchantmentEventType<?>, List<Entry>> byType : CALLBACKS.values()) {
            List<Entry> entries = byType.get(type);
            count += entries == null ? 0 : entries.size();
        }
        return count;
    }

    public static int registeredEnchantments() {
        return CALLBACKS.size();
    }

    public static List<Identifier> registeredEnchantmentIds() {
        List<Identifier> ids = new ArrayList<>(CALLBACKS.keySet());
        ids.sort(Comparator.comparing(Identifier::toString));
        return ids;
    }

    public static List<EnchantmentEventType<?>> activeTypes() {
        List<EnchantmentEventType<?>> types = new ArrayList<>();
        for (Map<EnchantmentEventType<?>, List<Entry>> byType : CALLBACKS.values()) {
            for (EnchantmentEventType<?> type : byType.keySet()) {
                if (!types.contains(type)) {
                    types.add(type);
                }
            }
        }
        types.sort(Comparator.comparing(type -> type.id().toString()));
        return types;
    }

    public static void clear() {
        CALLBACKS.clear();
        activeTypes = 0L;
    }

    private static Identifier idOf(Holder<Enchantment> enchantment) {
        return enchantment == null ? null : enchantment.unwrapKey().map(key -> key.identifier()).orElse(null);
    }
}
