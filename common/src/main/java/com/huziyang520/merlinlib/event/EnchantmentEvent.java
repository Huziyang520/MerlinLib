// The common shape of every event an enchantment can receive.

package com.huziyang520.merlinlib.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public interface EnchantmentEvent {
    LivingEntity getEntity();
    ServerLevel getLevel();

    default boolean isCanceled() {
        return false;
    }

    default void cancel() {
    }
}
