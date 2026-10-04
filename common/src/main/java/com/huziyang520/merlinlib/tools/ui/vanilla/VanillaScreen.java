package com.huziyang520.merlinlib.tools.ui.vanilla;

import com.huziyang520.merlinlib.ui.anim.ScreenIntro;
import com.huziyang520.merlinlib.ui.anim.UiAnimation;
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

    /** The colour vanilla dims the world with behind a screen: 0xC0101010. */
    private static final int WORLD_DIM = 0xC0101010;

    private PanelLayout layout;
    /** Whether a close was asked for and is being animated out. */
    private boolean closing;
    /** The animation of that close. */
    private ScreenIntro exitIntro;
    /**
     * The animation this screen opened with.
     *
     * <p>Made once, when the screen is made, and deliberately not per rebuild: a tab switch rebuilds the
     * widgets of this screen, and starting the opening animation again there made the panel jump every time a
     * tab was clicked.
     */
    private final ScreenIntro intro = subScreen() ? UiAnimation.subIntro() : UiAnimation.screenIntro();

    protected VanillaScreen(Component title) {
        super(title);
    }

    /**
     * Whether this screen is one of the second level screens the "sub screen opening / closing" switch is about.
     *
     * <p>Without it the switch did nothing at all: every screen took the main screen animation, so turning the
     * sub screen one off changed no screen anywhere. Only a screen that says it is one gets that treatment.
     *
     * @return {@code true} for a second level screen
     */
    protected boolean subScreen() {
        return false;
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
        if (this.closing && (this.exitIntro == null || !this.exitIntro.running())) {
            // The leave animation has finished: hand the screen over. Done here rather than in onClose because
            // the animation needs frames, and this is the method the game calls once per frame.
            Minecraft.getInstance().setScreenAndShow(parentScreen());
            return;
        }
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
        // Closing is animated too, and the switch to the parent happens when the animation is over - so the
        // screen leaves the way it arrived instead of blinking away.
        if (this.closing) {
            return;
        }
        this.closing = true;
        this.exitIntro = UiAnimation.intro();
    }

    /**
     * How far the screen's own animation has run.
     *
     * <p>0 at the start and 1 once it has arrived. On a close it counts back down, so one number describes both
     * directions and a screen that plays an animation out does not need its own idea of what closing means.
     *
     * @return the progress, between 0 and 1
     */
    protected final double introProgress() {
        if (!this.closing) {
            return this.intro.progress();
        }
        return this.exitIntro == null ? 1.0D : 1.0D - this.exitIntro.progress();
    }

    /**
     * Pushes the transform of the screen's opening or closing animation, about the centre of the window.
     *
     * <p>Every caller must pair it with {@link #endIntro}, in the same frame: the pose stack is shared with the
     * rest of the interface, and an unbalanced push would move everything drawn after this screen.
     *
     * @param graphics the render state extractor
     */
    protected final void beginIntro(GuiGraphicsExtractor graphics) {
        ScreenIntro animation = this.closing ? this.exitIntro : this.intro;
        double progress = this.introProgress();
        float centerX = this.width / 2.0F;
        float centerY = this.height / 2.0F;
        float scale = animation == null ? 1.0F : (float) animation.scale(progress);
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(centerX + (animation == null ? 0 : animation.offsetX(this.width, progress)),
                centerY + (animation == null ? 0 : animation.offsetY(this.height, progress)));
        pose.scale(scale, scale);
        pose.translate(-centerX, -centerY);
    }

    /**
     * Pops what {@link #beginIntro} pushed.
     *
     * @param graphics the render state extractor
     */
    protected final void endIntro(GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
    }

    /**
     * Draws the veil of a fading screen over the finished frame.
     *
     * <p>Over the frame rather than around the content on purpose: the widgets draw themselves in their own
     * colours and cannot be reached from here, so a veil drawn before them would leave the buttons blazing at
     * full brightness in the middle of a fade.
     *
     * <p>Called by a screen at the very end of its own {@code extractRenderState}, not from this class: a
     * subclass draws its own text after it has called {@code super}, so a veil drawn here would sit under that
     * text and the labels would stay bright while everything else faded.
     *
     * @param graphics the render state extractor
     */
    protected final void drawIntroVeil(GuiGraphicsExtractor graphics) {
        ScreenIntro animation = this.closing ? this.exitIntro : this.intro;
        if (animation == null || !animation.fades() || this.width <= 0 || this.height <= 0) {
            return;
        }
        int alpha = (int) Math.round(Math.max(0.0D, Math.min(1.0D, animation.veil(this.introProgress())))
                * 255.0D);
        if (alpha <= 0) {
            return;
        }
        graphics.fill(0, 0, this.width, this.height, alpha << 24);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
