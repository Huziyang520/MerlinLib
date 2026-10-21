package com.huziyang520.merlinlib.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.MerlinApi;
import net.minecraft.resources.Identifier;

/**
 * Content shipped by MerlinLib itself.
 *
 * <p>Built-in entries go through the very same api a dependent mod would use, so they are subject to
 * the same validation and can be disabled or overridden by a config file like anything else.
 */
public final class BuiltInContent {

    /** Enchantment that keeps an item at full durability forever. */
    public static final Identifier UNBREAKABLE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "unbreakable");

    private BuiltInContent() {
    }

    public static void register() {
        registerUnbreakable();
    }

    /**
     * {@code merlinlib:unbreakable}: single level, treasure only, never obtainable from an enchanting
     * table or an anvil in survival.
     *
     * <p>Implemented purely with vanilla components: the {@code minecraft:item_damage} component is
     * set to a large negative constant, so the first time the item would be damaged it is repaired
     * back to full and every later use keeps it there.
     */
    private static void registerUnbreakable() {
        JsonObject setValue = new JsonObject();
        setValue.addProperty("type", "minecraft:set");
        setValue.addProperty("value", -32768.0f);

        JsonObject effect = new JsonObject();
        effect.add("effect", setValue);

        JsonArray itemDamage = new JsonArray();
        itemDamage.add(effect);

        JsonObject effects = new JsonObject();
        effects.add("minecraft:item_damage", itemDamage);

        MerlinApi.enchantments()
                .register(UNBREAKABLE)
                .maxLevel(1)
                .weight(1)
                .anvilCost(4)
                .cost(25, 25, 75, 25)
                .slots("any")
                .supportedItems("#minecraft:enchantable/durability")
                .effects(effects)
                .acquisition(Acquisition.TREASURE_ONLY)
                .submit();
    }
}
