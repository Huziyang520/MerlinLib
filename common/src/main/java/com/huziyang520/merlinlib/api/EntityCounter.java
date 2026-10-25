// Small numbers kept on an entity, under ids the caller chooses.

package com.huziyang520.merlinlib.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class EntityCounter {
    private static final Map<Entity, Map<Identifier, Integer>> COUNTERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private EntityCounter() {
    }

    public static int get(Entity entity, Identifier id) {
        if (entity == null || id == null) {
            return 0;
        }
        synchronized (COUNTERS) {
            Map<Identifier, Integer> counters = COUNTERS.get(entity);
            return counters == null ? 0 : counters.getOrDefault(id, 0);
        }
    }

    public static void set(Entity entity, Identifier id, int value) {
        if (entity == null || id == null) {
            return;
        }
        synchronized (COUNTERS) {
            if (value == 0) {
                Map<Identifier, Integer> counters = COUNTERS.get(entity);
                if (counters != null) {
                    counters.remove(id);
                }
                return;
            }
            COUNTERS.computeIfAbsent(entity, key -> new java.util.HashMap<>()).put(id, value);
        }
    }

    public static int increment(Entity entity, Identifier id) {
        return addAndGet(entity, id, 1);
    }

    public static int addAndGet(Entity entity, Identifier id, int delta) {
        int next = get(entity, id) + delta;
        set(entity, id, next);
        return next;
    }

    public static void reset(Entity entity, Identifier id) {
        set(entity, id, 0);
    }

    public static boolean checkAndReset(Entity entity, Identifier id, int expected) {
        synchronized (COUNTERS) {
            if (get(entity, id) != expected) {
                return false;
            }
            set(entity, id, 0);
            return true;
        }
    }

    public static void clear(Entity entity) {
        if (entity != null) {
            COUNTERS.remove(entity);
        }
    }

    public static Set<Identifier> keys(Entity entity) {
        if (entity == null) {
            return Set.of();
        }
        synchronized (COUNTERS) {
            Map<Identifier, Integer> counters = COUNTERS.get(entity);
            return counters == null ? Set.of() : Set.copyOf(counters.keySet());
        }
    }
}
