package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Paints enchantment description lines in vanilla's grey, whoever added them.
 *
 * <h2>Why this exists</h2>
 *
 * <p>1.20.1 has no vanilla enchantment description at all: {@code Enchantment} declares no description
 * field and no hover method (checked with {@code javap -p} against this version's jar), and the vanilla
 * tooltip only lists the enchantment name. Description lines are therefore added by mods - on this
 * instance by EnchantmentDescriptions - and a line that arrives without a style renders in plain white
 * while the vanilla-coloured ones (and the ones that mod styles itself, {@code dark_gray}) are grey.
 * The report was exactly that split: some enchantments grey, some white.
 *
 * <h2>How it decides which lines to touch</h2>
 *
 * <p>It only rewrites lines that are <b>both</b>:
 *
 * <ul>
 *   <li><b>unstyled</b> - a line that already carries a colour came from a source that made a choice,
 *       and a choice is not this class's to overrule; EnchantmentDescriptions' own lines are already
 *       grey and are left alone;</li>
 *   <li><b>the exact text of some enchantment's {@code <description id>.desc} translation</b> - read
 *       back through {@link I18n}, so only lines that really are a description are matched, not a name
 *       or an unrelated hint that happens to be white.</li>
 * </ul>
 *
 * <p>The namespace is deliberately not part of the test: "an unstyled enchantment description is
 * white" is a defect wherever it comes from, and the rule that fixes it is the same rule for this
 * library's enchantments, a dependent mod's, and a third party's alike. Nothing is added or removed -
 * a matched line keeps its text and only gains the grey style, so a tooltip can never end up with the
 * description twice.
 *
 * <h2>Why the lookup is cached per language</h2>
 *
 * <p>Walking every registered enchantment on every tooltip would work but is wasted work, so the set of
 * description texts is built once - and rebuilt whenever the selected language changes, because the
 * texts themselves are the cache key. Switching to a language the mod does not ship simply produces a
 * smaller set; nothing needs to be invalidated by hand.
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EnchantmentTooltipColour {

    /** Every {@code .desc} translation that currently exists, keyed by the language they were read in. */
    private static Set<String> descriptions = Set.of();
    private static String descriptionsLanguage;

    private EnchantmentTooltipColour() {
    }

    /**
     * Rewrites unstyled description lines to vanilla grey.
     *
     * @param event the tooltip event
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty()) {
            return;
        }
        Set<String> known = descriptions();
        if (known.isEmpty()) {
            return;
        }
        for (int index = 0; index < tooltip.size(); index++) {
            Component line = tooltip.get(index);
            if (line.getStyle().getColor() != null) {
                continue;
            }
            if (known.contains(line.getString())) {
                tooltip.set(index, line.copy().withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /**
     * @return the texts of every {@code <description id>.desc} translation that exists right now
     */
    private static Set<String> descriptions() {
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        if (language.equals(descriptionsLanguage)) {
            return descriptions;
        }
        Set<String> collected = new HashSet<>();
        for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS) {
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            if (id == null) {
                continue;
            }
            String key = enchantment.getDescriptionId() + ".desc";
            if (!I18n.exists(key)) {
                continue;
            }
            String value = I18n.get(key);
            if (!value.isEmpty()) {
                collected.add(value);
            }
        }
        descriptions = collected;
        descriptionsLanguage = language;
        return descriptions;
    }
}
