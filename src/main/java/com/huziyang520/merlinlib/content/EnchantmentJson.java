package com.huziyang520.merlinlib.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

/**
 * Small helpers for reading and writing the json fragments the configuration format is made of.
 *
 * <p>On the 26.3 line this class also <em>built</em> the vanilla {@code description} component and the
 * generated datapack's tag references. Neither has a destination on 1.20.1 - the tooltip reads a
 * translation key and {@code supported_items} is evaluated in code - so what is left is the two
 * things that are still genuinely about json: reading an item reference out of a file, and building a
 * reference for the samples the mod writes.
 */
public final class EnchantmentJson {

    private EnchantmentJson() {
    }

    /**
     * Wraps an item id or tag reference into the shape the {@code supported_items} field accepts.
     *
     * <p>{@code "#namespace:tag"} stays a string, a bare name becomes a single element array,
     * mirroring the vanilla datapack syntax the config format documents.
     *
     * @param value the reference as written
     * @return the json form, or {@code null} when there is nothing to wrap
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

    /** Builds a vanilla shaped {@code {"base": x, "per_level_above_first": y}} cost object. */
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

    /**
     * Reads <b>every</b> item reference out of a {@code supported_items} / {@code primary_items} /
     * {@code exclusive_set} value, whichever of the three spellings it was written in.
     *
     * <p>A primitive is one reference, an array contributes all of its primitive elements, and an
     * object is rejected - the vanilla format has no object form for these fields, and silently
     * treating one as a list would turn a typo into an enchantment that matches nothing.
     *
     * <p><b>Why this returns a list and not a single reference.</b> The vanilla format explicitly
     * allows a list, and a real definition uses it: Practical Enchantments' {@code disguise}
     * declares seven head items as an array. An earlier version of this method returned only the
     * <em>first</em> primitive it found, which made six of those seven heads silently unmatchable -
     * the enchantment registered, appeared everywhere, and worked on one item type instead of seven.
     * That is the exact silent-failure shape this project keeps writing code to avoid, so the list
     * form is now read in full and the caller unions it.
     *
     * @param element the json value, may be {@code null}
     * @return the references, in written order; empty when the value is absent or not a reference
     */
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

    /**
     * Reads one item reference out of a {@code supported_items} / {@code primary_items} / {@code
     * exclusive_set} value.
     *
     * <p>Kept for the callers that genuinely want a single reference. A caller that is deciding
     * whether an item is allowed should use {@link #references(JsonElement)} instead - see the note
     * there about why the single-value form is a trap for a multi-item declaration.
     *
     * @param element the json value, may be {@code null}
     * @return the first reference, or empty when the value is absent or not a reference
     */
    public static java.util.Optional<String> reference(JsonElement element) {
        java.util.List<String> all = references(element);
        return all.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(all.get(0));
    }

    /**
     * Splits {@code "#namespace:tag"} or {@code "namespace:item"} into an id, defaulting the
     * namespace to {@code minecraft} the way vanilla resource references do.
     *
     * @param reference the reference, with or without a leading {@code #}
     * @return the id, or empty when it is not a legal identifier
     */
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
