// Small helpers for building vanilla shaped json fragments without depending on any loader type.

package com.huziyang520.merlinlib.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

public final class EnchantmentJson {
    private EnchantmentJson() {
    }

    public static JsonElement defaultDescription(ResourceLocation id) {
        JsonObject object = new JsonObject();
        object.addProperty("translate", EnchantmentDraft.defaultTranslationKey(id));
        return object;
    }

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
