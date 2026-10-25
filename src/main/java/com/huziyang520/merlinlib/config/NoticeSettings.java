// The per mod switches of the join notices, stored in config/MerlinLib/notices.toml.

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

public final class NoticeSettings {
    private static final Path FILE = Path.of("config", Constants.CONFIG_DIRECTORY, "notices.toml");
    private static final String SECTION = "mods";

    private static final String DEFAULTS = """
            # MerlinLib join notice switches, one line per business mod.
            # The mods that register a join notice through MerlinLib are listed here, in this order:
            # the key is the mod id and the value is whether its notices are sent.
            # A mod that is not listed uses the default it declared when it registered.
            # The global switch is notices.enabled in the shared configuration; this file adds the per mod
            # choices.
            """;

    private static final List<Path> LEGACY_FILES = List.of(
            Path.of("config", Constants.CONFIG_DIRECTORY, "server.toml"),
            Path.of("config", Constants.CONFIG_DIRECTORY, "server.toml.imported"),
            Path.of("config", Constants.MOD_ID + "-common.toml"));

    private static final String LEGACY_SECTION = "notices";
    private static final String LEGACY_KEY = "per_mod";
    private static final String LEGACY_CLIENT_SECTION = "general";
    private static final String LEGACY_CLIENT_KEY = "notice_enabled";

    private static final List<Path> LEGACY_CLIENT_FILES = List.of(
            ClientConfig.path(),
            Path.of("config", Constants.CONFIG_DIRECTORY, "client.toml"));

    private static boolean imported;

    private NoticeSettings() {
    }

    public static Path path() {
        return FILE;
    }

    public static synchronized void init() {
        if (imported) {
            return;
        }
        imported = true;

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
        for (Path candidate : LEGACY_CLIENT_FILES) {
            TomlFile.remove(candidate, Set.of(LEGACY_CLIENT_SECTION + "." + LEGACY_CLIENT_KEY));
        }
    }

    private static Boolean legacyClientSwitch() {
        for (Path candidate : LEGACY_CLIENT_FILES) {
            String value = readValue(candidate, LEGACY_CLIENT_SECTION, LEGACY_CLIENT_KEY);
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.parseBoolean(value);
            }
        }
        return null;
    }

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

    public static boolean isEnabled(String owner, boolean enabledByDefault) {
        Boolean stored = values().get(owner);
        return stored == null ? enabledByDefault : stored;
    }

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

        imported = true;
    }

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

    private static String legacyRaw() {
        for (Path candidate : LEGACY_FILES) {
            String value = readValue(candidate, LEGACY_SECTION, LEGACY_KEY);
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

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
