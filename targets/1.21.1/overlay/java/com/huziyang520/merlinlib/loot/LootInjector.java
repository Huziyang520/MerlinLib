// Turns the registered LootInjection rules into loot pools.

package com.huziyang520.merlinlib.loot;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.LootInjection;
import com.huziyang520.merlinlib.platform.Services;
import com.huziyang520.merlinlib.platform.services.ILootBridge;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import java.util.List;
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

    private static void inject(ResourceLocation tableId, ILootBridge.PoolSink sink, HolderGetter.Provider registries) {
        boolean isServerTable = tableId.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE);
        for (int index = 0; index < INJECTIONS.size(); index++) {
            LootInjection injection = INJECTIONS.get(index);
            if (!isServerTable && !injection.allowsModdedTables()) {
                continue;
            }
            if (!injection.getTargetTables().contains(tableId.toString())) {
                continue;
            }
            LootPool.Builder pool = buildPool(injection, registries);
            if (pool != null) {
                sink.add(pool);
            }
        }
    }

    private static LootPool.Builder buildPool(LootInjection injection, HolderGetter.Provider registries) {
        LootPool.Builder pool = LootPool.lootPool();
        boolean anyEntry = false;
        for (String enchantmentId : injection.getEnchantments()) {
            Holder<Enchantment> enchantment = resolve(registries, enchantmentId);
            if (enchantment == null) {
                Constants.LOG.warn("[MerlinLib] the loot injection for {} names {} into {}, which does not "
                        + "exist; the enchantment was skipped", enchantmentId, injection.getForm(),
                        injection.getTargetTables());
                continue;
            }
            pool.add(buildEntry(injection, enchantment));
            anyEntry = true;
        }
        return anyEntry ? pool : null;
    }

    private static LootPoolSingletonContainer.Builder<?> buildEntry(LootInjection injection,
                                                              Holder<Enchantment> enchantment) {
        Item item = injection.getForm() == LootInjection.Form.BOOK ? LootInjection.bookItem()
                : injection.getItem();
        LootPoolSingletonContainer.Builder<?> entry = LootItem.lootTableItem(item)
                .setWeight(injection.getWeight())
                .setQuality(injection.getQuality());
        if (injection.getChance() < 1.0F) {
            entry = entry.when(LootItemRandomChanceCondition.randomChance(injection.getChance()));
        }
        return entry.apply(EnchantRandomlyFunction.randomEnchantment().withEnchantment(enchantment));
    }

    private static Holder<Enchantment> resolve(HolderGetter.Provider registries, String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
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
