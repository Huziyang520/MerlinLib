package com.huziyang520.merlinlib.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.Identifier;

/**
 * Small helpers for building vanilla shaped json fragments without depending on any loader type.
 */
public final class EnchantmentJson {

    private EnchantmentJson() {
    }

    /** Builds the vanilla {@code description} component for an enchantment id. */
    public static JsonElement defaultDescription(Identifier id) {
        JsonObject object = new JsonObject();
        object.addProperty("translate", EnchantmentDraft.defaultTranslationKey(id));
        return object;
    }

    /**
     * Wraps an item id or tag reference into the shape the vanilla codec accepts.
     *
     * <p>{@code "#namespace:tag"} stays a string, everything else becomes a single element array.
     */
    public static JsonElement tag(String value) {
        if (value == null) {
            return null;
        }
        if (value.startsWith("#") || value.contains(":")) {
            return new JsonPrimitive(value);
        }
        JsonArray array = new JsonArray();
        array.add(value);
        return array;
    }

    /** Builds a vanilla {@code {"base": x, "per_level_above_first": y}} cost object. */
    public static JsonObject cost(int base, int perLevel) {
        JsonObject object = new JsonObject();
        object.addProperty("base", base);
        object.addProperty("per_level_above_first", perLevel);
        return object;
    }

    public static JsonArray stringArray(Iterable<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }
}
