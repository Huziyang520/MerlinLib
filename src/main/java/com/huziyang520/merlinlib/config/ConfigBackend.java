package com.huziyang520.merlinlib.config;

/**
 * A loader supplied store for MerlinLib's switches.
 */
public interface ConfigBackend {

    /**
     * @return a reader over the current values, never {@code null}
     */
    ConfigReader view();

    /**
     * Writes one key.
     *
     * <p>The value arrives as the text a player would have typed into the file, and the backend converts
     * it to the type the key was declared with. A key the backend does not know, or a value that cannot be
     * converted, is reported and dropped rather than guessed at.
     *
     * @param key the key, as {@code section.key}
     * @param raw the new value, as written in the file
     */
    void write(String key, String raw);

    /**
     * Flushes the written values to disk.
     */
    void save();

    /**
     * @return the file this backend owns, as a name below the game's {@code config} directory
     */
    String name();
}
