// Registration entry point for potions, plus the language keys of the four item forms a potion appears as.

package com.huziyang520.merlinlib.api;

import net.minecraft.resources.Identifier;
import java.util.List;
import java.util.Set;

public interface PotionApi {
    String FORM_POTION = "potion";
    String FORM_SPLASH = "splash_potion";
    String FORM_LINGERING = "lingering_potion";
    String FORM_ARROW = "tipped_arrow";

    PotionBuilder register(Identifier id);
    PotionBuilder potionFor(Identifier effectId);
    Set<Identifier> registeredIds();

    static List<String> translationKeys(String namespace, String potionPath) {
        String path = potionPath.startsWith(namespace + ":")
                ? potionPath.substring(namespace.length() + 1)
                : potionPath;
        return List.of(
                "item.minecraft." + FORM_POTION + ".effect." + path,
                "item.minecraft." + FORM_SPLASH + ".effect." + path,
                "item.minecraft." + FORM_LINGERING + ".effect." + path,
                "item.minecraft." + FORM_ARROW + ".effect." + path);
    }
}
