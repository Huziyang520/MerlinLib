package com.huziyang520.merlinlib.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Finds the enchantments an event should be delivered to.
 *
 * <h2>Two paths, and why both are needed</h2>
 *
 * <p>{@link #scan} walks the equipment of a living entity. It is the right path for anything the entity is
 * wearing or holding at the moment the event fires.
 *
 * <p>{@link #scanStack} walks one item stack, with no entity at all. It exists for the case that made a whole
 * class of enchantments silently do nothing: when a projectile hits, a thrown trident has already left the
 * hand, so the weapon is no longer in any slot and an equipment scan finds nothing. The event carries a
 * snapshot of the weapon instead, and this path reads it.
 *
 * <p>Both paths return contexts in a stable order - slot order, then the order the item's own enchantment list
 * holds them - so a callback that changes the world makes its changes in a reproducible sequence.
 *
 * <h2>What changed from the 26.3 line</h2>
 *
 * <p>The 1.20.1 line reads and writes enchantments as NBT, because the data component system this class used
 * to go through ({@code DataComponents.ENCHANTMENTS}, {@code DataComponents.STORED_ENCHANTMENTS},
 * {@code ItemEnchantments}) does not exist before 1.20.5. Both storages are read here, which is what makes a
 * book in a hand behave the way a book behaves everywhere else:
 *
 * <ul>
 *   <li>{@code EnchantmentHelper.getEnchantments(stack)} reads the {@code Enchantments} NBT list, which is
 *       where an enchanted item keeps its enchantments</li>
 *   <li>{@link EnchantedBookItem#getEnchantments(ItemStack)} reads the {@code StoredEnchantments} list, which
 *       is where an enchanted book keeps the ones it has not applied to anything yet</li>
 * </ul>
 *
 * <p>The merged map is the union of the two, with the higher level winning on a collision - the same rule the
 * data component version expressed by keeping one list.
 */
public final class EnchantmentScanner {

    private EnchantmentScanner() {
    }

    /**
     * Scans the slots an event type cares about.
     *
     * @param entity the living entity
     * @param type   the event type, whose {@link EnchantmentEventType#relevantSlots()} decides what is read
     * @return the enchantments found, in slot order; empty when there are none
     */
    public static List<EnchantmentContext> scan(LivingEntity entity, EnchantmentEventType<?> type) {
        List<EnchantmentContext> found = new ArrayList<>();
        if (entity == null) {
            return found;
        }
        for (EquipmentSlot slot : type.relevantSlots()) {
            addFromStack(entity.getItemBySlot(slot), slot, found);
        }
        return found;
    }

    /**
     * Scans every equipment slot.
     *
     * @param entity the living entity
     * @return the enchantments found, in slot order
     */
    public static List<EnchantmentContext> scanAllSlots(LivingEntity entity) {
        List<EnchantmentContext> found = new ArrayList<>();
        if (entity == null) {
            return found;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            addFromStack(entity.getItemBySlot(slot), slot, found);
        }
        return found;
    }

    /**
     * Scans one slot.
     *
     * @param entity the living entity
     * @param slot   the slot to read
     * @return the enchantments found on that slot's item
     */
    public static List<EnchantmentContext> scanSlot(LivingEntity entity, EquipmentSlot slot) {
        List<EnchantmentContext> found = new ArrayList<>();
        if (entity != null) {
            addFromStack(entity.getItemBySlot(slot), slot, found);
        }
        return found;
    }

    /**
     * Scans one item stack, for the events whose weapon is no longer worn.
     *
     * @param stack the stack, may be empty
     * @return the enchantments found on it, each with a {@code null} slot
     */
    public static List<EnchantmentContext> scanStack(ItemStack stack) {
        List<EnchantmentContext> found = new ArrayList<>();
        addFromStack(stack, null, found);
        return found;
    }

    /**
     * Reads the enchantments of one stack into a list.
     *
     * <p>Stored enchantments are read as well: an enchanted book carries its contents there, and a book in a
     * hand should behave like a book everywhere else.
     *
     * @param stack the stack, may be {@code null} or empty
     * @param slot  the slot to record, or {@code null} when the item is not worn
     * @param into  the list to add to
     */
    private static void addFromStack(ItemStack stack, EquipmentSlot slot, List<EnchantmentContext> into) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        for (Map.Entry<Enchantment, Integer> entry : enchantmentsOf(stack).entrySet()) {
            int level = entry.getValue() == null ? 0 : entry.getValue();
            if (entry.getKey() != null && level > 0) {
                into.add(new EnchantmentContext(entry.getKey(), level, stack, slot));
            }
        }
    }

    /**
     * Reads both enchantment storages of a stack.
     *
     * <p>The applied list first, then the stored list, so a book that is also enchanted directly keeps
     * whichever level is higher - the union rule described in the class Javadoc.
     *
     * @param stack the stack
     * @return the enchantments, keyed by enchantment; empty when it has none
     */
    private static Map<Enchantment, Integer> enchantmentsOf(ItemStack stack) {
        Map<Enchantment, Integer> merged = new java.util.LinkedHashMap<>(EnchantmentHelper.getEnchantments(stack));
        if (stack.getItem() instanceof EnchantedBookItem) {
            // An enchanted book keeps its contents in its own NBT list rather than in the applied one,
            // and the 1.20.1 helper for it returns the raw ListTag rather than a map. Each element is a
            // {id, lvl} compound, which is the same shape EnchantmentHelper writes.
            for (EnchantmentInstance instance : storedEnchantments(stack)) {
                merged.merge(instance.enchantment, instance.level, Math::max);
            }
        }
        return merged;
    }

    /**
     * Turns an enchanted book's stored enchantment list into instances.
     *
     * <p>Written by hand rather than through a helper because 1.20.1 has none:
     * {@code EnchantedBookItem.getEnchantments} hands back the {@code ListTag} itself. Elements that do
     * not name a registered enchantment are skipped - a book can outlive the mod that made it, and a
     * stale id must not stop the rest of the list from being read.
     *
     * @param stack the book
     * @return the stored enchantments, in list order
     */
    private static List<EnchantmentInstance> storedEnchantments(ItemStack stack) {
        List<EnchantmentInstance> instances = new ArrayList<>();
        net.minecraft.nbt.ListTag list = EnchantedBookItem.getEnchantments(stack);
        for (int index = 0; index < list.size(); index++) {
            net.minecraft.nbt.CompoundTag tag = list.getCompound(index);
            ResourceLocation id = EnchantmentHelper.getEnchantmentId(tag);
            if (id == null) {
                continue;
            }
            Enchantment enchantment = net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(id);
            if (enchantment == null) {
                continue;
            }
            instances.add(new EnchantmentInstance(enchantment, EnchantmentHelper.getEnchantmentLevel(tag)));
        }
        return instances;
    }
}
