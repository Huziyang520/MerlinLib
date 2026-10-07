package com.huziyang520.merlinlib.tools.ui.vanilla;

import com.huziyang520.merlinlib.ui.anim.ScreenIntro;
import com.huziyang520.merlinlib.ui.anim.UiAnimation;
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
 *     <li><b>the screen pauses the game</b> - otherwise the world keeps animating behind the panel and
 *     the screen reads as flicker.</li>
 * </ul>
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
 *
 * <p><b>Pose stack method names.</b> 1.21 renamed the pose stack operations; this version keeps the old
 * ones. {@link #beginIntro} uses {@code pushPose()} / {@code popPose()} and the {@code float} overload of
 * {@code translate}, where 26.3 wrote {@code pushMatrix()} / {@code popMatrix}.
 *
 * <p>The one thing a subclass must remember: {@link #beginIntro} and {@link #endIntro} must still be paired
 * inside a single frame. The pose stack is shared with the rest of the interface on this version too.
 */
public abstract class VanillaScreen extends Screen {

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

    /**
     * Draws the world dimming, the panel and the content background, then the widgets.
     *
     * <p>A subclass overrides this to add its own text and calls {@code super.render(...)} first, which is
     * the same shape the 26.3 screens used.
     *
     * <p><b>What changed / 1.20.1 note:</b> this method now also performs the background pass. On 1.21 the
     * game called {@code extractBackground} itself and a screen only had to draw its content; here nothing
     * draws the dimming or the panel unless the screen asks, so the call is made for every subclass.
     *
     * @param graphics    the graphics object
     * @param mouseX      the cursor x
     * @param mouseY      the cursor y
     * @param partialTick the partial tick
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Draws the background: the leave animation handover, the world dimming and the panel.
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
        if (this.closing && (this.exitIntro == null || !this.exitIntro.running())) {
            // The leave animation has finished: hand the screen over. Done here rather than in onClose because
            // the animation needs frames, and this is the method the game calls once per frame.
            Minecraft.getInstance().setScreen(parentScreen());
            return;
        }
        // The world dimming of a vanilla screen, plus the dirt background when there is no level. On this
        // version the base implementation fills exactly WORLD_DIM (-1072689136), which is the same veil 26.3
        // painted itself; it is inherited rather than repeated so a screen is not darker than every other one.
        super.renderBackground(graphics);
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
     * <p><b>What changed / 1.20.1 note:</b> the pose stack operations are the pre-1.21 names -
     * {@code pushPose()} here, {@code pushMatrix()} on 26.3 - and the two offsets are widened to
     * {@code float} explicitly. The parameter type is a {@link GuiGraphics}; 26.3 took a
     * {@code GuiGraphicsExtractor}. Both expose {@code pose()}, so the body is otherwise identical.
     *
     * @param graphics the graphics object
     */
    protected final void beginIntro(GuiGraphics graphics) {
        ScreenIntro animation = this.closing ? this.exitIntro : this.intro;
        double progress = this.introProgress();
        float centerX = this.width / 2.0F;
        float centerY = this.height / 2.0F;
        float scale = animation == null ? 1.0F : (float) animation.scale(progress);
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX + (animation == null ? 0 : animation.offsetX(this.width, progress)),
                centerY + (animation == null ? 0 : animation.offsetY(this.height, progress)), 0.0F);
        pose.scale(scale, scale, 1.0F);
        pose.translate(-centerX, -centerY, 0.0F);
    }

    /**
     * Pops what {@link #beginIntro} pushed.
     *
     * <p><b>What changed / 1.20.1 note:</b> {@code popPose()} on this version, {@code popMatrix()} on 26.3,
     * and a {@link GuiGraphics} instead of a {@code GuiGraphicsExtractor}.
     *
     * @param graphics the graphics object
     */
    protected final void endIntro(GuiGraphics graphics) {
        graphics.pose().popPose();
    }

    /**
     * Draws the veil of a fading screen over the finished frame.
     *
     * <p>Over the frame rather than around the content on purpose: the widgets draw themselves in their own
     * colours and cannot be reached from here, so a veil drawn before them would leave the buttons blazing at
     * full brightness in the middle of a fade.
     *
     * <p>Called by a screen at the very end of its own {@code render}, not from this class: a
     * subclass draws its own text after it has called {@code super}, so a veil drawn here would sit under that
     * text and the labels would stay bright while everything else faded.
     *
     * <p><b>What changed / 1.20.1 note:</b> a {@link GuiGraphics} instead of a
     * {@code GuiGraphicsExtractor}. The body is unchanged - {@code fill} exists on both.
     *
     * @param graphics the graphics object
     */
    protected final void drawIntroVeil(GuiGraphics graphics) {
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
