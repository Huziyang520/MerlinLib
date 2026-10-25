// Marks living entities whose drops must not spawn, with a short lifetime so a missed
// consumer cannot block a corpse forever.

package com.huziyang520.merlinlib.impl;

import net.minecraft.world.entity.LivingEntity;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DropGate {
    public static final DropGate INSTANCE = new DropGate();

    private static final long LIFETIME_MILLIS = 5_000L;
    private static final int MAX_ENTRIES = 1024;

    private final Map<UUID, Long> suppressed = new ConcurrentHashMap<>();

    private DropGate() {
    }

    public void suppress(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        if (suppressed.size() > MAX_ENTRIES) {
            suppressed.clear();
        }
        suppressed.put(entity.getUUID(), System.currentTimeMillis() + LIFETIME_MILLIS);
    }

    public boolean isSuppressed(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        Long expiry = suppressed.get(entity.getUUID());
        return expiry != null && expiry >= System.currentTimeMillis();
    }

    public void release(LivingEntity entity) {
        if (entity != null) {
            suppressed.remove(entity.getUUID());
        }
    }
}
