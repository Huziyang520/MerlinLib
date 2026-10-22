package com.huziyang520.merlinlib.platform.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ClientConfig;
import com.huziyang520.merlinlib.config.ConfigBackend;
import com.huziyang520.merlinlib.config.ConfigReader;
import com.huziyang520.merlinlib.config.ServerConfig;
import com.huziyang520.merlinlib.config.SplitReader;
import com.huziyang520.merlinlib.config.TomlFile;
import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.util.MerlinColor;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NeoForge configuration store: two {@code ModConfigSpec} files, one for the client switches and one for the
 * authoritative server switches.
 *
 * <h2>Why CLIENT and COMMON, and not SERVER</h2>
 *
 * <p>{@code ModConfig.Type.SERVER} is written per world and only exists while a world is running, so its values
 * would be unavailable on the title screen and every switch would read as its default until a save was opened.
 * The server switches here are global to the installation - one file on a dedicated server, the same file in
 * single player - which is exactly {@code COMMON}. {@code SERVER} is therefore not used at all: one behaviour,
 * one file, and no switch whose meaning depends on what happens to be open.
 *
 * <p>The native {@code ConfigurationScreen} NeoForge builds from these specs is the configuration screen this
 * mod ships. Each value declares a {@code merlinlib.configuration.<section>.<key>} translation key, so the
 * screen is translated like the rest of the mod and no label is hard coded.
 */
public final class NeoSpecBackend implements ConfigBackend {

    /** Sections that belong to the server store; everything else is client. */
    private static final Set<String> SERVER_SECTIONS = Set.of("security", "tools", "content", "notices");

    private final ModConfigSpec clientSpec;
    private final ModConfigSpec serverSpec;
    private final Map<String, ModConfigSpec.ConfigValue<?>> clientValues;
    private final Map<String, ModConfigSpec.ConfigValue<?>> serverValues;
    private final ConfigReader view;

    private NeoSpecBackend(ModConfigSpec clientSpec, Map<String, ModConfigSpec.ConfigValue<?>> clientValues,
                           ModConfigSpec serverSpec, Map<String, ModConfigSpec.ConfigValue<?>> serverValues) {
        this.clientSpec = clientSpec;
        this.clientValues = clientValues;
        this.serverSpec = serverSpec;
        this.serverValues = serverValues;
        this.view = new SplitReader(new SpecReader(clientSpec, clientValues),
                new SpecReader(serverSpec, serverValues), SERVER_SECTIONS);
    }

    /**
     * Declares both specs.
     *
     * <p>Called once from the mod constructor, before {@code registerConfig}, because a spec is only usable
     * after it has been built.
     *
     * @return the backend, holding the two specs to register
     */
    public static NeoSpecBackend create() {
        Map<String, ModConfigSpec.ConfigValue<?>> clientValues = new LinkedHashMap<>();
        Map<String, ModConfigSpec.ConfigValue<?>> serverValues = new LinkedHashMap<>();
        ModConfigSpec clientSpec = declareClient(clientValues);
        ModConfigSpec serverSpec = declareServer(serverValues);
        return new NeoSpecBackend(clientSpec, clientValues, serverSpec, serverValues);
    }

    /** @return the spec of the client switches, to register as {@code ModConfig.Type.CLIENT} */
    public ModConfigSpec clientSpec() {
        return this.clientSpec;
    }

    /** @return the spec of the server switches, to register as {@code ModConfig.Type.COMMON} */
    public ModConfigSpec serverSpec() {
        return this.serverSpec;
    }

    /**
     * Records a declared value under its {@code section.key} so the read and write paths do not have to know
     * which section it came from.
     */
    private static <V extends ModConfigSpec.ConfigValue<?>> V keep(
            Map<String, ModConfigSpec.ConfigValue<?>> values, String section, String key, V value) {
        values.put(section + "." + key, value);
        return value;
    }

    private static String translation(String section, String key) {
        return "merlinlib.configuration." + section + "." + key;
    }

    private static ModConfigSpec declareClient(Map<String, ModConfigSpec.ConfigValue<?>> values) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        builder.comment("Show the join notices MerlinLib sends, including the one about the configuration "
                + "screen. Takes effect on the next join.");
        keep(values, "general", "notice_enabled", builder.translation(translation("general", "notice_enabled"))
                .define("notice_enabled", true));
        builder.pop();

        builder.push("hud");
        builder.comment("Show the damage of the weapon you just used near the crosshair.");
        keep(values, "hud", "crosshair_damage", builder.translation(translation("hud", "crosshair_damage"))
                .define("crosshair_damage", false));
        builder.comment("How long the crosshair damage number stays on screen, in ticks.");
        keep(values, "hud", "crosshair_damage_ticks",
                builder.translation(translation("hud", "crosshair_damage_ticks"))
                        .defineInRange("crosshair_damage_ticks", 20, 1, 400));
        builder.comment("Fold a very large health bar into one heart plus the current and maximum numbers.");
        keep(values, "hud", "collapse_enabled", builder.translation(translation("hud", "collapse_enabled"))
                .define("collapse_enabled", true));
        builder.comment("Health above which the bar folds. 99 keeps a normal player bar untouched.");
        keep(values, "hud", "collapse_threshold",
                builder.translation(translation("hud", "collapse_threshold"))
                        .defineInRange("collapse_threshold", 99, 1, 1000000));
        builder.comment("Move the armour row up so it sits on the folded health row.");
        keep(values, "hud", "armour_auto_dodge", builder.translation(translation("hud", "armour_auto_dodge"))
                .define("armour_auto_dodge", true));
        builder.pop();

        builder.push("floating_text");
        builder.comment("Show damage numbers flying off a mob when it is hit.");
        keep(values, "floating_text", "enabled", builder.translation(translation("floating_text", "enabled"))
                .define("enabled", false));
        builder.comment("How long a damage number stays visible after a mob is hit, in ticks.");
        keep(values, "floating_text", "duration_ticks",
                builder.translation(translation("floating_text", "duration_ticks"))
                        .defineInRange("duration_ticks", 20, 1, 400));
        builder.comment("Base scale of a damage number. Higher means larger text.");
        keep(values, "floating_text", "base_scale",
                builder.translation(translation("floating_text", "base_scale"))
                        .defineInRange("base_scale", 1.0D, 0.1D, 8.0D));
        builder.comment("ARGB colour of a normal hit, written as a signed 32 bit number.");
        keep(values, "floating_text", "normal_color",
                builder.translation(translation("floating_text", "normal_color"))
                        .defineInRange("normal_color", MerlinColor.WHITE, Integer.MIN_VALUE, Integer.MAX_VALUE));
        builder.comment("ARGB colour of a critical hit, written as a signed 32 bit number.");
        keep(values, "floating_text", "critical_color",
                builder.translation(translation("floating_text", "critical_color"))
                        .defineInRange("critical_color", MerlinColor.ORANGE, Integer.MIN_VALUE, Integer.MAX_VALUE));
        builder.comment("Upper bound on simultaneous damage numbers; the oldest one is dropped first.");
        keep(values, "floating_text", "max_concurrent",
                builder.translation(translation("floating_text", "max_concurrent"))
                        .defineInRange("max_concurrent", 32, 1, 512));
        builder.pop();

        builder.push("particles");
        builder.comment("Cap how many particles this client draws, so a single huge hit cannot freeze it.");
        keep(values, "particles", "limit_enabled",
                builder.translation(translation("particles", "limit_enabled")).define("limit_enabled", true));
        builder.comment("How many particles may be drawn per 1/20 second.");
        keep(values, "particles", "limit", builder.translation(translation("particles", "limit"))
                .defineInRange("limit", 512, 1, 1000000));
        builder.pop();

        builder.push("editor");
        builder.comment("Allow the editor hotkey to open the item editor. Off by default, so the library can be "
                + "installed without claiming a key.");
        keep(values, "editor", "hotkey_enabled", builder.translation(translation("editor", "hotkey_enabled"))
                .define("hotkey_enabled", false));
        builder.comment("Value change when a - or + button is clicked while holding shift.");
        keep(values, "editor", "shift_step", builder.translation(translation("editor", "shift_step"))
                .defineInRange("shift_step", 10, 1, 100000));
        builder.comment("Value change per mouse wheel notch over a number field.");
        keep(values, "editor", "scroll_step", builder.translation(translation("editor", "scroll_step"))
                .defineInRange("scroll_step", 1, 1, 1000));
        builder.comment("Value change per mouse wheel notch while holding ctrl.");
        keep(values, "editor", "scroll_step_fast", builder.translation(translation("editor", "scroll_step_fast"))
                .defineInRange("scroll_step_fast", 10, 1, 100000));
        builder.pop();

        builder.push("macros");
        builder.comment("Enable the macro toolkit: the macro screen and running macros from their bound keys.");
        keep(values, "macros", "enabled", builder.translation(translation("macros", "enabled"))
                .define("enabled", true));
        builder.pop();

        return builder.build();
    }

    private static ModConfigSpec declareServer(Map<String, ModConfigSpec.ConfigValue<?>> values) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("security");
        builder.comment("When true, only operators may use the testing toolkit.");
        keep(values, "security", "restrict_tools_to_operators",
                builder.translation(translation("security", "restrict_tools_to_operators"))
                        .define("restrict_tools_to_operators", false));
        builder.comment("Require command permission (operator) to open the item editor.");
        keep(values, "security", "editor_requires_permission",
                builder.translation(translation("security", "editor_requires_permission"))
                        .define("editor_requires_permission", true));
        builder.comment("Require command permission (operator) to open the health editor.");
        keep(values, "security", "health_editor_requires_permission",
                builder.translation(translation("security", "health_editor_requires_permission"))
                        .define("health_editor_requires_permission", true));
        builder.pop();

        builder.push("tools");
        builder.comment("Master switch of the testing screens. The testing items stay in the creative tab.");
        keep(values, "tools", "enable_testing_toolkit",
                builder.translation(translation("tools", "enable_testing_toolkit"))
                        .define("enable_testing_toolkit", true));
        builder.comment("Upper bound for the damage value of the test weapons.");
        keep(values, "tools", "max_test_weapon_damage",
                builder.translation(translation("tools", "max_test_weapon_damage"))
                        .defineInRange("max_test_weapon_damage", Integer.MAX_VALUE, 1, Integer.MAX_VALUE));
        builder.comment("Upper bound for enchantment levels set through the item editor.");
        keep(values, "tools", "max_enchantment_level",
                builder.translation(translation("tools", "max_enchantment_level"))
                        .defineInRange("max_enchantment_level", Integer.MAX_VALUE, 1, Integer.MAX_VALUE));
        builder.comment("When true, an enchantment may be raised above the maximum level its definition declares.");
        keep(values, "tools", "allow_levels_above_max",
                builder.translation(translation("tools", "allow_levels_above_max"))
                        .define("allow_levels_above_max", true));
        builder.comment("When true, any enchantment may be added to any item.");
        keep(values, "tools", "allow_mismatched_enchantments",
                builder.translation(translation("tools", "allow_mismatched_enchantments"))
                        .define("allow_mismatched_enchantments", false));
        builder.comment("When true, the item editor opens for every item, including blocks.");
        keep(values, "tools", "allow_editing_all_items",
                builder.translation(translation("tools", "allow_editing_all_items"))
                        .define("allow_editing_all_items", false));
        builder.comment("When true, the base damage of an ordinary weapon can be edited too.");
        keep(values, "tools", "allow_non_test_item_damage",
                builder.translation(translation("tools", "allow_non_test_item_damage"))
                        .define("allow_non_test_item_damage", false));
        builder.comment("When true, the health editor only opens while holding the health editor item.");
        keep(values, "tools", "health_editor_requires_item",
                builder.translation(translation("tools", "health_editor_requires_item"))
                        .define("health_editor_requires_item", true));
        builder.pop();

        builder.push("content");
        builder.comment("Emergency switch: when true, no enchantment defined in config/MerlinLib/*.json is "
                + "generated. Code api registrations are unaffected.");
        keep(values, "content", "disable_generated_enchantments",
                builder.translation(translation("content", "disable_generated_enchantments"))
                        .define("disable_generated_enchantments", false));
        builder.pop();

        builder.push("notices");
        builder.comment("Show the join notices business mods registered through MerlinLib.");
        keep(values, "notices", "enabled", builder.translation(translation("notices", "enabled"))
                .define("enabled", true));
        builder.comment("Per mod switches, written as \"<modid>=true;<modid>=false>\".");
        keep(values, "notices", "per_mod", builder.translation(translation("notices", "per_mod"))
                .define("per_mod", ""));
        builder.pop();

        return builder.build();
    }

    @Override
    public ConfigReader view() {
        return this.view;
    }

    @Override
    public void write(String key, String value) {
        writeInto(this.clientSpec, this.clientValues, key, value);
        writeInto(this.serverSpec, this.serverValues, key, value);
    }

    @SuppressWarnings("unchecked")
    private static void writeInto(ModConfigSpec spec, Map<String, ModConfigSpec.ConfigValue<?>> values,
                                  String key, String value) {
        ModConfigSpec.ConfigValue<?> entry = values.get(key);
        if (entry == null || !spec.isLoaded()) {
            // A key this store does not declare - a file layout marker, for instance - has nowhere to go, and a
            // spec that FML has not loaded yet cannot take a new value.
            return;
        }
        Object current = entry.getDefault();
        try {
            if (current instanceof Boolean) {
                ((ModConfigSpec.ConfigValue<Boolean>) entry).set(Boolean.parseBoolean(value.trim()));
            } else if (current instanceof Integer) {
                Integer parsed = parseInt(value);
                if (parsed != null) {
                    ((ModConfigSpec.ConfigValue<Integer>) entry).set(parsed);
                }
            } else if (current instanceof Double) {
                ((ModConfigSpec.ConfigValue<Double>) entry).set(Double.parseDouble(value.trim()));
            } else {
                ((ModConfigSpec.ConfigValue<String>) entry).set(value);
            }
        } catch (RuntimeException exception) {
            Constants.LOG.warn("[MerlinLib] could not write {} = {} into the NeoForge configuration: {}",
                    key, value, exception.getMessage());
        }
    }

    /**
     * Parses an integer written as decimal, {@code 0xRRGGBB} or {@code #RRGGBB}.
     *
     * @param value the text to parse
     * @return the value, or {@code null} when it is not a number
     */
    private static Integer parseInt(String value) {
        String text = value.trim();
        try {
            if (text.startsWith("0x") || text.startsWith("0X")) {
                return (int) Long.parseLong(text.substring(2), 16);
            }
            if (text.startsWith("#")) {
                return (int) (Long.parseLong(text.substring(1), 16) | 0xFF000000L);
            }
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public void save() {
        if (this.clientSpec.isLoaded()) {
            this.clientSpec.save();
        }
        if (this.serverSpec.isLoaded()) {
            this.serverSpec.save();
        }
    }

    @Override
    public String name() {
        return "NeoForge ModConfigSpec";
    }

    /**
     * Moves the values of an older, file based installation into these specs, once.
     *
     * <p>The files are renamed to {@code *.imported} instead of deleted: they are the only copy of what the
     * player set up, they cost nothing to keep, and the rename is also what makes this a one-off - the next
     * launch finds no {@code client.toml} and does nothing. Only values these specs declare are taken; anything
     * a retired build wrote (a removed section, an old layout marker) has no home here and is left in the
     * renamed file for the player to read.
     *
     * <p>Called when FML reports the files as loaded: a spec that is not loaded cannot take a value, so before
     * that moment there is nothing to import into.
     */
    public void importLegacyFiles() {
        // Each half is imported on its own schedule: a dedicated server has no client configuration at all, so
        // waiting for both would leave its server.toml unmigrated forever.
        if (this.clientSpec.isLoaded()) {
            importFile(ClientConfig.path(), ClientConfig.defaults(), this.clientValues);
        }
        if (this.serverSpec.isLoaded()) {
            importFile(ServerConfig.path(), ServerConfig.defaults(), this.serverValues);
        }
    }

    private void importFile(java.nio.file.Path path, String defaults,
                            Map<String, ModConfigSpec.ConfigValue<?>> values) {
        if (!java.nio.file.Files.isRegularFile(path)) {
            return;
        }
        TomlFile file = TomlFile.read(path, defaults);
        int imported = 0;
        for (Map.Entry<String, ModConfigSpec.ConfigValue<?>> entry : values.entrySet()) {
            String key = entry.getKey();
            Object fallback = entry.getValue().getDefault();
            String text;
            if (fallback instanceof Boolean flag) {
                text = Boolean.toString(file.getBoolean(key, flag));
            } else if (fallback instanceof Integer number) {
                // A colour is stored as text in the file and as a packed integer here, so it is read with the
                // colour parser; everything else is a plain number in both places.
                text = Integer.toString(key.endsWith("color")
                        ? file.getColor(key, number)
                        : file.getInt(key, number, Integer.MIN_VALUE, Integer.MAX_VALUE));
            } else if (fallback instanceof Double number) {
                text = Double.toString(file.getDouble(key, number, -Double.MAX_VALUE, Double.MAX_VALUE));
            } else {
                text = file.getString(key, String.valueOf(fallback));
            }
            write(key, text);
            imported++;
        }
        save();
        java.nio.file.Path renamed = path.resolveSibling(path.getFileName() + ".imported");
        try {
            java.nio.file.Files.move(path, renamed, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Constants.LOG.info("[MerlinLib] imported {} value(s) from {} into the NeoForge configuration; the "
                    + "file was renamed to {}", imported, path.getFileName(), renamed.getFileName());
        } catch (java.io.IOException exception) {
            Constants.LOG.warn("[MerlinLib] imported {} value(s) from {} but could not rename it: {}",
                    imported, path.getFileName(), exception.getMessage());
        }
    }

    /**
     * Reads one spec.
     *
     * <p>A value is only readable once FML has loaded the file; before that every getter throws, so the declared
     * default is returned instead. That is what makes the first read - during the mod constructor - safe: the
     * real values replace them at the first {@code ModConfigEvent}.
     */
    private static final class SpecReader implements ConfigReader {

        private final ModConfigSpec spec;
        private final Map<String, ModConfigSpec.ConfigValue<?>> values;

        SpecReader(ModConfigSpec spec, Map<String, ModConfigSpec.ConfigValue<?>> values) {
            this.spec = spec;
            this.values = values;
        }

        private Object raw(String key) {
            ModConfigSpec.ConfigValue<?> entry = this.values.get(key);
            if (entry == null) {
                return null;
            }
            if (!this.spec.isLoaded()) {
                return entry.getDefault();
            }
            try {
                return entry.get();
            } catch (RuntimeException exception) {
                return entry.getDefault();
            }
        }

        @Override
        public boolean getBoolean(String key, boolean fallback) {
            Object value = raw(key);
            return value instanceof Boolean flag ? flag : fallback;
        }

        @Override
        public int getInt(String key, int fallback, int min, int max) {
            Object value = raw(key);
            if (!(value instanceof Integer number)) {
                return fallback;
            }
            return number < min || number > max ? fallback : number;
        }

        @Override
        public double getDouble(String key, double fallback, double min, double max) {
            Object value = raw(key);
            if (!(value instanceof Number number)) {
                return fallback;
            }
            double parsed = number.doubleValue();
            return parsed < min || parsed > max ? fallback : parsed;
        }

        @Override
        public int getColor(String key, int fallback) {
            Object value = raw(key);
            return value instanceof Integer packed ? packed : fallback;
        }

        @Override
        public String getString(String key, String fallback) {
            Object value = raw(key);
            return value == null ? fallback : String.valueOf(value);
        }

        @Override
        public List<ContentError> errors() {
            // The spec corrects an out of range value against its own bounds instead of reporting it, and FML
            // logs the correction itself, so there is nothing to collect here.
            return new ArrayList<>();
        }
    }
}
