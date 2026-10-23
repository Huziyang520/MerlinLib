package com.huziyang520.merlinlib.config;

/**
 * A loader supplied store for MerlinLib's switches.
 */
public interface ConfigBackend {

    /**
     * @return the reader the switches are taken from
     */
    ConfigReader view();

    /**
     * Stages one value.
     *
     * @param key   the option, as {@code section.key}
     * @param value the value, in the same textual form a TOML file would carry
     */
    void write(String key, String value);

    /**
     * Applies everything staged by {@link #write} to the underlying store.
     */
    void save();

    /**
     * @return a short name for the log, for example {@code "neoforge"} or {@code "cloth"}
     */
    String name();
}
