// Reader and writer of the small, commented TOML subset MerlinLib keeps its switches in: comments, blank lines, [section] ...

package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.util.MerlinColor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TomlFile implements ConfigReader {
    private final Path path;
    private final Map<String, String> values = new LinkedHashMap<>();
    private final List<ContentError> errors = new ArrayList<>();
    private String currentSection = "";

    private TomlFile(Path path) {
        this.path = path;
    }

    public static TomlFile read(Path path, String defaults) {
        TomlFile file = new TomlFile(path);
        if (!Files.exists(path)) {
            write(path, defaults);
        }
        if (!Files.isRegularFile(path)) {
            file.errors.add(new ContentError(path.getFileName().toString(), "<root>", "-", "a readable file", "file is missing"));
            return file;
        }
        try {
            file.parse(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            file.errors.add(new ContentError(path.getFileName().toString(), "<root>", "-", "a readable file", String.valueOf(exception.getMessage())));
        }
        return file;
    }

    public static void rewrite(Path path, String defaults, Map<String, String> overrides) {
        if (overrides.isEmpty()) {
            return;
        }
        if (!Files.exists(path)) {
            write(path, defaults);
        }

        List<String> lines;
        try {
            lines = new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return;
        }

        Map<String, String> pending = new LinkedHashMap<>(overrides);
        String section = "";
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                section = trimmed.substring(1, trimmed.length() - 1).trim();
                continue;
            }
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int equals = trimmed.indexOf('=');
            if (equals < 0) {
                continue;
            }
            String key = trimmed.substring(0, equals).trim();
            String full = section.isEmpty() ? key : section + "." + key;
            if (pending.containsKey(full)) {
                lines.set(i, key + " = " + pending.remove(full));
            }
        }

        for (Map.Entry<String, String> entry : pending.entrySet()) {
            String key = entry.getKey();
            int dot = key.lastIndexOf('.');
            String prefix = dot < 0 ? "" : key.substring(0, dot);
            String name = dot < 0 ? key : key.substring(dot + 1);
            insert(lines, prefix, name + " = " + entry.getValue());
        }

        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
        }
    }

    private static void insert(List<String> lines, String section, String line) {
        if (section.isEmpty()) {
            lines.add(0, line);
            return;
        }
        String header = "[" + section + "]";
        int headerIndex = -1;
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).trim().equals(header)) {
                headerIndex = index;
                break;
            }
        }
        if (headerIndex < 0) {
            if (!lines.isEmpty() && !lines.get(lines.size() - 1).isBlank()) {
                lines.add("");
            }
            lines.add(header);
            lines.add(line);
            return;
        }

        int insertAt = headerIndex + 1;
        for (int index = headerIndex + 1; index < lines.size(); index++) {
            String trimmed = lines.get(index).trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                break;
            }
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            insertAt = index + 1;
        }
        lines.add(insertAt, line);
    }

    public static void remove(Path path, Set<String> keys) {
        if (keys.isEmpty() || !Files.isRegularFile(path)) {
            return;
        }
        List<String> lines;
        try {
            lines = new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return;
        }

        List<Integer> doomed = new ArrayList<>();
        String section = "";
        for (int index = 0; index < lines.size(); index++) {
            String trimmed = lines.get(index).trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                section = trimmed.substring(1, trimmed.length() - 1).trim();
                continue;
            }
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int equals = trimmed.indexOf('=');
            if (equals < 0) {
                continue;
            }
            String key = trimmed.substring(0, equals).trim();
            String full = section.isEmpty() ? key : section + "." + key;
            if (!keys.contains(full)) {
                continue;
            }
            doomed.add(index);
            for (int above = index - 1; above >= 0 && lines.get(above).trim().startsWith("#"); above--) {
                doomed.add(above);
            }
        }
        if (doomed.isEmpty()) {
            return;
        }
        doomed.sort(Comparator.reverseOrder());
        for (int index : doomed) {
            lines.remove(index);
        }
        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
        }
    }

    private static void write(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private void parse(List<String> lines) {
        for (int index = 0; index < lines.size(); index++) {
            String line = stripComment(lines.get(index)).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                this.currentSection = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int separator = line.indexOf('=');
            if (separator < 0) {
                this.errors.add(new ContentError(this.path.getFileName().toString(), "line " + (index + 1), "-",
                        "key = value", "found '" + line + "'"));
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (key.isEmpty()) {
                this.errors.add(new ContentError(this.path.getFileName().toString(), "line " + (index + 1), "-", "key = value", "empty key"));
                continue;
            }
            this.values.put(this.currentSection.isEmpty() ? key : this.currentSection + "." + key, unquote(value));
        }
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

    public boolean getBoolean(String key, boolean fallback) {
        String value = this.values.get(key);
        if (value == null) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        this.invalid(key, "true or false", value);
        return fallback;
    }

    public int getInt(String key, int fallback, int min, int max) {
        String value = this.values.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            int parsed = Integer.decode(value.startsWith("0x") || value.startsWith("0X") ? value : value);
            if (parsed < min || parsed > max) {
                this.invalid(key, "an integer in [" + min + ", " + max + "]", value);
                return fallback;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            this.invalid(key, "an integer in [" + min + ", " + max + "]", value);
            return fallback;
        }
    }

    public double getDouble(String key, double fallback, double min, double max) {
        String value = this.values.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < min || parsed > max) {
                this.invalid(key, "a number in [" + min + ", " + max + "]", value);
                return fallback;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            this.invalid(key, "a number in [" + min + ", " + max + "]", value);
            return fallback;
        }
    }

    public int getColor(String key, int fallback) {
        String value = this.values.get(key);
        if (value == null) {
            return fallback;
        }
        return MerlinColor.parse(value).orElseGet(() -> {
            this.invalid(key, "a colour such as 0xRRGGBB, #RRGGBB or a palette name", value);
            return fallback;
        });
    }

    public String getString(String key, String fallback) {
        return this.values.getOrDefault(key, fallback);
    }

    public Map<String, String> entries() {
        return new LinkedHashMap<>(this.values);
    }

    public List<ContentError> errors() {
        return List.copyOf(this.errors);
    }

    public Path path() {
        return this.path;
    }

    private void invalid(String key, String expected, String found) {
        this.errors.add(new ContentError(this.path.getFileName().toString(), key, "-", expected, "found '" + found + "'"));
    }
}
