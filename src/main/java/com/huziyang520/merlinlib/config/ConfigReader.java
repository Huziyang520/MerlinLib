package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;

import java.util.List;

/**
 * A read only view of MerlinLib's switches.
 */
public interface ConfigReader {

    /**
     * @param key      the key, as {@code section.key}
     * @param fallback the value to use when the key is missing or not a boolean
     * @return the configured boolean
     */
    boolean getBoolean(String key, boolean fallback);

    /**
     * @param key      the key, as {@code section.key}
     * @param fallback the value to use when the key is missing or out of range
     * @param min      the lowest accepted value
     * @param max      the highest accepted value
     * @return the configured integer
     */
    int getInt(String key, int fallback, int min, int max);

    /**
     * @param key      the key, as {@code section.key}
     * @param fallback the value to use when the key is missing or out of range
     * @param min      the lowest accepted value
     * @param max      the highest accepted value
     * @return the configured number
     */
    double getDouble(String key, double fallback, double min, double max);

    /**
     * Reads a colour written either as {@code 0xRRGGBB}, {@code #RRGGBB} or a palette name.
     *
     * @param key      the key, as {@code section.key}
     * @param fallback the value to use when the key is missing or not a colour
     * @return the configured colour
     */
    int getColor(String key, int fallback);

    /**
     * @param key      the key, as {@code section.key}
     * @param fallback the value to use when the key is missing
     * @return the configured text
     */
    String getString(String key, String fallback);

    /**
     * @return problems found while reading; the affected options fell back to their defaults.
     */
    List<ContentError> errors();
}
