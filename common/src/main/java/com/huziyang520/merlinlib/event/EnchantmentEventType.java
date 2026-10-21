package com.huziyang520.merlinlib.event;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A type safe key for one kind of enchantment event, and the switch that makes dispatch cheap.
 *
 * <h2>The bit</h2>
 *
 * <p>Every type owns one bit. The dispatcher keeps a mask of the types that have at least one callback, and a
 * trigger checks its own bit before doing anything else - building a scan of six slots on every attack when
 * nobody listens to attacks is exactly the kind of cost a library must not add. Types are created as the
 * library and its dependents are initialised, so the number of types is bounded by the mods installed, not by
 * gameplay.
 *
 * <h2>The relevant slots</h2>
 *
 * <p>A type also declares which equipment slots make sense for it. A mining event has no reason to look at a
 * chestplate, and a projectile event is dispatched from a weapon snapshot rather than from the wearer at all.
 * The scanner uses this to skip slots it would otherwise read.
 *
 * @param <E> the event type this key stands for
 */
public final class EnchantmentEventType<E extends EnchantmentEvent> {

    /** The next free bit, handed out in creation order. */
    private static final AtomicInteger NEXT_BIT = new AtomicInteger();

    private static final int MAX_TYPES = Long.SIZE;

    private final Identifier id;
    private final Class<E> eventClass;
    private final int bit;
    private final Set<EquipmentSlot> relevantSlots = EnumSet.allOf(EquipmentSlot.class);

    private EnchantmentEventType(Identifier id, Class<E> eventClass) {
        this.id = id;
        this.eventClass = eventClass;
        this.bit = NEXT_BIT.getAndIncrement();
        if (this.bit >= MAX_TYPES) {
            throw new IllegalStateException("[MerlinLib] more than " + MAX_TYPES
                    + " enchantment event types were created; the dispatch mask cannot hold another one");
        }
    }

    /**
     * Creates a type.
     *
     * @param id         the id, unique across the library and its dependents
     * @param eventClass the event class, kept so a dispatch can be checked against it
     * @param <E>        the event type
     * @return the new type
     */
    public static <E extends EnchantmentEvent> EnchantmentEventType<E> create(Identifier id, Class<E> eventClass) {
        return new EnchantmentEventType<>(id, eventClass);
    }

    /**
     * Creates a type whose event class is not known here.
     *
     * <p>For the few places that only need a key - a config switch that turns a whole trigger off, for
     * example. A dispatch through such a key still works, because the dispatch itself never casts to the event
     * class; only the introspection helpers print it as unknown.
     *
     * @param id  the id
     * @param <E> the event type, unchecked on purpose
     * @return the new type
     */
    @SuppressWarnings("unchecked")
    public static <E extends EnchantmentEvent> EnchantmentEventType<E> create(Identifier id) {
        return (EnchantmentEventType<E>) new EnchantmentEventType<>(id, EnchantmentEvent.class);
    }

    /** @return the id of this type */
    public Identifier id() {
        return this.id;
    }

    /** @return the event class, or the root interface when it was not given */
    public Class<E> eventClass() {
        return this.eventClass;
    }

    /** @return this type's bit in the dispatch mask, between 0 and 63 */
    public int bit() {
        return this.bit;
    }

    /** @return the slot this type's bit stands for, e.g. {@code 1L << bit} */
    public long mask() {
        return 1L << this.bit;
    }

    /** @return the slots that make sense for this type; all of them unless narrowed */
    public Set<EquipmentSlot> relevantSlots() {
        return this.relevantSlots;
    }

    /**
     * Narrows the slots this type looks at.
     *
     * @param slots the slots to scan, must not be empty
     */
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
