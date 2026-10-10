package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.config.ClientConfig;
import com.huziyang520.merlinlib.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * The rules the folded health row and the armour bar follow, in one place.
 *
 * <h2>Why the armour bar is moved here rather than in the health row</h2>
 *
 * <p>Everything that positions the vanilla rows lives inside {@code Gui.renderPlayerHealth}, and on
 * 1.20.1 nothing aimed at that method runs: redirecting its {@code Player.getAttributeValue} call, its
 * {@code renderHearts} call argument, its layout slot, and even its {@code Math.max(float, float)} call
 * were each found by Mixin and each did nothing at runtime, while every hook inside
 * {@code Gui.renderHearts} worked. Measured, repeatedly, on a real client.
 *
 * <p>So this class does not try again. The armour row is drawn from the vanilla icon sheet, and the
 * sheet's armour row is the one place that image is used with {@code v = 9} - hearts are {@code v = 0},
 * air is {@code v = 18}, food is {@code v = 27}. A hook on {@code GuiGraphics.blit} can therefore
 * recognise an armour icon on sight and put it down one line above the heart baseline, which is exactly
 * where vanilla puts it when the row is a single row. That works no matter who draws it.
 */
public final class HudRowPolicy {

    /** The vanilla icon sheet. */
    private static final ResourceLocation ICONS =
            new ResourceLocation("minecraft", "textures/gui/icons.png");

    /** The {@code v} of the armour row on that sheet; every other HUD row uses a different one. */
    private static final int ARMOUR_ROW_V = 9;

    /** A HUD icon is nine by nine in every one of those rows. */
    private static final int ICON_SIZE = 9;

    /**
     * How far the heart baseline sits above the bottom of the screen.
     *
     * <p>Read out of {@code renderPlayerHealth}, which computes {@code this.screenHeight - 39} before it
     * draws anything.
     */
    private static final int HEART_BASELINE_FROM_BOTTOM = 39;

    /** The gap vanilla leaves between the heart baseline and the armour row above it. */
    private static final int ARMOUR_GAP = 10;

    private HudRowPolicy() {
    }

    /**
     * Whether the row folds to one heart plus the numbers.
     *
     * @param real the health the row was built from
     * @return true when the row collapses
     */
    public static boolean folding(float real) {
        ClientConfig config = ConfigManager.client();
        if (config == null || !config.hudCollapse()) {
            return false;
        }
        return real > Math.max(1.0F, config.hudCollapseThreshold());
    }

    /** Whether the player asked for the armour bar to be laid against the row that is really drawn. */
    public static boolean adaptingArmour() {
        ClientConfig config = ConfigManager.client();
        return config != null && config.hudCollapse() && config.hudArmourDodge();
    }

    /**
     * Whether one {@code blit} is an armour bar icon.
     *
     * @param texture the sheet being drawn from
     * @param v       the source row on that sheet
     * @param width   the icon width
     * @param height  the icon height
     * @return true when this is the armour row of the vanilla icon sheet
     */
    public static boolean isArmourIcon(ResourceLocation texture, int v, int width, int height) {
        return ICONS.equals(texture) && v == ARMOUR_ROW_V && width == ICON_SIZE
                && height == ICON_SIZE;
    }

    /** The y the armour bar belongs at once the row is folded: one line above the heart baseline. */
    public static int adaptedArmourY() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight() - HEART_BASELINE_FROM_BOTTOM
                - ARMOUR_GAP;
    }
}
