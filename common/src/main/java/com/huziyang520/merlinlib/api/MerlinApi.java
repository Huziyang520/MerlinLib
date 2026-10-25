// Single entry point of the MerlinLib api.

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.impl.EffectRegistry;
import com.huziyang520.merlinlib.impl.EnchantmentRegistry;
import com.huziyang520.merlinlib.impl.PotionRegistry;

public final class MerlinApi {
    private static final EnchantmentApi ENCHANTMENTS = EnchantmentRegistry.INSTANCE;
    private static final EffectApi EFFECTS = EffectRegistry.INSTANCE;
    private static final PotionApi POTIONS = PotionRegistry.INSTANCE;
    private static final EventApi EVENTS = EventApi.INSTANCE;

    private MerlinApi() {
    }

    public static EnchantmentApi enchantments() {
        return ENCHANTMENTS;
    }

    public static EffectApi effects() {
        return EFFECTS;
    }

    public static PotionApi potions() {
        return POTIONS;
    }

    public static LifecycleApi lifecycle() {
        return LifecycleApi.INSTANCE;
    }

    public static EventApi events() {
        return EVENTS;
    }

    public static LootApi loot() {
        return LootApi.INSTANCE;
    }

    public static NoticeApi notices() {
        return NoticeApi.INSTANCE;
    }

    public static AiApi ai() {
        return AiApi.INSTANCE;
    }
}
