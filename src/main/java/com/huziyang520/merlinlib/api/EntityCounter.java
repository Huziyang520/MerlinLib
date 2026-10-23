package com.huziyang520.merlinlib.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Small numbers kept on an entity, under ids the caller chooses.
 */
public final class EntityCounter {

    /** One map per entity, keyed weakly so a gone entity takes its counters with it. */
    private static final Map<Entity, Map<ResourceLocation, Integer>> COUNTERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private EntityCounter() {
    }

    /**
     * Reads a counter.
     *
     * @param entity the entity
     * @param id     the counter's id
     * @return its value, or {@code 0} when it was never set
     */
    public static int get(Entity entity, ResourceLocation id) {
        if (entity == null || id == null) {
            return 0;
        }
        synchronized (COUNTERS) {
            Map<ResourceLocation, Integer> counters = COUNTERS.get(entity);
            return counters == null ? 0 : counters.getOrDefault(id, 0);
        }
    }

    /**
     * Writes a counter.
     *
     * @param entity the entity
     * @param id     the counter's id
     * @param value  the value; {@code 0} is stored as "not set" and keeps the map from growing
     */
    public static void set(Entity entity, ResourceLocation id, int value) {
        if (entity == null || id == null) {
            return;
        }
        synchronized (COUNTERS) {
            if (value == 0) {
                Map<ResourceLocation, Integer> counters = COUNTERS.get(entity);
                if (counters != null) {
                    counters.remove(id);
                }
                return;
            }
            COUNTERS.computeIfAbsent(entity, key -> new java.util.HashMap<>()).put(id, value);
        }
    }

    /**
     * Adds one to a counter.
     *
     * @param entity the entity
     * @param id     the counter's id
     * @return the new value
     */
    public static int increment(Entity entity, ResourceLocation id) {
        return addAndGet(entity, id, 1);
    }

    /**
     * Adds to a counter and returns the result.
     *
     * @param entity the entity
     * @param id     the counter's id
     * @param delta  the amount to add, may be negative
     * @return the new value
     */
    public static int addAndGet(Entity entity, ResourceLocation id, int delta) {
        int next = get(entity, id) + delta;
        set(entity, id, next);
        return next;
    }

    /**
     * Sets a counter back to zero.
     *
     * @param entity the entity
     * @param id     the counter's id
     */
    public static void reset(Entity entity, ResourceLocation id) {
        set(entity, id, 0);
    }

    /**
     * Resets a counter when it holds an expected value.
     *
     * <p>The shape for "undo this once, if it is still the thing I did": the caller names the value it expects
     * to find, and only a match is cleared, so a counter that has since been changed by something else is left
     * alone.
     *
     * @param entity   the entity
     * @param id       the counter's id
     * @param expected the value to look for
     * @return {@code true} when the counter held that value and was reset
     */
    public static boolean checkAndReset(Entity entity, ResourceLocation id, int expected) {
        synchronized (COUNTERS) {
            if (get(entity, id) != expected) {
                return false;
            }
            set(entity, id, 0);
            return true;
        }
    }

    /**
     * Drops every counter of one entity.
     *
     * @param entity the entity
     */
    public static void clear(Entity entity) {
        if (entity != null) {
            COUNTERS.remove(entity);
        }
    }

    /**
     * @param entity the entity
     * @return the ids this entity has a non-zero counter under; empty when it has none
     */
    public static Set<ResourceLocation> keys(Entity entity) {
        if (entity == null) {
            return Set.of();
        }
        synchronized (COUNTERS) {
            Map<ResourceLocation, Integer> counters = COUNTERS.get(entity);
            return counters == null ? Set.of() : Set.copyOf(counters.keySet());
        }
    }
}
