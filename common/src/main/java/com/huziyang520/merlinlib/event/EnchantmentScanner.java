package com.huziyang520.merlinlib.event;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the enchantments an event should be delivered to.
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
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) {
            enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
        }
        if (enchantments == null || enchantments.isEmpty()) {
            return;
        }
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            int level = enchantments.getLevel(holder);
            if (level > 0) {
                into.add(new EnchantmentContext(holder, level, stack, slot));
            }
        }
    }
}
