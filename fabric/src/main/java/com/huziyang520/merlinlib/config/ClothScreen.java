package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.util.MerlinColor;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the Cloth Config screen over the client switches.
 *
 * <p>This class is the only one that mentions Cloth Config types, and it is only ever loaded after
 * {@link FabricConfig} has found the mod on the classpath, so a game without Cloth Config never has to
 * resolve a single Cloth class.
 *
 * <p>Nothing is written until the player presses save: the entries collect their values into a local map and
 * that map is handed to {@link ConfigManager#saveClient} in one go, which is also what makes the change
 * visible immediately.
 */
public final class ClothScreen {

    private ClothScreen() {
    }

    /**
     * Builds the screen.
     *
     * @param parent the screen to return to, or {@code null} to resume the game
     * @return the screen to show
     */
    public static Screen create(Screen parent) {
        ClientConfig config = ConfigManager.client();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.merlinlib.title"));
        ConfigEntryBuilder entries = builder.entryBuilder();
        Map<String, String> pending = new LinkedHashMap<>();

        ConfigCategory general = builder.getOrCreateCategory(category("general"));
        general.addEntry(entries.startBooleanToggle(label("general.notice_enabled"), config.noticeEnabled())
                .setDefaultValue(true)
                .setSaveConsumer(value -> pending.put("general.notice_enabled", value.toString()))
                .build());

        ConfigCategory hud = builder.getOrCreateCategory(category("hud"));
        hud.addEntry(entries.startBooleanToggle(label("hud.crosshair_damage"), config.crosshairDamage())
                .setDefaultValue(false)
                .setSaveConsumer(value -> pending.put("hud.crosshair_damage", value.toString()))
                .build());
        hud.addEntry(entries.startIntField(label("hud.crosshair_damage_ticks"), config.crosshairDamageTicks())
                .setDefaultValue(20)
                .setMin(1)
                .setMax(400)
                .setSaveConsumer(value -> pending.put("hud.crosshair_damage_ticks", value.toString()))
                .build());
        hud.addEntry(entries.startBooleanToggle(label("hud.collapse_enabled"), config.hudCollapse())
                .setDefaultValue(true)
                .setSaveConsumer(value -> pending.put("hud.collapse_enabled", value.toString()))
                .build());
        hud.addEntry(entries.startIntField(label("hud.collapse_threshold"), config.hudCollapseThreshold())
                .setDefaultValue(99)
                .setMin(1)
                .setMax(1000000)
                .setSaveConsumer(value -> pending.put("hud.collapse_threshold", value.toString()))
                .build());
        hud.addEntry(entries.startBooleanToggle(label("hud.armour_auto_dodge"), config.hudArmourDodge())
                .setDefaultValue(true)
                .setSaveConsumer(value -> pending.put("hud.armour_auto_dodge", value.toString()))
                .build());

        ConfigCategory floating = builder.getOrCreateCategory(category("floating_text"));
        floating.addEntry(entries.startBooleanToggle(label("floating_text.enabled"), config.floatingTextEnabled())
                .setDefaultValue(false)
                .setSaveConsumer(value -> pending.put("floating_text.enabled", value.toString()))
                .build());
        floating.addEntry(entries.startIntField(label("floating_text.duration_ticks"), config.floatingTextDurationTicks())
                .setDefaultValue(20)
                .setMin(1)
                .setMax(400)
                .setSaveConsumer(value -> pending.put("floating_text.duration_ticks", value.toString()))
                .build());
        floating.addEntry(entries.startDoubleField(label("floating_text.base_scale"), config.floatingTextBaseScale())
                .setDefaultValue(1.0D)
                .setMin(0.1D)
                .setMax(8.0D)
                .setSaveConsumer(value -> pending.put("floating_text.base_scale", value.toString()))
                .build());
        floating.addEntry(entries.startColorField(label("floating_text.normal_color"), config.floatingTextNormalColor())
                .setDefaultValue(MerlinColor.WHITE)
                .setAlphaMode(true)
                .setSaveConsumer(value -> pending.put("floating_text.normal_color", asColour(value)))
                .build());
        floating.addEntry(entries.startColorField(label("floating_text.critical_color"), config.floatingTextCriticalColor())
                .setDefaultValue(MerlinColor.ORANGE)
                .setAlphaMode(true)
                .setSaveConsumer(value -> pending.put("floating_text.critical_color", asColour(value)))
                .build());
        floating.addEntry(entries.startIntField(label("floating_text.max_concurrent"), config.floatingTextMaxConcurrent())
                .setDefaultValue(32)
                .setMin(1)
                .setMax(512)
                .setSaveConsumer(value -> pending.put("floating_text.max_concurrent", value.toString()))
                .build());

        ConfigCategory particles = builder.getOrCreateCategory(category("particles"));
        particles.addEntry(entries.startBooleanToggle(label("particles.limit_enabled"), config.particleLimitEnabled())
                .setDefaultValue(true)
                .setSaveConsumer(value -> pending.put("particles.limit_enabled", value.toString()))
                .build());
        particles.addEntry(entries.startIntField(label("particles.limit"), config.particleLimit())
                .setDefaultValue(512)
                .setMin(1)
                .setMax(1000000)
                .setSaveConsumer(value -> pending.put("particles.limit", value.toString()))
                .build());

        ConfigCategory editor = builder.getOrCreateCategory(category("editor"));
        editor.addEntry(entries.startBooleanToggle(label("editor.hotkey_enabled"), config.editorHotkeyEnabled())
                .setDefaultValue(false)
                .setSaveConsumer(value -> pending.put("editor.hotkey_enabled", value.toString()))
                .build());
        editor.addEntry(entries.startIntField(label("editor.shift_step"), config.editorShiftStep())
                .setDefaultValue(10)
                .setMin(1)
                .setMax(100000)
                .setSaveConsumer(value -> pending.put("editor.shift_step", value.toString()))
                .build());
        editor.addEntry(entries.startIntField(label("editor.scroll_step"), config.editorScrollStep())
                .setDefaultValue(1)
                .setMin(1)
                .setMax(1000)
                .setSaveConsumer(value -> pending.put("editor.scroll_step", value.toString()))
                .build());
        editor.addEntry(entries.startIntField(label("editor.scroll_step_fast"), config.editorScrollStepFast())
                .setDefaultValue(10)
                .setMin(1)
                .setMax(100000)
                .setSaveConsumer(value -> pending.put("editor.scroll_step_fast", value.toString()))
                .build());

        ConfigCategory macros = builder.getOrCreateCategory(category("macros"));
        macros.addEntry(entries.startBooleanToggle(label("macros.enabled"), config.macrosEnabled())
                .setDefaultValue(true)
                .setSaveConsumer(value -> pending.put("macros.enabled", value.toString()))
                .build());

        builder.setSavingRunnable(() -> {
            if (pending.isEmpty()) {
                return;
            }
            Map<String, String> values = new LinkedHashMap<>(pending);
            pending.clear();
            ConfigManager.saveClient(values);
        });
        return builder.build();
    }

    /**
     * @param section the section name of the file
     * @return its translated title
     */
    private static Component category(String section) {
        return Component.translatable("config.merlinlib." + section);
    }

    /**
     * @param key the option, as {@code section.key}
     * @return its translated label
     */
    private static Component label(String key) {
        return Component.translatable("config.merlinlib." + key);
    }

    /**
     * Writes a colour the way {@code client.toml} documents it.
     *
     * @param value the ARGB value from the colour picker
     * @return {@code 0xRRGGBB} for opaque colours, {@code 0xAARRGGBB} otherwise
     */
    private static String asColour(int value) {
        return (value >>> 24) == 0xFF
                ? String.format("0x%06X", value & 0xFFFFFF)
                : String.format("0x%08X", value);
    }
}
