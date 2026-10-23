package com.huziyang520.merlinlib.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * The common shape of every event an enchantment can receive.
 */
public interface EnchantmentEvent {

    /**
     * @return the entity the event happened to: the attacker for an attack, the victim for a hurt
 */
    LivingEntity getEntity();

    /**
     * @return the level the event happens in, always the server side one
 */
    ServerLevel getLevel();
}
