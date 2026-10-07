package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.config.ClientConfig;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.tools.ui.vanilla.PanelLayout;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaScreen;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The animation switches, opened from the general tab.
 *
 * <p>One row per place the animation is used, each with a switch of its own, and one row for the kind - which is
 * a button that cycles through the four kinds rather than a number field, because the player wants to see the
 * names, not remember that 3 means fade.
 *
 * <p>Only MerlinLib's own interface is affected. A business mod may read the same values through
 * {@link com.huziyang520.merlinlib.ui.anim.UiAnimation}, use the animation helpers with a setting of its own, or
 * ignore the feature; the switches here are deliberately not a global override for other people's screens.
 *
 * <h2>What changed / 1.20.1 note</h2>
 *
 * <p>Two renames and nothing else:
 *
 * <ul>
 *   <li>the render pass is {@link #render(GuiGraphics, int, int, float)} rather than
 *       {@code extractRenderState(GuiGraphicsExtractor, ...)};</li>
 *   <li>the text calls on {@code graphics} are <b>{@code drawString(font, component, x, y, shadow)}</b> on
 *       this version. 1.21 spells the same operation {@code graphics.text(...)}; every one of those calls in
 *       this screen's render body was rewritten, and the shadow flag is still passed as the last argument;</li>
 *   <li>{@code ConfigManager.saveClient(Map)} takes the same {@code section.key} map, and the keys below are
 *       unchanged, so the file this screen writes is identical to the one the 26.3 line wrote. The option
 *       names and the layout version in {@code ClientConfig} were carried over deliberately, so a player
 *       moving between the two lines keeps their switches.</li>
 * </ul>
 */
public final class AnimationSettingsScreen extends VanillaScreen {

    private static final String T = "gui.merlinlib.anim.";
    private static final int CONTROL_WIDTH = 96;
    private static final int ROW = 24;

    /** The kind names, in the order the number in the configuration means them. */
    private static final String[] KIND_KEYS = {
            "gui.merlinlib.anim.kind.pop", "gui.merlinlib.anim.kind.up",
            "gui.merlinlib.anim.kind.side", "gui.merlinlib.anim.kind.fade"};

    private final Screen parent;
    private boolean enabled;
    private boolean screen;
    private boolean tab;
    private boolean sub;
    private int kind;
    /**
     * The Y of the first row. The labels and the controls are both measured from this one number, so a row can
     * never drift away from its own control - which is what happened while the controls followed the layout
     * cursor and the labels counted rows of their own.
     */
    private int firstRow;

    /**
     * @param parent the screen to return to
     */
    public AnimationSettingsScreen(Screen parent) {
        super(Component.translatable(T + "title"));
        this.parent = parent;
    }

    @Override
    protected Screen parentScreen() {
        return this.parent;
    }

    @Override
    protected boolean drawsPanel() {
        return false;
    }

    @Override
    protected boolean subScreen() {
        return true;
    }

    /**
     * The labels of the rows, in order.
     *
     * <p>The master switch is the first row here because this is the screen about the animation: the key existed
     * from the start and nothing on screen could change it, so the only way to switch the feature off was to edit
     * the file by hand.
     */
    private static String[] rowKeys() {
        return new String[]{T + "enabled", T + "screen", T + "tab", T + "sub", T + "kind"};
    }

    @Override
    protected int contentWidth() {
        int widest = this.font.width(Component.translatable(T + "title"));
        for (String key : rowKeys()) {
            widest = Math.max(widest, this.font.width(Component.translatable(key)) + 12 + CONTROL_WIDTH);
        }
        for (String line : VanillaUi.wrap(this.font, Component.translatable(T + "hint").getString(),
                Math.max(120, this.width - 40))) {
            widest = Math.max(widest, this.font.width(line));
        }
        return Math.max(280, Math.min(widest + 24, this.width - 40));
    }

    @Override
    protected int contentHeight() {
        int hints = VanillaUi.wrap(this.font, Component.translatable(T + "hint").getString(),
                Math.max(120, this.width - 40)).size() * 10;
        return Math.min(12 + 8 + rowKeys().length * ROW + hints + 12 + VanillaUi.WIDGET_HEIGHT,
                this.height - 40);
    }

    @Override
    protected void build(PanelLayout layout) {
        ClientConfig config = ConfigManager.client();
        this.enabled = config.uiAnimationEnabled();
        this.screen = config.animationScreen();
        this.tab = config.animationTab();
        this.sub = config.animationSub();
        this.kind = Math.max(0, Math.min(3, config.uiAnimationKind()));

        int left = layout.left();
        int controlX = left + this.area(layout) - CONTROL_WIDTH;
        // The title row, then the rows. The cursor is moved through the same steps the old code took so the
        // panel keeps its shape, but the controls no longer take their Y from it: they take it from rowY(), the
        // same number the labels are drawn with.
        layout.row(12);
        layout.gap(8);
        this.firstRow = layout.cursor();
        layout.row(rowKeys().length * ROW);
        layout.gap(12);

        this.addRenderableWidget(VanillaUi.button(stateLabel(this.enabled), button -> {
            this.enabled = !this.enabled;
            button.setMessage(stateLabel(this.enabled));
        }, controlX, rowY(0), CONTROL_WIDTH));

        this.addRenderableWidget(VanillaUi.button(stateLabel(this.screen), button -> {
            this.screen = !this.screen;
            button.setMessage(stateLabel(this.screen));
        }, controlX, rowY(1), CONTROL_WIDTH));

        this.addRenderableWidget(VanillaUi.button(stateLabel(this.tab), button -> {
            this.tab = !this.tab;
            button.setMessage(stateLabel(this.tab));
        }, controlX, rowY(2), CONTROL_WIDTH));

        this.addRenderableWidget(VanillaUi.button(stateLabel(this.sub), button -> {
            this.sub = !this.sub;
            button.setMessage(stateLabel(this.sub));
        }, controlX, rowY(3), CONTROL_WIDTH));

        this.addRenderableWidget(VanillaUi.button(kindLabel(this.kind), button -> {
            // Cycling rather than typing: the names are the whole point, and a number field asked the player to
            // remember what 3 meant.
            this.kind = (this.kind + 1) % KIND_KEYS.length;
            button.setMessage(kindLabel(this.kind));
        }, controlX, rowY(4), CONTROL_WIDTH));
        int bottom = layout.contentBottom() - VanillaUi.WIDGET_HEIGHT;
        int half = layout.sliceWidth(2);
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.config.save"),
                button -> this.save(), layout.sliceX(0, 2), bottom, half));
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.cancel"),
                button -> this.onClose(), layout.sliceX(1, 2), bottom, half));
    }

    /** @return the row width, the same measurement the controls were placed with */
    private int area(PanelLayout layout) {
        return layout.contentWidth() - VanillaUi.SCROLLBAR_WIDTH - 2;
    }

    private static Component stateLabel(boolean enabled) {
        return Component.translatable(enabled ? "gui.merlinlib.config.on" : "gui.merlinlib.config.off");
    }

    private static Component kindLabel(int kind) {
        return Component.translatable(KIND_KEYS[Math.max(0, Math.min(KIND_KEYS.length - 1, kind))]);
    }

    /**
     * @param index the row, counting from the top
     * @return the Y of that row, the number both its control and its label are placed with
     */
    private int rowY(int index) {
        return this.firstRow + index * ROW;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        beginIntro(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        PanelLayout layout = layout();
        int left = layout.left();
        int width = area(layout) - CONTROL_WIDTH - 12;

        graphics.drawString(this.font, Component.translatable(T + "title"), left, layout.top() + 12,
                VanillaUi.TEXT, true);

        String[] keys = rowKeys();
        for (int index = 0; index < keys.length; index++) {
            // The +6 sits the text inside the button beside it, the same nudge the main settings screen uses -
            // and from the very same rowY() the button was placed with, so the two can never drift apart.
            graphics.drawString(this.font, Component.literal(VanillaUi.clip(this.font,
                            Component.translatable(keys[index]).getString(), width)),
                    left, rowY(index) + 6, VanillaUi.TEXT, true);
        }

        int hintTop = layout.contentBottom() - VanillaUi.WIDGET_HEIGHT - 6
                - VanillaUi.wrap(this.font, Component.translatable(T + "hint").getString(),
                layout.contentWidth()).size() * 10;
        int line = 0;
        for (String text : VanillaUi.wrap(this.font, Component.translatable(T + "hint").getString(),
                layout.contentWidth())) {
            graphics.drawString(this.font, Component.literal(text), left, hintTop + line * 10,
                    VanillaUi.TEXT_HINT, true);
            line++;
        }
        endIntro(graphics);
        drawIntroVeil(graphics);
    }

    private void save() {
        ConfigManager.saveClient(java.util.Map.of(
                "gui.animation_enabled", Boolean.toString(this.enabled),
                "gui.animation_screen", Boolean.toString(this.screen),
                "gui.animation_tab", Boolean.toString(this.tab),
                "gui.animation_sub", Boolean.toString(this.sub),
                "gui.animation_kind", Integer.toString(this.kind)));
        this.onClose();
    }
}
