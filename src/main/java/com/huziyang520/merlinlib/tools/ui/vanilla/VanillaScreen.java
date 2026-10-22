package com.huziyang520.merlinlib.tools.ui.vanilla;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class of the MerlinLib toolkit screens.
 *
 * <p>It owns the three things that are easy to get wrong:
 * <ul>
 *     <li><b>the panel is drawn at the right moment</b> - in the background pass, which vanilla runs after
 *     the world dimming and before the widgets. Drawing it in {@code render} would paint over the controls,
 *     and skipping the background pass would let the world show through the panel;</li>
 *     <li><b>the panel is sized from measured content</b> - see {@link #contentWidth()}, so a screen
 *     fits every language instead of one hard coded width;</li>
 *     <li><b>the screen pauses the game</b> - otherwise the world keeps animating behind the panel and
 *     the screen reads as flicker.</li>
 * </ul>
 *
 * <p><b>No entrance or exit animation.</b> These screens appear and disappear at once, like a vanilla one.
 * The helpers in {@code ui.anim} are still part of the library's api for mods that want them, but nothing
 * here calls them and no pose transform is applied to the content any more: a toolkit screen is a tool, and
 * waiting for it to slide into place made editing slower, not nicer.
 *
 * <h2>Render pass on this version</h2>
 *
 * <p>A screen has a single {@link #render(GuiGraphics, int, int, float)}, and the background has its own
 * method beside it ({@code renderBackground(GuiGraphics, int, int, float)}). {@code Screen#render} calls the
 * background pass itself, so this class calls it as the first line of {@link #render} and remembers that it
 * did: without the flag every frame painted the dimming and the panel twice.
 *
 * <p>The world dimming is vanilla's: {@code Screen#renderBackground} fills exactly the veil a MerlinLib screen
 * used to paint itself, so it is inherited rather than repeated - drawing a second one would make a
 * panel-less screen darker than every other screen in the game.
 */
public abstract class VanillaScreen extends Screen {

    private PanelLayout layout;
    /**
     * Whether this frame's background has already been drawn.
     *
     * <p>Reset at the top of {@link #render} and set by {@link #renderBackground}, which the base class calls
     * as well, so the second call becomes a no-op.
     */
    private boolean backgroundDrawn;

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
     * Draws the background, then the widgets and the content the screen paints itself.
     *
     * <p>A subclass adds its own text and overlays by overriding {@link #renderContent}, which is called after
     * the widgets. It must not call {@code super.render} for that: this method already draws the widgets, once.
     *
     * @param graphics    the graphics object
     * @param mouseX      the cursor x
     * @param mouseY      the cursor y
     * @param partialTick the partial tick
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Exactly one background per frame. Screen#render itself calls renderBackground, so calling it here as
        // well drew the world dimming and the panel twice; the flag makes the second call a no-op.
        this.backgroundDrawn = false;
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawContentBackground(graphics);
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
     * <p><b>1.21.1 note:</b> the signature is
     * {@code renderBackground(GuiGraphics, int mouseX, int mouseY, float partialTick)} - the three view
     * parameters are present on this line, so this override matches the four argument method the game calls.
     *
     * @param graphics    the graphics object
     * @param mouseX      the cursor x, unused
     * @param mouseY      the cursor y, unused
     * @param partialTick the partial tick, unused
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.backgroundDrawn) {
            return;
        }
        this.backgroundDrawn = true;
        // The world dimming of a vanilla screen, plus the dirt background when there is no level. The base
        // implementation fills exactly WORLD_DIM, which is the same veil this screen would paint itself; it is
        // inherited rather than repeated so a screen is not darker than every other one.
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
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
     * @param graphics the graphics object
     */
    protected void drawContentBackground(GuiGraphics graphics) {
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
        Minecraft.getInstance().setScreen(parentScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
