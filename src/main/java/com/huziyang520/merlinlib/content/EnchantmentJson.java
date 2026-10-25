// Small helpers for reading and writing the json fragments the configuration format is made of.

package com.huziyang520.merlinlib.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

public final class EnchantmentJson {
    private EnchantmentJson() {
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

    public static com.google.gson.JsonObject cost(int base, int perLevel) {
        com.google.gson.JsonObject object = new com.google.gson.JsonObject();
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

    public static java.util.List<String> references(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return java.util.List.of();
        }
        if (element.isJsonPrimitive()) {
            return java.util.List.of(element.getAsString());
        }
        if (element.isJsonArray()) {
            java.util.List<String> found = new java.util.ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonPrimitive()) {
                    found.add(item.getAsString());
                }
            }
            return java.util.List.copyOf(found);
        }
        return java.util.List.of();
    }

    public static java.util.Optional<String> reference(JsonElement element) {
        java.util.List<String> all = references(element);
        return all.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(all.get(0));
    }

    public static java.util.Optional<ResourceLocation> idOf(String reference) {
        if (reference == null || reference.isBlank()) {
            return java.util.Optional.empty();
        }
        String text = reference.trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.isEmpty()) {
            return java.util.Optional.empty();
        }
        if (!text.contains(":")) {
            text = "minecraft:" + text;
        }
        try {
            return java.util.Optional.of(new ResourceLocation(text));
        } catch (RuntimeException exception) {
            return java.util.Optional.empty();
        }
    }
}
