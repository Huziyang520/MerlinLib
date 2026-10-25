// Content shipped by MerlinLib itself.

package com.huziyang520.merlinlib.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.MerlinApi;
import net.minecraft.resources.Identifier;

public final class BuiltInContent {
    public static final Identifier UNBREAKABLE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "unbreakable");

    private BuiltInContent() {
    }

    public static void register() {
        registerUnbreakable();
    }

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
