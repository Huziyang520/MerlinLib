package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.util.MerlinColor;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The NeoForge store for MerlinLib's switches.
 *
 * <p>NeoForge already owns a configuration system - a {@code ModConfigSpec}, the file it generates for it
 * and the screen that edits it - so MerlinLib hands its switches over instead of keeping a second format
 * of its own. This class is the bridge: it declares every key once, in the spec, and answers the
 * {@link ConfigReader} calls from the values the spec holds.
 *
 * <p>Two things are deliberate:
 *
 * <ul>
 *   <li><b>values are read through the spec, never cached here.</b> NeoForge can reload a configuration at
 *       any time, and a copy kept in this class would keep answering with the old value afterwards.</li>
 *   <li><b>nothing is read before the spec reports itself loaded.</b> {@link ConfigValue#get()} throws in
 *       that window, and a mod that throws while the game is starting is worse than one that answers with
 *       the default, which is what this class does instead.</li>
 * </ul>
 *
 * <p>Colours are stored as text ({@code 0xRRGGBB}, {@code #RRGGBB} or a palette name) because that is the
 * form the file and the screen both understand, and because an integer field in a configuration screen
 * would show a colour as a meaningless decimal.
 *
 * <p>Internal to the NeoForge module; see {@link NeoConfigs} for the two instances.
 */
final class NeoSpecBackend implements ConfigBackend {

    private final String name;
    private final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
    private final Map<String, ModConfigSpec.BooleanValue> booleans = new LinkedHashMap<>();
    private final Map<String, ModConfigSpec.IntValue> integers = new LinkedHashMap<>();
    private final Map<String, ModConfigSpec.DoubleValue> doubles = new LinkedHashMap<>();
    private final Map<String, ModConfigSpec.ConfigValue<String>> strings = new LinkedHashMap<>();
    private ModConfigSpec spec;

    private NeoSpecBackend(String name) {
        this.name = name;
    }

    /**
     * Declares one half of the configuration.
     *
     * @param name        the name used in the log
     * @param definitions receives this backend, and declares the keys on it
     * @return the finished backend
     */
    static NeoSpecBackend declare(String name, Consumer<NeoSpecBackend> definitions) {
        NeoSpecBackend backend = new NeoSpecBackend(name);
        definitions.accept(backend);
        backend.spec = backend.builder.build();
        return backend;
    }

    /** @return the spec to register with the mod container */
    ModConfigSpec spec() {
        return this.spec;
    }

    /**
     * Opens a section.
     *
     * @param path the section name
     * @return this backend, for chaining
     */
    NeoSpecBackend push(String path) {
        this.builder.push(path);
        return this;
    }

    /**
     * Closes the innermost section.
     *
     * @return this backend, for chaining
     */
    NeoSpecBackend pop() {
        this.builder.pop();
        return this;
    }

    NeoSpecBackend bool(String path, boolean defaultValue, String comment) {
        this.builder.comment(comment);
        this.booleans.put(path, this.builder.define(path, defaultValue));
        return this;
    }

    NeoSpecBackend number(String path, int defaultValue, int min, int max, String comment) {
        this.builder.comment(comment);
        this.integers.put(path, this.builder.defineInRange(path, defaultValue, min, max));
        return this;
    }

    NeoSpecBackend decimal(String path, double defaultValue, double min, double max, String comment) {
        this.builder.comment(comment);
        this.doubles.put(path, this.builder.defineInRange(path, defaultValue, min, max));
        return this;
    }

    NeoSpecBackend text(String path, String defaultValue, String comment) {
        this.builder.comment(comment);
        this.strings.put(path, this.builder.define(path, defaultValue));
        return this;
    }

    @Override
    public ConfigReader view() {
        return new View(this);
    }

    @Override
    public void write(String key, String value) {
        if (!this.spec.isLoaded()) {
            // Nothing can be written before the configuration has been read; the value is simply not carried
            // over, which is the same answer the reader would give for a key it does not know.
            return;
        }
        try {
            if (this.booleans.containsKey(key)) {
                this.booleans.get(key).set(Boolean.parseBoolean(value));
            } else if (this.integers.containsKey(key)) {
                this.integers.get(key).set(Integer.decode(value));
            } else if (this.doubles.containsKey(key)) {
                this.doubles.get(key).set(Double.parseDouble(value));
            } else if (this.strings.containsKey(key)) {
                this.strings.get(key).set(value);
            }
        } catch (RuntimeException exception) {
            // A value the spec's own range check rejects must not break the caller: the default stays.
            Constants.LOG.warn(
                    "[MerlinLib] {} rejected {}={}: {}", this.name, key, value, exception.getMessage());
        }
    }

    @Override
    public void save() {
        if (this.spec.isLoaded()) {
            this.spec.save();
        }
    }

    @Override
    public String name() {
        return this.name;
    }

    /**
     * Reads one value at a time, so a reload of the configuration is visible immediately.
     */
    private record View(NeoSpecBackend backend) implements ConfigReader {

        @Override
        public boolean getBoolean(String key, boolean fallback) {
            ModConfigSpec.BooleanValue value = this.backend.booleans.get(key);
            return value == null || !this.backend.spec.isLoaded() ? fallback : value.get();
        }

        @Override
        public int getInt(String key, int fallback, int min, int max) {
            ModConfigSpec.IntValue value = this.backend.integers.get(key);
            if (value == null || !this.backend.spec.isLoaded()) {
                return fallback;
            }
            int read = value.get();
            return read < min || read > max ? fallback : read;
        }

        @Override
        public double getDouble(String key, double fallback, double min, double max) {
            ModConfigSpec.DoubleValue value = this.backend.doubles.get(key);
            if (value == null || !this.backend.spec.isLoaded()) {
                return fallback;
            }
            double read = value.get();
            return read < min || read > max ? fallback : read;
        }

        @Override
        public int getColor(String key, int fallback) {
            ModConfigSpec.ConfigValue<String> value = this.backend.strings.get(key);
            if (value == null || !this.backend.spec.isLoaded()) {
                return fallback;
            }
            return MerlinColor.parse(value.get()).orElse(fallback);
        }

        @Override
        public String getString(String key, String fallback) {
            ModConfigSpec.ConfigValue<String> value = this.backend.strings.get(key);
            return value == null || !this.backend.spec.isLoaded() ? fallback : value.get();
        }

        @Override
        public List<ContentError> errors() {
            // The spec validates every value it reads and corrects what it can, so there is nothing left to
            // report by the time it is asked.
            return List.of();
        }
    }
}
