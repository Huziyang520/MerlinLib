// Turns EnchantmentDraft drafts into the files of the generated datapack.

package com.huziyang520.merlinlib.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.huziyang520.merlinlib.content.Acquisition;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.content.EnchantmentJson;
import net.minecraft.resources.ResourceLocation;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class EnchantmentJsonWriter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final ResourceLocation TAG_IN_ENCHANTING_TABLE = ResourceLocation.withDefaultNamespace("in_enchanting_table");
    public static final ResourceLocation TAG_NON_TREASURE = ResourceLocation.withDefaultNamespace("non_treasure");
    public static final ResourceLocation TAG_TREASURE = ResourceLocation.withDefaultNamespace("treasure");
    public static final ResourceLocation TAG_TRADEABLE = ResourceLocation.withDefaultNamespace("tradeable");
    public static final ResourceLocation TAG_ON_TRADED_EQUIPMENT = ResourceLocation.withDefaultNamespace("on_traded_equipment");
    public static final ResourceLocation TAG_ON_RANDOM_LOOT = ResourceLocation.withDefaultNamespace("on_random_loot");
    public static final ResourceLocation TAG_CURSE = ResourceLocation.withDefaultNamespace("curse");

    private EnchantmentJsonWriter() {
    }

    public static JsonObject toJson(EnchantmentDraft draft) {
        JsonObject json = new JsonObject();
        json.add("description", draft.description());
        json.addProperty("weight", draft.weight());
        json.addProperty("max_level", draft.maxLevel());
        json.add("min_cost", EnchantmentJson.cost(draft.minCostBase(), draft.minCostPerLevel()));
        json.add("max_cost", EnchantmentJson.cost(draft.maxCostBase(), draft.maxCostPerLevel()));
        json.addProperty("anvil_cost", draft.anvilCost());
        json.add("slots", EnchantmentJson.stringArray(draft.slots()));
        json.add("supported_items", draft.supportedItems());
        if (draft.primaryItems() != null) {
            json.add("primary_items", draft.primaryItems());
        }
        if (draft.exclusiveSet() != null) {
            json.add("exclusive_set", draft.exclusiveSet());
        }
        if (!draft.effects().isEmpty()) {
            json.add("effects", draft.effects());
        }
        return json;
    }

    public static byte[] toBytes(EnchantmentDraft draft) {
        return GSON.toJson(toJson(draft)).getBytes(StandardCharsets.UTF_8);
    }

    public static Map<ResourceLocation, JsonObject> acquisitionTags(Collection<EnchantmentDraft> drafts) {
        Map<ResourceLocation, List<ResourceLocation>> perTag = new TreeMap<>(java.util.Comparator.comparing(ResourceLocation::toString));
        for (EnchantmentDraft draft : drafts) {
            Acquisition acquisition = draft.acquisition();
            if (acquisition.enchantingTable()) {
                perTag.computeIfAbsent(TAG_IN_ENCHANTING_TABLE, key -> new java.util.ArrayList<>()).add(draft.id());
            }
            if (acquisition.villagerTrade()) {
                perTag.computeIfAbsent(TAG_TRADEABLE, key -> new java.util.ArrayList<>()).add(draft.id());
                perTag.computeIfAbsent(TAG_ON_TRADED_EQUIPMENT, key -> new java.util.ArrayList<>()).add(draft.id());
            }
            if (acquisition.randomLoot()) {
                perTag.computeIfAbsent(TAG_ON_RANDOM_LOOT, key -> new java.util.ArrayList<>()).add(draft.id());
            }
            if (acquisition.treasureOnly()) {
                perTag.computeIfAbsent(TAG_TREASURE, key -> new java.util.ArrayList<>()).add(draft.id());
            } else {
                perTag.computeIfAbsent(TAG_NON_TREASURE, key -> new java.util.ArrayList<>()).add(draft.id());
            }
            if (acquisition.curse()) {
                perTag.computeIfAbsent(TAG_CURSE, key -> new java.util.ArrayList<>()).add(draft.id());
            }
        }

        Map<ResourceLocation, JsonObject> files = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : perTag.entrySet()) {
            JsonObject file = new JsonObject();
            file.addProperty("replace", false);
            JsonArray values = new JsonArray();
            entry.getValue().stream().sorted().forEach(id -> values.add(id.toString()));
            file.add("values", values);
            files.put(entry.getKey(), file);
        }
        return files;
    }

    public static byte[] tagBytes(JsonObject tag) {
        return GSON.toJson(tag).getBytes(StandardCharsets.UTF_8);
    }
}
