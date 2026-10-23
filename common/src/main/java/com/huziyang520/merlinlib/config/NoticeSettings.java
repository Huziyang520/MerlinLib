package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The per mod switches of the join notices, stored in {@code config/MerlinLib/notices.toml}.
 */
public final class NoticeSettings {

    /** The file every per mod switch lives in. */
    private static final Path FILE = Path.of("config", Constants.CONFIG_DIRECTORY, "notices.toml");

    /** The table the switches are written under. */
    private static final String SECTION = "mods";

    /**
     * The template written on the first save; also the documentation of the file.
     *
     * <p>The header is written by {@link #save(Map)}, which appends one line per mod below it.
     */
    private static final String DEFAULTS = """
            # MerlinLib join notice switches, one line per business mod.
            # The mods that register a join notice through MerlinLib are listed here, in this order:
            # the key is the mod id and the value is whether its notices are sent.
            # A mod that is not listed uses the default it declared when it registered.
            # The global switch is notices.enabled in server.toml; this file adds the per mod choices.
            """;

    /** The {@code server.toml} file the old string was written to. */
    private static final Path LEGACY_FILE = Path.of("config", Constants.CONFIG_DIRECTORY, "server.toml");

    /** The copy the old string ends up in when a backend imported that file on an earlier launch. */
    private static final Path LEGACY_IMPORTED = Path.of("config", Constants.CONFIG_DIRECTORY, "server.toml.imported");

    /** The section and key the old string was stored under. */
    private static final String LEGACY_SECTION = "notices";
    private static final String LEGACY_KEY = "per_mod";

    /** The {@code client.toml} key that used to switch MerlinLib's own notice, and its section. */
    private static final String LEGACY_CLIENT_SECTION = "general";
    private static final String LEGACY_CLIENT_KEY = "notice_enabled";

    /** The copy the client file ends up in when a loader backend imported it on an earlier launch. */
    private static final Path LEGACY_CLIENT_IMPORTED =
            Path.of("config", Constants.CONFIG_DIRECTORY, "client.toml.imported");

    /** Guards the one-off import; also the reason it never runs twice in a process. */
    private static boolean imported;

    private NoticeSettings() {
    }

    /** @return the file the switches are read from and written to */
    public static Path path() {
        return FILE;
    }

    /**
     * Runs the one-off imports: the old {@code general.notice_enabled} of {@code client.toml} and the old
     * {@code notices.per_mod} string.
     *
     * <p>Called from the shared bootstrap, which runs while the mod is still being constructed - before
     * NeoForge loads and rewrites {@code server.toml} from its spec. Safe to call more than once.
     */
    public static synchronized void init() {
        if (imported) {
            return;
        }
        imported = true;
        // The older of the two imports goes first: it is the one that creates the file, and the per mod
        // string would be skipped by the existence check if the client switch had already written it.
        if (!Files.exists(FILE)) {
            Map<String, Boolean> values = legacy();
            if (!values.isEmpty()) {
                save(values);
                Constants.LOG.info("[MerlinLib] imported {} per mod join notice switch(es) from notices.per_mod "
                        + "into {}", values.size(), FILE.getFileName());
            }
        }
        importClientSwitch();
    }

    /**
     * Moves the old {@code general.notice_enabled} of {@code client.toml} into MerlinLib's own row of this
     * file, then deletes the key so the two answers cannot drift apart.
     *
     * <p>Idempotent twice over: the value is only written while this file has no row for MerlinLib yet, and
     * the key is then removed from the client file, so a second launch finds nothing to import. The old key
     * is gone from the client template, which is why a plain save does not drop it on its own.
     */
    private static void importClientSwitch() {
        Boolean old = legacyClientSwitch();
        if (old == null) {
            return;
        }
        Map<String, Boolean> current = values();
        if (!current.containsKey(Constants.MOD_ID)) {
            current.put(Constants.MOD_ID, old);
            save(current);
            Constants.LOG.info("[MerlinLib] imported general.notice_enabled={} into {} as {}.{}",
                    old, FILE.getFileName(), SECTION, Constants.MOD_ID);
        }
        TomlFile.remove(ClientConfig.path(), Set.of(LEGACY_CLIENT_SECTION + "." + LEGACY_CLIENT_KEY));
    }

    /**
     * @return the old client switch, or {@code null} when the file no longer has it
     */
    private static Boolean legacyClientSwitch() {
        for (Path candidate : List.of(ClientConfig.path(), LEGACY_CLIENT_IMPORTED)) {
            String value = readValue(candidate, LEGACY_CLIENT_SECTION, LEGACY_CLIENT_KEY);
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.parseBoolean(value);
            }
        }
        return null;
    }

    /**
     * Reads every switch.
     *
     * <p>Read fresh on every call rather than cached: the file is small, it is only read when a player
     * joins or when the screen opens, and a hand edit then takes effect on the next join instead of on the
     * next launch.
     *
     * @return the switches, keyed by mod id, in file order
     */
    public static Map<String, Boolean> values() {
        init();
        Map<String, Boolean> values = new LinkedHashMap<>();
        if (!Files.isRegularFile(FILE)) {
            return values;
        }
        String section = "";
        List<String> lines;
        try {
            lines = Files.readAllLines(FILE, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Constants.LOG.warn("[MerlinLib] could not read {}: {}", FILE, exception.getMessage());
            return values;
        }
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
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
            String key = line.substring(0, equals).trim();
            String value = unquote(line.substring(equals + 1).trim());
            if (!key.isEmpty()) {
                values.put(key, Boolean.parseBoolean(value));
            }
        }
        return values;
    }

    /**
     * @param owner         the mod id
     * @param enabledByDefault the default the mod declared when it registered its notice
     * @return whether that mod's notices are sent
     */
    public static boolean isEnabled(String owner, boolean enabledByDefault) {
        Boolean stored = values().get(owner);
        return stored == null ? enabledByDefault : stored;
    }

    /**
     * Rewrites the whole file from the given switches.
     *
     * <p>Whole file on purpose: the set of keys is the set of installed mods, which changes when a mod is
     * added or removed, and a rewrite is the only way to drop the line of a mod that is gone. The file
     * carries one comment block and nothing else, so nothing hand written is lost that mattered.
     *
     * @param values the switches to store, keyed by mod id
     */
    public static synchronized void save(Map<String, Boolean> values) {
        StringBuilder content = new StringBuilder(DEFAULTS);
        content.append('\n').append('[').append(SECTION).append(']').append('\n');
        for (Map.Entry<String, Boolean> entry : values.entrySet()) {
            content.append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n');
        }
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, content.toString(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Constants.LOG.warn("[MerlinLib] could not write {}: {}", FILE, exception.getMessage());
        }
        // The file was just written, so the import must never run over it even if it had not run yet.
        imported = true;
    }

    /**
     * Reads the old {@code notices.per_mod} string, which held {@code modid=true;other=false}.
     *
     * @return the switches it named, empty when there was no old value
     */
    private static Map<String, Boolean> legacy() {
        String raw = legacyRaw();
        if (raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Boolean> values = new LinkedHashMap<>();
        for (String part : raw.split("[;,]")) {
            int equals = part.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String owner = part.substring(0, equals).trim();
            String value = part.substring(equals + 1).trim();
            if (!owner.isEmpty() && !value.isEmpty()) {
                values.put(owner, Boolean.parseBoolean(value));
            }
        }
        return values;
    }

    /**
     * The raw value of the old key, read from the text of the files it could be in.
     *
     * @return the value as written, or an empty string when it is not there
     */
    private static String legacyRaw() {
        for (Path candidate : List.of(LEGACY_FILE, LEGACY_IMPORTED)) {
            String value = readValue(candidate, LEGACY_SECTION, LEGACY_KEY);
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    /**
     * Finds one key in one file by reading it as text, so the answer does not depend on which store the
     * loader currently answers configuration reads from.
     *
     * @param file    the file to scan
     * @param section the table the key is expected in
     * @param wanted  the key inside that table
     * @return the value as written, or an empty string
     */
    private static String readValue(Path file, String section, String wanted) {
        if (!Files.isRegularFile(file)) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        try {
            lines.addAll(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return "";
        }
        String current = "";
        for (String raw : lines) {
            String line = stripComment(raw).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                current = line.substring(1, line.length() - 1).trim();
                continue;
            }
            if (!section.equals(current)) {
                continue;
            }
            int equals = line.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            if (wanted.equals(line.substring(0, equals).trim())) {
                return unquote(line.substring(equals + 1).trim());
            }
        }
        return "";
    }

    private static String stripComment(String line) {
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                quoted = !quoted;
            } else if (current == '#' && !quoted) {
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
