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
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Single source of truth for "what content exists right now".
 *
 * <p>The snapshot is rebuilt lazily whenever the config files change or the java api registers
 * something new. That removes any ordering requirement between the datapack reader, the resource
 * reload listener and the registry loader: whoever asks first builds the snapshot and everybody else
 * reuses it.
 */
public final class ContentManager {

    private static volatile ContentSnapshot cached;
    private static volatile String cachedFingerprint = "";
    private static volatile ContentSnapshot reported;
    private static volatile ContentReport lastReport = ContentReport.EMPTY;

    private ContentManager() {
    }

    /**
     * @return the current snapshot, rebuilding it when the config files or the api changed.
     */
    public static ContentSnapshot current() {
        String fingerprint = ConfigDirectory.fingerprint() + "|api=" + EnchantmentRegistry.INSTANCE.revision();
        ContentSnapshot local = cached;
        if (local != null && fingerprint.equals(cachedFingerprint)) {
            return local;
        }
        synchronized (ContentManager.class) {
            if (cached != null && fingerprint.equals(cachedFingerprint)) {
                return cached;
            }
            ContentSnapshot built = build();
            cached = built;
            cachedFingerprint = fingerprint;
            return built;
        }
    }

    /**
     * Forces a rebuild and returns what changed since the last time this method ran. Called after
     * startup and after every {@code /reload}.
     *
     * @return the change report, already logged
     */
    public static ContentReport refresh() {
        ContentSnapshot previous;
        ContentSnapshot next;
        synchronized (ContentManager.class) {
            previous = reported == null ? ContentSnapshot.EMPTY : reported;
            ContentSnapshot built = build();
            cached = built;
            cachedFingerprint = ConfigDirectory.fingerprint() + "|api=" + EnchantmentRegistry.INSTANCE.revision();
            reported = built;
            next = built;
        }
        ContentReport report = diff(previous, next);
        lastReport = report;
        logReport(report, next);
        return report;
    }

    /**
     * @return the report produced by the most recent {@link #refresh()}, empty before the first one.
     */
    public static Optional<ContentReport> lastReport() {
        return Optional.of(lastReport);
    }

    private static ContentSnapshot build() {
        ContentSnapshot snapshot = ContentConfigLoader.load(
                EnchantmentRegistry.INSTANCE.apiEntries().values(),
                EnchantmentRegistry.INSTANCE.disabledIds()
        );

        // The generated content can be switched off entirely; code api registrations stay unaffected.
        if (ConfigManager.server().disableGeneratedEnchantments()
                && snapshot.enchantments().values().stream().anyMatch(draft -> draft.source() == ContentSource.CONFIG)) {
            Map<Identifier, EnchantmentDraft> kept = new LinkedHashMap<>();
            for (Map.Entry<Identifier, EnchantmentDraft> entry : snapshot.enchantments().entrySet()) {
                if (entry.getValue().source() != ContentSource.CONFIG) {
                    kept.put(entry.getKey(), entry.getValue());
                }
            }
            Constants.LOG.info("[MerlinLib] generated enchantments are disabled by the server configuration, {} code registered one(s) kept", kept.size());
            snapshot = new ContentSnapshot(kept, snapshot.disabled(), snapshot.effectOverrides(), snapshot.errors());
        }

        EffectOverrides.setConfigLayer(snapshot.effectOverrides());
        return snapshot;
    }

    private static ContentReport diff(ContentSnapshot previous, ContentSnapshot next) {
        List<Identifier> added = new ArrayList<>();
        List<Identifier> updated = new ArrayList<>();
        List<Identifier> removed = new ArrayList<>();
        List<Identifier> failed = new ArrayList<>();

        for (Map.Entry<Identifier, EnchantmentDraft> entry : next.enchantments().entrySet()) {
            EnchantmentDraft before = previous.enchantments().get(entry.getKey());
            if (before == null) {
                added.add(entry.getKey());
            } else if (!sameDefinition(before, entry.getValue())) {
                updated.add(entry.getKey());
            }
        }
        for (Identifier id : previous.enchantments().keySet()) {
            if (!next.enchantments().containsKey(id)) {
                removed.add(id);
            }
        }

        Set<Identifier> broken = new LinkedHashSet<>();
        for (ContentError error : next.errors()) {
            if (!"-".equals(error.entryId())) {
                broken.add(Identifier.parse(error.entryId()));
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
            Constants.LOG.info("[MerlinLib] content reloaded, {} enchantment(s) active, nothing changed", total);
            return;
        }
        Constants.LOG.info("[MerlinLib] content reloaded, {} enchantment(s) active\n{}", total, report.describe());
    }
}
