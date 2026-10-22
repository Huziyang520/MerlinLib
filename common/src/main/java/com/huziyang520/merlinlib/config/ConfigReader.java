package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;

import java.util.List;

/**
 * Read only view of one configuration store.
 *
 * <p>The switches of MerlinLib are read through this interface so the store can change without touching a
 * single caller: the default store is the commented TOML file ({@link TomlFile}), while a loader may inject
 * its own store - NeoForge hands one backed by {@code ModConfigSpec}, Fabric may hand one backed by whatever
 * its configuration library uses - through {@link ConfigManager#setBackend(ConfigBackend)}.
 *
 * <p>Keys are addressed as {@code section.key} exactly as they are written in the file
 * ({@code hud.collapse_enabled}), or as a bare {@code key} for a value before any section header.
 *
 * <p>Bad input never throws: a value that cannot be understood is reported through {@link #errors()} and
 * the affected key falls back to the default the caller passed in, so one typo cannot stop the game.
 */
public interface ConfigReader {

    /**
     * @param key      the {@code section.key} to read
     * @param fallback the value to use when the key is missing or unreadable
     * @return the stored boolean
     */
    boolean getBoolean(String key, boolean fallback);

    /**
     * @param key      the {@code section.key} to read
     * @param fallback the value to use when the key is missing, unreadable or out of range
     * @param min      the lowest accepted value
     * @param max      the highest accepted value
     * @return the stored integer
     */
    int getInt(String key, int fallback, int min, int max);

    /**
     * @param key      the {@code section.key} to read
     * @param fallback the value to use when the key is missing, unreadable or out of range
     * @param min      the lowest accepted value
     * @param max      the highest accepted value
     * @return the stored number
     */
    double getDouble(String key, double fallback, double min, double max);

    /**
     * Reads a colour written either as {@code 0xRRGGBB}, {@code #RRGGBB}, a packed integer or a palette name.
     *
     * @param key      the {@code section.key} to read
     * @param fallback the packed ARGB value to use when the key is missing or unreadable
     * @return the stored colour
     */
    int getColor(String key, int fallback);

    /**
     * @param key      the {@code section.key} to read
     * @param fallback the value to use when the key is missing
     * @return the stored text
     */
    String getString(String key, String fallback);

    /**
     * @return problems found while reading; the affected keys fell back to their defaults
     */
    List<ContentError> errors();
}
