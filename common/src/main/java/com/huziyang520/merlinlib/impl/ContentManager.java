// Resolves which enchantments MerlinLib serves right now: exactly the ones registered through the code API.

package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.content.ContentReport;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class ContentManager {
    private static volatile Map<Identifier, EnchantmentDraft> cached;
    private static volatile long cachedRevision = Long.MIN_VALUE;
    private static volatile Map<Identifier, EnchantmentDraft> reported;
    private static volatile ContentReport lastReport = ContentReport.EMPTY;

    private ContentManager() {
    }

    public static Map<Identifier, EnchantmentDraft> current() {
        long revision = EnchantmentRegistry.INSTANCE.revision();
        Map<Identifier, EnchantmentDraft> local = cached;
        if (local != null && revision == cachedRevision) {
            return local;
        }
        synchronized (ContentManager.class) {
            if (cached == null || revision != cachedRevision) {
                cached = build();
                cachedRevision = revision;
            }
            return cached;
        }
    }

    public static ContentReport refresh() {
        ContentValidator.install();
        Map<Identifier, EnchantmentDraft> previous;
        Map<Identifier, EnchantmentDraft> next;
        synchronized (ContentManager.class) {
            previous = reported == null ? Map.of() : reported;
            next = build();
            cached = next;
            cachedRevision = EnchantmentRegistry.INSTANCE.revision();
            reported = next;
        }
        ContentReport report = diff(previous, next);
        lastReport = report;
        logReport(report, next);
        return report;
    }

    public static Optional<ContentReport> lastReport() {
        return Optional.of(lastReport);
    }

    private static Map<Identifier, EnchantmentDraft> build() {
        if (ConfigManager.server().disableGeneratedEnchantments()) {
            Constants.LOG.info("[MerlinLib] the generated datapack is switched off in server.toml, no enchantment is served");
            return Map.of();
        }
        Map<Identifier, EnchantmentDraft> active = new TreeMap<>(Comparator.comparing(Identifier::toString));
        active.putAll(EnchantmentRegistry.INSTANCE.apiEntries());
        active.keySet().removeAll(EnchantmentRegistry.INSTANCE.disabledIds());
        return Collections.unmodifiableMap(active);
    }

    private static ContentReport diff(Map<Identifier, EnchantmentDraft> previous,
                                      Map<Identifier, EnchantmentDraft> next) {
        List<Identifier> added = new ArrayList<>();
        List<Identifier> updated = new ArrayList<>();
        List<Identifier> removed = new ArrayList<>();
        for (Map.Entry<Identifier, EnchantmentDraft> entry : next.entrySet()) {
            EnchantmentDraft before = previous.get(entry.getKey());
            if (before == null) {
                added.add(entry.getKey());
            } else if (!before.equals(entry.getValue())) {
                updated.add(entry.getKey());
            }
        }
        for (Identifier id : previous.keySet()) {
            if (!next.containsKey(id)) {
                removed.add(id);
            }
        }
        return new ContentReport(List.copyOf(added), List.copyOf(updated), List.copyOf(removed), List.of(), List.of());
    }

    private static void logReport(ContentReport report, Map<Identifier, EnchantmentDraft> active) {
        if (!report.hasChanges()) {
            Constants.LOG.info("[MerlinLib] content reloaded, {} enchantment(s) active, nothing changed", active.size());
            return;
        }
        Constants.LOG.info("[MerlinLib] content reloaded, {} enchantment(s) active\n{}", active.size(), report.describe());
    }
}
