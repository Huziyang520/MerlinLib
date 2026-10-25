// Reports language keys, supported item tags and exclusive groups that cannot resolve for the enchantments MerlinLib serves.

package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ContentValidator {
    private static boolean installed;

    private ContentValidator() {
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        Services.LIFECYCLE.onServerStarted(ContentValidator::validate);
    }

    public static List<String> validate(MinecraftServer server) {
        List<String> problems = new ArrayList<>();
        if (server == null) {
            return problems;
        }
        Registry<Enchantment> enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Registry<Item> items = server.registryAccess().lookupOrThrow(Registries.ITEM);
        Map<Identifier, EnchantmentDraft> served = ContentManager.current();
        for (Identifier id : served.keySet()) {
            Optional<Holder.Reference<Enchantment>> registered = enchantments.get(id);
            if (registered.isEmpty()) {
                problems.add(id + ": declared through the api but absent from the enchantment registry, so the generated datapack entry did not load");
                continue;
            }
            Enchantment enchantment = registered.get().value();
            checkLanguageKey(id, enchantment, problems);
            checkSupportedItems(id, enchantment, items, problems);
            checkExclusiveSet(id, registered.get(), enchantment, problems);
        }
        if (problems.isEmpty()) {
            Constants.LOG.info("[MerlinLib] content validation passed for {} enchantment(s)", served.size());
            return problems;
        }
        Constants.LOG.warn("[MerlinLib] content validation found {} problem(s); nothing was blocked", problems.size());
        for (String problem : problems) {
            Constants.LOG.warn("[MerlinLib] content validation: {}", problem);
        }
        return problems;
    }

    private static void checkLanguageKey(Identifier id, Enchantment enchantment, List<String> problems) {
        String expected = EnchantmentDraft.defaultTranslationKey(id);
        Component description = enchantment.description();
        if (description == null || !(description.getContents() instanceof TranslatableContents contents)) {
            problems.add(id + ": the description is not a translatable component, so neither " + expected + " nor "
                    + expected + ".desc can be looked up");
            return;
        }
        String key = contents.getKey();
        if (!key.equals(expected)) {
            problems.add(id + ": the description is translated by " + key + " instead of " + expected + " (plus "
                    + expected + ".desc)");
        }
    }

    private static void checkSupportedItems(Identifier id, Enchantment enchantment, Registry<Item> items,
                                            List<String> problems) {
        HolderSet<Item> supported = enchantment.getSupportedItems();
        if (supported == null) {
            problems.add(id + ": supported_items is missing");
            return;
        }
        Optional<TagKey<Item>> tag = supported.unwrapKey();
        if (tag.isEmpty()) {
            if (supported.size() == 0) {
                problems.add(id + ": supported_items lists no item at all");
            }
            return;
        }
        if (!items.getTagOrEmpty(tag.get()).iterator().hasNext()) {
            problems.add(id + ": the supported_items tag " + tag.get().location()
                    + " is unknown or empty, so no item can ever carry this enchantment");
        }
    }

    private static void checkExclusiveSet(Identifier id, Holder<Enchantment> holder, Enchantment enchantment,
                                          List<String> problems) {
        HolderSet<Enchantment> exclusive = enchantment.exclusiveSet();
        if (exclusive == null) {
            return;
        }
        if (exclusive.contains(holder)) {
            problems.add(id + ": its exclusive set contains itself, so it can never be combined twice");
        }
        Optional<TagKey<Enchantment>> tag = exclusive.unwrapKey();
        if (tag.isPresent() && exclusive.size() == 0) {
            problems.add(id + ": the exclusive set tag " + tag.get().location() + " is unknown or empty");
        }
    }
}
