package com.huziyang520.merlinlib.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.huziyang520.merlinlib.content.Acquisition;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.content.EnchantmentJson;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Turns {@link EnchantmentDraft drafts} into the files of the generated datapack.
 *
 * <p>Everything produced here uses the vanilla enchantment format unchanged, so the result behaves
 * exactly like a hand written datapack and stays compatible with datapack inspection tools.
 */
public final class EnchantmentJsonWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** Vanilla tag that makes an enchantment appear in the enchanting table. */
    public static final Identifier TAG_IN_ENCHANTING_TABLE = Identifier.withDefaultNamespace("in_enchanting_table");
    /** Vanilla tag for enchantments that are not treasure. */
    public static final Identifier TAG_NON_TREASURE = Identifier.withDefaultNamespace("non_treasure");
    /** Vanilla tag for treasure enchantments. */
    public static final Identifier TAG_TREASURE = Identifier.withDefaultNamespace("treasure");
    /** Vanilla tag for librarian trades. */
    public static final Identifier TAG_TRADEABLE = Identifier.withDefaultNamespace("tradeable");
    /** Vanilla tag for equipment sold by villagers. */
    public static final Identifier TAG_ON_TRADED_EQUIPMENT = Identifier.withDefaultNamespace("on_traded_equipment");
    /** Vanilla tag for randomly enchanted loot, used by fishing and loot chests. */
    public static final Identifier TAG_ON_RANDOM_LOOT = Identifier.withDefaultNamespace("on_random_loot");
    /** Vanilla tag marking a curse. */
    public static final Identifier TAG_CURSE = Identifier.withDefaultNamespace("curse");

    private EnchantmentJsonWriter() {
    }

    /**
     * @param draft the enchantment to serialize
     * @return the vanilla shaped json object
     */
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

    /**
     * @param draft the enchantment to serialize
     * @return the exact bytes written into {@code data/<namespace>/enchantment/<path>.json}
     */
    public static byte[] toBytes(EnchantmentDraft draft) {
        return GSON.toJson(toJson(draft)).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Builds one tag file per vanilla acquisition tag, listing the MerlinLib enchantments that asked
     * for it. Tag files never use {@code replace: true}, so they extend the vanilla tags instead of
     * replacing them.
     *
     * @param drafts all effective enchantments
     * @return tag id to file content, keyed by {@code <namespace>/<path>} under {@code tags/enchantment}
     */
    public static Map<Identifier, JsonObject> acquisitionTags(Collection<EnchantmentDraft> drafts) {
        Map<Identifier, List<Identifier>> perTag = new TreeMap<>(java.util.Comparator.comparing(Identifier::toString));
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

        Map<Identifier, JsonObject> files = new LinkedHashMap<>();
        for (Map.Entry<Identifier, List<Identifier>> entry : perTag.entrySet()) {
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
