package com.huziyang520.merlinlib.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * A living entity ticked, seen globally rather than by enchantment.
 *
 * <p>Fired through {@link GlobalEvents#LIVING_ENTITY_TICK} for listeners that decide for themselves which
 * entities and which enchantments they care about - the shape a "while wearing this, keep doing something"
 * effect needs, where scanning every listener's equipment once per tick would be wasted work.
 *
 * @param level     the level
 * @param entity    the entity that ticked
 * @param tickCount its age in ticks
 */
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
