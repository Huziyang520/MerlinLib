// The rules the folded health row and the armour bar follow, in one place.

package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.config.ClientConfig;
import com.huziyang520.merlinlib.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class HudRowPolicy {
    private static final ResourceLocation ICONS =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/icons.png");

    private static final int ARMOUR_ROW_V = 9;
    private static final int ICON_SIZE = 9;
    private static final int HEART_BASELINE_FROM_BOTTOM = 39;
    private static final int ARMOUR_GAP = 10;

    private HudRowPolicy() {
    }

    public static boolean folding(float real) {
        ClientConfig config = ConfigManager.client();
        if (config == null || !config.hudCollapse()) {
            return false;
        }
        return real > Math.max(1.0F, config.hudCollapseThreshold());
    }

    public static boolean adaptingArmour() {
        ClientConfig config = ConfigManager.client();
        return config != null && config.hudCollapse() && config.hudArmourDodge();
    }

    public static boolean isArmourIcon(ResourceLocation texture, int v, int width, int height) {
        return ICONS.equals(texture) && v == ARMOUR_ROW_V && width == ICON_SIZE
                && height == ICON_SIZE;
    }

    public static int adaptedArmourY() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight() - HEART_BASELINE_FROM_BOTTOM
                - ARMOUR_GAP;
    }
}
