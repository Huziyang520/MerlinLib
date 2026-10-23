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
import java.util.Optional;
import java.util.Set;

/**
 * Reader and writer of the small, commented TOML subset MerlinLib keeps its switches in: comments,
 * blank lines, {@code [section]} headers and {@code key = value} pairs of booleans, integers, floats
 * and strings.
 */
public final class TomlFile implements ConfigReader {

    private final Path path;
    private final Map<String, String> values = new LinkedHashMap<>();
    private final List<ContentError> errors = new ArrayList<>();
    private String currentSection = "";

    private TomlFile(Path path) {
        this.path = path;
    }

    /**
     * Reads a file, creating it with {@code defaults} first when it does not exist yet.
     *
     * @param path     file to read
     * @param defaults content written when the file is missing
     * @return the parsed view, never {@code null} even when the file is unreadable
     */
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

    /**
     * Writes selected values back into an existing file, in place.
     *
     * <p>Only the values named in {@code overrides} are touched: every comment, blank line and order is
     * kept, so saving one switch never destroys the documentation the file
     * exists to carry. A key that is not in the file yet is appended, under its section header, which is
     * what makes a file written by an older version gain new options without being reset.
     *
     * <p>Keys are addressed the same way as when reading: {@code section.key}, or just {@code key} for a
     * value before any section header.
     *
     * @param path      the file to update, created from {@code defaults} when missing
     * @param defaults  the template written when the file does not exist yet
     * @param overrides the values to replace, keyed by {@code section.key}
     */
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
            // A read only file must not break the screen: the values simply stay as they were on disk.
            return;
        }
    }

    /**
     * Inserts a line into the section it belongs to, creating the header only when it is missing.
     *
     * <p>Appending new keys at the end of the file instead would add a second header for a section that is
     * already there. This reader copes with that, but a file holding two {@code [tools]} tables is rejected
     * by strict TOML tools, and a configuration file that other programs refuse to open is a bug of ours.
     *
     * @param lines   the file's lines, modified in place
     * @param section the section name, empty for a key before any header
     * @param line    the complete {@code key = value} line to insert
     */
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
        // Walk to the end of the section: past comments and blank lines, stopping at the next header.
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

    /**
     * Replaces a file with a fresh copy of a template, carrying the given values over.
     *
     * <p>Used by the layout migrations, which have to be able to drop a whole section: {@link #rewrite}
     * only ever replaces or adds single keys and leaves everything else exactly as it was, which is right
     * for a normal save but wrong when an option no longer exists.
     *
     * @param path     the file to replace, created when missing
     * @param defaults the template to write
     * @param values   the values to carry over, keyed by {@code section.key}; unknown keys are ignored
     */
    public static void rebuild(Path path, String defaults, Map<String, String> values) {
        write(path, defaults);
        rewrite(path, defaults, values);
    }

    /**
     * Deletes keys from a file, together with the comment lines that introduce them.
     *
     * <p>Used for a value a build no longer has: {@link #rewrite} can only replace or add a line, so a key
     * that must disappear would otherwise stay in the file for good. Everything else is left alone.
     *
     * @param path the file to edit, ignored when it is missing
     * @param keys the keys to delete, as {@code section.key}
     */
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
            // A read only file must not break startup; the stale line simply stays.
        }
    }

    /**
     * @param template the commented template of a configuration file
     * @return every key the template declares, as {@code section.key}
     */
    public static Set<String> keysOf(String template) {
        Set<String> keys = new java.util.LinkedHashSet<>();
        String section = "";
        for (String raw : template.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int equals = line.indexOf('=');
            if (equals < 0) {
                continue;
            }
            String key = line.substring(0, equals).trim();
            keys.add(section.isEmpty() ? key : section + "." + key);
        }
        return keys;
    }

    private static void write(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Reported later as a missing file; nothing better can be done here.
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

    private Optional<String> raw(String key) {
        return Optional.ofNullable(this.values.get(key));
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

    /**
     * Reads a colour written either as {@code 0xRRGGBB}, {@code #RRGGBB} or a palette name.
     */
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

    /**
     * @return problems found while reading; the affected options fell back to their defaults.
     */
    public List<ContentError> errors() {
        return List.copyOf(this.errors);
    }

    public Path path() {
        return this.path;
    }

    /**
     * @return every value the file holds, keyed by {@code section.key}, in file order
     */
    public Map<String, String> values() {
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(this.values));
    }

    private void invalid(String key, String expected, String found) {
        this.errors.add(new ContentError(this.path.getFileName().toString(), key, "-", expected, "found '" + found + "'"));
    }
}
