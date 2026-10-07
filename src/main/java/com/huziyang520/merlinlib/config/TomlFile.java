package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.util.MerlinColor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tiny reader for the TOML subset MerlinLib writes: comments, blank lines, {@code [section]} headers
 * and {@code key = value} pairs of booleans, integers, floats and quoted strings.
 *
 * <p>Why not a full TOML library: the 26.3 line could not use one because Fabric did not ship a TOML
 * parser. Forge 1.20.1 does ship night-config, and this project has it on the compile classpath for
 * exactly that reason, but the file format below is a strict subset of TOML and the writer has to be
 * able to insert a key into an existing section without rewriting the file, which is not something
 * night-config exposes. The self contained reader stayed.
 *
 * <p>Files are only ever written when missing, never rewritten wholesale: hand written comments are
 * never lost. Reading problems are collected as {@link ContentError}s and the affected key silently
 * falls back to its default, so one typo cannot stop the game from starting.
 */
public final class TomlFile {

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
     * kept, so the configuration screen can save a switch without destroying the documentation the file
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
            int parsed = Integer.decode(value);
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
        Optional<Integer> parsed = MerlinColor.parse(value);
        if (parsed.isPresent()) {
            return parsed.get();
        }
        this.invalid(key, "a colour such as 0xRRGGBB, #RRGGBB or a palette name", value);
        return fallback;
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

    private void invalid(String key, String expected, String found) {
        this.errors.add(new ContentError(this.path.getFileName().toString(), key, "-", expected, "found '" + found + "'"));
    }
}
