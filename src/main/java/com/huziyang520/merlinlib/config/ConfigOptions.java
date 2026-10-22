package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads and writes the switches described by {@link OptionSpec}, wherever they live.
 *
 * <h2>What this class adds</h2>
 *
 * <p>{@link ConfigManager} already routes reads and writes through the active {@link ConfigBackend}, but its
 * helpers take the toml path and template to fall back to, which a configuration screen should not have to
 * know. This class takes a {@link OptionSpec} instead: it picks {@code client.toml} or {@code server.toml}
 * from the spec's section, and it hands the write back to the same {@code saveClient} / {@code saveServer}
 * pair the server uses, so a value set in a screen is followed by the same reload a command performs.
 *
 * <h2>Strings, not rich types</h2>
 *
 * <p>The read side returns the raw textual form and the write side takes it, because that is the currency of
 * {@link ConfigBackend#write}: a screen formats a value once, and both storages - the toml file and Jupiter's
 * JSON - receive the same text. A screen that had to switch on the kind itself to write would be a third
 * place where the schema is re-implemented.
 */
public final class ConfigOptions {

    private ConfigOptions() {
    }

    /**
     * @param spec the switch to read
     * @return its current value, formatted the way the toml files store it
     */
    public static String read(OptionSpec spec) {
        ConfigReader reader = readerFor(spec.section());
        switch (spec.kind()) {
            case BOOLEAN:
                return Boolean.toString(reader.getBoolean(spec.key(), spec.booleanDefault()));
            case INTEGER:
                return Integer.toString(reader.getInt(spec.key(), spec.intDefault(),
                        (int) spec.min(), (int) spec.max()));
            case DOUBLE:
                return Double.toString(reader.getDouble(spec.key(), spec.doubleDefault(), spec.min(), spec.max()));
            case TEXT:
                return reader.getString(spec.key(), spec.textDefault());
            default:
                return spec.textDefault();
        }
    }

    /**
     * Stores the switches a screen changed.
     *
     * <p>The values are split by side and each side is saved once, so a screen that edits both a client and a
     * server option performs two writes and two reloads rather than one per key. Keys that are not part of
     * {@link OptionSpec} are dropped: the set of switches is fixed by code, and a screen cannot add to it.
     *
     * @param values the new values, keyed by {@link OptionSpec#key()}
     */
    public static void write(Map<String, String> values) {
        if (values.isEmpty()) {
            return;
        }
        Map<String, String> client = new LinkedHashMap<>();
        Map<String, String> server = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            OptionSpec spec = find(entry.getKey());
            if (spec == null) {
                Constants.LOG.debug("[MerlinLib] {} is not a MerlinLib option, ignoring it", entry.getKey());
                continue;
            }
            if (OptionSpec.onClient(spec.section())) {
                client.put(entry.getKey(), entry.getValue());
            } else {
                server.put(entry.getKey(), entry.getValue());
            }
        }
        if (!client.isEmpty()) {
            ConfigManager.saveClient(client);
        }
        if (!server.isEmpty()) {
            ConfigManager.saveServer(server);
        }
    }

    /**
     * @param key the flat key
     * @return the spec it names, or {@code null} when there is none
     */
    public static OptionSpec find(String key) {
        for (OptionSpec spec : OptionSpec.all()) {
            if (spec.key().equals(key)) {
                return spec;
            }
        }
        return null;
    }

    /**
     * @param section the section id
     * @return a reader over that section's file, or over the active backend
     */
    private static ConfigReader readerFor(String section) {
        return OptionSpec.onClient(section)
                ? ConfigManager.read(ClientConfig.path(), ClientConfig.defaults())
                : ConfigManager.read(ServerConfig.path(), ServerConfig.defaults());
    }
}
