package com.huziyang520.merlinlib.tools.ui.vanilla;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class of the MerlinLib screens.
 *
 * <p>It owns the three things that are easy to get wrong:
 * <ul>
 *     <li><b>the panel is drawn at the right moment</b> - in {@code extractBackground}, which vanilla
 *     calls after the world dimming and before the widgets. Drawing it in {@code extractRenderState}
 *     would paint over the controls, and skipping the background pass would let the world show
 *     through the panel;</li>
 *     <li><b>the panel is sized from measured content</b> - see {@link #contentWidth()}, so a screen
 *     fits every language instead of one hard coded width;</li>
 *     <li><b>the screen pauses the game</b> - otherwise the world keeps animating behind the panel and
 *     the screen reads as flicker.</li>
 * </ul>
 */
public abstract class VanillaScreen extends Screen {

    private PanelLayout layout;

    protected VanillaScreen(Component title) {
        super(title);
    }

    /** @return the content width this screen needs, measured from its translated labels */
    protected abstract int contentWidth();

    /** @return the content height this screen needs */
    protected abstract int contentHeight();

    /**
     * The tallest content the window can hold.
     *
     * <p>A screen that asks for more than this runs off the bottom of the window - at a high GUI scale the
     * item editor lost its hint line and its buttons that way. Screens use this from
     * {@link #contentHeight()} and show less content instead: a shorter list, fewer rows.
     *
     * @return the usable content height in pixels
     */
    protected final int contentHeightLimit() {
        return Math.max(60, this.height - 2 * (VanillaUi.PADDING + 4));
    }

    /**
     * Builds the widgets of the screen, using the given layout to place them.
     *
     * @param layout the panel layout, already measured and centred
     */
    protected abstract void build(PanelLayout layout);

    @Override
    protected void init() {
        super.init();
        this.layout = PanelLayout.centered(this, contentWidth(), contentHeight());
        this.build(this.layout);
    }

    /** @return the layout, built on demand for the rare call before {@code init} */
    protected final PanelLayout layout() {
        if (this.layout == null) {
            this.layout = PanelLayout.centered(this, contentWidth(), contentHeight());
        }
        return this.layout;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        if (drawsPanel()) {
            layout().draw(graphics);
        }
        drawContentBackground(graphics);
    }

    /**
     * Draws everything that has to sit between the panel and the widgets: recessed list areas, row
     * highlights, and anything else the content is painted on.
     *
     * <p>This exists because of a real bug: a list area drawn in {@code extractRenderState} covers the
     * widgets of the rows it contains, which made the - / + / X and "add" buttons of a list invisible while
     * still being clickable. The background pass is the only place where a fill can safely go underneath
     * the widgets.
     *
     * @param graphics the render state extractor
     */
    protected void drawContentBackground(GuiGraphicsExtractor graphics) {
    }

    /**
     * Whether the vanilla panel is drawn behind the content.
     *
     * <p>A screen that is nothing but a column of vanilla buttons - an options screen - reads better with
     * the panel switched off, because the buttons already carry the vanilla frame. Such a screen draws its
     * text with a shadow instead, so it stays readable over any world.
     *
     * @return {@code true} to draw the panel
     */
    protected boolean drawsPanel() {
        return true;
    }

    /**
     * The screen a call to {@link #onClose()} returns to.
     *
     * @return the parent screen, or {@code null} to resume the game
     */
    protected Screen parentScreen() {
        return null;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parentScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
