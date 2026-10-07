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
 *
 * <p>The rules are kept as data and read when a table loads, so registering one from a mod constructor works
 * for the very first world. Each rule becomes one pool whose entries are the enchantments it lists, each with
 * its own chance condition, weight and quality - the same shape a datapack author would write by hand, which
 * is what keeps the behaviour recognisable to anyone who knows vanilla loot tables.
 *
 * <p>A rule whose enchantment does not exist (a typo, or a mod that is not installed) is logged and skipped
 * instead of stopping the table from loading: a missing entry is a small problem, a broken table is a big one.
 *
 * <h2>What changed on 1.20.1</h2>
 *
 * <p><b>One loader hook instead of two.</b> The 26.3 line installed a Fabric
 * {@code LootTableEvents.MODIFY} listener and a NeoForge {@code LootTableLoadEvent} listener and kept the
 * narrower {@code HolderGetter.Provider} parameter so that one method signature covered both. On 1.20.1 there
 * is a single hook - Forge's {@code LootTableLoadEvent}, fired on the <b>game bus</b>
 * ({@code MinecraftForge.EVENT_BUS}), not the mod bus - and {@code ForgeLootBridge} hides it behind
 * {@link ILootBridge}. The registry parameter is therefore the full {@link HolderLookup.Provider} the bridge
 * hands out, which is what {@code RegistryAccess} implements.
 *
 * <p><b>The registries are no longer used to resolve enchantments.</b> On 26.3 an enchantment id was looked
 * up through {@code HolderGetter.Provider#lookupOrThrow(Registries.ENCHANTMENT)} so that a data driven
 * enchantment registry could be resolved per reload. 1.20.1 has no data driven enchantment registry:
 * enchantments are plain entries of {@link BuiltInRegistries#ENCHANTMENT}, frozen once at startup, so the id
 * is resolved straight against that registry. The parameter is still accepted because
 * {@link ILootBridge.TableListener} declares it and because it keeps this method's shape identical to the
 * 26.3 one; it is deliberately unused.
 *
 * <p><b>Enchantments are values, not holders.</b> {@code EnchantRandomlyFunction.Builder#withEnchantment}
 * takes an {@code Enchantment} on this version rather than a {@code Holder<Enchantment>}, and
 * {@code LootPool.lootPool()} has no {@code Holder.direct} wrapper to unwrap, so the whole
 * {@code Holder}-dereferencing layer of the 26.3 code disappears. This is a simplification, not a behaviour
 * change.
 *
 * <p><b>The entry builder's type changed.</b> 26.3 built entries through {@code UniformContainerBase.Builder},
 * which does not exist on 1.20.1; the counterpart here is
 * {@code LootPoolSingletonContainer.Builder}, the base of {@code LootItem} and the only type that declares
 * both {@code setWeight}/{@code setQuality} and {@code apply}. See {@link #buildEntry} for why the wider
 * {@code LootPoolEntryContainer.Builder} is not enough.
 *
 * <p><b>The enchanted book path is unchanged.</b> {@code EnchantRandomlyFunction} on 1.20.1 rolls a level
 * between {@code getMinLevel()} and {@code getMaxLevel()} and, when the stack is
 * {@code Items.BOOK}, replaces it with an {@code Items.ENCHANTED_BOOK} stack and calls
 * {@code EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(enchantment, level))} - verified
 * against the 1.20.1 bytecode. The 26.3 line did the same thing through the data component system's
 * equivalent path, so a {@link LootInjection.Form#BOOK} rule produces the same drop on both versions, at a
 * random level within the enchantment's own range.
 *
 * <p><b>What has no 1.20.1 equivalent.</b> Nothing in this class was dropped. The one capability the 26.3
 * line had and this class does not use is naming a pool (a NeoForge only extension); as on 26.3, the pools
 * built here are unnamed, because vanilla tables are built from unnamed pools and nothing looks a pool up
 * again by name.
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
     * <p>1.20.1 note: the builder is held as {@code LootPoolSingletonContainer.Builder<?>} rather than as
     * {@code LootPoolEntryContainer.Builder<?>} - the type {@code LootItem.lootTableItem} returns - because
     * only the singleton builder declares {@code apply}, which is what attaches the enchantment. The 26.3
     * line reached the same methods through {@code UniformContainerBase}, which does not exist on this
     * version; {@code LootPoolSingletonContainer} is its 1.20.1 counterpart and the base of {@code LootItem}.
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
     * <p>1.20.1 keeps enchantments in {@link BuiltInRegistries#ENCHANTMENT}, which is frozen once the
     * registry events have run - there is no per reload lookup to fail, so unlike the 26.3 version this
     * method cannot throw and needs no try block around the lookup itself. The table's own registries are
     * deliberately not consulted: on this version they are the same object.
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
