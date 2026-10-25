// Per enchantment coefficient table, read from config/MerlinLib/enchantments.toml and falling back to the caller default.

package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;
import net.minecraft.resources.Identifier;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class Coefficients {
    private static final Path PATH = Path.of("config", Constants.CONFIG_DIRECTORY, "enchantments.toml");
    private static final String TEMPLATE = """
            # One section per enchantment, named exactly after its id, holding the numbers that enchantment reads.
            # Headers are written without quotes because this file is read by the small TOML subset reader.
            # A missing section or a missing key falls back to the default declared by the mod that owns the enchantment.
            #
            # [practical_enchantments:lifesteal]
            # heal_ratio = 0.04
            """;

    private final Map<String, String> values;
    private final List<ContentError> errors;

    private Coefficients(Map<String, String> values, List<ContentError> errors) {
        this.values = Map.copyOf(values);
        this.errors = List.copyOf(errors);
    }

    public static Coefficients load() {
        TomlFile file = TomlFile.read(PATH, TEMPLATE);
        return new Coefficients(file.values(), file.errors());
    }

    public static Coefficients empty() {
        return new Coefficients(Map.of(), List.of());
    }

    public boolean defined(Identifier id, String key) {
        return this.values.containsKey(full(id, key));
    }

    public double coefficient(Identifier id, String key, double fallback) {
        String raw = this.values.get(full(id, key));
        if (raw == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public int coefficient(Identifier id, String key, int fallback) {
        double raw = coefficient(id, key, Double.NaN);
        if (Double.isNaN(raw) || raw != Math.rint(raw) || raw < Integer.MIN_VALUE || raw > Integer.MAX_VALUE) {
            return fallback;
        }
        return (int) raw;
    }

    public List<ContentError> errors() {
        return this.errors;
    }

    private static String full(Identifier id, String key) {
        return id + "." + key;
    }
}
