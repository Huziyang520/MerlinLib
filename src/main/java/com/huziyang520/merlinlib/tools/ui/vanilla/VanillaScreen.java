package com.huziyang520.merlinlib.tools.ui.vanilla;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class of the MerlinLib screens.
 *
 * <p>It owns the three things that are easy to get wrong:
 * <ul>
 *     <li><b>the panel is drawn at the right moment</b> - in the background pass, which vanilla runs after
 *     the world dimming and before the widgets. Drawing it in {@code render} would paint over the controls,
 *     and skipping the background pass would let the world show through the panel;</li>
 *     <li><b>the panel is sized from measured content</b> - see {@link #contentWidth()}, so a screen
 *     fits every language instead of one hard coded width;</li>
 *     <li><b>the screen pauses the game</b> - otherwise the world keeps moving behind the panel and
 *     the screen reads as flicker.</li>
 * </ul>
 *
 * <h2>No opening or closing animation</h2>
 *
 * <p>A toolkit screen appears and disappears at once. The animation helpers in
 * {@code com.huziyang520.merlinlib.ui.anim} are still part of the public api for dependent mods, but this
 * mod no longer plays them: a screen that slides in and out is exactly the thing that felt slow while
 * editing an item.
 *
 * <h2>What changed / 1.20.1 note</h2>
 *
 * <p>The two-pass interface of 1.21 is gone, and with it the {@code GuiGraphicsExtractor}. On this version
 * a screen has a single {@link #render(GuiGraphics, int, int, float)} and the background has its own
 * method beside it, so the 26.3 pair maps like this:
 *
 * <table border="1">
 *   <caption>render pass mapping</caption>
 *   <tr><th>26.3</th><th>1.20.1</th></tr>
 *   <tr><td>{@code extractRenderState(GuiGraphicsExtractor, int, int, float)}</td>
 *       <td>{@code render(GuiGraphics, int, int, float)}</td></tr>
 *   <tr><td>{@code extractBackground(GuiGraphicsExtractor, int, int, float)}</td>
 *       <td>{@code renderBackground(GuiGraphics)} - <b>three fewer parameters</b></td></tr>
 * </table>
 *
 * <p><b>{@code renderBackground} is not called for us.</b> On 1.21 the game invoked the background pass
 * itself. On 1.20.1 it does not: every vanilla screen calls {@code renderBackground} as the first line of
 * its own {@code render}, and a screen that forgets simply has no dimming and no panel. This class
 * therefore calls it from {@link #render}, which is what lets a subclass keep the 26.3 habit of writing
 * "{@code super.render(...)}, then my text" and still get the background drawn exactly once.
 *
 * <p><b>The world dimming is vanilla's now.</b> 26.3 painted its own {@code 0xC0101010} veil because the
 * base class there did not. This version's {@code Screen#renderBackground} fills exactly that colour
 * ({@code -1072689136}) over the window, so the veil is inherited rather than repeated - drawing a second
 * one would double the darkness and make a panel-less screen look like a different screen from every other
 * one in the game.
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
     * <p><b>What changed / 1.20.1 note:</b> this method also performs the background pass, because on this
     * version nothing draws the dimming or the panel unless the screen asks: every vanilla screen calls
     * {@code renderBackground(GuiGraphics)} as the first line of its own {@code render} (three fewer parameters
     * than 26.3's {@code extractBackground}). The 26.3 screens relied on the game calling the background pass
     * itself, which is why the call lives here rather than in each subclass.
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
     * <p><b>What changed / 1.20.1 note:</b> the 26.3 signature was
     * {@code extractBackground(GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick)}. This
     * version's is {@code renderBackground(GuiGraphics)} - the three view parameters are gone because the
     * background pass cannot depend on the cursor or the tick here - so this override has a <b>different
     * parameter list</b> from the one it replaces, and a subclass that overrode the 26.3 method to customise
     * the background must move to this one.
     *
     * <p>The cursor parameters were unused in the 26.3 body as well, so nothing was lost in the move.
     *
     * @param graphics the graphics object
     */
    @Override
    public void renderBackground(GuiGraphics graphics) {
        // The world dimming of a vanilla screen, plus the dirt background when there is no level. On this
        // version the base implementation fills exactly WORLD_DIM (-1072689136), which is the same veil 26.3
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
     * <p><b>What changed / 1.20.1 note:</b> the parameter is a {@link GuiGraphics}, not a
     * {@code GuiGraphicsExtractor}. The method name and its role are unchanged, so a subclass still
     * overrides exactly this name; only the parameter type moves, and every caller inside it swaps
     * {@code graphics.text(...)} for {@code graphics.drawString(...)}.
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
