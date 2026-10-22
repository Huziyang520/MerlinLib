package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;

import java.util.List;

/**
 * A read only view of the switch files.
 *
 * <p>Two implementations exist: {@link TomlFile}, which parses the {@code .toml} files this mod has
 * always written, and the Jupiter backed view (see {@link JupiterBackend}), which reads the same values
 * out of a container owned by the Jupiter config library when that library is installed.
 *
 * <p>Both answer exactly the same way, including the fallback rule: a value that is missing or outside
 * its documented range is reported through {@link #errors()} and the caller's fallback is returned, so a
 * typo in a hand edited file can never stop the game from starting. Callers therefore do not know, and
 * must not need to know, where a value came from.
 *
 * <h2>Why the signature mirrors TomlFile</h2>
 *
 * <p>Method for method, parameter for parameter: {@code getInt(key, fallback, min, max)} rather than a
 * bare getter, because the range is what turns a bad file into a logged problem instead of a crash. The
 * colours are read through the same {@code getColor} spelling, which accepts {@code 0xRRGGBB},
 * {@code #RRGGBB} or a palette name.
 */
public interface ConfigReader {

    /**
     * @param key      the flat key, {@code section.name}
     * @param fallback the value used when the key is missing or unreadable
     * @return the stored boolean
     */
    boolean getBoolean(String key, boolean fallback);

    /**
     * @param key      the flat key, {@code section.name}
     * @param fallback the value used when the key is missing or out of range
     * @param min      the smallest accepted value
     * @param max      the largest accepted value
     * @return the stored integer
     */
    int getInt(String key, int fallback, int min, int max);

    /**
     * @param key      the flat key, {@code section.name}
     * @param fallback the value used when the key is missing or out of range
     * @param min      the smallest accepted value
     * @param max      the largest accepted value
     * @return the stored number
     */
    double getDouble(String key, double fallback, double min, double max);

    /**
     * @param key      the flat key, {@code section.name}
     * @param fallback the colour used when the key is missing or unparseable
     * @return the stored colour as ARGB
     */
    int getColor(String key, int fallback);

    /**
     * @param key      the flat key, {@code section.name}
     * @param fallback the value used when the key is missing
     * @return the stored text
     */
    String getString(String key, String fallback);

    /**
     * @return problems found while reading; the affected options fell back to their defaults
     */
    List<ContentError> errors();
}
