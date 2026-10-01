package com.huziyang520.merlinlib.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import net.minecraft.resources.Identifier;

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
 * <p>Design rules:
 * <ul>
 *     <li>one broken file never stops the others; failures are collected, not thrown</li>
 *     <li>every failure carries file + json path + expectation, never a bare "parse error"</li>
 *     <li>nothing is written to the log from here; the caller decides how loud to be</li>
 * </ul>
 *
 * <p>Accepted file shapes:
 * <pre>{@code
 * [ { "id": "unbreakable", ... } ]                       // plain array
 * { "enchantments": [ { "id": "unbreakable", ... } ] }    // keyed array (recommended)
 * { "id": "unbreakable", ... }                            // single entry
 * }</pre>
 */
public final class ContentConfigLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ContentConfigLoader() {
    }

    /**
     * Reads every file and merges the content with the java api registrations.
     *
     * @param apiEntries     enchantments registered through {@code MerlinApi}
     * @param disabledByApi  ids disabled through {@code MerlinApi}
     * @return the effective snapshot, never {@code null}
     */
    public static ContentSnapshot load(Collection<EnchantmentDraft> apiEntries, Set<Identifier> disabledByApi) {
        List<ContentError> errors = new ArrayList<>();
        Map<Identifier, EnchantmentDraft> fromConfig = new LinkedHashMap<>();
        Set<Identifier> disabled = new LinkedHashSet<>(disabledByApi);

        ensureDirectory();

        Map<Identifier, EffectOverrides.Entry> effectOverrides = new LinkedHashMap<>();
        for (Path file : ConfigDirectory.jsonFiles()) {
            String name = file.getFileName().toString();
            if (RESERVED_FILES.contains(name)) {
                // MerlinLib's own files sit in the same folder as the content files. Reading one of them as a
                // list of enchantments does not fail loudly - it fails per entry, so a macro file with two
                // macros produced two "this definition could not be loaded" warnings on every reload, which
                // made a healthy configuration look broken. Names listed here are simply not content.
                continue;
            }
            if (EFFECTS_FILE.equals(name)) {
                readEffectOverrides(file, effectOverrides, errors);
            } else {
                readFile(file, fromConfig, disabled, errors);
            }
        }

        Map<Identifier, EnchantmentDraft> effective = new LinkedHashMap<>();
        for (EnchantmentDraft draft : apiEntries) {
            effective.put(draft.id(), draft);
        }
        for (Map.Entry<Identifier, EnchantmentDraft> entry : fromConfig.entrySet()) {
            Identifier id = entry.getKey();
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

        for (Identifier id : disabled) {
            EnchantmentDraft removed = effective.remove(id);
            if (removed != null) {
                Constants.LOG.info("[MerlinLib] {} is disabled, it will not exist in game", id);
            }
        }

        return new ContentSnapshot(effective, List.copyOf(disabled), effectOverrides, errors);
    }

    private static void readFile(Path file, Map<Identifier, EnchantmentDraft> into, Set<Identifier> disabled, List<ContentError> errors) {
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

    private static void readEntry(String file, String path, JsonObject object, Map<Identifier, EnchantmentDraft> into,
                                  Set<Identifier> disabled, List<ContentError> errors) {
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
        Identifier id = IdValidator.parse(rawId).orElseThrow();

        boolean enabled = boolField(object, "enabled", true, file, path, id.toString(), errors);

        JsonElement description = object.has("description")
                ? object.get("description")
                : translationKeyDescription(object, id);

        int weight = intField(object, "weight", 10, 1, 1024, file, path, id, errors);
        int maxLevel = intField(object, "max_level", 1, 1, 255, file, path, id, errors);
        int anvilCost = intField(object, "anvil_cost", 1, 0, Integer.MAX_VALUE, file, path, id, errors);

        int[] minCost = costField(object, "min_cost", 1, 11, file, path, id, errors);
        int[] maxCost = costField(object, "max_cost", 21, 11, file, path, id, errors);

        List<String> slots = stringList(object, "slots", List.of("any"), file, path, id, errors);
        JsonElement supportedItems = elementOrDefault(object, "supported_items", EnchantmentJson.tag("minecraft:enchantable/durability"));
        JsonElement primaryItems = object.has("primary_items") ? object.get("primary_items") : null;
        JsonElement exclusiveSet = object.has("exclusive_set") ? object.get("exclusive_set") : null;
        JsonObject effects = objectField(object, "effects", file, path, id, errors);
        Acquisition acquisition = acquisitionField(object, file, path, id, errors);

        EnchantmentDraft draft = new EnchantmentDraft(
                id,
                ContentSource.CONFIG,
                enabled,
                description,
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
                acquisition
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

    private static JsonElement translationKeyDescription(JsonObject object, Identifier id) {
        JsonElement key = object.get("translation_key");
        if (key != null && key.isJsonPrimitive()) {
            JsonObject component = new JsonObject();
            component.addProperty("translate", key.getAsString());
            return component;
        }
        return EnchantmentJson.defaultDescription(id);
    }

    private static int intField(JsonObject object, String key, int fallback, int min, int max, String file, String path,
                                Identifier id, List<ContentError> errors) {
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
                                   Identifier id, List<ContentError> errors) {
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
                                           Identifier id, List<ContentError> errors) {
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

    private static JsonObject objectField(JsonObject object, String key, String file, String path, Identifier id,
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

    private static Acquisition acquisitionField(JsonObject object, String file, String path, Identifier id,
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
     */
    public static void ensureDirectory() {
        Path root = ConfigDirectory.root();
        try {
            Files.createDirectories(root);
            for (String name : new String[] { TOML_CLIENT, TOML_SERVER }) {
                Path file = root.resolve(name);
                if (!Files.exists(file)) {
                    Files.writeString(file, sampleToml(name), StandardCharsets.UTF_8);
                }
            }
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
     * <p>{@code macros.json} belongs to the macro toolkit ({@code MacroStorage}); anything else added here must
     * be a file MerlinLib itself owns and writes.
     */
    private static final java.util.Set<String> RESERVED_FILES = java.util.Set.of("macros.json");

    /**
     * Reads {@code effects.json}: a list of entries shaped
     * {@code {"id": ..., "enabled": true, "color": "cyan"}}.
     *
     * <p>Effects are a code registry, so this file can only recolour or disable an effect that a mod
     * declared in code; it can never create a new one. That boundary is intentional and documented in
     * the README.
     */
    private static void readEffectOverrides(Path file, Map<Identifier, EffectOverrides.Entry> into, List<ContentError> errors) {
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
            Optional<Identifier> parsed = IdValidator.parse(idElement.getAsString());
            if (parsed.isEmpty()) {
                errors.add(new ContentError(fileName, path + ".id", idElement.getAsString(), "a legal identifier", "not a well formed id"));
                continue;
            }
            Identifier id = parsed.get();
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

    /** Name of the client side switch file. */
    public static final String TOML_CLIENT = "client.toml";
    /** Name of the server side switch file. */
    public static final String TOML_SERVER = "server.toml";

    private static String sampleToml(String name) {
        if (TOML_CLIENT.equals(name)) {
            return """
                    # MerlinLib client configuration.
                    # Every option here only affects this client and takes effect immediately after saving.
                    # Content definitions (enchantments, effects, potions) live in the .json files next to this one
                    # and are applied on the server side with /reload.

                    [hud]
                    # Show the nominal damage of the weapon you just used near the crosshair.
                    damage_display = true

                    [floating_text]
                    # How long a damage number stays visible, in ticks (20 ticks = 1 second).
                    duration_ticks = 20
                    # Base scale of a damage number, in the usual gui scale units.
                    base_scale = 1.0
                    # ARGB colours: normal hit and critical hit. 0xFFFFFFFF is opaque white.
                    normal_color = 0xFFFFFFFF
                    critical_color = 0xFFFFAA00
                    """;
        }
        return """
                # MerlinLib server configuration.
                # This file is authoritative: the client only displays what the server allows.

                # When true, only operators may use the testing toolkit.
                restrict_tools_to_operators = false
                # Master switch of the built in testing toolkit (test weapons, editors, macros).
                enable_testing_toolkit = true
                # Upper bound for the damage value of the test weapons.
                max_test_weapon_damage = 100000
                # Upper bound for enchantment levels set through the editor.
                max_enchantment_level = 255
                # Upper bound for the health editor, defaults to the java int limit.
                max_health_value = 2147483647
                """;
    }

    private static final String SAMPLE_EFFECTS = """
            {
              "_comment": "MerlinLib effect overrides. Effects are registered in code by mods; this file can only recolour or disable them.",
              "_comment_color": "color accepts \\"#RRGGBB\\", \\"0xRRGGBB\\", a decimal number or a palette name such as \\"cyan\\".",
              "effects": [
                {
                  "id": "minecraft:slowness",
                  "enabled": true,
                  "color": "0x5A6ACF"
                }
              ]
            }
            """;

    private static final String SAMPLE_ENCHANTMENTS = """
            {
              "_comment": "MerlinLib enchantment definitions. Delete or rename this file to start from scratch.",
              "_comment_id": "id may be written with or without a namespace, 'unbreakable' means 'merlinlib:unbreakable'.",
              "_comment_reload": "Changes are applied with the vanilla /reload command.",
              "enchantments": [
                {
                  "id": "example_frostbite",
                  "enabled": false,
                  "translation_key": "enchantment.merlinlib.example_frostbite",
                  "weight": 5,
                  "max_level": 3,
                  "min_cost": { "base": 10, "per_level_above_first": 9 },
                  "max_cost": { "base": 40, "per_level_above_first": 9 },
                  "anvil_cost": 2,
                  "slots": ["mainhand"],
                  "supported_items": "#minecraft:enchantable/weapon",
                  "primary_items": "#minecraft:enchantable/melee_weapon",
                  "exclusive_set": "#minecraft:exclusive_set/damage",
                  "acquisition": {
                    "enchanting_table": true,
                    "villager_trade": true,
                    "fishing": false,
                    "loot_chest": true,
                    "treasure_only": false,
                    "curse": false
                  },
                  "effects": {
                    "minecraft:damage": [
                      {
                        "effect": {
                          "type": "minecraft:add",
                          "value": {
                            "type": "minecraft:linear",
                            "base": 1.0,
                            "per_level_above_first": 0.5
                          }
                        }
                      }
                    ]
                  }
                }
              ]
            }
            """;
}
