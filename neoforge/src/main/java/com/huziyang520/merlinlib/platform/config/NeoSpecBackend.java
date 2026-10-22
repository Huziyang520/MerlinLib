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

    /**
     * Declares the description of one value in both languages.
     *
     * <p>The native {@code ConfigurationScreen} shows a value's comment as its tooltip, and a comment is a plain
     * string - there is no translation key behind it. Writing the Chinese line next to the English one is what
     * makes the tooltip readable for a player running the game in Chinese, and it also documents the generated
     * file in both languages.
     */
    private static void comment(ModConfigSpec.Builder builder, String english, String chinese) {
        builder.comment(english, chinese);
    }

    private static ModConfigSpec declareClient(Map<String, ModConfigSpec.ConfigValue<?>> values) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        comment(builder, "Show the join notices MerlinLib sends, including the one about the configuration "
                + "screen. Takes effect on the next join.", "显示 MerlinLib 发出的进服提示，包括关于设置界面的那一条。下一次进入世界时生效。");
        keep(values, "general", "notice_enabled", builder.translation(translation("general", "notice_enabled"))
                .define("notice_enabled", true));
        builder.pop();

        builder.push("hud");
        comment(builder, "Show the damage of the weapon you just used near the crosshair.", "在准星附近显示刚刚使用武器的伤害数字。");
        keep(values, "hud", "crosshair_damage", builder.translation(translation("hud", "crosshair_damage"))
                .define("crosshair_damage", false));
        comment(builder, "How long the crosshair damage number stays on screen, in ticks.", "准星伤害数字在屏幕上停留多少 tick（20 tick 为 1 秒）。");
        keep(values, "hud", "crosshair_damage_ticks",
                builder.translation(translation("hud", "crosshair_damage_ticks"))
                        .defineInRange("crosshair_damage_ticks", 20, 1, 400));
        comment(builder, "Fold a very large health bar into one heart plus the current and maximum numbers.", "把超长血条折叠成一颗心加当前 / 最大数值显示。");
        keep(values, "hud", "collapse_enabled", builder.translation(translation("hud", "collapse_enabled"))
                .define("collapse_enabled", true));
        comment(builder, "Health above which the bar folds. 99 keeps a normal player bar untouched.", "超过该生命值时血条才折叠；99 时普通玩家的血条不受影响。");
        keep(values, "hud", "collapse_threshold",
                builder.translation(translation("hud", "collapse_threshold"))
                        .defineInRange("collapse_threshold", 99, 1, 1000000));
        comment(builder, "Move the armour row up so it sits on the folded health row.", "把护甲条上移到折叠后的血条那一行。");
        keep(values, "hud", "armour_auto_dodge", builder.translation(translation("hud", "armour_auto_dodge"))
                .define("armour_auto_dodge", true));
        builder.pop();

        builder.push("floating_text");
        comment(builder, "Show damage numbers flying off a mob when it is hit.", "怪物被击中时在其身上飘出伤害数字。");
        keep(values, "floating_text", "enabled", builder.translation(translation("floating_text", "enabled"))
                .define("enabled", false));
        comment(builder, "How long a damage number stays visible after a mob is hit, in ticks.", "伤害飘字在屏幕上停留多少 tick。");
        keep(values, "floating_text", "duration_ticks",
                builder.translation(translation("floating_text", "duration_ticks"))
                        .defineInRange("duration_ticks", 20, 1, 400));
        comment(builder, "Base scale of a damage number. Higher means larger text.", "伤害飘字的缩放，数值越大字越大。");
        keep(values, "floating_text", "base_scale",
                builder.translation(translation("floating_text", "base_scale"))
                        .defineInRange("base_scale", 1.0D, 0.1D, 8.0D));
        comment(builder, "ARGB colour of a normal hit, written as a signed 32 bit number.", "普通命中的伤害颜色，写成 32 位有符号 ARGB 数值。");
        keep(values, "floating_text", "normal_color",
                builder.translation(translation("floating_text", "normal_color"))
                        .defineInRange("normal_color", MerlinColor.WHITE, Integer.MIN_VALUE, Integer.MAX_VALUE));
        comment(builder, "ARGB colour of a critical hit, written as a signed 32 bit number.", "暴击命中的伤害颜色，写成 32 位有符号 ARGB 数值。");
        keep(values, "floating_text", "critical_color",
                builder.translation(translation("floating_text", "critical_color"))
                        .defineInRange("critical_color", MerlinColor.ORANGE, Integer.MIN_VALUE, Integer.MAX_VALUE));
        comment(builder, "Upper bound on simultaneous damage numbers; the oldest one is dropped first.", "同时存在的伤害飘字上限，超出时先丢弃最早的一个。");
        keep(values, "floating_text", "max_concurrent",
                builder.translation(translation("floating_text", "max_concurrent"))
                        .defineInRange("max_concurrent", 32, 1, 512));
        builder.pop();

        builder.push("particles");
        comment(builder, "Cap how many particles this client draws, so a single huge hit cannot freeze it.", "限制本客户端每 1/20 秒绘制的粒子数量，避免一次超大伤害把客户端卡死。");
        keep(values, "particles", "limit_enabled",
                builder.translation(translation("particles", "limit_enabled")).define("limit_enabled", true));
        comment(builder, "How many particles may be drawn per 1/20 second.", "每 1/20 秒最多绘制多少个粒子。");
        keep(values, "particles", "limit", builder.translation(translation("particles", "limit"))
                .defineInRange("limit", 512, 1, 1000000));
        builder.pop();

        builder.push("editor");
        comment(builder, "Allow the editor hotkey to open the item editor. Off by default, so the library can be "
                + "installed without claiming a key.", "允许用快捷键打开物品编辑界面。默认关闭，安装本库不会自动占用按键。");
        keep(values, "editor", "hotkey_enabled", builder.translation(translation("editor", "hotkey_enabled"))
                .define("hotkey_enabled", false));
        comment(builder, "Value change when a - or + button is clicked while holding shift.", "按住 Shift 点击 - / + 时的数值变化量。");
        keep(values, "editor", "shift_step", builder.translation(translation("editor", "shift_step"))
                .defineInRange("shift_step", 10, 1, 100000));
        comment(builder, "Value change per mouse wheel notch over a number field.", "在数值框上滚动一格滚轮的变化量。");
        keep(values, "editor", "scroll_step", builder.translation(translation("editor", "scroll_step"))
                .defineInRange("scroll_step", 1, 1, 1000));
        comment(builder, "Value change per mouse wheel notch while holding ctrl.", "按住 Ctrl 滚动一格滚轮的变化量。");
        keep(values, "editor", "scroll_step_fast", builder.translation(translation("editor", "scroll_step_fast"))
                .defineInRange("scroll_step_fast", 10, 1, 100000));
        builder.pop();

        builder.push("macros");
        comment(builder, "Enable the macro toolkit: the macro screen and running macros from their bound keys.", "启用指令宏：指令宏界面，以及用绑定键执行宏。");
        keep(values, "macros", "enabled", builder.translation(translation("macros", "enabled"))
                .define("enabled", true));
        builder.pop();

        return builder.build();
    }

    private static ModConfigSpec declareServer(Map<String, ModConfigSpec.ConfigValue<?>> values) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("security");
        comment(builder, "When true, only operators may use the testing toolkit.", "开启后只有 OP 可以使用测试工具集。");
        keep(values, "security", "restrict_tools_to_operators",
                builder.translation(translation("security", "restrict_tools_to_operators"))
                        .define("restrict_tools_to_operators", false));
        comment(builder, "Require command permission (operator) to open the item editor.", "打开物品编辑界面需要命令权限（OP）。");
        keep(values, "security", "editor_requires_permission",
                builder.translation(translation("security", "editor_requires_permission"))
                        .define("editor_requires_permission", true));
        comment(builder, "Require command permission (operator) to open the health editor.", "打开血量编辑器需要命令权限（OP）。");
        keep(values, "security", "health_editor_requires_permission",
                builder.translation(translation("security", "health_editor_requires_permission"))
                        .define("health_editor_requires_permission", true));
        builder.pop();

        builder.push("tools");
        comment(builder, "Master switch of the testing screens. The testing items stay in the creative tab.", "测试界面总开关；关闭后物品编辑、血量编辑、指令宏界面都无法打开，测试物品仍留在创造物品栏。");
        keep(values, "tools", "enable_testing_toolkit",
                builder.translation(translation("tools", "enable_testing_toolkit"))
                        .define("enable_testing_toolkit", true));
        comment(builder, "Upper bound for the damage value of the test weapons.", "测试武器伤害数值的上限。默认取整数上限。");
        keep(values, "tools", "max_test_weapon_damage",
                builder.translation(translation("tools", "max_test_weapon_damage"))
                        .defineInRange("max_test_weapon_damage", Integer.MAX_VALUE, 1, Integer.MAX_VALUE));
        comment(builder, "Upper bound for enchantment levels set through the item editor.", "通过物品编辑设置的附魔等级上限。默认取整数上限。");
        keep(values, "tools", "max_enchantment_level",
                builder.translation(translation("tools", "max_enchantment_level"))
                        .defineInRange("max_enchantment_level", Integer.MAX_VALUE, 1, Integer.MAX_VALUE));
        comment(builder, "When true, an enchantment may be raised above the maximum level its definition declares.", "开启后附魔等级可以超过其定义里声明的最高等级。");
        keep(values, "tools", "allow_levels_above_max",
                builder.translation(translation("tools", "allow_levels_above_max"))
                        .define("allow_levels_above_max", true));
        comment(builder, "When true, any enchantment may be added to any item.", "开启后任意附魔都可以加到任意物品上。");
        keep(values, "tools", "allow_mismatched_enchantments",
                builder.translation(translation("tools", "allow_mismatched_enchantments"))
                        .define("allow_mismatched_enchantments", false));
        comment(builder, "When true, the item editor opens for every item, including blocks.", "开启后物品编辑界面对所有物品都能打开，包括方块。");
        keep(values, "tools", "allow_editing_all_items",
                builder.translation(translation("tools", "allow_editing_all_items"))
                        .define("allow_editing_all_items", false));
        comment(builder, "When true, the base damage of an ordinary weapon can be edited too.", "开启后普通武器的基础伤害也能编辑，而不只是测试武器。");
        keep(values, "tools", "allow_non_test_item_damage",
                builder.translation(translation("tools", "allow_non_test_item_damage"))
                        .define("allow_non_test_item_damage", false));
        comment(builder, "When true, the health editor only opens while holding the health editor item.", "开启后必须手持血量编辑器才能打开血量编辑器。");
        keep(values, "tools", "health_editor_requires_item",
                builder.translation(translation("tools", "health_editor_requires_item"))
                        .define("health_editor_requires_item", true));
        builder.pop();

        builder.push("content");
        comment(builder, "Emergency switch: when true, no enchantment defined in config/MerlinLib/*.json is "
                + "generated. Code api registrations are unaffected.", "应急开关：开启后不再生成 config/MerlinLib/*.json 里定义的附魔，代码 API 注册不受影响。");
        keep(values, "content", "disable_generated_enchantments",
                builder.translation(translation("content", "disable_generated_enchantments"))
                        .define("disable_generated_enchantments", false));
        builder.pop();

        builder.push("notices");
        comment(builder, "Show the join notices business mods registered through MerlinLib.",
                "显示业务模组通过 MerlinLib 登记的进服提示。关闭后所有进服提示一起静音。");
        keep(values, "notices", "enabled", builder.translation(translation("notices", "enabled"))
                .define("enabled", true));
        // The per mod switches are not declared here, and that is on purpose: their keys are mod ids that are only
        // known at runtime, so a spec could not declare them, and the retired "notices.per_mod" string is not
        // something a settings screen should show as a text field. They live in config/MerlinLib/notices.toml and
        // are edited in game with /merlinlib notices - see com.huziyang520.merlinlib.config.NoticeSettings.
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
