package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.ConfigScope;
import com.huziyang520.merlinlib.config.NoticeSettings;
import com.huziyang520.merlinlib.notice.NoticeEntry;
import com.huziyang520.merlinlib.notice.NoticeRegistry;
import com.huziyang520.merlinlib.tools.ui.vanilla.PanelLayout;
import com.huziyang520.merlinlib.tools.ui.vanilla.ScrollArea;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaScreen;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The join notice page: one master switch, then one switch per business mod.
 *
 * <h2>What it replaces</h2>
 *
 * <p>The per mod switches used to be a text box on the configuration screen, holding a string written as
 * {@code modid=true;other=false}. That was the only shape the loader's file format could hold for keys that
 * exist only at runtime, but it meant a server owner had to know every mod id by heart and type a string with
 * no way to see a mistake. This page lists the mods that actually registered a notice and gives each of them a
 * button.
 *
 * <h2>Where the mods come from</h2>
 *
 * <p>From {@link NoticeRegistry#codeNotices()}, keyed by {@link NoticeEntry#owner()} - the namespace of the
 * notice id, which is the mod that registered it. Nothing is stored here beyond the choices: a mod that is
 * uninstalled simply stops appearing, and its line in {@code notices.toml} is harmless.
 *
 * <p>MerlinLib's own notices are listed first, because the library is the one owner a player always has; every
 * other mod follows in alphabetical order so the page does not reshuffle itself between sessions. When no mod
 * besides MerlinLib registers a notice, the list says so instead of showing an empty box.
 *
 * <h2>Saving</h2>
 *
 * <p>The buttons only change what the page is about to write. Saving writes {@code notices.enabled} through
 * {@link ConfigManager} and the per mod switches through {@link NoticeSettings}, which rewrites
 * {@code config/MerlinLib/notices.toml} as a whole. Nothing is read at startup by the notice sender, so a saved
 * page applies to the next player who joins.
 */
public class NoticesScreen extends VanillaScreen {

    /** Narrowest content that still lays out sensibly. */
    private static final int MIN_CONTENT_WIDTH = 260;
    /** Width of a per mod on/off button. */
    private static final int TOGGLE_WIDTH = 56;
    /** Height of one list row. */
    private static final int ROW_HEIGHT = VanillaUi.LIST_ROW_HEIGHT;
    /** Distance between two list rows: the row plus a gap, so the buttons never touch. */
    private static final int ROW_STEP = ROW_HEIGHT + 4;
    /** Rows shown at most on a tall window, so a server with many mods still gets a centred panel. */
    private static final int MAX_ROWS = 8;
    /** Height of the title line and the gap under the separator. */
    private static final int HEADER = 12 + 4;
    /** Height of the master switch row plus the gap under it. */
    private static final int MASTER_ROW = VanillaUi.WIDGET_HEIGHT + 8;
    /** Height of the label above the mod list. */
    private static final int MODS_LABEL = 12;

    private final Screen parent;

    /** The mods that registered a notice, MerlinLib first and the rest alphabetically. */
    private final List<String> owners = new ArrayList<>();

    /** The choices this page is showing, applied on save. */
    private final Map<String, Boolean> pending = new LinkedHashMap<>();

    /** The master switch as this page is showing it. */
    private boolean master;

    private ScrollArea list;
    private Button masterButton;
    private final List<Button> rowButtons = new ArrayList<>();

    private int masterY;
    private int modsLabelY;
    private int hintY;

    /**
     * @param parent the screen to return to, or {@code null} to resume the game
     */
    public NoticesScreen(Screen parent) {
        super(Component.translatable("gui.merlinlib.notices.title"));
        this.parent = parent;

        Map<String, Boolean> defaults = new LinkedHashMap<>();
        Set<String> seen = new LinkedHashSet<>();
        for (NoticeEntry entry : NoticeRegistry.codeNotices()) {
            seen.add(entry.owner());
            // A mod may register several notices with different defaults; the page speaks for the mod, so
            // "any of its notices wants to be shown" is the default it starts from.
            defaults.merge(entry.owner(), entry.enabledByDefault(), (left, right) -> left || right);
        }
        this.owners.addAll(seen);
        this.owners.sort((left, right) -> {
            boolean leftLibrary = Constants.MOD_ID.equals(left);
            boolean rightLibrary = Constants.MOD_ID.equals(right);
            if (leftLibrary != rightLibrary) {
                return leftLibrary ? -1 : 1;
            }
            return left.compareTo(right);
        });
        for (String owner : this.owners) {
            this.pending.put(owner, NoticeSettings.isEnabled(owner, defaults.getOrDefault(owner, true)));
        }
        this.master = ConfigManager.server().noticesEnabled();
    }

    @Override
    protected Screen parentScreen() {
        return this.parent;
    }

    // ---------------------------------------------------------------- layout

    /**
     * @return whether at least one business mod registered a notice
     */
    private boolean hasBusinessMod() {
        for (String owner : this.owners) {
            if (!Constants.MOD_ID.equals(owner)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the rows the list has to be able to show: one per mod, plus the "no business mods" line
     */
    private int rowsNeeded() {
        return this.owners.size() + (hasBusinessMod() ? 0 : 1);
    }

    /**
     * @return how many rows fit in the window, at least one
     */
    private int visibleRows() {
        int fit = Math.max(1, (contentHeightLimit() - fixedHeight()) / ROW_STEP);
        return Math.min(Math.min(fit, MAX_ROWS), Math.max(1, rowsNeeded()));
    }

    /**
     * @return the height of everything except the list
     */
    private int fixedHeight() {
        return HEADER + MASTER_ROW + MODS_LABEL + 6 + hintLines() * 10 + 6 + VanillaUi.WIDGET_HEIGHT;
    }

    /**
     * @return how many lines the hint takes at the panel width, which decides the panel height
     */
    private int hintLines() {
        int width = Math.min(contentWidth(), VanillaUi.MAX_PANEL_WIDTH - 2 * VanillaUi.PADDING) - 16;
        return VanillaUi.wrap(this.font, hint().getString(), width).size();
    }

    @Override
    protected int contentWidth() {
        int widest = this.font.width(this.title);
        for (String owner : this.owners) {
            widest = Math.max(widest, this.font.width(owner));
        }
        return Math.max(MIN_CONTENT_WIDTH,
                widest + VanillaUi.GAP + TOGGLE_WIDTH + VanillaUi.PADDING);
    }

    @Override
    protected int contentHeight() {
        return fixedHeight() + visibleRows() * ROW_STEP;
    }

    @Override
    protected void build(PanelLayout layout) {
        int left = layout.left();
        int content = layout.contentWidth();

        layout.gap(HEADER);

        this.masterY = layout.row(VanillaUi.WIDGET_HEIGHT);
        this.masterButton = this.addRenderableWidget(VanillaUi.button(masterLabel(),
                button -> {
                    this.master = !this.master;
                    refreshMaster();
                }, left, this.masterY, content));
        layout.gap(8);

        this.modsLabelY = layout.cursor();
        layout.gap(MODS_LABEL);

        int listTop = layout.cursor();
        int rows = visibleRows();
        this.list = new ScrollArea(left, listTop, content, rows * ROW_STEP, ROW_STEP);
        this.list.setContentHeight(rowsNeeded() * ROW_STEP);
        layout.gap(rows * ROW_STEP + 6);

        this.hintY = layout.cursor();
        layout.gap(hintLines() * 10 + 6);

        int footer = layout.row(VanillaUi.WIDGET_HEIGHT);
        int half = layout.sliceWidth(2);
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.notices.save"),
                button -> this.save(), layout.sliceX(0, 2), footer, half));
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.cancel"),
                button -> this.onClose(), layout.sliceX(1, 2), footer, half));

        this.rowButtons.clear();
        for (int slot = 0; slot < rows; slot++) {
            final int index = slot;
            this.rowButtons.add(this.addRenderableWidget(VanillaUi.button(Component.empty(),
                    button -> toggle(index),
                    // rowWidth, not the whole area: the last 6 px of the list are the vanilla scrollbar, and a
                    // button placed over it both covers the bar and cannot be clicked past.
                    left + this.list.rowWidth() - TOGGLE_WIDTH, listTop, TOGGLE_WIDTH)));
        }
        layoutRows();
    }

    /**
     * Moves the per mod buttons to the rows they belong to and hides the ones beyond the list.
     *
     * <p>The buttons are real widgets rather than painted text, so clicking, hovering and Tab focus all work;
     * that also means their position has to follow the scroll offset rather than being drawn at it.
     */
    private void layoutRows() {
        if (this.list == null) {
            return;
        }
        int first = this.list.firstRow();
        for (int slot = 0; slot < this.rowButtons.size(); slot++) {
            int index = first + slot;
            Button button = this.rowButtons.get(slot);
            boolean visible = index < this.owners.size();
            button.setY(this.list.y() + slot * ROW_STEP + (ROW_STEP - VanillaUi.WIDGET_HEIGHT) / 2);
            button.visible = visible;
            button.active = visible;
            if (visible) {
                button.setMessage(stateLabel(this.pending.getOrDefault(this.owners.get(index), true)));
            }
        }
    }

    // ---------------------------------------------------------------- data

    /**
     * Flips one mod's switch on the page. Nothing is written until the page is saved.
     *
     * @param slot the row slot that was clicked
     */
    private void toggle(int slot) {
        if (this.list == null) {
            return;
        }
        int index = this.list.firstRow() + slot;
        if (index < 0 || index >= this.owners.size()) {
            return;
        }
        String owner = this.owners.get(index);
        this.pending.put(owner, !this.pending.getOrDefault(owner, true));
        layoutRows();
    }

    /** Writes both kinds of switch, then closes. */
    private void save() {
        ConfigManager.write(ConfigScope.SERVER,
                Map.of("notices.enabled", Boolean.toString(this.master)));
        for (Map.Entry<String, Boolean> entry : this.pending.entrySet()) {
            NoticeSettings.set(entry.getKey(), entry.getValue());
        }
        NoticeSettings.save();
        this.onClose();
    }

    // ---------------------------------------------------------------- labels

    private Component masterLabel() {
        return Component.translatable("gui.merlinlib.notices.master", stateLabel(this.master));
    }

    private static Component stateLabel(boolean on) {
        return Component.translatable(on ? "gui.merlinlib.notices.on" : "gui.merlinlib.notices.off");
    }

    private static Component hint() {
        return Component.translatable("gui.merlinlib.notices.hint");
    }

    private void refreshMaster() {
        if (this.masterButton != null) {
            this.masterButton.setMessage(masterLabel());
        }
    }

    // ---------------------------------------------------------------- drawing

    @Override
    protected void drawContentBackground(GuiGraphics graphics) {
        // Deliberately empty: the rows are labels and real buttons on the panel itself. A recessed list area
        // would have to be painted here, and this pass runs after the widgets - an opaque fill at that point
        // covers the buttons it is supposed to sit behind.
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PanelLayout layout = layout();
        int left = layout.left();
        int content = layout.contentWidth();

        VanillaUi.text(graphics, this.font, this.title, left, layout.top(), VanillaUi.TEXT, content);
        VanillaUi.separator(graphics, left, layout.right(), layout.top() + 12);
        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.notices.mods"),
                left, this.modsLabelY, VanillaUi.TEXT_HINT, content);

        if (this.list != null) {
            int first = this.list.firstRow();
            int labelWidth = Math.max(20, this.list.rowWidth() - TOGGLE_WIDTH - 10);
            for (int slot = 0; slot < this.rowButtons.size(); slot++) {
                int index = first + slot;
                if (index >= this.owners.size()) {
                    break;
                }
                int rowY = this.list.y() + slot * ROW_STEP;
                VanillaUi.text(graphics, this.font, Component.literal(this.owners.get(index)),
                        this.list.x() + 4, rowY + (ROW_HEIGHT - 8) / 2, VanillaUi.TEXT, labelWidth);
            }
            if (!hasBusinessMod()) {
                int emptyRow = this.owners.size() - first;
                if (emptyRow >= 0 && emptyRow < visibleRows()) {
                    VanillaUi.text(graphics, this.font,
                            Component.translatable("gui.merlinlib.notices.empty"),
                            this.list.x() + 4,
                            this.list.y() + emptyRow * ROW_STEP + (ROW_HEIGHT - 8) / 2,
                            VanillaUi.TEXT_DISABLED, this.list.rowWidth() - 8);
                }
            }
            this.list.render(graphics);
        }

        int line = 0;
        for (String text : VanillaUi.wrap(this.font, hint().getString(), content)) {
            VanillaUi.text(graphics, this.font, Component.literal(text), left, this.hintY + line * 10,
                    VanillaUi.TEXT_HINT, content);
            line++;
        }
    }

    // ---------------------------------------------------------------- input

    /**
     * Forwards a click that no widget wanted to the scrollbar, so the list can be dragged.
     *
     * @param mouseX the cursor x
     * @param mouseY the cursor y
     * @param button the mouse button
     * @return {@code true} when the click was consumed
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.list != null && this.list.mouseClicked(mouseX, mouseY, button)) {
            // The scrollbar moved the rows, so the buttons have to follow it in the same frame.
            layoutRows();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.list != null && this.list.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            layoutRows();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.list != null && this.list.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Scrolls the mod list with the wheel.
     *
     * @param mouseX  the cursor x
     * @param mouseY  the cursor y
     * @param scrollX the horizontal delta, unused
     * @param scrollY the wheel delta
     * @return {@code true} when the scroll was consumed
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.list != null && this.list.mouseScrolled(mouseX, mouseY, scrollY)) {
            layoutRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
