// The last damage and last kill of each living entity, behind MerlinApi#combat().

package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.api.DamageSnapshot;
import com.huziyang520.merlinlib.api.KillInfo;
import net.minecraft.world.entity.LivingEntity;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatTracker {
    public static final CombatTracker INSTANCE = new CombatTracker();

    private static final long LIFETIME_MILLIS = 5_000L;
    private static final int MAX_ENTRIES = 1024;

    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

    private CombatTracker() {
    }

    public void recordDamage(DamageSnapshot snapshot) {
        if (snapshot == null || snapshot.victim() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        entries.compute(snapshot.victim().getUUID(), (key, existing) -> {
            Entry entry = liveEntry(existing, now);
            entry.damage = snapshot;
            entry.expiresAt = now + LIFETIME_MILLIS;
            return entry;
        });
        prune(now);
    }

    public void recordKill(KillInfo kill) {
        if (kill == null || kill.victim() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        entries.compute(kill.victim().getUUID(), (key, existing) -> {
            Entry entry = liveEntry(existing, now);
            entry.kill = kill;
            entry.expiresAt = now + LIFETIME_MILLIS;
            return entry;
        });
        prune(now);
    }

    public DamageSnapshot lastDamage(LivingEntity victim) {
        Entry entry = live(victim);
        return entry == null ? null : entry.damage;
    }

    public KillInfo lastKill(LivingEntity victim) {
        Entry entry = live(victim);
        return entry == null ? null : entry.kill;
    }

    public void clear(LivingEntity victim) {
        if (victim != null) {
            entries.remove(victim.getUUID());
        }
    }

    public void clear() {
        entries.clear();
    }

    private Entry live(LivingEntity victim) {
        if (victim == null) {
            return null;
        }
        UUID id = victim.getUUID();
        Entry entry = entries.get(id);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAt <= System.currentTimeMillis()) {
            entries.remove(id);
            return null;
        }
        return entry;
    }

    private static Entry liveEntry(Entry existing, long now) {
        if (existing == null || existing.expiresAt <= now) {
            return new Entry();
        }
        return existing;
    }

    private void prune(long now) {
        if (entries.size() <= MAX_ENTRIES) {
            return;
        }
        entries.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        if (entries.size() > MAX_ENTRIES) {
            entries.clear();
        }
    }

    private static final class Entry {
        private volatile DamageSnapshot damage;
        private volatile KillInfo kill;
        private volatile long expiresAt;
    }
}
