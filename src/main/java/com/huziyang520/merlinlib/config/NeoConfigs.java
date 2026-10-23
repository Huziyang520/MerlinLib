package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

/**
 * Declares MerlinLib's switches as two NeoForge configurations - the client half and the shared half -
 * and hands them to the mod container, which writes the files and shows the editing screen.
 */
public final class NeoConfigs {

    /** The client file, below the game's {@code config} directory. */
    public static final String CLIENT_FILE = Constants.MOD_ID + "-client.toml";

    /** The common file, below the game's {@code config} directory. */
    public static final String COMMON_FILE = Constants.MOD_ID + "-common.toml";

    /** Layout this build writes into the client file; client only, the shared half carries no marker. */
    public static final int CLIENT_VERSION = 3;

    /** Name of the pre-specification client file inside {@link ConfigDirectory#root()}. */
    private static final String LEGACY_CLIENT_FILE = "client.toml";

    /** Name of the pre-specification shared file inside {@link ConfigDirectory#root()}. */
    private static final String LEGACY_COMMON_FILE = "server.toml";

    private static final NeoSpecBackend CLIENT =
            NeoSpecBackend.declare("neoforge client", CLIENT_FILE, LEGACY_CLIENT_FILE, NeoConfigs::client);
    private static final NeoSpecBackend COMMON =
            NeoSpecBackend.declare("neoforge common", COMMON_FILE, LEGACY_COMMON_FILE, NeoConfigs::common);

    private NeoConfigs() {
    }

    /**
     * The type the shared switches are registered under. 1.21.1 still declares {@code COMMON}, which is
     * the unsynced half each side reads from its own file.
     */
    public static ModConfig.Type sharedType() {
        return ModConfig.Type.COMMON;
    }

    /**
     * Binds both specs to the mod.
     *
     * @param container the mod container handed to the entrypoint
     */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT.spec(), CLIENT_FILE);
        container.registerConfig(sharedType(), COMMON.spec(), COMMON_FILE);
    }

    /** @return the backend of the client half */
    public static ConfigBackend clientBackend() {
        return CLIENT;
    }

    /** @return the backend of the shared half */
    public static ConfigBackend commonBackend() {
        return COMMON;
    }

    /** @return the spec backend of the client half, for the one time legacy import */
    static NeoSpecBackend clientSpec() {
        return CLIENT;
    }

    /** @return the spec backend of the shared half, for the one time legacy import */
    static NeoSpecBackend commonSpec() {
        return COMMON;
    }

    /**
     * Declares the client switches, in the order {@code client.toml} documents them. The layout version is
     * client only and therefore declared here, never in the shared half.
     */
    private static void client(NeoSpecBackend spec) {
        spec.push("general")
                .number("config_version", CLIENT_VERSION, 0, Integer.MAX_VALUE,
                        "File layout version, written by the mod. Client only: the shared half carries no "
                                + "layout marker.")
                .bool("notice_enabled", true, "Show MerlinLib's own join notice in chat.")
                .pop();

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

    /**
     * Declares the shared switches, in the order {@code server.toml} documents them. The layout version is
     * not declared here: it is a client only marker.
     */
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
