// The store behind an optional configuration screen: client.toml itself.

package com.huziyang520.merlinlib.config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TomlBackend implements ConfigBackend {
    private final String name;
    private final Map<String, String> staged = new LinkedHashMap<>();

    public TomlBackend(String name) {
        this.name = name;
    }

    @Override
    public ConfigReader view() {
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
