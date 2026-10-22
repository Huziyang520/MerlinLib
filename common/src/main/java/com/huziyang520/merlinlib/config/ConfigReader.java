package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;

import java.util.List;

/**
 * A read only view of MerlinLib's switches.
 *
 * <p>Every option is addressed as {@code section.key} and comes with the value the caller wants when the
 * key is missing or cannot be understood, so a broken file can never stop the game from starting.
 *
 * <p>There are two implementations: {@link TomlFile}, which reads the commented files under
 * {@code config/MerlinLib}, and the platform configuration of a loader (NeoForge's {@code ModConfigSpec},
 * Fabric's optional Cloth Config screen), which is installed through {@link ConfigManager#setBackend}.
 * Consumers never care which one is in use.
 */
public interface ConfigReader {

    /**
     * @param key      the option, as {@code section.key}
     * @param fallback the value to use when the key is missing or not a boolean
     * @return the value of the option
     */
    boolean getBoolean(String key, boolean fallback);

    /**
     * @param key      the option, as {@code section.key}
     * @param fallback the value to use when the key is missing, not a number or out of range
     * @param min      the lowest accepted value
     * @param max      the highest accepted value
     * @return the value of the option
     */
    int getInt(String key, int fallback, int min, int max);

    /**
     * @param key      the option, as {@code section.key}
     * @param fallback the value to use when the key is missing, not a number or out of range
     * @param min      the lowest accepted value
     * @param max      the highest accepted value
     * @return the value of the option
     */
    double getDouble(String key, double fallback, double min, double max);

    /**
     * Reads a colour written either as {@code 0xRRGGBB}, {@code #RRGGBB} or a palette name.
     *
     * @param key      the option, as {@code section.key}
     * @param fallback the value to use when the key is missing or not a colour
     * @return the ARGB value of the colour
     */
    int getColor(String key, int fallback);

    /**
     * @param key      the option, as {@code section.key}
     * @param fallback the value to use when the key is missing
     * @return the value of the option
     */
    String getString(String key, String fallback);

    /**
     * @return problems found while reading; the affected options fell back to their defaults.
     */
    List<ContentError> errors();
}
