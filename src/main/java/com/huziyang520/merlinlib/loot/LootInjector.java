package com.huziyang520.merlinlib.loot;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.LootInjection;
import com.huziyang520.merlinlib.platform.Services;
import com.huziyang520.merlinlib.platform.services.ILootBridge;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Turns the registered {@link LootInjection} rules into loot pools.
 */
public final class LootInjector {

    /** The registered rules, in registration order. */
    private static final List<LootInjection> INJECTIONS = new CopyOnWriteArrayList<>();

    /** Whether the loader hook has been installed; installing twice would double every injection. */
    private static boolean installed;

    private LootInjector() {
    }

    /**
     * Registers a rule.
     *
     * @param injection the rule
 */
    public static void add(LootInjection injection) {
        INJECTIONS.add(injection);
        Constants.LOG.debug("[MerlinLib] loot injection registered: {}", injection);
    }

    /**
     * Installs the loader hook that feeds the rules into loading tables.
     *
     * <p>Called once from MerlinLib's own initialisation; a dependent mod only registers rules.
 */
    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        Services.LOOT.onTableLoad(LootInjector::inject);
        Constants.LOG.info("[MerlinLib] loot injection is active with {} rule(s)", INJECTIONS.size());
    }

    /** @return how many rules are registered, for diagnostics */
    public static int size() {
        return INJECTIONS.size();
    }

    /**
     * Adds the pools of every rule that targets this table.
     *
     * @param tableId    the table being loaded
     * @param sink       where the pools go
     * @param registries the registries; accepted to match {@link ILootBridge.TableListener} and deliberately
     *                   unused on 1.20.1, where enchantments live in a single built-in registry
 */
    private static void inject(ResourceLocation tableId, ILootBridge.PoolSink sink,
                               HolderLookup.Provider registries) {
        boolean isServerTable = tableId.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE);
        for (int index = 0; index < INJECTIONS.size(); index++) {
            LootInjection injection = INJECTIONS.get(index);
            if (!isServerTable && !injection.allowsModdedTables()) {
                // A datapack of another mod owns its own tables, and injecting into them from a rule that
                // names them would surprise that mod's author rather than help them. Only a rule that asks
                // for it explicitly gets through.
                continue;
            }
            if (!injection.getTargetTables().contains(tableId.toString())) {
                continue;
            }
            LootPool.Builder pool = buildPool(injection);
            if (pool != null) {
                sink.add(pool);
            }
        }
    }

    /**
     * Builds one pool for one rule.
     *
     * @param injection the rule
     * @return the pool builder, or {@code null} when none of the rule's enchantments exist
 */
    private static LootPool.Builder buildPool(LootInjection injection) {
        // No pool name: naming a pool is a NeoForge only extension, and vanilla loot tables are built from
        // unnamed pools. Nothing here needs to look a pool up again by name.
        LootPool.Builder pool = LootPool.lootPool();
        boolean anyEntry = false;
        for (String enchantmentId : injection.getEnchantments()) {
            Enchantment enchantment = resolve(enchantmentId);
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

    /**
     * Builds the one entry of a pool.
 *
     * @param injection   the rule
     * @param enchantment the enchantment the drop carries
     * @return the entry builder
 */
    private static LootPoolSingletonContainer.Builder<?> buildEntry(LootInjection injection,
                                                                   Enchantment enchantment) {
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

    /**
     * Resolves an enchantment id against the built-in enchantment registry.
 *
     * @param id the full enchantment id
     * @return the enchantment, or {@code null} when it is not registered or the id is not a legal id
 */
    private static Enchantment resolve(String id) {
        // ResourceLocation.tryParse, not the ResourceLocation(String) constructor: the id comes from a hand
        // written config file or from another mod's registration, and a malformed one must be reported as a
        // skipped entry rather than thrown out of the middle of a loot table load.
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            return null;
        }
        try {
            return BuiltInRegistries.ENCHANTMENT.get(parsed);
        } catch (RuntimeException error) {
            // Defensive: a registry that is somehow not open yet must not break the table load.
            Constants.LOG.warn("[MerlinLib] could not resolve the enchantment {}: {}", id, error.toString());
            return null;
        }
    }

    /**
     * Clears every rule; used by tests and by a full reload of a dependent mod's configuration.
     *
     * <p>The loader hook stays installed: dropping it would need a way to remove a listener, and an installed
     * hook with no rules does nothing.
 */
    public static void clear() {
        INJECTIONS.clear();
    }

    /** @return the registered rules, for diagnostics */
    public static List<LootInjection> injections() {
        return List.copyOf(INJECTIONS);
    }
}
