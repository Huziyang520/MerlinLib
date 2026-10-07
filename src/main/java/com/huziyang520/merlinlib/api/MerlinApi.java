package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.impl.EffectRegistry;
import com.huziyang520.merlinlib.impl.EnchantmentRegistry;
import com.huziyang520.merlinlib.impl.PotionRegistry;

/**
 * Single entry point of the MerlinLib api. Everything a dependent mod needs hangs off here.
 *
 * <p>The api is safe to call from any mod constructor. On 1.20.1 that is also the only moment it can
 * be called for anything that registers: enchantments, effects and potions all go into Forge
 * registries, which are frozen for the rest of the process once the registry events have run.
 *
 * <p>That is the one behavioural difference from the 26.3 line, and it is worth stating plainly.
 * There, content was applied through a generated data pack, so a registration made at any time -
 * including from {@code /reload} - was picked up. Here a registration has to happen while the game
 * is starting. Everything the api says about <em>where</em> to call it from still holds: a mod
 * constructor is the right place, and always was.
 */
public final class MerlinApi {

    private static final EnchantmentApi ENCHANTMENTS = EnchantmentRegistry.INSTANCE;
    private static final EffectApi EFFECTS = EffectRegistry.INSTANCE;
    private static final PotionApi POTIONS = PotionRegistry.INSTANCE;
    private static final EventApi EVENTS = EventApi.INSTANCE;

    private MerlinApi() {
    }

    /**
     * @return the enchantment api. Enchantments can also be declared in {@code config/MerlinLib/*.json};
     *         on this version both paths end in the same code registry, and the config file wins.
     */
    public static EnchantmentApi enchantments() {
        return ENCHANTMENTS;
    }

    /**
     * @return the mob effect api. Effects are a code registry: they can only be created here, config
     *         files may just recolour or disable them.
     */
    public static EffectApi effects() {
        return EFFECTS;
    }

    /**
     * @return the potion api. Same boundary as effects: declared in code, overridden by config.
     */
    public static PotionApi potions() {
        return POTIONS;
    }

    /**
     * @return the server lifecycle hooks, for work that has to run once per server (reading configuration,
     *         registering a join notice, warming a cache). Safe to call from a mod constructor, before
     *         MerlinLib has initialised.
     */
    public static LifecycleApi lifecycle() {
        return LifecycleApi.INSTANCE;
    }

    /**
     * @return the enchantment event api, for handing an enchantment a callback on attacks, hurts, projectile
     *         hits, block drops and ticks. Safe to call from a mod constructor.
     *
     * <p>On 1.20.1 this is not an optional convenience: it is the only way an enchantment can do
     * anything, because the numeric effect system that made a 26.3 enchantment behave is 1.20.5+.
     */
    public static EventApi events() {
        return EVENTS;
    }

    /**
     * @return the loot injection api, for putting an enchantment into the game's own loot tables as an
     *         enchanted book. Safe to call from a mod constructor.
     */
    public static LootApi loot() {
        return LootApi.INSTANCE;
    }

    /**
     * @return the join notice api, for a message a business mod wants shown in the chat when a player
     *         arrives. Safe to call from a mod constructor, and again from {@code onServerStarting} when the
     *         default should follow the mod's own configuration.
     */
    public static NoticeApi notices() {
        return NoticeApi.INSTANCE;
    }

    /**
     * @return the mob behaviour api, for content that changes what a mob wants to do about a player -
     *         disguises, deterrents, taming effects. Safe to call from a mod constructor.
     */
    public static AiApi ai() {
        return AiApi.INSTANCE;
    }
}
