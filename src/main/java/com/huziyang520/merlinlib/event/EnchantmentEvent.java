package com.huziyang520.merlinlib.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * The common shape of every event an enchantment can receive.
 *
 * <p>An event always knows the entity it happens to and the level it happens in, because those two are what
 * the dispatch path needs to find the enchantments to call: the entity is scanned for equipment, and the
 * level is where the effect is applied. Everything else an event carries is on its own record, so a callback
 * takes the concrete type and reads real fields instead of fishing values out of a map.
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
