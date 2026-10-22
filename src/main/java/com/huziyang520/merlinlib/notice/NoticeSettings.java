package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The per mod join notice switches, stored in their own file.
 *
 * <h2>Why a file of its own</h2>
 *
 * <p>The switches used to live in one {@code server.toml} line holding {@code modid=true;other=false}, for a
 * reason that no longer binds: the template writer could only put a value back into a line it already
 * contained, so a key invented at runtime - one per mod, and the list of mods is only known once they have
 * registered - would have been dropped on the next write. Keeping every choice in one documented string
 * solved that, and handed every player a raw string to edit by hand.
 *
 * <p>A file of its own solves the first problem without the second. The file is generated, so its keys do
 * not have to be predicted: {@code [mods]} holds one {@code modid = true} line per mod, it is rewritten whole
 * on every save, and no template has to agree with it. The player never sees the file unless they go looking,
 * because {@code /merlinlib notices} draws the same switches.
 *
 * <h2>One-way migration</h2>
 *
 * <p>A world that has been played before carries the old {@code notices.per_mod} line. When this file does
 * not exist yet, the old line is read once and its choices are written here. Idempotent by construction: the
 * write creates the file, and a file that exists is never migrated again. The old line stays in
 * {@code server.toml} untouched - rewriting it would mean keeping the string format alive for a second
 * reason, and it is not read by anything else.
 *
 * <h2>Where it is read</h2>
 *
 * <p>On the server, when a player joins ({@link NoticeManager}). The list of mods is knowable on both sides,
 * because the same mods run on both, so the switches page can be drawn on the client - but the value that
 * decides whether a notice is sent is read here, on the side that sends it.
 */
public final class NoticeSettings {

    /** The file, relative to the MerlinLib configuration directory. */
    public static final String FILE_NAME = "notices.toml";

    /** The section every switch lives in, the same name the old string was documented with. */
    public static final String SECTION = "mods";

    /**
     * The loaded switches, or {@code null} before the first read.
     *
     * <p>A single map for the whole process, because the file is the authority and every reader wants the
     * same answer: the join handler, the switches page, and the migration.
     */
    private static volatile Map<String, Boolean> overrides;

    private NoticeSettings() {
    }

    /**
     * @param entry the notice
     * @return whether it should be sent: the stored switch, or the default its own mod declared
     */
    public static boolean isEnabled(NoticeEntry entry) {
        return isEnabled(entry.owner(), entry.enabledByDefault());
    }

    /**
     * @param owner       the mod id
     * @param enabledByDefault the default that mod declared
     * @return the stored switch, or the default when the mod is not listed
     */
    public static boolean isEnabled(String owner, boolean enabledByDefault) {
        Boolean stored = values().get(owner);
        return stored == null ? enabledByDefault : stored;
    }

    /**
     * @param owner the mod id
     * @return the stored switch, or {@code null} when the mod's own default stands
     */
    public static Boolean override(String owner) {
        return values().get(owner);
    }

    /** @return a copy of every stored switch, for a screen that wants to draw them all */
    public static synchronized Map<String, Boolean> overrides() {
        return Map.copyOf(values());
    }

    /**
     * Records one choice in memory. Nothing is written until {@link #save()}, so a screen that is closed
     * without confirming leaves the file alone.
     *
     * @param owner   the mod id
     * @param enabled the choice
     */
    public static synchronized void set(String owner, boolean enabled) {
        values().put(owner, enabled);
    }

    /** Writes the whole file, in one go, sorted by mod id. */
    public static synchronized void save() {
        write(values());
    }

    /**
     * Forgets what was loaded, so the next read comes from the file again.
     *
     * <p>The counterpart of {@code ConfigManager#reload}: a page that wrote the file should not be the only
     * reader that sees the new values.
     */
    public static synchronized void reload() {
        overrides = null;
    }

    /** @return {@code <gameDir>/config/MerlinLib/notices.toml} */
    public static Path path() {
        return Path.of("config", Constants.CONFIG_DIRECTORY, FILE_NAME);
    }

    // ---------------------------------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------------------------------

    private static Map<String, Boolean> values() {
        Map<String, Boolean> current = overrides;
        if (current == null) {
            synchronized (NoticeSettings.class) {
                current = overrides;
                if (current == null) {
                    current = load();
                    overrides = current;
                }
            }
        }
        return current;
    }

    private static Map<String, Boolean> load() {
        Path file = path();
        Map<String, Boolean> loaded = new LinkedHashMap<>();
        if (Files.exists(file)) {
            read(file, loaded);
            return loaded;
        }
        Map<String, Boolean> migrated = migrateLegacy();
        if (!migrated.isEmpty()) {
            Constants.LOG.info("[MerlinLib] moved {} per mod join notice switch(es) from server.toml "
                    + "(notices.per_mod) into {}", migrated.size(), file);
        }
        loaded.putAll(migrated);
        // Written even when nothing was migrated: an empty file documents the format, and its existence is
        // what makes the migration a one-off.
        write(loaded);
        return loaded;
    }

    /**
     * Reads the switches an older build kept in {@code server.toml}.
     *
     * <p>The only place the old string format is still understood. A configuration that cannot be read - a
     * backend that owns the switches, for instance - simply migrates nothing.
     *
     * @return the migrated switches, empty when there was nothing to move
     */
    private static Map<String, Boolean> migrateLegacy() {
        Map<String, Boolean> migrated = new LinkedHashMap<>();
        String raw;
        try {
            raw = ConfigManager.server().legacyNoticePerMod();
        } catch (RuntimeException unavailable) {
            Constants.LOG.warn("[MerlinLib] the old notices.per_mod switches could not be read", unavailable);
            return migrated;
        }
        if (raw == null || raw.isBlank()) {
            return migrated;
        }
        for (String part : raw.split("[;,]")) {
            int equals = part.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String owner = part.substring(0, equals).trim();
            String value = part.substring(equals + 1).trim();
            if (!isModId(owner)) {
                continue;
            }
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                migrated.put(owner, Boolean.parseBoolean(value));
            }
        }
        return migrated;
    }

    /**
     * Reads {@code [mods]} out of a file.
     *
     * <p>Deliberately forgiving: a line that is not a switch is skipped rather than thrown, because this file
     * is hand editable and a typo in it must not stop a player from joining. Comments are stripped the same
     * way {@code TomlFile} strips them, so the two readers agree on what a comment is.
     *
     * @param file the file to read
     * @param into where the switches are collected
     */
    private static void read(Path file, Map<String, Boolean> into) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException unreadable) {
            Constants.LOG.error("[MerlinLib] could not read {}; every mod keeps its own default",
                    file, unreadable);
            return;
        }
        String section = "";
        for (String raw : lines) {
            String line = stripComment(raw).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }
            if (!SECTION.equals(section)) {
                continue;
            }
            int equals = line.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String owner = line.substring(0, equals).trim();
            String value = unquote(line.substring(equals + 1).trim());
            if (!isModId(owner)) {
                Constants.LOG.warn("[MerlinLib] {}: '{}' is not a mod id, ignoring it", FILE_NAME, owner);
                continue;
            }
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                into.put(owner, Boolean.parseBoolean(value));
            } else {
                Constants.LOG.warn("[MerlinLib] {}: {} = '{}' is not true or false, ignoring it",
                        FILE_NAME, owner, value);
            }
        }
    }

    /**
     * Writes the whole file.
     *
     * <p>Whole rather than in place, unlike the two switch files: this one is generated, so there is no
     * hand written comment to preserve, and a stale {@code modid} line for a mod that is no longer installed
     * would otherwise stay forever.
     *
     * @param values the switches to write, in any order
     */
    private static void write(Map<String, Boolean> values) {
        List<String> owners = new ArrayList<>(values.keySet());
        Collections.sort(owners);

        StringBuilder builder = new StringBuilder();
        builder.append("# MerlinLib join notice switches, one line per mod.\n")
                .append("# true shows that mod's join notices when a player enters the world, false hides them.\n")
                .append("# A mod that is not listed uses the default it declared when it registered.\n")
                .append("# Written by /merlinlib notices; editing it by hand works too.\n")
                .append('\n')
                .append('[').append(SECTION).append("]\n");
        for (String owner : owners) {
            builder.append(owner).append(" = ").append(values.get(owner)).append('\n');
        }

        Path file = path();
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException unwritable) {
            // The choices stay live in memory for this session; the next launch falls back to the defaults.
            Constants.LOG.error("[MerlinLib] could not write {}", file, unwritable);
        }
    }

    /**
     * @param owner a candidate mod id
     * @return whether it matches the character set a resource location namespace allows
     */
    private static boolean isModId(String owner) {
        return owner.matches("[a-z0-9_.-]+");
    }

    private static String stripComment(String line) {
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                inQuotes = !inQuotes;
            } else if (current == '#' && !inQuotes) {
                return line.substring(0, index);
            }
        }
        return line;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
