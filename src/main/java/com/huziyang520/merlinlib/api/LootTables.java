package com.huziyang520.merlinlib.api;

/**
 * The full ids of the vanilla loot tables a dependent mod is most likely to inject into.
 *
 * <p>They are plain strings, and every table in the game can be targeted by its id - these constants only
 * exist so that a typo becomes a compile error instead of a rule that silently never fires. The names are the
 * ones under {@code minecraft:chests/} and {@code minecraft:entities/}; a table that does not exist is never
 * loaded, so a misspelled id is invisible at runtime.
 *
 * <p>1.20.1 note: a direct port of the constant list, with one deliberate exception. The
 * {@code minecraft:chests/trial_chambers/*} family below is <b>1.21+ content and does not exist on
 * 1.20.1</b>. The constants are kept rather than deleted so that a rule carried over from the 26.3 line
 * still compiles; a rule that names one is reported at load time like any other unknown table and then
 * simply never fires, which is the same outcome as naming a table that is not installed. Removing them
 * would turn a working build into a compile error for no runtime gain.
 */
public final class LootTables {

    private static final String CHESTS = "minecraft:chests/";
    private static final String ENTITIES = "minecraft:entities/";

    public static final String SPAWN_BONUS_CHEST = CHESTS + "spawn_bonus_chest";
    public static final String END_CITY_TREASURE = CHESTS + "end_city_treasure";
    public static final String SIMPLE_DUNGEON = CHESTS + "simple_dungeon";
    public static final String ABANDONED_MINESHAFT = CHESTS + "abandoned_mineshaft";
    public static final String NETHER_BRIDGE = CHESTS + "nether_bridge";
    public static final String STRONGHOLD_LIBRARY = CHESTS + "stronghold_library";
    public static final String STRONGHOLD_CORRIDOR = CHESTS + "stronghold_corridor";
    public static final String STRONGHOLD_CROSSING = CHESTS + "stronghold_crossing";
    public static final String DESERT_PYRAMID = CHESTS + "desert_pyramid";
    public static final String JUNGLE_TEMPLE = CHESTS + "jungle_temple";
    public static final String JUNGLE_TEMPLE_DISPENSER = CHESTS + "jungle_temple_dispenser";
    public static final String IGLOO_CHEST = CHESTS + "igloo_chest";
    public static final String WOODLAND_MANSION = CHESTS + "woodland_mansion";
    public static final String UNDERWATER_RUIN_SMALL = CHESTS + "underwater_ruin_small";
    public static final String UNDERWATER_RUIN_BIG = CHESTS + "underwater_ruin_big";
    public static final String BURIED_TREASURE = CHESTS + "buried_treasure";
    public static final String SHIPWRECK_MAP = CHESTS + "shipwreck_map";
    public static final String SHIPWRECK_SUPPLY = CHESTS + "shipwreck_supply";
    public static final String SHIPWRECK_TREASURE = CHESTS + "shipwreck_treasure";
    public static final String PILLAGER_OUTPOST = CHESTS + "pillager_outpost";
    public static final String BASTION_TREASURE = CHESTS + "bastion_treasure";
    public static final String BASTION_OTHER = CHESTS + "bastion_other";
    public static final String BASTION_BRIDGE = CHESTS + "bastion_bridge";
    public static final String BASTION_HOGLIN_STABLE = CHESTS + "bastion_hoglin_stable";
    public static final String RUINED_PORTAL = CHESTS + "ruined_portal";
    public static final String ANCIENT_CITY = CHESTS + "ancient_city";
    public static final String ANCIENT_CITY_ICE_BOX = CHESTS + "ancient_city_ice_box";
    public static final String VILLAGE_WEAPONSMITH = CHESTS + "village/village_weaponsmith";
    public static final String VILLAGE_TOOLSMITH = CHESTS + "village/village_toolsmith";
    public static final String VILLAGE_ARMORER = CHESTS + "village/village_armorer";
    public static final String VILLAGE_CARTOGRAPHER = CHESTS + "village/village_cartographer";
    public static final String VILLAGE_MASON = CHESTS + "village/village_mason";
    public static final String VILLAGE_SHEPHERD = CHESTS + "village/village_shepherd";
    public static final String VILLAGE_BUTCHER = CHESTS + "village/village_butcher";
    public static final String VILLAGE_FLETCHER = CHESTS + "village/village_fletcher";
    public static final String VILLAGE_FISHER = CHESTS + "village/village_fisher";
    public static final String VILLAGE_TANNERY = CHESTS + "village/village_tannery";
    public static final String VILLAGE_TEMPLE = CHESTS + "village/village_temple";
    public static final String VILLAGE_DESERT_HOUSE = CHESTS + "village/village_desert_house";
    public static final String VILLAGE_PLAINS_HOUSE = CHESTS + "village/village_plains_house";
    public static final String VILLAGE_SAVANNA_HOUSE = CHESTS + "village/village_savanna_house";
    public static final String VILLAGE_SNOWY_HOUSE = CHESTS + "village/village_snowy_house";
    public static final String VILLAGE_TAIGA_HOUSE = CHESTS + "village/village_taiga_house";

    /**
     * 1.21+ only; kept for source compatibility, never fires on 1.20.1 - see the class javadoc.
     *
     * @deprecated the trial chambers do not exist on 1.20.1, so no rule naming this table can ever fire
     */
    @Deprecated
    public static final String TRIAL_CHAMBERS_REWARD = CHESTS + "trial_chambers/reward";

    /**
     * 1.21+ only; kept for source compatibility, never fires on 1.20.1 - see the class javadoc.
     *
     * @deprecated the trial chambers do not exist on 1.20.1, so no rule naming this table can ever fire
     */
    @Deprecated
    public static final String TRIAL_CHAMBERS_REWARD_COMMON = CHESTS + "trial_chambers/reward_common";

    /**
     * 1.21+ only; kept for source compatibility, never fires on 1.20.1 - see the class javadoc.
     *
     * @deprecated the trial chambers do not exist on 1.20.1, so no rule naming this table can ever fire
     */
    @Deprecated
    public static final String TRIAL_CHAMBERS_REWARD_RARE = CHESTS + "trial_chambers/reward_rare";

    /**
     * 1.21+ only; kept for source compatibility, never fires on 1.20.1 - see the class javadoc.
     *
     * @deprecated the trial chambers do not exist on 1.20.1, so no rule naming this table can ever fire
     */
    @Deprecated
    public static final String TRIAL_CHAMBERS_REWARD_UNIQUE = CHESTS + "trial_chambers/reward_unique";

    /**
     * 1.21+ only; kept for source compatibility, never fires on 1.20.1 - see the class javadoc.
     *
     * @deprecated the trial chambers do not exist on 1.20.1, so no rule naming this table can ever fire
     */
    @Deprecated
    public static final String TRIAL_CHAMBERS_REWARD_OMINOUS = CHESTS + "trial_chambers/reward_ominous";

    public static final String ZOMBIE = ENTITIES + "zombie";
    public static final String SKELETON = ENTITIES + "skeleton";
    public static final String WITHER_SKELETON = ENTITIES + "wither_skeleton";
    public static final String BLAZE = ENTITIES + "blaze";
    public static final String ENDERMAN = ENTITIES + "enderman";
    public static final String WITHER = ENTITIES + "wither";
    public static final String ENDER_DRAGON = ENTITIES + "ender_dragon";

    private LootTables() {
    }
}
