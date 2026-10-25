// Finds the enchantments an event should be delivered to.

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
        for (Map.Entry<Enchantment, Integer> entry : enchantmentsOf(stack).entrySet()) {
            int level = entry.getValue() == null ? 0 : entry.getValue();
            if (entry.getKey() != null && level > 0) {
                into.add(new EnchantmentContext(entry.getKey(), level, stack, slot));
            }
        }
    }

    private static Map<Enchantment, Integer> enchantmentsOf(ItemStack stack) {
        Map<Enchantment, Integer> merged = new java.util.LinkedHashMap<>(EnchantmentHelper.getEnchantments(stack));
        if (stack.getItem() instanceof EnchantedBookItem) {
            for (EnchantmentInstance instance : storedEnchantments(stack)) {
                merged.merge(instance.enchantment, instance.level, Math::max);
            }
        }
        return merged;
    }

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
