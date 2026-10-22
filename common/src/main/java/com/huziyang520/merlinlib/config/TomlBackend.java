package com.huziyang520.merlinlib.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The store behind an optional configuration screen: {@code client.toml} itself.
 *
 * <h2>Why a screen builder needs a store at all</h2>
 *
 * <p>A screen builder - Cloth Config, Jupiter - is only a way to show one value at a time and hand a map
 * back when the player saves; neither of them is a file format of its own for a
 * {@link ConfigBackend} to adopt blindly. MerlinLib therefore keeps {@code client.toml} as the single
 * source of truth and this class is the bridge: {@link #write} stages the values the screen collected and
 * {@link #save} folds them into the documented file, comments and all.
 *
 * <p>Two consequences are worth stating, because they are what makes removing the mod harmless:
 *
 * <ul>
 *   <li>a player who edits the file by hand and a player who uses the screen see the same values, because
 *       there is one file;</li>
 *   <li>removing Cloth Config or Jupiter loses nothing - the same file is read by the {@link TomlFile}
 *       fallback, so the switches simply stop having a screen.</li>
 * </ul>
 */
public final class TomlBackend implements ConfigBackend {

    /** The name reported in the log, so a pack author can tell which screen is in use. */
    private final String name;
    /** Values collected by the screen, applied by {@link #save()}. */
    private final Map<String, String> staged = new LinkedHashMap<>();

    /**
     * @param name the screen builder this store backs, for the log
     */
    public TomlBackend(String name) {
        this.name = name;
    }

    @Override
    public ConfigReader view() {
        // Read from the file on every call, never from a copy: that is what keeps a hand edit and a screen
        // edit in step, and what makes an external reload visible without restarting.
        return TomlFile.read(ClientConfig.path(), ClientConfig.defaults());
    }

    @Override
    public void write(String key, String value) {
        this.staged.put(key, value);
    }

    @Override
    public void save() {
        if (this.staged.isEmpty()) {
            return;
        }
        Map<String, String> values = new LinkedHashMap<>(this.staged);
        this.staged.clear();
        TomlFile.rewrite(ClientConfig.path(), ClientConfig.defaults(), values);
    }

    @Override
    public String name() {
        return this.name;
    }
}
