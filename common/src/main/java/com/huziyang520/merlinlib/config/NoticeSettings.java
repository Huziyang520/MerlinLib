package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.notice.NoticeEntry;
import com.huziyang520.merlinlib.notice.NoticePreferences;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Per mod switches for the join notices, in {@code config/MerlinLib/notices.toml}.
 *
 * <h2>Why a file of its own</h2>
 *
 * <p>The mods that send a notice are only known at runtime, so the switches cannot be declared one by one in
 * {@code server.toml} - and the configuration writers of both loaders can only put a value back into a key they
 * were told about in advance. An earlier build therefore kept every choice inside one string,
 * {@code notices.per_mod = "modid=true;other=false"}, which worked but could only be edited by hand and showed
 * up in every settings screen as an opaque text field.
 *
 * <p>Here the keys are the mods themselves, and the file is written whole on every change: the set of keys is
 * only ever as large as the set of installed mods, the order is alphabetical, and nothing in the file is hand
 * written enough to be worth preserving. Reading goes through {@link TomlFile}, which is the TOML reader this
 * mod already ships, so no new dependency is pulled in.
 *
 * <h2>Migration</h2>
 *
 * <p>The old string is imported once, guarded by the {@code imported_legacy} marker in this same file: the first
 * read merges it in and writes the marker as {@code true}, and every later read is a plain file read. The old
 * {@code notices.per_mod} line is removed from {@code server.toml} when this happens, because a value that two
 * places disagree about is worse than no value at all.
 */
public final class NoticeSettings {

    /** The section the per mod switches live in. */
    public static final String SECTION = "mods";

    /** The retired key in {@code server.toml} that the first read imports and then removes. */
    public static final String LEGACY_KEY = "notices.per_mod";

    /** Marker that makes the import a one-off. */
    private static final String MARKER = "imported_legacy";

    /** Comments written above the marker, shared by the template and by every whole file write. */
    private static final String HEADER = """
            # MerlinLib join notice switches, one per mod.
            # A mod that is not listed here uses the default it declared when it registered its notice.
            # `true` sends that mod's notices when a player joins, `false` keeps them quiet.
            # The master switch for all of them is notices.enabled in server.toml, and the same switches can be
            # changed in game with /merlinlib notices.

            """;

    /** Template written when the file does not exist yet; the marker starts as {@code false}. */
    private static final String DEFAULTS = HEADER + MARKER + " = false\n\n[" + SECTION + "]\n";

    private NoticeSettings() {
    }

    /** @return {@code config/MerlinLib/notices.toml} */
    public static Path path() {
        return ConfigDirectory.root().resolve("notices.toml");
    }

    /**
     * Reads the stored switches, importing the retired string on the first read.
     *
     * @return mod id to whether its notices are sent, in file order
     */
    public static synchronized Map<String, Boolean> snapshot() {
        TomlFile file = TomlFile.read(path(), DEFAULTS);
        Map<String, Boolean> values = parse(file.keys(SECTION));
        if (!file.getBoolean(MARKER, false)) {
            Map<String, Boolean> imported = legacyValues();
            if (!imported.isEmpty()) {
                // A switch this file already holds wins: it was chosen after the file existed, and the string in
                // server.toml may have been left behind by an earlier build.
                for (Map.Entry<String, Boolean> entry : imported.entrySet()) {
                    values.putIfAbsent(entry.getKey(), entry.getValue());
                }
                Constants.LOG.info("[MerlinLib] imported {} per mod notice switch(es) from {} into {}",
                        imported.size(), LEGACY_KEY, path().getFileName());
            }
            writeAll(values, true);
            dropLegacyKey();
        }
        return values;
    }

    /**
     * @param owner    the mod id
     * @param fallback the value to use when nothing is stored for that mod
     * @return whether that mod's notices are sent
     */
    public static boolean isEnabled(String owner, boolean fallback) {
        return snapshot().getOrDefault(owner, fallback);
    }

    /**
     * @param overrides the switches, read once with {@link #snapshot()}
     * @param entry     the notice
     * @return whether that notice is allowed to be sent
     */
    public static boolean isEnabled(Map<String, Boolean> overrides, NoticeEntry entry) {
        return overrides.getOrDefault(entry.owner(), entry.enabledByDefault());
    }

    /**
     * Stores one switch and rewrites the whole file.
     *
     * @param owner   the mod id
     * @param enabled whether that mod's notices are sent
     */
    public static synchronized void set(String owner, boolean enabled) {
        Map<String, Boolean> values = snapshot();
        values.put(owner, enabled);
        writeAll(values, true);
    }

    /**
     * Rewrites the file from the given switches.
     *
     * <p>The whole file is written rather than one line patched: the keys are runtime mod ids, the file carries no
     * comments a player would miss, and a full write is what keeps a removed switch from lingering as a line
     * nothing reads any more.
     *
     * @param values          mod id to whether its notices are sent
     * @param importedLegacy  the state of the migration marker
     */
    private static void writeAll(Map<String, Boolean> values, boolean importedLegacy) {
        StringBuilder text = new StringBuilder(HEADER);
        text.append(MARKER).append(" = ").append(importedLegacy).append("\n\n");
        text.append('[').append(SECTION).append("]\n");
        for (Map.Entry<String, Boolean> entry : new TreeMap<>(values).entrySet()) {
            text.append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n');
        }
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            // A read only directory must not break a settings screen: the switches simply stay as they were.
            Constants.LOG.warn("[MerlinLib] could not write {}: {}", file, exception.getMessage());
        }
    }

    /**
     * Removes the retired {@code notices.per_mod} line from {@code server.toml}.
     *
     * <p>Removed whether or not a loader store is installed: the key is retired from the declared options, so
     * nothing writes it back, and a line that no screen and no code reads any more is only misleading
     * documentation. When NeoForge has already renamed the file to {@code server.toml.imported} there is no line
     * to remove here - that copy is kept on purpose, as the record of what the player had set up.
     */
    private static void dropLegacyKey() {
        TomlFile.removeKey(ServerConfig.path(), LEGACY_KEY);
    }

    /**
     * Reads the retired {@code notices.per_mod} string from wherever it may still be.
     *
     * <p>Three places, because a switch made under the old layout can be in any of them: the live server
     * configuration (which on Fabric is the TOML file itself), that file as it is on disk, and the copy NeoForge
     * renames an imported file to. Only the first one that has a value per mod matters - they are merged, and the
     * first mention of a mod wins.
     *
     * @return mod id to the choice the old string held
     */
    private static Map<String, Boolean> legacyValues() {
        Map<String, Boolean> found = new LinkedHashMap<>();
        collect(found, ConfigManager.server().noticePerMod());
        for (Path file : List.of(ServerConfig.path(),
                ServerConfig.path().resolveSibling("server.toml.imported"))) {
            if (!Files.isRegularFile(file)) {
                continue;
            }
            collect(found, TomlFile.read(file, ServerConfig.defaults()).getString(LEGACY_KEY, ""));
        }
        return found;
    }

    private static void collect(Map<String, Boolean> into, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (Map.Entry<String, String> entry : NoticePreferences.parseLegacy(raw).entrySet()) {
            String value = entry.getValue();
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                into.putIfAbsent(entry.getKey(), Boolean.parseBoolean(value));
            }
        }
    }

    private static Map<String, Boolean> parse(Map<String, String> raw) {
        Map<String, Boolean> values = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String value = entry.getValue();
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                values.put(entry.getKey(), Boolean.parseBoolean(value));
            }
        }
        return values;
    }
}
