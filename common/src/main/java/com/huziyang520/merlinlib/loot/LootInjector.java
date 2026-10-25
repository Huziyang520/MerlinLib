// Turns the registered LootInjection rules into one loot pool per table and enchantment set.

package com.huziyang520.merlinlib.loot;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.LootInjection;
import com.huziyang520.merlinlib.platform.Services;
import com.huziyang520.merlinlib.platform.services.ILootBridge;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LootInjector {
    private static final List<LootInjection> INJECTIONS = new CopyOnWriteArrayList<>();
    private static boolean installed;

    private LootInjector() {
    }

    public static void add(LootInjection injection) {
        INJECTIONS.add(injection);
        Constants.LOG.debug("[MerlinLib] loot injection registered: {}", injection);
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        Services.LOOT.onTableLoad(LootInjector::inject);
        Constants.LOG.info("[MerlinLib] loot injection is active with {} rule(s)", INJECTIONS.size());
    }

    public static int size() {
        return INJECTIONS.size();
    }

    private static void inject(Identifier tableId, ILootBridge.PoolSink sink, HolderGetter.Provider registries) {
        boolean isServerTable = tableId.getNamespace().equals(Identifier.DEFAULT_NAMESPACE);
        Map<String, List<LootInjection>> grouped = new LinkedHashMap<>();
        for (int index = 0; index < INJECTIONS.size(); index++) {
            LootInjection injection = INJECTIONS.get(index);
            if (!isServerTable && !injection.allowsModdedTables()) {
                continue;
            }
            if (!injection.getTargetTables().contains(tableId.toString())) {
                continue;
            }
            grouped.computeIfAbsent(poolKey(injection), key -> new ArrayList<>()).add(injection);
        }
        for (List<LootInjection> rules : grouped.values()) {
            LootPool.Builder pool = buildPool(rules, registries);
            if (pool != null) {
                sink.add(pool);
            }
        }
    }

    private static String poolKey(LootInjection injection) {
        return new TreeSet<>(injection.getEnchantments()).toString();
    }

    private static LootPool.Builder buildPool(List<LootInjection> rules, HolderGetter.Provider registries) {
        LootPool.Builder pool = LootPool.lootPool();
        boolean anyEntry = false;
        for (LootInjection rule : rules) {
            for (String enchantmentId : rule.getEnchantments()) {
                Holder<Enchantment> enchantment = resolve(registries, enchantmentId);
                if (enchantment == null) {
                    Constants.LOG.warn("[MerlinLib] the loot injection for {} names {} into {}, which does not "
                            + "exist; the enchantment was skipped", enchantmentId, rule.getForm(),
                            rule.getTargetTables());
                    continue;
                }
                pool.add(buildEntry(rule, enchantment));
                anyEntry = true;
            }
        }
        return anyEntry ? pool : null;
    }

    private static UniformContainerBase.Builder<?> buildEntry(LootInjection injection,
                                                              Holder<Enchantment> enchantment) {
        Item item = injection.getForm() == LootInjection.Form.BOOK ? LootInjection.bookItem()
                : injection.getItem();
        UniformContainerBase.Builder<?> entry = LootItem.lootTableItem(item)
                .setWeight(injection.getWeight())
                .setQuality(injection.getQuality());
        if (injection.getChance() < 1.0F) {
            entry = entry.when(Holder.direct(
                    LootItemRandomChanceCondition.randomChance(injection.getChance()).build()));
        }
        return entry.apply(Holder.direct(
                EnchantRandomlyFunction.randomEnchantment().withEnchantment(enchantment).build()));
    }

    private static Holder<Enchantment> resolve(HolderGetter.Provider registries, String id) {
        Identifier parsed = Identifier.tryParse(id);
        if (parsed == null) {
            return null;
        }
        try {
            return registries.lookupOrThrow(Registries.ENCHANTMENT)
                    .get(ResourceKey.create(Registries.ENCHANTMENT, parsed))
                    .orElse(null);
        } catch (RuntimeException error) {
            Constants.LOG.warn("[MerlinLib] could not resolve the enchantment {}: {}", id, error.toString());
            return null;
        }
    }

    public static void clear() {
        INJECTIONS.clear();
    }

    public static List<LootInjection> injections() {
        return List.copyOf(INJECTIONS);
    }
}
