package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * The per mod switches of the join notices, stored in their own file.
 *
 * <h2>Why not a configuration key</h2>
 *
 * <p>The list of mods is only known at runtime: it is whatever business mods registered through
 * {@code MerlinApi.notices()} before the server started. A configuration option has to exist in the
 * specification before that, so the switches used to live in one hand written string
 * ({@code notices.per_mod}, written as {@code modid=true;other=false}) which the player had to edit by
 * hand - and which was drawn as a text box on the configuration screen. A page of switches is what the
 * feature actually needs, and a page needs a store that can hold keys invented at runtime, which the
 * loader's file format cannot: it writes back exactly the options it declared and drops anything else.
 *
 * <h2>The file</h2>
 *
 * <pre>
 * [general]
 * legacy_imported = true
 *
 * [mods]
 * merlinlib = true
 * someothermod = false
 * </pre>
 *
 * <p>One line per mod, keyed by the mod id - the namespace of the notices it registered, which is also
 * what {@code NoticeEntry#owner()} answers. A mod that is absent uses the default it declared when it
 * registered, so a new mod needs no entry and an uninstalled one leaves a harmless line behind. The whole
 * file is rewritten on every save, which is what keeps the order stable and a hand edited file tidy.
 *
 * <h2>Carrying over the old value</h2>
 *
 * <p>The old string is read once, from the common configuration file as the previous build wrote it, and
 * carried into this file. The {@code legacy_imported} marker is what makes that a one time event: the
 * second launch finds it set and does not look at the old key at all. A mod that already has a line here
 * keeps it, so re-running the import can never overwrite a choice made on the page.
 *
 * <p>The import has to happen <em>before</em> the loader reads the common file: the loader rewrites a file
 * whose entries no longer match the specification, and {@code notices.per_mod} is deliberately no longer
 * declared there. {@link #captureLegacy()} is therefore called from the mod constructor, and the lazy
 * {@link #load()} below covers every other entry point.
 */
public final class NoticeSettings {

    /** Name of the file inside {@link ConfigDirectory#root()}. */
    public static final String FILE_NAME = "notices.toml";

    /** The section holding one {@code modid = true|false} line per mod. */
    private static final String MODS = "mods";

    /** The marker saying the old {@code notices.per_mod} value has already been carried over. */
    private static final String IMPORTED = "general.legacy_imported";

    /** The old key, read once and never written again. */
    private static final String LEGACY_KEY = "notices.per_mod";

    /** The file the previous build wrote that key to. Spelled out rather than taken from the spec, so this
     * class stays independent of the loader and can be read before any specification exists. */
    private static final String LEGACY_FILE = Constants.MOD_ID + "-common.toml";

    /** The file the build before the specification wrote it to. */
    private static final String PRE_SPEC_LEGACY_FILE = "server.toml";

    /** Written when the file is missing, so the format is documented where it is used. */
    private static final String DEFAULT_TEXT = String.join("\n",
            "# MerlinLib join notice switches.",
            "#",
            "# One line per mod below [mods]: \"modid = true\" shows that mod's notices when a player joins,",
            "# \"modid = false\" hides them. A mod that is not listed uses the default it declared when it",
            "# registered its notices. This file is written by the \"Join notices\" page, which",
            "# /merlinlib notices opens; editing it by hand works just as well.",
            "#",
            "# legacy_imported records that the old notices.per_mod string of the common configuration",
            "# file has been carried over. It is written by the mod; leaving it alone is the whole point.",
            "",
            "[general]",
            "legacy_imported = false",
            "",
            "[mods]",
            "");

    /** The switches, in the order they are written. */
    private static final Map<String, Boolean> SWITCHES = new LinkedHashMap<>();

    /** Whether the file has been read in this process. */
    private static boolean loaded;

    private NoticeSettings() {
    }

    /**
     * @return {@code <config>/MerlinLib/notices.toml}
     */
    public static Path file() {
        return ConfigDirectory.root().resolve(FILE_NAME);
    }

    /**
     * Reads the file, carrying the old value over the first time it is read.
     *
     * <p>Safe to call from anywhere: only the first call does any work, and it runs the import when the
     * file has no marker yet.
     */
    public static synchronized void load() {
        if (loaded) {
            return;
        }
        // Set first: the import below calls back into save(), and a second read must not re-enter here.
        loaded = true;

        TomlFile file = TomlFile.read(file(), DEFAULT_TEXT);
        for (Map.Entry<String, String> entry : file.entries().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(MODS + ".")) {
                String owner = key.substring(MODS.length() + 1);
                if (!owner.isEmpty()) {
                    SWITCHES.put(owner, parse(entry.getValue(), true));
                }
            }
        }

        if (!file.getBoolean(IMPORTED, false)) {
            int imported = importLegacy();
            // Written either way: the marker is part of the file from now on, and a fresh install gets the
            // documented layout instead of the text it was created from.
            save();
            if (imported > 0) {
                Constants.LOG.info("[MerlinLib] carried {} per mod join notice switch(es) over from {} into {}",
                        imported, LEGACY_KEY, file());
            }
        }
    }

    /**
     * Carries the old {@code notices.per_mod} value over before the loader touches the common file.
     *
     * <p>Called from the mod constructor, which runs before the loader reads the configuration files: the
     * loader drops a key the specification no longer declares while it loads, so anything read after that
     * point would find nothing. Doing it here also means the very first launch of this build already
     * remembers what the player had chosen.
     */
    public static synchronized void captureLegacy() {
        load();
    }

    /**
     * @param owner the mod id, the namespace of the notices it registered
     * @return the stored choice, or {@code null} when the mod has never been configured
     */
    public static synchronized Boolean get(String owner) {
        load();
        return SWITCHES.get(owner);
    }

    /**
     * @param owner    the mod id
     * @param fallback the mod's own registered default, used when no line mentions it
     * @return whether that mod's notices are shown
     */
    public static boolean isEnabled(String owner, boolean fallback) {
        load();
        Boolean stored = SWITCHES.get(owner);
        return stored == null ? fallback : stored;
    }

    /**
     * Records one choice. Nothing is written until {@link #save()} is called, so a page of switches can be
     * applied at once.
     *
     * @param owner   the mod id
     * @param enabled the choice
     */
    public static synchronized void set(String owner, boolean enabled) {
        load();
        SWITCHES.put(owner, enabled);
    }

    /**
     * @return a copy of every stored switch, for a page that shows them
     */
    public static synchronized Map<String, Boolean> switches() {
        load();
        return new LinkedHashMap<>(SWITCHES);
    }

    /**
     * Rewrites the whole file: the header, the marker and one line per mod in alphabetical order.
     */
    public static synchronized void save() {
        Map<String, Boolean> sorted = new TreeMap<>(SWITCHES);
        StringBuilder text = new StringBuilder();
        text.append("# MerlinLib join notice switches.\n");
        text.append("#\n");
        text.append("# One line per mod below [mods]: \"modid = true\" shows that mod's notices when a player joins,\n");
        text.append("# \"modid = false\" hides them. A mod that is not listed uses the default it declared when it\n");
        text.append("# registered its notices. This file is written by the \"Join notices\" page, which\n");
        text.append("# /merlinlib notices opens; editing it by hand works just as well.\n");
        text.append("#\n");
        text.append("# legacy_imported records that the old notices.per_mod string of the common configuration\n");
        text.append("# file has been carried over. It is written by the mod; leaving it alone is the whole point.\n");
        text.append('\n');
        text.append("[general]\n");
        text.append("legacy_imported = true\n");
        text.append('\n');
        text.append("[mods]\n");
        for (Map.Entry<String, Boolean> entry : sorted.entrySet()) {
            text.append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n');
        }
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), text.toString(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Constants.LOG.error("[MerlinLib] could not write {}", file(), exception);
        }
    }

    // ------------------------------------------------------------------ the old value

    /**
     * Reads the old string and applies every mod it mentions that has no line of its own yet.
     *
     * @return how many switches were taken over
     */
    private static int importLegacy() {
        String raw = legacyRaw(LEGACY_FILE);
        if (raw.isEmpty()) {
            raw = legacyRaw(PRE_SPEC_LEGACY_FILE);
        }
        int imported = 0;
        for (Map.Entry<String, String> entry : parseLegacy(raw).entrySet()) {
            if (SWITCHES.containsKey(entry.getKey())) {
                continue;
            }
            SWITCHES.put(entry.getKey(), parse(entry.getValue(), true));
            imported++;
        }
        return imported;
    }

    /**
     * Reads one key out of a file in the configuration directory, without creating anything.
     *
     * @param name the file name
     * @return the old value, or an empty string when the file or the key is not there
     */
    private static String legacyRaw(String name) {
        Path path = ConfigDirectory.root().resolve(name);
        if (!Files.isRegularFile(path)) {
            return "";
        }
        return TomlFile.read(path, "").getString(LEGACY_KEY, "");
    }

    /**
     * Parses the {@code modid=true;other=false} form the old key used.
     *
     * <p>Both {@code ;} and {@code ,} are accepted as separators, and anything without an {@code =} is
     * ignored: the point is to lose nobody's choice to a stray space, not to validate a format that is no
     * longer written.
     *
     * @param raw the old value
     * @return the entries it held, in the order they were written
     */
    public static Map<String, String> parseLegacy(String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String part : raw.split("[;,]")) {
            int equals = part.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String owner = part.substring(0, equals).trim();
            String value = part.substring(equals + 1).trim();
            if (!owner.isEmpty() && !value.isEmpty()) {
                values.put(owner, value);
            }
        }
        return values;
    }

    /**
     * @param value    the text a file held
     * @param fallback the value to use when the text is neither {@code true} nor {@code false}
     * @return the boolean it named
     */
    private static boolean parse(String value, boolean fallback) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return fallback;
    }
}
