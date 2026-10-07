package com.huziyang520.merlinlib.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.Acquisition;
import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.content.ContentSource;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.content.EnchantmentJson;
import com.huziyang520.merlinlib.effect.EffectOverrides;
import com.huziyang520.merlinlib.impl.IdValidator;
import com.huziyang520.merlinlib.util.MerlinColor;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads {@code config/MerlinLib/*.json} into immutable enchantment drafts.
 *
 * <h2>What changed from the 26.3 line</h2>
 *
 * <p>The file format did not change at all. What changed is what happens to the result: on 26.3 these
 * drafts were written back out as a generated datapack, so a field the datapack could not express
 * simply travelled through as json. On 1.20.1 they become the constructor arguments of a registered
 * {@code Enchantment}, which means every field has to be a value this version actually has. The
 * reader therefore resolves the two fields that changed type at parse time and reports the one that
 * has no destination:
 *
 * <ul>
 *   <li>{@code slots} is resolved into the slot names the draft maps onto {@code EquipmentSlot}; an
 *       unknown name is reported rather than silently widening the enchantment to every slot</li>
 *   <li>{@code supported_items} / {@code primary_items} / {@code exclusive_set} keep their json shape,
 *       because the draft parses them into item and tag lookups when the enchantment is created</li>
 *   <li>{@code description} is reduced to its {@code translate} value - the only part 1.20.1 has a
 *       place for, since an enchantment's name is a language key on this version</li>
 *   <li>{@code effects} is read and reported as unusable: the numeric effect system it describes
 *       arrives in 1.20.5. A file that declares effects gets a warning naming the id, once, instead
 *       of an enchantment that silently does nothing</li>
 * </ul>
 *
 * <p>Design rules, unchanged: one broken file never stops the others; every failure carries file +
 * json path + expectation; nothing is logged from here, the caller decides how loud to be.
 *
 * <p>Accepted file shapes:
 * <pre>{@code
 * [ { "id": "unbreakable", ... } ]                       // plain array
 * { "enchantments": [ { "id": "unbreakable", ... } ] }    // keyed array (recommended)
 * { "id": "unbreakable", ... }                            // single entry
 * }</pre>
 */
public final class ContentConfigLoader {

    private ContentConfigLoader() {
    }

    /**
     * Reads every file and merges the content with the java api registrations.
     *
     * @param apiEntries    enchantments registered through {@code MerlinApi}
     * @param disabledByApi ids disabled through {@code MerlinApi}
     * @return the effective snapshot, never {@code null}
     */
    public static ContentSnapshot load(Collection<EnchantmentDraft> apiEntries, Set<ResourceLocation> disabledByApi) {
        List<ContentError> errors = new ArrayList<>();
        Map<ResourceLocation, EnchantmentDraft> fromConfig = new LinkedHashMap<>();
        Set<ResourceLocation> disabled = new LinkedHashSet<>(disabledByApi);

        ensureDirectory();

        Map<ResourceLocation, EffectOverrides.Entry> effectOverrides = new LinkedHashMap<>();
        for (Path file : ConfigDirectory.jsonFiles()) {
            String name = file.getFileName().toString();
            if (RESERVED_FILES.contains(name)) {
                // MerlinLib's own files sit in the same folder as the content files. Reading one of them as a
                // list of enchantments does not fail loudly - it fails per entry, so a macro file with two
                // macros produced two "this definition could not be loaded" warnings on every load, which
                // made a healthy configuration look broken. Names listed here are simply not content.
                continue;
            }
            if (EFFECTS_FILE.equals(name)) {
                readEffectOverrides(file, effectOverrides, errors);
            } else {
                readFile(file, fromConfig, disabled, errors);
            }
        }

        Map<ResourceLocation, EnchantmentDraft> effective = new LinkedHashMap<>();
        for (EnchantmentDraft draft : apiEntries) {
            effective.put(draft.id(), draft);
        }
        for (Map.Entry<ResourceLocation, EnchantmentDraft> entry : fromConfig.entrySet()) {
            ResourceLocation id = entry.getKey();
            EnchantmentDraft draft = entry.getValue();
            EnchantmentDraft existing = effective.get(id);
            if (existing != null && existing.source().overrides(draft.source())) {
                Constants.LOG.warn(
                        "[MerlinLib] {} is declared by the {} and also by a config file; keeping the {} one",
                        id, existing.source().displayName(), existing.source().displayName()
                );
                continue;
            }
            if (existing != null) {
                Constants.LOG.info("[MerlinLib] config file overrides the {} declaration of {}", existing.source().displayName(), id);
            }
            effective.put(id, draft);
        }

        for (ResourceLocation id : disabled) {
            EnchantmentDraft removed = effective.remove(id);
            if (removed != null) {
                Constants.LOG.info("[MerlinLib] {} is disabled, it will not exist in game", id);
            }
        }

        return new ContentSnapshot(effective, List.copyOf(disabled), effectOverrides, errors);
    }

    private static void readFile(Path file, Map<ResourceLocation, EnchantmentDraft> into, Set<ResourceLocation> disabled, List<ContentError> errors) {
        String fileName = file.getFileName().toString();
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            errors.add(new ContentError(fileName, "<root>", "-", "a readable UTF-8 text file", exception.getMessage()));
            return;
        }

        JsonElement root;
        try {
            root = JsonParser.parseString(text);
        } catch (RuntimeException exception) {
            errors.add(new ContentError(fileName, "<root>", "-", "valid json", exception.getMessage()));
            return;
        }

        List<JsonElement> entries = new ArrayList<>();
        if (root.isJsonArray()) {
            root.getAsJsonArray().forEach(entries::add);
        } else if (root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            if (object.has("enchantments")) {
                JsonElement list = object.get("enchantments");
                if (list.isJsonArray()) {
                    list.getAsJsonArray().forEach(entries::add);
                } else {
                    errors.add(new ContentError(fileName, "enchantments", "-", "an array", "found " + describe(list)));
                    return;
                }
            } else {
                entries.add(object);
            }
        } else {
            errors.add(new ContentError(fileName, "<root>", "-", "an object or an array", "found " + describe(root)));
            return;
        }

        int index = -1;
        for (JsonElement element : entries) {
            index++;
            String path = "enchantments[" + index + "]";
            if (!element.isJsonObject()) {
                errors.add(new ContentError(fileName, path, "-", "an object", "found " + describe(element)));
                continue;
            }
            readEntry(fileName, path, element.getAsJsonObject(), into, disabled, errors);
        }
    }

    private static void readEntry(String file, String path, JsonObject object, Map<ResourceLocation, EnchantmentDraft> into,
                                  Set<ResourceLocation> disabled, List<ContentError> errors) {
        JsonElement idElement = object.get("id");
        if (idElement == null || !idElement.isJsonPrimitive()) {
            errors.add(new ContentError(file, path + ".id", "-", "a string id, e.g. \"unbreakable\"", "missing"));
            return;
        }
        String rawId = idElement.getAsString();
        Optional<String> idProblem = IdValidator.describeProblem(rawId);
        if (idProblem.isPresent()) {
            errors.add(new ContentError(file, path + ".id", rawId, "a legal identifier", idProblem.get()));
            return;
        }
        ResourceLocation id = IdValidator.parse(rawId).orElseThrow();

        boolean enabled = boolField(object, "enabled", true, file, path, id.toString(), errors);

        String translationKey = translationKeyField(object, id);

        int weight = intField(object, "weight", 10, 1, 1024, file, path, id, errors);
        int maxLevel = intField(object, "max_level", 1, 1, 255, file, path, id, errors);
        int anvilCost = intField(object, "anvil_cost", 1, 0, Integer.MAX_VALUE, file, path, id, errors);

        int[] minCost = costField(object, "min_cost", 1, 11, file, path, id, errors);
        int[] maxCost = costField(object, "max_cost", 21, 11, file, path, id, errors);

        List<String> slots = stringList(object, "slots", List.of("any"), file, path, id, errors);
        validateSlots(slots, file, path, id, errors);
        JsonElement supportedItems = elementOrDefault(object, "supported_items", EnchantmentJson.tag("minecraft:enchantable/durability"));
        JsonElement primaryItems = object.has("primary_items") ? object.get("primary_items") : null;
        JsonElement exclusiveSet = object.has("exclusive_set") ? object.get("exclusive_set") : null;
        JsonObject effects = objectField(object, "effects", file, path, id, errors);
        Acquisition acquisition = acquisitionField(object, file, path, id, errors);

        if (effects.size() > 0) {
            // Reported here, per id, because the alternative is an enchantment that exists, shows up in
            // every list, and does nothing whatsoever when used. The numeric effect system these entries
            // are written for (minecraft:add, LevelBasedValue) arrives in 1.20.5; on 1.20.1 behaviour
            // comes from the event api. Saying so once per entry beats saying nothing at all.
            Constants.LOG.warn("[MerlinLib] {} declares {} numeric effect(s); 1.20.1 has no data driven enchantment "
                            + "effects, so they are recorded but not applied. Give the enchantment its behaviour through "
                            + "MerlinApi.events() instead.", id, effects.size());
        }

        EnchantmentDraft draft = new EnchantmentDraft(
                id,
                ContentSource.CONFIG,
                enabled,
                translationKey,
                weight,
                maxLevel,
                minCost[0],
                minCost[1],
                maxCost[0],
                maxCost[1],
                anvilCost,
                slots,
                supportedItems,
                primaryItems,
                exclusiveSet,
                effects,
                acquisition,
                // A config file has no way to name a vanilla category, and the declared item set is
                // what actually filters this enchantment, so the default (accept anything) stands.
                null
        );

        if (!enabled) {
            disabled.add(id);
            return;
        }

        EnchantmentDraft previous = into.put(id, draft);
        if (previous != null) {
            Constants.LOG.warn("[MerlinLib] {} is declared twice in the config directory, the later file wins", id);
        }
    }

    /**
     * Reads the language key, from either the {@code translation_key} field or a {@code description}
     * component.
     *
     * <p>A {@code description} written as a literal {@code text} is accepted and ignored: 1.20.1 looks
     * the name up by key, and a hard coded string would show up untranslated in every language. The
     * documented spelling on this version is {@code translation_key}.
     */
    private static String translationKeyField(JsonObject object, ResourceLocation id) {
        JsonElement key = object.get("translation_key");
        if (key != null && key.isJsonPrimitive()) {
            return key.getAsString();
        }
        JsonElement description = object.get("description");
        if (description != null && description.isJsonObject()) {
            JsonElement translate = description.getAsJsonObject().get("translate");
            if (translate != null && translate.isJsonPrimitive()) {
                return translate.getAsString();
            }
        }
        return EnchantmentDraft.defaultTranslationKey(id);
    }

    /**
     * Reports slot names that cannot be resolved.
     *
     * <p>A name the draft does not recognise is not fatal - the draft falls back to "every slot" - but it
     * almost always means a typo, and an enchantment that quietly applies to everything is far harder to
     * notice than one that reports its own misspelling.
     */
    private static void validateSlots(List<String> slots, String file, String path, ResourceLocation id, List<ContentError> errors) {
        for (String slot : slots) {
            String value = slot == null ? "" : slot.trim().toLowerCase(java.util.Locale.ROOT);
            boolean known = switch (value) {
                case "any", "hand", "armor", "body", "mainhand", "main_hand", "offhand", "off_hand",
                     "head", "helmet", "chest", "chestplate", "legs", "leggings", "feet", "boots" -> true;
                default -> false;
            };
            if (!known) {
                errors.add(new ContentError(file, path + ".slots", id.toString(),
                        "one of any, mainhand, offhand, head, chest, legs, feet",
                        "found '" + slot + "', which this version has no slot for"));
            }
        }
    }

    private static int intField(JsonObject object, String key, int fallback, int min, int max, String file, String path,
                                ResourceLocation id, List<ContentError> errors) {
        JsonElement element = object.get(key);
        if (element == null) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            errors.add(new ContentError(file, path + "." + key, id.toString(), "an integer in [" + min + ", " + max + "]", "found " + describe(element)));
            return fallback;
        }
        int value = element.getAsInt();
        if (value < min || value > max) {
            errors.add(new ContentError(file, path + "." + key, id.toString(), "an integer in [" + min + ", " + max + "]", "found " + value));
            return fallback;
        }
        return value;
    }

    private static boolean boolField(JsonObject object, String key, boolean fallback, String file, String path,
                                     String id, List<ContentError> errors) {
        JsonElement element = object.get(key);
        if (element == null) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            errors.add(new ContentError(file, path + "." + key, id, "true or false", "found " + describe(element)));
            return fallback;
        }
        return element.getAsBoolean();
    }

    private static int[] costField(JsonObject object, String key, int base, int perLevel, String file, String path,
                                   ResourceLocation id, List<ContentError> errors) {
        JsonElement element = object.get(key);
        if (element == null) {
            return new int[] { base, perLevel };
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return new int[] { element.getAsInt(), 0 };
        }
        if (!element.isJsonObject()) {
            errors.add(new ContentError(file, path + "." + key, id.toString(), "an integer or {base, per_level_above_first}",
                    "found " + describe(element)));
            return new int[] { base, perLevel };
        }
        JsonObject cost = element.getAsJsonObject();
        int resolvedBase = intField(cost, "base", base, 0, Integer.MAX_VALUE, file, path + "." + key, id, errors);
        int resolvedPerLevel = intField(cost, "per_level_above_first", 0, 0, Integer.MAX_VALUE, file, path + "." + key, id, errors);
        return new int[] { resolvedBase, resolvedPerLevel };
    }

    private static List<String> stringList(JsonObject object, String key, List<String> fallback, String file, String path,
                                           ResourceLocation id, List<ContentError> errors) {
        JsonElement element = object.get(key);
        if (element == null) {
            return fallback;
        }
        if (element.isJsonPrimitive()) {
            return List.of(element.getAsString());
        }
        if (!element.isJsonArray()) {
            errors.add(new ContentError(file, path + "." + key, id.toString(), "a string or an array of strings", "found " + describe(element)));
            return fallback;
        }
        List<String> values = new ArrayList<>();
        for (JsonElement item : element.getAsJsonArray()) {
            if (!item.isJsonPrimitive()) {
                errors.add(new ContentError(file, path + "." + key, id.toString(), "an array of strings", "found " + describe(item)));
                continue;
            }
            values.add(item.getAsString());
        }
        return values.isEmpty() ? fallback : values;
    }

    private static JsonObject objectField(JsonObject object, String key, String file, String path, ResourceLocation id,
                                          List<ContentError> errors) {
        JsonElement element = object.get(key);
        if (element == null) {
            return new JsonObject();
        }
        if (!element.isJsonObject()) {
            errors.add(new ContentError(file, path + "." + key, id.toString(), "an object", "found " + describe(element)));
            return new JsonObject();
        }
        return element.getAsJsonObject();
    }

    private static Acquisition acquisitionField(JsonObject object, String file, String path, ResourceLocation id,
                                                List<ContentError> errors) {
        JsonElement element = object.get("acquisition");
        if (element == null) {
            return Acquisition.DEFAULT;
        }
        if (!element.isJsonObject()) {
            errors.add(new ContentError(file, path + ".acquisition", id.toString(), "an object", "found " + describe(element)));
            return Acquisition.DEFAULT;
        }
        JsonObject acquisition = element.getAsJsonObject();
        Acquisition defaults = Acquisition.DEFAULT;
        String base = path + ".acquisition";
        return new Acquisition(
                boolField(acquisition, "enchanting_table", defaults.enchantingTable(), file, base, id.toString(), errors),
                boolField(acquisition, "villager_trade", defaults.villagerTrade(), file, base, id.toString(), errors),
                boolField(acquisition, "fishing", defaults.fishing(), file, base, id.toString(), errors),
                boolField(acquisition, "loot_chest", defaults.lootChest(), file, base, id.toString(), errors),
                boolField(acquisition, "treasure_only", defaults.treasureOnly(), file, base, id.toString(), errors),
                boolField(acquisition, "curse", defaults.curse(), file, base, id.toString(), errors)
        );
    }

    private static JsonElement elementOrDefault(JsonObject object, String key, JsonElement fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element;
    }

    private static String describe(JsonElement element) {
        if (element == null) {
            return "nothing";
        }
        if (element.isJsonNull()) {
            return "null";
        }
        if (element.isJsonArray()) {
            return "an array";
        }
        if (element.isJsonObject()) {
            return "an object";
        }
        return "'" + element.getAsString() + "'";
    }

    /**
     * Creates {@code config/MerlinLib} together with a documented sample file on first launch so the
     * feature is discoverable without reading the source code.
     *
     * <p>The two {@code .toml} files are written by {@code ClientConfig}/{@code ServerConfig} themselves
     * when they are first read, so this only writes the content samples.
     */
    public static void ensureDirectory() {
        Path root = ConfigDirectory.root();
        try {
            Files.createDirectories(root);
            Path sample = root.resolve("enchantments.json");
            if (!Files.exists(sample)) {
                Files.writeString(sample, SAMPLE_ENCHANTMENTS, StandardCharsets.UTF_8);
            }
            Path effects = root.resolve(EFFECTS_FILE);
            if (!Files.exists(effects)) {
                Files.writeString(effects, SAMPLE_EFFECTS, StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            Constants.LOG.error("Could not create the MerlinLib config directory at {}", root, exception);
        }
    }

    /** File name holding effect colour and disable overrides. */
    public static final String EFFECTS_FILE = "effects.json";

    /**
     * Files in the configuration folder that are not enchantment content.
     *
     * <p>{@code macros.json} belongs to the macro toolkit ({@code MacroStorage}); anything else added here
     * must be a file MerlinLib itself owns and writes.
     */
    private static final Set<String> RESERVED_FILES = Set.of("macros.json");

    /**
     * Reads {@code effects.json}: a list of entries shaped
     * {@code {"id": ..., "enabled": true, "color": "cyan"}}.
     *
     * <p>Effects are a code registry, so this file can only recolour or disable an effect that a mod
     * declared in code; it can never create a new one. That boundary is intentional and documented in
     * the README.
     */
    private static void readEffectOverrides(Path file, Map<ResourceLocation, EffectOverrides.Entry> into, List<ContentError> errors) {
        String fileName = file.getFileName().toString();
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            errors.add(new ContentError(fileName, "<root>", "-", "a readable UTF-8 text file", exception.getMessage()));
            return;
        }

        JsonElement root;
        try {
            root = JsonParser.parseString(text);
        } catch (RuntimeException exception) {
            errors.add(new ContentError(fileName, "<root>", "-", "valid json", exception.getMessage()));
            return;
        }

        JsonArray entries = null;
        if (root.isJsonArray()) {
            entries = root.getAsJsonArray();
        } else if (root.isJsonObject() && root.getAsJsonObject().get("effects") instanceof JsonArray array) {
            entries = array;
        }
        if (entries == null) {
            errors.add(new ContentError(fileName, "<root>", "-", "an array or {\"effects\": [...]}", "found " + describe(root)));
            return;
        }

        int index = -1;
        for (JsonElement element : entries) {
            index++;
            String path = "effects[" + index + "]";
            if (!element.isJsonObject()) {
                errors.add(new ContentError(fileName, path, "-", "an object", "found " + describe(element)));
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            JsonElement idElement = object.get("id");
            if (idElement == null || !idElement.isJsonPrimitive()) {
                errors.add(new ContentError(fileName, path + ".id", "-", "a string effect id such as \"minecraft:speed\"", "missing"));
                continue;
            }
            Optional<ResourceLocation> parsed = IdValidator.parse(idElement.getAsString());
            if (parsed.isEmpty()) {
                errors.add(new ContentError(fileName, path + ".id", idElement.getAsString(), "a legal identifier", "not a well formed id"));
                continue;
            }
            ResourceLocation id = parsed.get();
            boolean enabled = boolField(object, "enabled", true, fileName, path, id.toString(), errors);

            Optional<Integer> color = Optional.empty();
            JsonElement colorElement = object.get("color");
            if (colorElement != null) {
                if (colorElement.isJsonPrimitive() && colorElement.getAsJsonPrimitive().isNumber()) {
                    color = Optional.of(MerlinColor.rgb(colorElement.getAsInt()));
                } else if (colorElement.isJsonPrimitive()) {
                    Optional<Integer> parsedColor = MerlinColor.parse(colorElement.getAsString());
                    if (parsedColor.isEmpty()) {
                        errors.add(new ContentError(fileName, path + ".color", id.toString(),
                                "a colour such as \"#RRGGBB\", \"0xRRGGBB\" or a palette name", "found '" + colorElement.getAsString() + "'"));
                    }
                    color = parsedColor;
                } else {
                    errors.add(new ContentError(fileName, path + ".color", id.toString(), "a colour string", "found " + describe(colorElement)));
                }
            }
            into.put(id, new EffectOverrides.Entry(color, enabled));
        }
    }

    /**
     * The sample content file, written once on first launch.
     *
     * <p>Written as concatenated literals rather than a text block: this text is embedded in a class that
     * is compiled for Java 17, and every other string constant in this project is written the same way so a
     * future re-target stays mechanical. Backslashes are doubled where the sample itself has to contain
     * one.
     */
    private static final String SAMPLE_EFFECTS =
            "{\n"
            + "  \"_comment\": \"MerlinLib effect overrides. Effects are registered in code by mods; this file can only recolour or disable them.\",\n"
            + "  \"_comment_color\": \"color accepts \\\"#RRGGBB\\\", \\\"0xRRGGBB\\\", a decimal number or a palette name such as \\\"cyan\\\".\",\n"
            + "  \"effects\": [\n"
            + "    {\n"
            + "      \"id\": \"minecraft:slowness\",\n"
            + "      \"enabled\": true,\n"
            + "      \"color\": \"0x5A6ACF\"\n"
            + "    }\n"
            + "  ]\n"
            + "}\n";

    /**
     * The sample enchantment file, written once on first launch.
     *
     * <p>{@code _comment_reload} differs from the 26.3 line on purpose: that line told the author to run
     * {@code /reload}, which re-read the generated datapack there. On 1.20.1 an enchantment is a plain
     * registry entry created while the game starts, so the honest instruction is a restart. A sample file
     * that promises a command which does nothing would be worse than no sample at all.
     */
    private static final String SAMPLE_ENCHANTMENTS =
            "{\n"
            + "  \"_comment\": \"MerlinLib enchantment definitions. Delete or rename this file to start from scratch.\",\n"
            + "  \"_comment_id\": \"id may be written with or without a namespace, 'unbreakable' means 'merlinlib:unbreakable'.\",\n"
            + "  \"_comment_reload\": \"1.20.1 registers enchantments while the game starts, so changes apply after a restart.\",\n"
            + "  \"_comment_effects\": \"The 'effects' block is the 1.20.5+ format and is not applied on 1.20.1; behaviour comes from MerlinApi.events().\",\n"
            + "  \"enchantments\": [\n"
            + "    {\n"
            + "      \"id\": \"example_frostbite\",\n"
            + "      \"enabled\": false,\n"
            + "      \"translation_key\": \"enchantment.merlinlib.example_frostbite\",\n"
            + "      \"weight\": 5,\n"
            + "      \"max_level\": 3,\n"
            + "      \"min_cost\": { \"base\": 10, \"per_level_above_first\": 9 },\n"
            + "      \"max_cost\": { \"base\": 40, \"per_level_above_first\": 9 },\n"
            + "      \"anvil_cost\": 2,\n"
            + "      \"slots\": [\"mainhand\"],\n"
            + "      \"supported_items\": \"#minecraft:enchantable/weapon\",\n"
            + "      \"primary_items\": \"#minecraft:enchantable/melee_weapon\",\n"
            + "      \"exclusive_set\": [\"minecraft:sharpness\", \"minecraft:smite\"],\n"
            + "      \"acquisition\": {\n"
            + "        \"enchanting_table\": true,\n"
            + "        \"villager_trade\": true,\n"
            + "        \"fishing\": false,\n"
            + "        \"loot_chest\": true,\n"
            + "        \"treasure_only\": false,\n"
            + "        \"curse\": false\n"
            + "      }\n"
            + "    }\n"
            + "  ]\n"
            + "}\n";
}
