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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Turns the registered {@link LootInjection} rules into loot pools.
 *
 * <p>The rules are kept as data and read when a table loads, so registering one from a mod constructor works
 * for the very first world. Each rule becomes one pool whose entries are the enchantments it lists, each with
 * its own chance condition, weight and quality - the same shape a datapack author would write by hand, which
 * is what keeps the behaviour recognisable to anyone who knows vanilla loot tables.
 *
 * <p>A rule whose enchantment does not exist (a typo, or a mod that is not installed) is logged and skipped
 * instead of stopping the table from loading: a missing entry is a small problem, a broken table is a big one.
 *
 * <p>The registries argument is the narrow {@link HolderGetter.Provider} rather than
 * {@link HolderLookup.Provider}, because that is what the NeoForge event hands out; the Fabric event's wider
 * provider satisfies it, so one signature covers both.
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
     * @param registries the registries, used to resolve enchantment ids
     */
    private static void inject(Identifier tableId, ILootBridge.PoolSink sink, HolderGetter.Provider registries) {
        boolean isServerTable = tableId.getNamespace().equals(Identifier.DEFAULT_NAMESPACE);
        if (!isServerTable) {
            // Vanilla tables only: a datapack of another mod owns its own tables, and injecting into them from
            // a rule that names them would surprise that mod's author rather than help them.
            return;
        }
        for (int index = 0; index < INJECTIONS.size(); index++) {
            LootInjection injection = INJECTIONS.get(index);
            if (!injection.getTargetTables().contains(tableId.toString())) {
                continue;
            }
            LootPool.Builder pool = buildPool(injection, registries);
            if (pool != null) {
                sink.add(pool);
            }
        }
    }

    /**
     * Builds one pool for one rule.
     *
     * @param injection  the rule
     * @param registries the registries
     * @return the pool builder, or {@code null} when none of the rule's enchantments exist
     */
    private static LootPool.Builder buildPool(LootInjection injection, HolderGetter.Provider registries) {
        // No pool name: naming a pool is a NeoForge only extension, and vanilla loot tables are built from
        // unnamed pools. Nothing here needs to look a pool up again by name.
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

    /**
     * Builds the one entry of a pool.
     *
     * @param injection   the rule
     * @param enchantment the enchantment the drop carries
     * @return the entry builder
     */
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

    /**
     * Resolves an enchantment id against the registries of the table being loaded.
     *
     * @param registries the registries
     * @param id         the full enchantment id
     * @return the holder, or {@code null} when it is not registered
     */
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
            // A registry that is not open yet (an early reload, an odd loader) must not break the table load.
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
