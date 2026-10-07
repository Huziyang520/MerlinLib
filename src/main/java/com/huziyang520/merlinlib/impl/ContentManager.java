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

/**
 * Single source of truth for "what content exists right now".
 *
 * <h2>What changed from the 26.3 line</h2>
 *
 * <p>On 26.3 the snapshot was rebuilt lazily whenever the config files changed, which was what let
 * {@code /reload} pick up an edited file without a restart: the reader was a resource reload
 * listener and the registry it fed was data driven.
 *
 * <p>1.20.1 registers an enchantment once, during the mod event bus phase, into a registry that is
 * frozen immediately afterwards. Nothing can add to it later, so this class reads the content exactly
 * once - {@link #refresh()} is called from the bootstrap and that is the only call that can produce a
 * registration. {@link #current()} still exists and still answers, because the command, the item
 * editor and the diagnostics all ask it what is registered.
 *
 * <p>The fingerprint is still computed and logged. It is no longer a cache key, but it is the honest
 * way to tell an author whether the files on disk are the ones this process registered - which is
 * precisely the question a player asks after editing a definition on a version that needs a restart.
 */
public final class ContentManager {

    private static volatile ContentSnapshot cached;
    private static volatile String registeredFingerprint = "";
    private static volatile ContentReport lastReport = ContentReport.EMPTY;

    private ContentManager() {
    }

    /**
     * @return the snapshot this process registered, or an empty one before {@link #refresh()} ran.
     */
    public static ContentSnapshot current() {
        ContentSnapshot local = cached;
        return local == null ? ContentSnapshot.EMPTY : local;
    }

    /**
     * Reads every content definition and registers what it finds.
     *
     * <p>Called exactly once, from the mod bootstrap, before the registry event fires. Calling it
     * again would read the files again and register a second time, which the registry rejects; the
     * method is therefore guarded rather than merely documented as "call once".
     *
     * @return the change report, already logged
     */
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

    /**
     * @return the report produced by the most recent {@link #refresh()}, empty before the first one.
     */
    public static Optional<ContentReport> lastReport() {
        return Optional.of(lastReport);
    }

    /**
     * @return the fingerprint of the content files as they were when this process registered them.
     *
     * <p>Compared against the live {@link ConfigDirectory#fingerprint()} by the command, so an author
     * who edited a file and has not restarted is told exactly that instead of being shown a
     * definition the game is not running.
     */
    public static String registeredFingerprint() {
        return registeredFingerprint;
    }

    private static ContentSnapshot build() {
        ContentSnapshot snapshot = ContentConfigLoader.load(
                EnchantmentRegistry.INSTANCE.apiEntries().values(),
                EnchantmentRegistry.INSTANCE.disabledIds()
        );

        // The generated content can be switched off entirely; code api registrations stay unaffected.
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

    /**
     * Writes every effective enchantment into the game registry, through the loader bridge.
     *
     * <p>This is the step that has no counterpart on the 26.3 line: there, "the snapshot exists" and
     * "the enchantment exists" were the same statement, because the snapshot was serialised into a
     * data pack that the game then read. Here they are two statements, and this is the second one.
     *
     * @param snapshot the effective content
     */
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
