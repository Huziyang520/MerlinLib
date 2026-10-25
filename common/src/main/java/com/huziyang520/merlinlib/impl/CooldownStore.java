// Tick based cooldown bookkeeping behind MerlinApi#cooldowns().

package com.huziyang520.merlinlib.impl;

import net.minecraft.world.entity.Entity;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownStore {
    public static final CooldownStore INSTANCE = new CooldownStore();

    private static final int MAX_ENTRIES = 8192;

    private final Map<String, Long> expiries = new ConcurrentHashMap<>();

    private CooldownStore() {
    }

    private static String key(Entity entity, String name) {
        return entity.getUUID() + "|" + name;
    }

    private static long now(Entity entity) {
        return entity.level().getGameTime();
    }

    public boolean ready(Entity entity, String name, int ticks) {
        if (entity == null || name == null || name.isEmpty()) {
            return false;
        }
        long current = now(entity);
        String key = key(entity, name);
        Long expiry = expiries.get(key);
        if (expiry != null && expiry > current) {
            return false;
        }
        expiries.put(key, current + Math.max(0, ticks));
        prune(current);
        return true;
    }

    public int remaining(Entity entity, String name) {
        if (entity == null || name == null) {
            return 0;
        }
        Long expiry = expiries.get(key(entity, name));
        if (expiry == null) {
            return 0;
        }
        long left = expiry - now(entity);
        return left <= 0L ? 0 : (int) Math.min(Integer.MAX_VALUE, left);
    }

    public void trigger(Entity entity, String name, int ticks) {
        if (entity == null || name == null) {
            return;
        }
        expiries.put(key(entity, name), now(entity) + Math.max(0, ticks));
    }

    public void reset(Entity entity, String name) {
        if (entity != null && name != null) {
            expiries.remove(key(entity, name));
        }
    }

    private void prune(long current) {
        if (expiries.size() <= MAX_ENTRIES) {
            return;
        }
        expiries.entrySet().removeIf(entry -> entry.getValue() <= current);
    }
}
