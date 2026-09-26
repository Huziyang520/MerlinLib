package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.impl.EffectRegistry;
import com.huziyang520.merlinlib.impl.EnchantmentRegistry;
import com.huziyang520.merlinlib.impl.PotionRegistry;

/**
 * Single entry point of the MerlinLib api. Everything a dependent mod needs hangs off here.
 *
 * <p>The api is safe to call from any mod constructor and from any loader; content is applied to the
 * game through the generated datapack, so registrations made before the first world load and before
 * every {@code /reload} are picked up automatically.
 */
public final class MerlinApi {

    private static final EnchantmentApi ENCHANTMENTS = EnchantmentRegistry.INSTANCE;
    private static final EffectApi EFFECTS = EffectRegistry.INSTANCE;
    private static final PotionApi POTIONS = PotionRegistry.INSTANCE;

    private MerlinApi() {
    }

    /**
     * @return the enchantment api. Enchantments are data driven, so they can also be declared in
     *         {@code config/MerlinLib/*.json}.
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
}
