package com.huziyang520.merlinlib.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Fabric store for MerlinLib's client switches.
 *
 * <h2>Why this is thin</h2>
 *
 * <p>Cloth Config is a screen builder, not a configuration format: it collects values and hands them to a
 * save callback, and it is up to the mod to keep them somewhere. MerlinLib keeps them in {@code client.toml},
 * the same documented file the fallback reads, which has two consequences worth stating:
 *
 * <ul>
 *   <li>the file remains the single source of truth, so a player who edits it by hand and a player who uses
 *       the screen see the same values;</li>
 *   <li>removing Cloth Config does not lose anything - the same file is read by the {@link TomlFile}
 *       fallback, so the switches simply stop having a screen.</li>
 * </ul>
 *
 * <p>Installed by {@link FabricConfig} only when Cloth Config is present; without it the class is never
 * created and the TOML file is read directly.
 */
public final class ClothBackend implements ConfigBackend {

    /** Values collected by the screen, applied by {@link #save()}. */
    private final Map<String, String> staged = new LinkedHashMap<>();

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
        return "cloth";
    }
}
