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
     * Every {@code section.key} the file held.
     *
     * <p>Needed where the keys are not known ahead of time: the per mod switches of the join notices are
     * one line per mod, and the mods only exist at runtime. The map is a copy, so a caller enumerating it
     * cannot change what this reader answers.
     *
     * @return the values, keyed by {@code section.key} (or the bare key at the top level), in file order
     */
    public Map<String, String> entries() {
        return new LinkedHashMap<>(this.values);
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
