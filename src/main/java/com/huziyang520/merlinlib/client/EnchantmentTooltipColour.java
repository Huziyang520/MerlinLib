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
