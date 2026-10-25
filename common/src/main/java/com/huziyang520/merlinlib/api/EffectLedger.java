// Grants and revokes the effects this library owns, reached through MerlinApi#ledger().

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.impl.EffectLedgerStore;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class EffectLedger {
    public static final EffectLedger INSTANCE = new EffectLedger();

    private EffectLedger() {
    }

    public boolean grant(LivingEntity entity, Holder<MobEffect> effect, int duration, int amplifier, String name) {
        if (entity == null || effect == null || duration <= 0) {
            return false;
        }
        MobEffectInstance current = entity.getEffect(effect);
        if (current != null && (current.getAmplifier() > amplifier
                || (current.getAmplifier() == amplifier && current.getDuration() > duration))) {
            return false;
        }
        entity.addEffect(new MobEffectInstance(effect, duration, amplifier));
        EffectLedgerStore.INSTANCE.record(entity, name, amplifier, duration);
        return true;
    }

    public boolean owned(LivingEntity entity, Holder<MobEffect> effect, String name) {
        if (entity == null || effect == null || name == null) {
            return false;
        }
        MobEffectInstance current = entity.getEffect(effect);
        return current != null && EffectLedgerStore.INSTANCE.holds(entity, name, current.getAmplifier());
    }

    public boolean revokeOwned(LivingEntity entity, Holder<MobEffect> effect, String name) {
        if (!owned(entity, effect, name)) {
            return false;
        }
        entity.removeEffect(effect);
        EffectLedgerStore.INSTANCE.release(entity, name);
        return true;
    }

    public void clear(LivingEntity entity) {
        EffectLedgerStore.INSTANCE.clear(entity);
    }
}
