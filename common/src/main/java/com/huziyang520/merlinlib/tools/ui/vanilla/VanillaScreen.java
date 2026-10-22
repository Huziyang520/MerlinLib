package com.huziyang520.merlinlib.tools.ui.vanilla;

import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
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
 *
 * <h2>No opening or closing animation</h2>
 *
 * <p>These screens appear and disappear at once. The animation helpers in
 * {@code com.huziyang520.merlinlib.ui.anim} are still shipped for other mods, but nothing in this mod calls
 * them: an opening transform also had to be paired with a delayed handover on close, and a screen that waited
 * for frames before showing its parent could be left half open by a single dropped frame.
 */
public abstract class VanillaScreen extends Screen {

    /** The colour vanilla dims the world with behind a screen: 0xC0101010. */
    private static final int WORLD_DIM = 0xC0101010;

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
     * Draws the content backdrop, then the widgets and the content the screen paints itself.
     *
     * <p>A subclass adds its own text and overlays by overriding {@link #renderContent}, which is called after
     * the widgets. It must not call {@code super.extractRenderState} for that: this method already draws the
     * widgets, once.
     *
     * @param graphics    the render state extractor
     * @param mouseX      the cursor x
     * @param mouseY      the cursor y
     * @param partialTick the partial tick
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The recessed list areas belong to the content and have to sit behind the rows they contain; drawn in
        // the background pass they would cover the buttons of those rows.
        drawContentBackground(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderContent(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Draws everything this screen adds on top of its widgets.
     *
     * <p>Called after the widgets so labels and overlays stay above them. The default draws nothing.
     *
     * @param graphics    the render state extractor
     * @param mouseX      the cursor x
     * @param mouseY      the cursor y
     * @param partialTick the partial tick
     */
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        // The world dimming of a vanilla screen, drawn by us: without it the world behind a panel-less screen
        // stayed at full brightness, and the frame the game itself dims (before our first layout pass) showed
        // up as a single flash of a mask that then disappeared. Keeping the mask removes the flash and matches
        // every other screen in the game.
        if (this.width > 0 && this.height > 0) {
            graphics.fill(0, 0, this.width, this.height, WORLD_DIM);
        }
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
     * <p>This exists because of a real bug: a list area drawn in {@code extractRenderState} covers the
     * widgets of the rows it contains, which made the - / + / X and "add" buttons of a list invisible while
     * still being clickable.
     *
     * @param graphics the render state extractor
     */
    protected void drawContentBackground(GuiGraphicsExtractor graphics) {
    }

    /**
     * Whether the vanilla panel is drawn behind the content.
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
        // The handover goes through the shared switcher: the method that shows a screen moved between 26.1 and
        // 26.2, and only one of the two exists on either side.
        MerlinScreens.show(parentScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
