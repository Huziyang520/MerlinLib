package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Presents two stores as the one view a {@link ConfigBackend} has to offer.
 *
 * <p>A backend owns two configurations - the client switches and the authoritative server ones - but a
 * screen is built from one flat list of options, so the backend answers through a single
 * {@link ConfigReader}. Which half a key belongs to is decided by its section, taken from the part before
 * the first dot: {@code hud.*} is client, {@code tools.*} is server. A key without a section is client,
 * because every option of this mod is written under a section.
 *
 * <p>Routing by section rather than by trying one store and then the other is deliberate: both stores
 * answer for a key they do not hold by returning the caller's default, so "which one had it" cannot be told
 * apart from "neither had it" and a fallback chain would silently return defaults for every option.
 */
public final class SplitReader implements ConfigReader {

    private final ConfigReader client;
    private final ConfigReader server;
    private final Set<String> serverSections;

    /**
     * @param client         the store holding the client switches
     * @param server         the store holding the server switches
     * @param serverSections the sections that belong to the server store
     */
    public SplitReader(ConfigReader client, ConfigReader server, Set<String> serverSections) {
        this.client = client;
        this.server = server;
        this.serverSections = Set.copyOf(serverSections);
    }

    private ConfigReader readerFor(String key) {
        int dot = key.indexOf('.');
        String section = dot < 0 ? "" : key.substring(0, dot);
        return this.serverSections.contains(section) ? this.server : this.client;
    }

    @Override
    public boolean getBoolean(String key, boolean fallback) {
        return readerFor(key).getBoolean(key, fallback);
    }

    @Override
    public int getInt(String key, int fallback, int min, int max) {
        return readerFor(key).getInt(key, fallback, min, max);
    }

    @Override
    public double getDouble(String key, double fallback, double min, double max) {
        return readerFor(key).getDouble(key, fallback, min, max);
    }

    @Override
    public int getColor(String key, int fallback) {
        return readerFor(key).getColor(key, fallback);
    }

    @Override
    public String getString(String key, String fallback) {
        return readerFor(key).getString(key, fallback);
    }

    @Override
    public List<ContentError> errors() {
        List<ContentError> collected = new ArrayList<>(this.client.errors());
        collected.addAll(this.server.errors());
        return List.copyOf(collected);
    }
}
