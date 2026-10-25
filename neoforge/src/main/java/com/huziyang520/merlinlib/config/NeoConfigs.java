// Declares MerlinLib's switches as two NeoForge configurations - the client half and the shared half - and hands them to ...

package com.huziyang520.merlinlib.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

public final class NeoConfigs {
    private static final ModConfig.Type SHARED_TYPE = resolveSharedType();
    private static final NeoSpecBackend CLIENT = NeoSpecBackend.declare("neoforge client", NeoConfigs::client);
    private static final NeoSpecBackend COMMON = NeoSpecBackend.declare("neoforge common", NeoConfigs::common);

    private NeoConfigs() {
    }

    private static ModConfig.Type resolveSharedType() {
        try {
            return ModConfig.Type.valueOf("COMMON");
        } catch (IllegalArgumentException removedOnThisLoader) {
            return ModConfig.Type.valueOf("LOCAL");
        }
    }

    public static ModConfig.Type sharedType() {
        return SHARED_TYPE;
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT.spec());
        container.registerConfig(SHARED_TYPE, COMMON.spec());
    }

    public static ConfigBackend clientBackend() {
        return CLIENT;
    }

    public static ConfigBackend commonBackend() {
        return COMMON;
    }

    private static void client(NeoSpecBackend spec) {
        spec.push("hud")
                .bool("crosshair_damage", false,
                        "Show the listed damage of the weapon you just used near the crosshair.")
                .number("crosshair_damage_ticks", 20, 1, 400,
                        "How long the crosshair damage number stays on screen, in ticks.")
                .bool("collapse_enabled", true,
                        "Fold a very large health bar into one heart plus the numbers.")
                .number("collapse_threshold", 99, 1, 1000000,
                        "Health above which the bar folds. 99 keeps a normal player bar untouched.")
                .bool("armour_auto_dodge", true,
                        "Move the armour row up onto the folded health row.")
                .pop();

        spec.push("floating_text")
                .bool("enabled", false,
                        "Show damage numbers flying off a mob when it is hit. Off by default: a testing aid.")
                .number("duration_ticks", 20, 1, 400, "How long a damage number stays visible, in ticks.")
                .decimal("base_scale", 1.0D, 0.1D, 8.0D, "Base scale of a damage number.")
                .text("normal_color", "0xFFFFFFFF", "Colour of a normal hit, as 0xRRGGBB or a palette name.")
                .text("critical_color", "0xFFFFAA00", "Colour of a critical hit, as 0xRRGGBB or a palette name.")
                .number("max_concurrent", 32, 1, 512,
                        "Upper bound on simultaneous damage numbers; the oldest one is dropped first.")
                .pop();

        spec.push("particles")
                .bool("limit_enabled", true,
                        "Cap how many particles this client draws, so a single huge hit cannot freeze it.")
                .number("limit", 512, 1, 1000000, "How many particles may be drawn per 1/20 second.")
                .pop();

        spec.push("editor")
                .bool("hotkey_enabled", false,
                        "Allow the editor hotkey to open the item editor. Off by default, so the library does "
                                + "not claim a key.")
                .number("shift_step", 10, 1, 100000,
                        "Value change when a - or + button is clicked while holding shift.")
                .number("scroll_step", 1, 1, 1000, "Value change per mouse wheel notch over a number field.")
                .number("scroll_step_fast", 10, 1, 100000,
                        "Value change per mouse wheel notch while holding ctrl.")
                .pop();

        spec.push("macros")
                .bool("enabled", true,
                        "Enable the macro toolkit: the macro screen and running macros from their bound keys.")
                .pop();
    }

    private static void common(NeoSpecBackend spec) {
        spec.push("security")
                .bool("restrict_tools_to_operators", false,
                        "When true, only operators may use the testing toolkit.")
                .bool("editor_requires_permission", true,
                        "Require command permission (operator) to open the item editor.")
                .bool("health_editor_requires_permission", true,
                        "Require command permission (operator) to open the health editor.")
                .pop();

        spec.push("tools")
                .bool("enable_testing_toolkit", true,
                        "Master switch of the testing screens. With it off no toolkit screen opens, while the "
                                + "testing items stay in the creative tab.")
                .number("max_test_weapon_damage", Integer.MAX_VALUE, 1, Integer.MAX_VALUE,
                        "Upper bound for the damage value of the test weapons.")
                .number("max_enchantment_level", Integer.MAX_VALUE, 1, Integer.MAX_VALUE,
                        "Upper bound for enchantment levels set through the item editor.")
                .bool("allow_levels_above_max", true,
                        "Allow an enchantment above the maximum level its own definition declares.")
                .bool("allow_mismatched_enchantments", false,
                        "Allow any enchantment on any item, for example efficiency on a sword.")
                .bool("allow_editing_all_items", false,
                        "Open the item editor for every item, including blocks and other things that are not "
                                + "weapons, tools or equipment.")
                .bool("allow_non_test_item_damage", false,
                        "Allow editing the base damage of an ordinary weapon too.")
                .bool("health_editor_requires_item", true,
                        "Only open the health editor while holding the health editor item.")
                .pop();

        spec.push("content")
                .bool("disable_generated_enchantments", false,
                        "Generate none of the enchantments defined in config/MerlinLib/*.json.")
                .pop();

        spec.push("notices")
                .bool("enabled", true, "Show the join notices business mods registered through MerlinLib.")
                .pop();
    }
}
