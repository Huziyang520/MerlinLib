package com.huziyang520.merlinlib.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The store behind an optional configuration screen: {@code client.toml} itself. A screen collects
 * values and hands them over on save; this class folds them into the documented file, comments and
 * all.
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
