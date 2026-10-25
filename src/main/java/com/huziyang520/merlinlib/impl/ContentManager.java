// Single source of truth for "what content exists right now".

package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigDirectory;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.ContentConfigLoader;
import com.huziyang520.merlinlib.config.ContentSnapshot;
import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.content.ContentReport;
import com.huziyang520.merlinlib.content.ContentSource;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.effect.EffectOverrides;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ContentManager {
    private static volatile ContentSnapshot cached;
    private static volatile String registeredFingerprint = "";
    private static volatile ContentReport lastReport = ContentReport.EMPTY;

    private ContentManager() {
    }

    public static ContentSnapshot current() {
        ContentSnapshot local = cached;
        return local == null ? ContentSnapshot.EMPTY : local;
    }

    public static ContentReport refresh() {
        synchronized (ContentManager.class) {
            ContentSnapshot built = build();
            ContentReport report = diff(current(), built);
            cached = built;
            registeredFingerprint = ConfigDirectory.fingerprint();
            lastReport = report;
            registerAll(built);
            logReport(report, built);
            return report;
        }
    }

    public static Optional<ContentReport> lastReport() {
        return Optional.of(lastReport);
    }

    public static String registeredFingerprint() {
        return registeredFingerprint;
    }

    private static ContentSnapshot build() {
        ContentSnapshot snapshot = ContentConfigLoader.load(
                EnchantmentRegistry.INSTANCE.apiEntries().values(),
                EnchantmentRegistry.INSTANCE.disabledIds()
        );

        if (ConfigManager.server().disableGeneratedEnchantments()
                && snapshot.enchantments().values().stream().anyMatch(draft -> draft.source() == ContentSource.CONFIG)) {
            Map<ResourceLocation, EnchantmentDraft> kept = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, EnchantmentDraft> entry : snapshot.enchantments().entrySet()) {
                if (entry.getValue().source() != ContentSource.CONFIG) {
                    kept.put(entry.getKey(), entry.getValue());
                }
            }
            Constants.LOG.info("[MerlinLib] config file enchantments are disabled by the server configuration, {} code registered one(s) kept", kept.size());
            snapshot = new ContentSnapshot(kept, snapshot.disabled(), snapshot.effectOverrides(), snapshot.errors());
        }

        EffectOverrides.setConfigLayer(snapshot.effectOverrides());
        return snapshot;
    }

    private static void registerAll(ContentSnapshot snapshot) {
        for (EnchantmentDraft draft : snapshot.enchantments().values()) {
            EnchantmentRegistry.submitRegistration(draft);
        }
    }

    private static ContentReport diff(ContentSnapshot previous, ContentSnapshot next) {
        List<ResourceLocation> added = new ArrayList<>();
        List<ResourceLocation> updated = new ArrayList<>();
        List<ResourceLocation> removed = new ArrayList<>();
        List<ResourceLocation> failed = new ArrayList<>();

        for (Map.Entry<ResourceLocation, EnchantmentDraft> entry : next.enchantments().entrySet()) {
            EnchantmentDraft before = previous.enchantments().get(entry.getKey());
            if (before == null) {
                added.add(entry.getKey());
            } else if (!sameDefinition(before, entry.getValue())) {
                updated.add(entry.getKey());
            }
        }
        for (ResourceLocation id : previous.enchantments().keySet()) {
            if (!next.enchantments().containsKey(id)) {
                removed.add(id);
            }
        }

        Set<ResourceLocation> broken = new LinkedHashSet<>();
        for (ContentError error : next.errors()) {
            if (!"-".equals(error.entryId())) {
                IdValidator.parse(error.entryId()).ifPresent(broken::add);
            }
        }
        failed.addAll(broken);
        return new ContentReport(List.copyOf(added), List.copyOf(updated), List.copyOf(removed), List.copyOf(failed), next.errors());
    }

    private static boolean sameDefinition(EnchantmentDraft left, EnchantmentDraft right) {
        return left.equals(right);
    }

    private static void logReport(ContentReport report, ContentSnapshot snapshot) {
        int total = snapshot.enchantments().size();
        if (!report.hasChanges() && !report.hasErrors()) {
            Constants.LOG.info("[MerlinLib] content loaded, {} enchantment(s) active, nothing unexpected", total);
            return;
        }
        Constants.LOG.info("[MerlinLib] content loaded, {} enchantment(s) active\n{}", total, report.describe());
    }
}
