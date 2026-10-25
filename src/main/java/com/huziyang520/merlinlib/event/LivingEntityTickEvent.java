// A living entity ticked, seen globally rather than by enchantment.

package com.huziyang520.merlinlib.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public record LivingEntityTickEvent(ServerLevel level, LivingEntity entity,
                                    int tickCount) implements EnchantmentEvent {
    @Override
    public LivingEntity getEntity() {
        return this.entity;
    }

    @Override
    public ServerLevel getLevel() {
        return this.level;
    }
}
