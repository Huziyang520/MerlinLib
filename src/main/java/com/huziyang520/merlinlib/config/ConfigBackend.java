package com.huziyang520.merlinlib.config;

/**
 * Where the configuration values physically live.
 *
 * <p>{@link ConfigManager} owns one of these. Two implementations exist:
 *
 * <ul>
 *   <li>no backend at all, which is the default: the values are read from and written to
 *       {@code config/MerlinLib/client.toml} and {@code server.toml} by {@link TomlFile};</li>
 *   <li>a Jupiter backed one, which stores the very same values in a container the Jupiter mod owns, so
 *       that its own configuration screen edits them. See {@link JupiterBackend}.</li>
 * </ul>
 *
 * <p>The interface is deliberately small and free of any third party type: this package must stay
 * loadable when Jupiter is not installed, which is the normal case.
 */
public interface ConfigBackend {

    /**
     * @return a reader over the values this backend holds
     */
    ConfigReader view();

    /**
     * Stores one value.
     *
     * <p>The raw form is the one the TOML files use, so a caller that already builds overrides for
     * {@link TomlFile#rewrite} can drive either storage without knowing which one is active. A key the
     * backend does not know is ignored rather than invented: the set of switches is fixed by the code.
     *
     * @param key      the flat key, {@code section.name}
     * @param rawValue the value, as it would be written to a toml file
     */
    void write(String key, String rawValue);

    /**
     * Flushes the values to their storage. A backend without a file of its own may do nothing.
     */
    void save();

    /**
     * @return the backend's name, for log lines that have to say where the values came from
     */
    String name();
}
