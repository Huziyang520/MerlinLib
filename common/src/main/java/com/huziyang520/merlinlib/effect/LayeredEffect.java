// Per-entity layer stack for effects that stack instead of using the amplifier.
//
// A vanilla MobEffectInstance only has one duration and one amplifier, and adding
// the same effect again merges instead of stacking, so an effect that wants N
// independent layers keeps them here and mirrors the count into the amplifier.

package com.huziyang520.merlinlib.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LayeredEffect {
    private final Map<UUID, Deque<Integer>> layers = new ConcurrentHashMap<>();
    private final int layerTicks;
    private final int maxLayers;

    public LayeredEffect(int layerTicks, int maxLayers) {
        this.layerTicks = Math.max(1, layerTicks);
        this.maxLayers = Math.max(1, maxLayers);
    }

    public int layerTicks() {
        return this.layerTicks;
    }

    public int maxLayers() {
        return this.maxLayers;
    }

    /** Adds one layer and reports whether the stack just overflowed. */
    public boolean push(LivingEntity entity) {
        return push(entity, this.layerTicks);
    }

    /** Adds one layer that lives for {@code ticks} instead of the default layer length. */
    public boolean push(LivingEntity entity, int ticks) {
        Deque<Integer> stack = this.layers.computeIfAbsent(entity.getUUID(), id -> new ArrayDeque<>());
        stack.addLast(Math.max(1, ticks));
        return stack.size() > this.maxLayers;
    }

    public int count(LivingEntity entity) {
        Deque<Integer> stack = this.layers.get(entity.getUUID());
        return stack == null ? 0 : stack.size();
    }

    /** Whether this entity currently has a layer record, so callers can tell "never applied" from "expired". */
    public boolean tracked(LivingEntity entity) {
        return this.layers.containsKey(entity.getUUID());
    }

    /** Longest remaining layer time, used to mirror the real timer onto the effect instance. */
    public int longest(LivingEntity entity) {
        Deque<Integer> stack = this.layers.get(entity.getUUID());
        if (stack == null) {
            return 0;
        }
        int best = 0;
        for (int remaining : stack) {
            best = Math.max(best, remaining);
        }
        return best;
    }

    /** Ages every layer and returns how many are still alive. */
    public int tickDown(LivingEntity entity, int ticks) {
        Deque<Integer> stack = this.layers.get(entity.getUUID());
        if (stack == null) {
            return 0;
        }
        Deque<Integer> fresh = new ArrayDeque<>(stack.size());
        for (int remaining : stack) {
            int left = remaining - Math.max(1, ticks);
            if (left > 0) {
                fresh.addLast(left);
            }
        }
        if (fresh.isEmpty()) {
            this.layers.remove(entity.getUUID());
        } else {
            this.layers.put(entity.getUUID(), fresh);
        }
        return fresh.size();
    }

    /** Removes up to {@code amount} layers and returns the summed remaining time. */
    public int drain(LivingEntity entity, int amount) {
        Deque<Integer> stack = this.layers.get(entity.getUUID());
        if (stack == null) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < amount && !stack.isEmpty(); i++) {
            total += stack.pollFirst();
        }
        if (stack.isEmpty()) {
            this.layers.remove(entity.getUUID());
        }
        return total;
    }

    public void clear(LivingEntity entity) {
        this.layers.remove(entity.getUUID());
    }

    /**
     * Keeps the visible effect instance in step with the stack: the timer shows the
     * longest remaining layer and the amplifier shows the count, so the HUD and Jade
     * read "name II" instead of an endless effect.
     */
    public void syncDisplay(LivingEntity entity, Holder<MobEffect> effect) {
        int count = this.count(entity);
        if (count <= 0) {
            if (entity.hasEffect(effect)) {
                entity.removeEffect(effect);
            }
            return;
        }
        MobEffectInstance current = entity.getEffect(effect);
        int expectedAmplifier = Math.min(count, this.maxLayers) - 1;
        if (current != null && !current.isInfiniteDuration() && current.getAmplifier() == expectedAmplifier) {
            return;
        }
        if (current != null) {
            entity.removeEffect(effect);
        }
        entity.addEffect(new MobEffectInstance(effect, Math.max(1, this.longest(entity)),
                expectedAmplifier, false, true, true));
    }
}
