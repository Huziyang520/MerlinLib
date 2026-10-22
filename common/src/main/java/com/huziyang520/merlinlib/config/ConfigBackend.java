package com.huziyang520.merlinlib.config;

/**
 * A loader supplied store for MerlinLib's switches.
 *
 * <p>A backend replaces {@link TomlFile} as the place the switches are read from, and is where
 * {@code /merlinlib config} looks for something to show. Two things follow from that:
 *
 * <ul>
 *   <li>the switches still have exactly one reader ({@link #view()}), so a consumer cannot tell whether it
 *       is talking to a hand written TOML file or to a platform configuration;</li>
 *   <li>writing goes through {@link #write} + {@link #save}, which is what lets a screen collect several
 *       changes and apply them at once.</li>
 * </ul>
 *
 * <p>Installed through {@link ConfigManager#setBackend} and {@link ConfigManager#setServerBackend} once the
 * platform store is ready to be read and written - on NeoForge that is the moment the {@code ModConfigSpec}
 * reports itself loaded, not the mod constructor.
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
