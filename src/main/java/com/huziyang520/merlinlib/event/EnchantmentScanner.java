// Finds the enchantments an event should be delivered to.

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

public final class EnchantmentScanner {
    private EnchantmentScanner() {
    }

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

    public static List<EnchantmentContext> scanSlot(LivingEntity entity, EquipmentSlot slot) {
        List<EnchantmentContext> found = new ArrayList<>();
        if (entity != null) {
            addFromStack(entity.getItemBySlot(slot), slot, found);
        }
        return found;
    }

    public static List<EnchantmentContext> scanStack(ItemStack stack) {
        List<EnchantmentContext> found = new ArrayList<>();
        addFromStack(stack, null, found);
        return found;
    }

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
