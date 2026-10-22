package com.huziyang520.merlinlib.config;

/**
 * A configuration store a loader injects into {@link ConfigManager}.
 *
 * <p>Only the interface lives in the common project: the implementation belongs to the loader that owns
 * the store (NeoForge's {@code ModConfigSpec}, Fabric's optional Cloth Config screen over the TOML files),
 * and the loader entrypoint installs it during initialization. Everything in this module therefore talks to
 * {@link ConfigReader} and never to a loader type.
 *
 * <p>One backend covers both files: {@link #view()} answers for every key of the client and of the server
 * configuration, because a screen is built from one list of options and the keys never collide.
 */
public interface ConfigBackend {

    /**
     * The live values. Read at the moment an option is used, never cached by a caller, so a saved switch is
     * in effect immediately.
     *
     * @return the combined view of every configuration option
     */
    ConfigReader view();

    /**
     * Writes one option into the store.
     *
     * <p>Values arrive as text because that is what a configuration file holds: booleans are
     * {@code true}/{@code false}, numbers are decimal, colours are {@code 0xAARRGGBB} and a colour name is
     * accepted as a last resort.
     *
     * @param key   the {@code section.key} to write
     * @param value the value to store
     */
    void write(String key, String value);

    /** Persists everything written since the last save. */
    void save();

    /** @return a short name of the store, for the log line that says which one is active */
    String name();
}
