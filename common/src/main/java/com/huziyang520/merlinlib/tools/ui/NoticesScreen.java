package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.NoticeSettings;
import com.huziyang520.merlinlib.notice.NoticeEntry;
import com.huziyang520.merlinlib.notice.NoticeRegistry;
import com.huziyang520.merlinlib.tools.ui.vanilla.PanelLayout;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaScreen;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The join notice switches: one master switch, then one row per mod that registered a notice.
 *
 * <h2>Where the rows come from</h2>
 *
 * <p>Not from a list written here. The mods are exactly the owners of the notices in
 * {@link NoticeRegistry#codeNotices()}, which is the same list the server offers on join, so the screen can never
 * fall behind the notices that actually exist - a mod that starts registering a notice appears here on its own,
 * and one that stops disappears. MerlinLib is listed first because its own notice is the one a player is most
 * likely to be looking for; the business mods follow alphabetically, which is the only order that stays stable
 * as mods are added.
 *
 * <h2>Where the switches are stored</h2>
 *
 * <p>The master switch is {@code notices.enabled} in {@code server.toml}, the same value the configuration screen
 * edits. The per mod switches are {@code config/MerlinLib/notices.toml}, one key per mod - see
 * {@link NoticeSettings}, which also imports the retired {@code notices.per_mod} string the first time it runs.
 * Both are read at the moment a player joins, so a change made here applies to the next join rather than to the
 * next launch.
 *
 * <p>No animation: like every other MerlinLib screen, it appears and disappears at once - see
 * {@link VanillaScreen}.
 */
public final class NoticesScreen extends VanillaScreen {

    /** Width of the on/off button on the right of every row. */
    private static final int TOGGLE_WIDTH = 56;

    /** A panel narrower than this pushes its labels and switches too far apart. */
    private static final int MIN_CONTENT_WIDTH = 220;

    /** Height of the title line. */
    private static final int TITLE_LINE = 12;

    /** Gap between the title and the first row. */
    private static final int HEADER_GAP = 6;

    /** Height of a note line, and of one line of the hint. */
    private static final int NOTE_LINE = 12;
    private static final int HINT_LINE = 10;

    /** The hint is a footnote; more than this and it would push the switches off the panel. */
    private static final int MAX_HINT_LINES = 3;

    /** The screen to return to, or {@code null} to resume the game. */
    private final Screen parent;

    /** Every mod that owns a code notice, MerlinLib first. */
    private final List<String> owners = new ArrayList<>();

    /** The default each owner declared, used when nothing is stored for it yet. */
    private final Map<String, Boolean> defaults = new LinkedHashMap<>();

    /** The switch in effect for each owner. */
    private final Map<String, Boolean> values = new LinkedHashMap<>();

    /** Whether the master switch is on. */
    private boolean masterOn;

    /** The owners that actually fit on the panel. */
    private final List<String> shown = new ArrayList<>();

    /** The label and top edge of every row, recorded while the widgets are placed. */
    private final List<Row> rows = new ArrayList<>();

    /** The wrapped hint, as many lines as it needs. */
    private List<String> hintLines = List.of();

    /** Whether a note is drawn under the rows. */
    private boolean noteShown;

    /** Whether the note is the "some mods are not shown" line rather than the "no business mod" one. */
    private boolean truncated;

    /** Top edge of the note and of the hint, or {@code -1} when they are not drawn. */
    private int noteY = -1;
    private int hintY = -1;

    /** Guard so a second layout pass does not re-plan from scratch. */
    private boolean planned;

    /** One row: the label of a switch and the top edge of the widget beside it. */
    private record Row(Component label, int y) {
    }

    /**
     * @param parent the screen to return to, or {@code null} to resume the game
     */
    public NoticesScreen(Screen parent) {
        super(Component.translatable("gui.merlinlib.notices.title"));
        this.parent = parent;
        this.masterOn = ConfigManager.server().noticesEnabled();
        collect();
    }

    @Override
    protected Screen parentScreen() {
        return this.parent;
    }

    /**
     * Re-plans on every layout pass.
     *
     * <p>{@code init} runs again on a window resize and whenever a switch is flipped (the screen rebuilds its
     * widgets to relabel the buttons), and how many rows fit depends on the window, so the plan cannot outlive a
     * layout pass.
     */
    @Override
    protected void init() {
        this.planned = false;
        this.noteY = -1;
        this.hintY = -1;
        super.init();
    }

    /**
     * Reads the owners and the switches in effect.
     *
     * <p>Done once in the constructor rather than in {@code init}: {@code init} runs again whenever a switch is
     * flipped (the screen rebuilds its widgets to relabel the buttons), and re-reading there would also re-read
     * the master switch from disk, which is fine - but re-reading the owners would drop a switch the player just
     * changed for a mod whose notice was registered in the meantime.
     */
    private void collect() {
        Map<String, Boolean> stored = NoticeSettings.snapshot();
        Map<String, Boolean> declared = new LinkedHashMap<>();
        for (NoticeEntry entry : NoticeRegistry.codeNotices()) {
            // A mod may register several notices; the row is per mod, so its default is on when any of its
            // notices asks to be on.
            declared.merge(entry.owner(), entry.enabledByDefault(), (first, second) -> first || second);
        }
        if (declared.containsKey(Constants.MOD_ID)) {
            this.owners.add(Constants.MOD_ID);
        }
        List<String> business = new ArrayList<>(declared.keySet());
        business.remove(Constants.MOD_ID);
        Collections.sort(business);
        this.owners.addAll(business);
        for (String owner : this.owners) {
            boolean fallback = declared.getOrDefault(owner, true);
            this.defaults.put(owner, fallback);
            this.values.put(owner, stored.getOrDefault(owner, fallback));
        }
    }

    @Override
    protected int contentWidth() {
        List<Component> labels = new ArrayList<>();
        labels.add(Component.translatable("gui.merlinlib.notices.master"));
        for (String owner : this.owners) {
            labels.add(label(owner));
        }
        labels.add(Component.translatable("gui.merlinlib.notices.empty"));
        return Math.max(MIN_CONTENT_WIDTH,
                VanillaUi.measureWidth(this.font, labels, TOGGLE_WIDTH + VanillaUi.GAP, MIN_CONTENT_WIDTH));
    }

    @Override
    protected int contentHeight() {
        plan();
        int note = this.noteShown ? NOTE_LINE : 0;
        return TITLE_LINE + HEADER_GAP + (1 + this.shown.size()) * VanillaUi.ROW_STEP + note + hintHeight()
                + VanillaUi.GAP + VanillaUi.WIDGET_HEIGHT;
    }

    /**
     * Decides what fits, once.
     *
     * <p>The row count depends on the height available and the height depends on the row count, so the fixed part
     * of the panel - title, note, hint and the close button - is measured first and the remaining space is divided
     * by the row step. A list too long for the window loses its tail rather than running off the bottom, and says
     * so.
     */
    private void plan() {
        if (this.planned) {
            return;
        }
        this.planned = true;
        this.hintLines = wrapHint();
        boolean note = this.owners.size() <= 1;
        int capacity = Math.max(1, (contentHeightLimit() - fixedHeight(note)) / VanillaUi.ROW_STEP);
        int fit = Math.max(0, capacity - 1);
        boolean cut = fit < this.owners.size();
        if (cut && !note) {
            // The note needs a line of its own, so the room for rows shrinks by one.
            note = true;
            capacity = Math.max(1, (contentHeightLimit() - fixedHeight(true)) / VanillaUi.ROW_STEP);
            fit = Math.max(0, capacity - 1);
            cut = fit < this.owners.size();
        }
        this.truncated = cut;
        this.noteShown = note || cut;
        this.shown.clear();
        this.shown.addAll(this.owners.subList(0, Math.min(fit, this.owners.size())));
    }

    /** @return the height of everything that is not a switch row */
    private int fixedHeight(boolean note) {
        return TITLE_LINE + HEADER_GAP + (note ? NOTE_LINE : 0) + hintHeight() + VanillaUi.GAP
                + VanillaUi.WIDGET_HEIGHT;
    }

    /** @return the height of the wrapped hint plus the gap above it */
    private int hintHeight() {
        return this.hintLines.size() * HINT_LINE + VanillaUi.GAP;
    }

    private List<String> wrapHint() {
        int width = Math.max(120, panelWidth() - 2 * VanillaUi.PADDING);
        List<String> lines = VanillaUi.wrap(this.font,
                Component.translatable("gui.merlinlib.notices.hint").getString(), width);
        return lines.size() > MAX_HINT_LINES ? lines.subList(0, MAX_HINT_LINES) : lines;
    }

    @Override
    protected void build(PanelLayout layout) {
        plan();
        this.rows.clear();
        layout.gap(TITLE_LINE + HEADER_GAP);

        int y = layout.row(VanillaUi.ROW_STEP);
        this.rows.add(new Row(Component.translatable("gui.merlinlib.notices.master"), y));
        this.addRenderableWidget(toggle(this.masterOn, layout, y, button -> {
            this.masterOn = !this.masterOn;
            ConfigManager.saveServer(Map.of("notices.enabled", Boolean.toString(this.masterOn)));
            this.rebuildWidgets();
        }));

        for (String owner : this.shown) {
            y = layout.row(VanillaUi.ROW_STEP);
            this.rows.add(new Row(label(owner), y));
            boolean current = this.values.getOrDefault(owner, this.defaults.getOrDefault(owner, true));
            this.addRenderableWidget(toggle(current, layout, y, button -> {
                boolean next = !this.values.getOrDefault(owner, this.defaults.getOrDefault(owner, true));
                NoticeSettings.set(owner, next);
                this.values.put(owner, next);
                this.rebuildWidgets();
            }));
        }

        if (this.noteShown) {
            this.noteY = layout.row(NOTE_LINE);
        }
        if (!this.hintLines.isEmpty()) {
            // The gap above the hint is part of hintHeight(), so the reserved height and the drawn height match.
            layout.gap(VanillaUi.GAP);
            this.hintY = layout.row(this.hintLines.size() * HINT_LINE);
        }
        layout.gap(VanillaUi.GAP);
        int buttonY = layout.row(VanillaUi.WIDGET_HEIGHT);
        int width = Math.min(160, layout.contentWidth());
        int x = layout.left() + (layout.contentWidth() - width) / 2;
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.done"),
                button -> this.onClose(), x, buttonY, width));
    }

    /**
     * Builds the on/off button of one row.
     *
     * @param on      the value the button shows
     * @param layout  the panel, for the right edge
     * @param y       the top edge of the row
     * @param onPress what a press does
     * @return the button
     */
    private Button toggle(boolean on, PanelLayout layout, int y, Button.OnPress onPress) {
        Component text = Component.translatable(on ? "gui.merlinlib.toggle.on" : "gui.merlinlib.toggle.off");
        return VanillaUi.button(text, onPress, layout.right() - TOGGLE_WIDTH, y, TOGGLE_WIDTH);
    }

    /** @param owner a mod id @return the text shown for it, which is the mod id except for MerlinLib itself */
    private Component label(String owner) {
        return Constants.MOD_ID.equals(owner)
                ? Component.literal("MerlinLib")
                : Component.literal(owner);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        PanelLayout layout = layout();
        int left = layout.left();
        int content = layout.contentWidth();

        VanillaUi.text(graphics, this.font, this.title, left, layout.top(), VanillaUi.TEXT, content);
        VanillaUi.separator(graphics, left, layout.right(), layout.top() + TITLE_LINE);

        for (Row row : this.rows) {
            VanillaUi.text(graphics, this.font, row.label(), left, row.y() + 6,
                    VanillaUi.TEXT, content - TOGGLE_WIDTH - VanillaUi.GAP);
        }

        if (this.noteY >= 0) {
            Component note = this.truncated
                    ? Component.translatable("gui.merlinlib.notices.more", this.owners.size() - this.shown.size())
                    : Component.translatable("gui.merlinlib.notices.empty");
            VanillaUi.text(graphics, this.font, note, left, this.noteY, VanillaUi.TEXT_DISABLED, content);
        }
        if (this.hintY >= 0) {
            int y = this.hintY;
            for (String line : this.hintLines) {
                VanillaUi.text(graphics, this.font, Component.literal(line), left, y,
                        VanillaUi.TEXT_HINT, content);
                y += HINT_LINE;
            }
        }
    }
}
