package com.huziyang520.merlinlib.tools.ui.vanilla;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class of the MerlinLib screens.
 */
public abstract class VanillaScreen extends Screen {

    private PanelLayout layout;

    protected VanillaScreen(Component title) {
        super(title);
    }

    /**
     * The width the panel is actually built with.
     *
     * <p>{@link #contentWidth()} measures what the screen wants; this caps it, so a screen that measures a long
     * piece of text does not stretch the panel across the window and leave its labels and controls at opposite
     * ends. Every layout question a screen asks is answered by the layout object, so the cap is applied once
     * here rather than in each screen.
     *
     * @return the panel width, never more than {@link VanillaUi#MAX_PANEL_WIDTH} or the window allows
 */
    protected int panelWidth() {
        return Math.min(contentWidth(), Math.min(VanillaUi.MAX_PANEL_WIDTH, this.width - VanillaUi.PADDING));
    }

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
        this.layout = PanelLayout.centered(this, panelWidth(), contentHeight());
        this.build(this.layout);
    }

    /** @return the layout, built on demand for the rare call before {@code init} */
    protected final PanelLayout layout() {
        if (this.layout == null) {
            this.layout = PanelLayout.centered(this, panelWidth(), contentHeight());
        }
        return this.layout;
    }

    /**
     * Draws the background, then the widgets and the content.
 *
     * <p><b>The order is the flicker fix.</b> The world dimming and the panel belong to the background pass
     * and are drawn first; only after that do the widgets and the content the screen paints itself follow.
     * An earlier version pushed a transform first and called the background from inside it, which scaled a
     * full-screen black veil down to 92% around the centre: the outer band of the window was not covered by
     * the veil at all, so the bright world showed through a frame-wide border. Dimming is now always exactly
     * the window, on every frame.
 *
     * <p>A subclass adds its own text and overlays by overriding {@link #renderContent}, which is called
     * after the widgets. It must not call {@code super.render} for that: this method already draws the
     * widgets, once.
 *
     * @param graphics    the graphics object
     * @param mouseX      the cursor x
     * @param mouseY      the cursor y
     * @param partialTick the partial tick
 */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        // Between the panel and the widgets on purpose: the recessed list areas belong to the content and
        // have to sit behind the rows they frame, and a fill drawn in render() would cover the row buttons.
        drawContentBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderContent(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Draws everything this screen adds on top of its widgets.
     *
     * <p>Called after the widgets so labels and overlays stay above them. The default draws nothing.
     *
     * @param graphics    the graphics object
     * @param mouseX      the cursor x
     * @param mouseY      the cursor y
     * @param partialTick the partial tick
 */
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    /**
     * Draws the background: the world dimming and the panel.
 *
     * @param graphics the graphics object
 */
    @Override
    public void renderBackground(GuiGraphics graphics) {
        // The world dimming of a vanilla screen, plus the dirt background when there is no level. On this
        // painted itself; it is inherited rather than repeated so a screen is not darker than every other one.
        super.renderBackground(graphics);
        // A window size of zero means this frame arrives before the first layout pass; drawing the panel then
        // would put a tiny one in the corner for that frame, which reads as a flash.
        if (drawsPanel() && this.width > 0 && this.height > 0) {
            layout().draw(graphics);
        }
    }

    /**
     * Draws everything that has to sit between the panel and the widgets: recessed list areas, row
     * highlights, and anything else the content is painted on.
 *
     * <p>This exists because of a real bug: a list area drawn in {@code render} covers the widgets of the
     * rows it contains, which made the - / + / X and "add" buttons of a list invisible while still being
     * clickable. The background pass is the only place where a fill can safely go underneath the widgets.
 *
     * @param graphics the graphics object
 */
    protected void drawContentBackground(GuiGraphics graphics) {
    }

    /**
     * Whether the vanilla panel is drawn behind the content.
     *
     * <p>A screen that is nothing but a column of vanilla buttons reads better with the panel switched off,
     * because the buttons already carry the vanilla frame. Such a screen draws its text with a shadow
     * instead, so it stays readable over any world.
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
        // Straight to the parent, in the same frame: there is no closing animation to wait for.
        Minecraft.getInstance().setScreen(parentScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
