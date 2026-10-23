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

/**
 * The per mod switches of the join notices, stored in {@code config/MerlinLib/notices.toml}.
 */
public final class NoticeSettings {

    /** The file every per mod switch lives in. */
    private static final Path FILE = Path.of("config", Constants.CONFIG_DIRECTORY, "notices.toml");

    /** The table the switches are written under. */
    private static final String SECTION = "mods";

    /** The template written on the first save; also the documentation of the file. */
    private static final String DEFAULTS = """
            # MerlinLib join notice switches, one line per business mod.
            # The mods that register a join notice through MerlinLib are listed here, in this order:
            # the key is the mod id and the value is whether its notices are sent.
            # A mod that is not listed uses the default it declared when it registered.
            # The global switch is notices.enabled in the shared configuration; this file adds the per mod
            # choices.
            """;

    /** The files the old {@code notices.per_mod} string could have been written to. */
    private static final List<Path> LEGACY_FILES = List.of(
            Path.of("config", Constants.CONFIG_DIRECTORY, "server.toml"),
            Path.of("config", Constants.CONFIG_DIRECTORY, "server.toml.imported"),
            Path.of("config", Constants.MOD_ID + "-common.toml"));

    /** The section and key the old string was stored under. */
    private static final String LEGACY_SECTION = "notices";
    private static final String LEGACY_KEY = "per_mod";

    /** Guards the one-off import; also the reason it never runs twice in a process. */
    private static boolean imported;

    private NoticeSettings() {
    }

    /** @return the file the switches are read from and written to */
    public static Path path() {
        return FILE;
    }

    /**
     * Runs the one-off import of the old {@code notices.per_mod} string.
     *
     * <p>Called from the shared bootstrap, which runs while the mod is still being constructed - before
     * the loader rewrites the shared file from its spec. Safe to call more than once.
     */
    public static synchronized void init() {
        if (imported) {
            return;
        }
        imported = true;
        if (Files.exists(FILE)) {
            return;
        }
        Map<String, Boolean> values = legacy();
        if (values.isEmpty()) {
            return;
        }
        save(values);
        Constants.LOG.info("[MerlinLib] imported {} per mod join notice switch(es) from notices.per_mod "
                + "into {}", values.size(), FILE.getFileName());
    }

    /**
     * Reads every switch.
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
     * @param owner            the mod id
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
     * @return the value as written, or an empty string when it is not there
     */
    private static String legacyRaw() {
        for (Path candidate : LEGACY_FILES) {
            String value = legacyRaw(candidate);
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
     * @param file the file to scan
     * @return the value as written, or an empty string
     */
    private static String legacyRaw(Path file) {
        if (!Files.isRegularFile(file)) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        try {
            lines.addAll(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return "";
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
            if (!LEGACY_SECTION.equals(section)) {
                continue;
            }
            int equals = line.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            if (LEGACY_KEY.equals(line.substring(0, equals).trim())) {
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
