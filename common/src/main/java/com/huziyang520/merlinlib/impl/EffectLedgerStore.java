// Remembers the effects this library granted so revoking removes only our own copy.

package com.huziyang520.merlinlib.impl;

import net.minecraft.world.entity.LivingEntity;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EffectLedgerStore {
    public static final EffectLedgerStore INSTANCE = new EffectLedgerStore();

    private static final int MAX_ENTRIES = 8192;

    private final Map<String, Grant> grants = new ConcurrentHashMap<>();

    private EffectLedgerStore() {
    }

    private static String key(LivingEntity entity, String name) {
        return entity.getUUID() + "|" + name;
    }

    private static long now(LivingEntity entity) {
        return entity.level().getGameTime();
    }

    public void record(LivingEntity entity, String name, int amplifier, int duration) {
        if (entity == null || name == null) {
            return;
        }
        if (grants.size() > MAX_ENTRIES) {
            long current = now(entity);
            grants.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= current);
        }
        grants.put(key(entity, name), new Grant(amplifier, duration, now(entity) + duration));
    }

    public boolean holds(LivingEntity entity, String name, int amplifier) {
        if (entity == null || name == null) {
            return false;
        }
        Grant grant = grants.get(key(entity, name));
        return grant != null && grant.expiresAt() > now(entity) && grant.amplifier() == amplifier;
    }

    public boolean release(LivingEntity entity, String name) {
        return entity != null && name != null && grants.remove(key(entity, name)) != null;
    }

    public void clear(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        String prefix = entity.getUUID() + "|";
        grants.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public record Grant(int amplifier, int duration, long expiresAt) {
    }
}
