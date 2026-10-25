// A type safe key for one kind of enchantment event, and the switch that makes dispatch cheap.

package com.huziyang520.merlinlib.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class EnchantmentEventType<E extends EnchantmentEvent> {
    private static final AtomicInteger NEXT_BIT = new AtomicInteger();
    private static final int MAX_TYPES = Long.SIZE;
    private final ResourceLocation id;
    private final Class<E> eventClass;
    private final int bit;
    private final Set<EquipmentSlot> relevantSlots = EnumSet.allOf(EquipmentSlot.class);

    private EnchantmentEventType(ResourceLocation id, Class<E> eventClass) {
        this.id = id;
        this.eventClass = eventClass;
        this.bit = NEXT_BIT.getAndIncrement();
        if (this.bit >= MAX_TYPES) {
            throw new IllegalStateException("[MerlinLib] more than " + MAX_TYPES
                    + " enchantment event types were created; the dispatch mask cannot hold another one");
        }
    }

    public static <E extends EnchantmentEvent> EnchantmentEventType<E> create(ResourceLocation id, Class<E> eventClass) {
        return new EnchantmentEventType<>(id, eventClass);
    }

    @SuppressWarnings("unchecked")
    public static <E extends EnchantmentEvent> EnchantmentEventType<E> create(ResourceLocation id) {
        return (EnchantmentEventType<E>) new EnchantmentEventType<>(id, EnchantmentEvent.class);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public Class<E> eventClass() {
        return this.eventClass;
    }

    public int bit() {
        return this.bit;
    }

    public long mask() {
        return 1L << this.bit;
    }

    public Set<EquipmentSlot> relevantSlots() {
        return this.relevantSlots;
    }

    public void setRelevantSlots(EnumSet<EquipmentSlot> slots) {
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("[MerlinLib] an event type cannot have an empty slot set");
        }
        this.relevantSlots.clear();
        this.relevantSlots.addAll(slots);
    }

    @Override
    public String toString() {
        return "EnchantmentEventType[" + this.id + "]";
    }
}
