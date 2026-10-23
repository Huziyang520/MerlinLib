package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.util.MerlinColor;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The NeoForge store for MerlinLib's switches: one spec per half, answered through {@link ConfigReader}.
 */
final class NeoSpecBackend implements ConfigBackend {

    /** Prefix of every translation key this backend sets, shared by both halves. */
    private static final String KEY_PREFIX = Constants.MOD_ID + ".configuration.";

    private final String name;
    private final String fileName;
    private final String legacyFileName;
    private final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
    private final java.util.Deque<String> path = new java.util.ArrayDeque<>();
    private final Map<String, ModConfigSpec.ConfigValue<?>> values = new LinkedHashMap<>();
    private ModConfigSpec spec;
    /** Whether the one time import of the pre-specification file has already been attempted. */
    private boolean importAttempted;

    private NeoSpecBackend(String name, String fileName, String legacyFileName) {
        this.name = name;
        this.fileName = fileName;
        this.legacyFileName = legacyFileName;
    }

    /**
     * Declares one half of the configuration.
     *
     * @param name           the name used in the log
     * @param fileName       the file the loader writes for this half
     * @param legacyFileName the pre-specification file of the same half
     * @param definitions    receives this backend, and declares the keys on it
     * @return the finished backend
     */
    static NeoSpecBackend declare(String name, String fileName, String legacyFileName,
                                  Consumer<NeoSpecBackend> definitions) {
        NeoSpecBackend backend = new NeoSpecBackend(name, fileName, legacyFileName);
        definitions.accept(backend);
        backend.spec = backend.builder.build();
        return backend;
    }

    /** @return the spec to register with the mod container */
    ModConfigSpec spec() {
        return this.spec;
    }

    /** @return the file this half is written to */
    String fileName() {
        return this.fileName;
    }

    NeoSpecBackend push(String path) {
        this.builder.push(path);
        this.path.addLast(path);
        return this;
    }

    NeoSpecBackend pop() {
        this.builder.pop();
        this.path.removeLast();
        return this;
    }

    /**
     * The translation key of one option, as {@code merlinlib.configuration.<section>.<option>}, plus
     * {@code .tooltip} for its tooltip.
     */
    private String label(String option) {
        return KEY_PREFIX + full(option);
    }

    NeoSpecBackend bool(String option, boolean defaultValue, String comment) {
        this.builder.comment(comment);
        this.builder.translation(label(option));
        this.values.put(full(option), this.builder.define(option, defaultValue));
        return this;
    }

    NeoSpecBackend number(String option, int defaultValue, int min, int max, String comment) {
        this.builder.comment(comment);
        this.builder.translation(label(option));
        this.values.put(full(option), this.builder.defineInRange(option, defaultValue, min, max));
        return this;
    }

    NeoSpecBackend decimal(String option, double defaultValue, double min, double max, String comment) {
        this.builder.comment(comment);
        this.builder.translation(label(option));
        this.values.put(full(option), this.builder.defineInRange(option, defaultValue, min, max));
        return this;
    }

    NeoSpecBackend text(String option, String defaultValue, String comment) {
        this.builder.comment(comment);
        this.builder.translation(label(option));
        this.values.put(full(option), this.builder.define(option, defaultValue));
        return this;
    }

    /** @return {@code section.key} of an option declared in the open section */
    private String full(String option) {
        return this.path.isEmpty() ? option : String.join(".", this.path) + "." + option;
    }

    @Override
    public ConfigReader view() {
        return new View(this);
    }

    @Override
    public void write(String key, String value) {
        if (!this.spec.isLoaded()) {
            return;
        }
        ModConfigSpec.ConfigValue<?> target = this.values.get(key);
        if (target == null) {
            return;
        }
        Object converted = convert(target, value);
        if (converted == null) {
            return;
        }
        setUnchecked(target, converted);
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
     * Copies the values of the pre-specification file into this spec, once, and deletes the old file.
     */
    synchronized void importLegacy() {
        if (this.importAttempted || !this.spec.isLoaded()) {
            return;
        }
        this.importAttempted = true;
        Path legacy = ConfigDirectory.root().resolve(this.legacyFileName);
        if (!Files.isRegularFile(legacy)) {
            return;
        }
        TomlFile old = TomlFile.read(legacy, "");
        int copied = 0;
        for (String key : this.values.keySet()) {
            String raw = old.getString(key, null);
            if (raw != null) {
                write(key, raw);
                copied++;
            }
        }
        applyLegacyLayout(old);
        save();
        Constants.LOG.info("[MerlinLib] imported {} value(s) from the old {} into {}", copied,
                this.legacyFileName, this.fileName);
        try {
            Files.delete(legacy);
            Constants.LOG.info("[MerlinLib] deleted the old {} after the import", this.legacyFileName);
        } catch (IOException exception) {
            Constants.LOG.warn("[MerlinLib] could not delete {}: {}", legacy, exception.getMessage());
        }
    }

    /**
     * Applies the one-off layout changes a key by key copy cannot express.
     *
     * <p>Only a value that still holds the old default is moved, because any other value was chosen by hand.
     *
     * @param old the file that was just read
     */
    private void applyLegacyLayout(TomlFile old) {
        if (this.fileName.equals(NeoConfigs.CLIENT_FILE)) {
            int version = old.getInt("general.config_version", 0, 0, Integer.MAX_VALUE);
            int guiVersion = old.getInt("gui.config_version", 0, 0, Integer.MAX_VALUE);
            if (Math.max(version, guiVersion) < 1 && old.getBoolean("floating_text.enabled", true)) {
                // Damage numbers used to be on by default; a file from before that change keeps showing them
                // otherwise, because a file always wins over the defaults.
                write("floating_text.enabled", "false");
            }
            return;
        }
        int version = old.getInt("general.config_version", 0, 0, Integer.MAX_VALUE);
        int toolsVersion = old.getInt("tools.config_version", 0, 0, Integer.MAX_VALUE);
        if (Math.max(version, toolsVersion) >= 4) {
            return;
        }
        if (old.getInt("tools.max_enchantment_level", 255, 1, Integer.MAX_VALUE) == 255) {
            write("tools.max_enchantment_level", Integer.toString(Integer.MAX_VALUE));
        }
        if (old.getInt("tools.max_test_weapon_damage", 100000, 1, Integer.MAX_VALUE) == 100000) {
            write("tools.max_test_weapon_damage", Integer.toString(Integer.MAX_VALUE));
        }
        if (old.getBoolean("tools.allow_editing_all_items", false)) {
            write("tools.allow_editing_all_items", "false");
        }
        if (!old.getBoolean("tools.allow_levels_above_max", false)) {
            write("tools.allow_levels_above_max", "true");
        }
    }

    /** Converts the text of a file line into the type the key was declared with. */
    private Object convert(ModConfigSpec.ConfigValue<?> value, String raw) {
        Object fallback = value.getDefault();
        try {
            if (fallback instanceof Boolean) {
                if ("true".equalsIgnoreCase(raw)) {
                    return Boolean.TRUE;
                }
                if ("false".equalsIgnoreCase(raw)) {
                    return Boolean.FALSE;
                }
                return null;
            }
            if (fallback instanceof Integer) {
                return clamp(value, Integer.parseInt(raw.trim()));
            }
            if (fallback instanceof Long) {
                return clamp(value, Long.parseLong(raw.trim()));
            }
            if (fallback instanceof Double) {
                return clamp(value, Double.parseDouble(raw.trim()));
            }
            if (fallback instanceof String) {
                return raw;
            }
        } catch (NumberFormatException exception) {
            return null;
        }
        return null;
    }

    /** Clamps a number to the range the key was declared with. */
    @SuppressWarnings("unchecked")
    private static <T extends Comparable<? super T>> T clamp(ModConfigSpec.ConfigValue<?> value, T number) {
        ModConfigSpec.ValueSpec spec = value.getSpec();
        ModConfigSpec.Range<?> range = spec == null ? null : spec.getRange();
        if (range == null) {
            return number;
        }
        ModConfigSpec.Range<T> typed = (ModConfigSpec.Range<T>) range;
        if (number.compareTo(typed.getMin()) < 0) {
            return typed.getMin();
        }
        if (number.compareTo(typed.getMax()) > 0) {
            return typed.getMax();
        }
        return number;
    }

    @SuppressWarnings("unchecked")
    private static void setUnchecked(ModConfigSpec.ConfigValue<?> value, Object newOne) {
        ((ModConfigSpec.ConfigValue<Object>) value).set(newOne);
    }

    /** Reads one value at a time, so a reload of the configuration is visible immediately. */
    private record View(NeoSpecBackend backend) implements ConfigReader {

        @Override
        public boolean getBoolean(String key, boolean fallback) {
            ModConfigSpec.ConfigValue<?> value = this.backend.values.get(key);
            return value == null || !this.backend.spec.isLoaded() ? fallback : (Boolean) value.get();
        }

        @Override
        public int getInt(String key, int fallback, int min, int max) {
            ModConfigSpec.ConfigValue<?> value = this.backend.values.get(key);
            if (value == null || !this.backend.spec.isLoaded()) {
                return fallback;
            }
            int read = (Integer) value.get();
            return read < min || read > max ? fallback : read;
        }

        @Override
        public double getDouble(String key, double fallback, double min, double max) {
            ModConfigSpec.ConfigValue<?> value = this.backend.values.get(key);
            if (value == null || !this.backend.spec.isLoaded()) {
                return fallback;
            }
            double read = (Double) value.get();
            return read < min || read > max ? fallback : read;
        }

        @Override
        public int getColor(String key, int fallback) {
            ModConfigSpec.ConfigValue<?> value = this.backend.values.get(key);
            if (value == null || !this.backend.spec.isLoaded()) {
                return fallback;
            }
            return MerlinColor.parse((String) value.get()).orElse(fallback);
        }

        @Override
        public String getString(String key, String fallback) {
            ModConfigSpec.ConfigValue<?> value = this.backend.values.get(key);
            return value == null || !this.backend.spec.isLoaded() ? fallback : (String) value.get();
        }

        @Override
        public List<ContentError> errors() {
            return List.of();
        }
    }
}
