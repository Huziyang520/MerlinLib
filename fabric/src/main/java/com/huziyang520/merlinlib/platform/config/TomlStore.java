package com.huziyang520.merlinlib.platform.config;

import com.huziyang520.merlinlib.config.ClientConfig;
import com.huziyang520.merlinlib.config.ConfigBackend;
import com.huziyang520.merlinlib.config.ConfigOptions;
import com.huziyang520.merlinlib.config.ConfigReader;
import com.huziyang520.merlinlib.config.ServerConfig;
import com.huziyang520.merlinlib.config.SplitReader;
import com.huziyang520.merlinlib.config.TomlFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fabric configuration store: the two TOML files, untouched.
 *
 * <p>Fabric ships no configuration screen of its own, so the files are the store no matter which optional
 * screen mod is installed. This class is that store: it reads both files through {@link SplitReader} and
 * writes a value back into the file its section belongs to, keeping every comment in place.
 *
 * <p>It exists as its own type because two screens sit on top of it - Cloth Config and Jupiter - and the
 * store must behave identically under both. Only the screens differ, so only the screens are separate.
 */
final class TomlStore implements ConfigBackend {

    /** Values written by a screen but not saved yet, keyed by {@code section.key}. */
    private final Map<String, String> pending = new LinkedHashMap<>();

    private final String name;

    /**
     * @param name a short description of the store, for the log line that says which one is active
     */
    TomlStore(String name) {
        this.name = name;
    }

    @Override
    public ConfigReader view() {
        // Read fresh on every call: view() is asked for once per configuration load, not once per getter, and a
        // file edited by hand is then picked up by the next /reload without any cache to invalidate.
        return new SplitReader(TomlFile.read(ClientConfig.path(), ClientConfig.defaults()),
                TomlFile.read(ServerConfig.path(), ServerConfig.defaults()), ConfigOptions.SERVER_SECTIONS);
    }

    @Override
    public void write(String key, String value) {
        this.pending.put(key, value);
    }

    @Override
    public void save() {
        Map<String, String> clientValues = new LinkedHashMap<>();
        Map<String, String> serverValues = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : this.pending.entrySet()) {
            if (ConfigOptions.SERVER_SECTIONS.contains(ConfigOptions.sectionOf(entry.getKey()))) {
                serverValues.put(entry.getKey(), entry.getValue());
            } else {
                clientValues.put(entry.getKey(), entry.getValue());
            }
        }
        this.pending.clear();
        TomlFile.rewrite(ClientConfig.path(), ClientConfig.defaults(), clientValues);
        TomlFile.rewrite(ServerConfig.path(), ServerConfig.defaults(), serverValues);
    }

    @Override
    public String name() {
        return this.name;
    }
}
