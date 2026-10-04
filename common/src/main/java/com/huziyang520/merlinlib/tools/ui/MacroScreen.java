package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.tools.hotkey.HotkeyRegistry;
import com.huziyang520.merlinlib.tools.hotkey.KeyCombo;
import com.huziyang520.merlinlib.tools.macro.MacroStorage;
import com.huziyang520.merlinlib.tools.ui.vanilla.PanelLayout;
import com.huziyang520.merlinlib.tools.ui.vanilla.ScrollArea;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaScreen;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The command macro screen.
 *
 * <p>A macro is a name, a key combination and a list of commands. The commands are edited in a vanilla
 * multi line text area - one command per line - and the combination is captured by pressing keys.
 *
 * <h2>Capturing a combination</h2>
 *
 * <p>Clicking the key button starts a <em>fresh</em> capture (the old binding is dropped, so a new binding
 * cannot silently inherit the previous keys), and the capture ends on Escape or on a click anywhere else.
 * While it is running, keys - Enter included, since it is a key like any other - are added to the
 * combination; pressing a key that is already in it removes it, so a mistake is corrected by pressing the
 * wrong key again. Clicking the key button itself binds a mouse button the same way. The current combination
 * is shown on the button the whole time.
 *
 * <p>Ending on a click is not decoration: a capture that never ends swallows every keystroke, which makes the
 * name and command fields impossible to type into and quietly binds every letter the player types.
 */
public class MacroScreen extends VanillaScreen {

    /** Macro rows visible at once on a window tall enough. */
    private static final int MAX_ROWS = 6;
    /** Fewest macro rows a window too short for {@link #MAX_ROWS} still shows. */
    private static final int MIN_ROWS = 1;
    /** Height of one macro row. */
    private static final int ROW_HEIGHT = VanillaUi.LIST_ROW_HEIGHT;
    /** Width of the per row delete button. */
    private static final int DELETE_WIDTH = VanillaUi.STEP_BUTTON;
    /** Narrowest width of the macro list column. */
    private static final int MIN_LIST_WIDTH = 150;
    /** Narrowest width of the editor column. */
    private static final int MIN_EDITOR_WIDTH = 230;
    /** Height of the multi line command area. */
    private static final int COMMANDS_HEIGHT = 90;
    /** Width of the key capture button. */
    private static final int KEY_WIDTH = 170;
    /** Height of a label plus the control under it. */
    private static final int LABEL = 12;

    private final Screen parent;
    private final List<MacroStorage.Macro> macros = new ArrayList<>(MacroStorage.macros());

    private int selected = -1;
    private KeyCombo pending = KeyCombo.of();
    /** The combination a capture started from, restored when it is cancelled. */
    private KeyCombo beforeCapture = KeyCombo.of();
    private boolean capturing;

    private EditBox nameBox;
    private MultiLineEditBox commandsBox;
    private Button keyButton;
    private ScrollArea list;
    private final List<Button> deleteButtons = new ArrayList<>();

    private int listLabelY;
    private int nameLabelY;
    private int commandsLabelY;
    private int keyLabelY;
    private int hintY;
    private int hovered = -1;

    /**
     * @param parent the screen to return to, or {@code null} to resume the game
     */
    public MacroScreen(Screen parent) {
        super(Component.translatable("gui.merlinlib.macro.title"));
        this.parent = parent;
    }

    @Override
    protected Screen parentScreen() {
        return this.parent;
    }

    @Override
    protected int contentWidth() {
        int widestName = 0;
        for (MacroStorage.Macro macro : this.macros) {
            widestName = Math.max(widestName, this.font.width(macro.name()));
        }
        int listWidth = Math.max(MIN_LIST_WIDTH, Math.min(widestName + 30, 220));
        int editorWidth = Math.max(MIN_EDITOR_WIDTH, Math.max(
                this.font.width(Component.translatable("gui.merlinlib.macro.commands_hint")),
                KEY_WIDTH + this.font.width(Component.translatable("gui.merlinlib.macro.key")) + 40));
        return listWidth + VanillaUi.GAP * 2 + editorWidth;
    }

    @Override
    protected int contentHeight() {
        int listColumn = LABEL + visibleRows() * ROW_HEIGHT + 6 + VanillaUi.WIDGET_HEIGHT;
        int editorColumn = (LABEL + VanillaUi.WIDGET_HEIGHT + 6) * 2 + LABEL + COMMANDS_HEIGHT;
        return Math.min(12 + 4 + Math.max(listColumn, editorColumn) + 6 + 10 + 6 + VanillaUi.WIDGET_HEIGHT,
                contentHeightLimit());
    }

    /**
     * How many macro rows fit at the current window height.
     *
     * <p>The editor column has a fixed height - two labelled fields, the command area and the key button - so
     * it is subtracted first; what is left belongs to the list. A high GUI scale therefore shortens the list
     * instead of running off the bottom of the window.
     *
     * @return the number of rows to show, between {@link #MIN_ROWS} and {@link #MAX_ROWS}
     */
    private int visibleRows() {
        int editorColumn = (LABEL + VanillaUi.WIDGET_HEIGHT + 6) * 2 + LABEL + COMMANDS_HEIGHT;
        int other = 12 + 4 + 6 + 10 + 6 + VanillaUi.WIDGET_HEIGHT
                + LABEL + 6 + VanillaUi.WIDGET_HEIGHT;
        int forList = contentHeightLimit() - Math.max(editorColumn, other);
        return VanillaUi.clamp(forList / ROW_HEIGHT, MIN_ROWS, MAX_ROWS);
    }

    @Override
    protected void build(PanelLayout layout) {
        int left = layout.left();
        int listWidth = listWidth(layout);
        int editorX = left + listWidth + VanillaUi.GAP * 2;
        int editorWidth = layout.right() - editorX;

        // The bottom of the panel is laid out first, from the panel's own bottom edge. The hint and the two
        // buttons belong there rather than to wherever a cursor walking down the rows happens to end up: with a
        // tall editor column the cursor ran past the bottom of the panel, and the hint and the buttons then
        // overlapped each other outside it. The hint's height is measured because it wraps.
        int bottom = layout.contentBottom() - VanillaUi.WIDGET_HEIGHT;
        int hintLines = VanillaUi.wrap(this.font,
                Component.translatable("gui.merlinlib.macro.hint").getString(),
                layout.contentWidth()).size();
        this.hintY = bottom - 6 - hintLines * 10;

        layout.gap(12 + 4);
        int bodyTop = layout.cursor();

        // ---------------------------------------------------------- left: the macro list
        this.listLabelY = bodyTop;
        this.list = new ScrollArea(left, bodyTop + LABEL, listWidth, visibleRows() * ROW_HEIGHT, ROW_HEIGHT);
        this.list.setContentHeight(this.macros.size() * ROW_HEIGHT);

        int listButtonsY = this.list.y() + this.list.height() + 6;
        int half = (listWidth - VanillaUi.GAP) / 2;
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.macro.new"),
                button -> this.createMacro(), left, listButtonsY, half));
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.macro.delete_all"),
                button -> this.deleteAll(), left + half + VanillaUi.GAP, listButtonsY,
                listWidth - half - VanillaUi.GAP));

        // ---------------------------------------------------------- right: the editor
        this.nameLabelY = bodyTop;
        int nameY = bodyTop + LABEL;
        this.nameBox = VanillaUi.field(this.font, editorX, nameY, editorWidth,
                Component.translatable("gui.merlinlib.macro.name"),
                this.selected >= 0 ? this.macros.get(this.selected).name() : "", 48);
        this.addRenderableWidget(this.nameBox);

        this.commandsLabelY = nameY + VanillaUi.WIDGET_HEIGHT + 6;
        int commandsY = this.commandsLabelY + LABEL;
        // The command area grows only as far as the row above the hint. A fixed height could not be clamped, so
        // in a small window it reached down into the hint and the buttons - which is what put the character
        // counter of the box on top of them.
        int commandsHeight = Math.max(30, Math.min(COMMANDS_HEIGHT,
                this.hintY - 6 - LABEL - 6 - VanillaUi.WIDGET_HEIGHT - commandsY));
        this.commandsBox = MultiLineEditBox.builder()
                .setX(editorX)
                .setY(commandsY)
                .setShowBackground(true)
                .setShowDecorations(true)
                .setPlaceholder(Component.translatable("gui.merlinlib.macro.commands_hint"))
                .build(this.font, editorWidth, commandsHeight,
                        Component.translatable("gui.merlinlib.macro.commands"));
        this.commandsBox.setCharacterLimit(4096);
        this.commandsBox.setLineLimit(64);
        this.addRenderableWidget(this.commandsBox);

        this.keyLabelY = commandsY + commandsHeight + 6;
        this.keyButton = this.addRenderableWidget(VanillaUi.button(keyLabel(), button -> this.beginCapture(),
                editorX, this.keyLabelY + LABEL, KEY_WIDTH));

        layout.cursorTo(bottom + VanillaUi.WIDGET_HEIGHT);
        int bottomHalf = layout.sliceWidth(2);
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.macro.apply"),
                button -> this.commit(), layout.sliceX(0, 2), bottom, bottomHalf));
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.done"),
                button -> this.finish(), layout.sliceX(1, 2), bottom, bottomHalf));

        this.deleteButtons.clear();
        for (int slot = 0; slot < visibleRows(); slot++) {
            final int index = slot;
            this.deleteButtons.add(this.addRenderableWidget(VanillaUi.compact(Component.literal("X"),
                    button -> this.deleteMacro(index),
                    // rowWidth, not the whole area: the last 6 px of the list are the vanilla scrollbar, and a
                    // button placed over it both covered the bar and could not be dragged past.
                    left + this.list.rowWidth() - DELETE_WIDTH - 4, 0)));
        }

        if (this.selected < 0 && !this.macros.isEmpty()) {
            this.selected = 0;
        }
        loadSelection();
        layoutRows();
    }

    private int listWidth(PanelLayout layout) {
        int widestName = 0;
        for (MacroStorage.Macro macro : this.macros) {
            widestName = Math.max(widestName, this.font.width(macro.name()));
        }
        int wanted = Math.max(MIN_LIST_WIDTH, Math.min(widestName + 30, 220));
        int maximum = layout.contentWidth() - VanillaUi.GAP * 2 - MIN_EDITOR_WIDTH;
        return Math.max(MIN_LIST_WIDTH, Math.min(wanted, Math.max(MIN_LIST_WIDTH, maximum)));
    }

    /** Moves the per row delete buttons and hides the rows beyond the viewport. */
    private void layoutRows() {
        if (this.list == null) {
            return;
        }
        int firstRow = this.list.firstRow();
        for (int slot = 0; slot < visibleRows(); slot++) {
            boolean visible = firstRow + slot < this.macros.size();
            Button button = this.deleteButtons.get(slot);
            button.setY(this.list.y() + slot * ROW_HEIGHT + 1);
            button.visible = visible;
            button.active = visible;
        }
    }

    // ---------------------------------------------------------------- data

    private void loadSelection() {
        if (this.selected < 0 || this.selected >= this.macros.size()) {
            this.pending = KeyCombo.of();
            if (this.nameBox != null) {
                this.nameBox.setValue("");
            }
            if (this.commandsBox != null) {
                this.commandsBox.setValue("");
            }
        } else {
            MacroStorage.Macro macro = this.macros.get(this.selected);
            this.pending = macro.combo();
            if (this.nameBox != null) {
                this.nameBox.setValue(macro.name());
            }
            if (this.commandsBox != null) {
                this.commandsBox.setValue(String.join("\n", macro.commands()));
            }
        }
        this.capturing = false;
        refreshKeyButton();
    }

    private void refreshKeyButton() {
        if (this.keyButton != null) {
            this.keyButton.setMessage(keyLabel());
        }
    }

    /** Copies the editor into the selected macro, so switching rows or saving never loses an edit. */
    private void commit() {
        if (this.selected < 0 || this.selected >= this.macros.size()) {
            return;
        }
        List<String> commands = new ArrayList<>();
        if (this.commandsBox != null) {
            for (String line : this.commandsBox.getValue().split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    commands.add(trimmed);
                }
            }
        }
        String name = this.nameBox == null || this.nameBox.getValue().trim().isEmpty()
                ? Component.translatable("gui.merlinlib.macro.untitled").getString()
                : this.nameBox.getValue().trim();
        this.macros.set(this.selected, new MacroStorage.Macro(name, this.pending.keyList(), commands));
        persist();
    }

    private void persist() {
        MacroStorage.replaceAll(this.macros);
        MacroStorage.save();
        if (this.list != null) {
            this.list.setContentHeight(this.macros.size() * ROW_HEIGHT);
        }
        layoutRows();
    }

    private void finish() {
        commit();
        this.onClose();
    }

    private void createMacro() {
        commit();
        this.macros.add(new MacroStorage.Macro(
                Component.translatable("gui.merlinlib.macro.new_name", this.macros.size() + 1).getString(),
                List.of(), List.of()));
        this.selected = this.macros.size() - 1;
        persist();
        loadSelection();
    }

    private void deleteMacro(int slot) {
        int index = this.list.firstRow() + slot;
        if (index < 0 || index >= this.macros.size()) {
            return;
        }
        this.macros.remove(index);
        if (this.selected >= this.macros.size()) {
            this.selected = this.macros.size() - 1;
        }
        persist();
        loadSelection();
    }

    private void deleteAll() {
        this.macros.clear();
        this.selected = -1;
        persist();
        loadSelection();
    }

    /**
     * Starts a fresh capture.
     *
     * <p>Fresh, not additive: the point of clicking the button is to set a new combination, and keeping the
     * previous keys is how the previous version ended up with a combination of eight keys nobody intended.
     */
    private void beginCapture() {
        if (this.selected < 0) {
            return;
        }
        // Remembered so Escape can put it back: starting a capture drops the old binding, and "cancel" that
        // only stops the capture would leave the binding destroyed - which is what it used to do.
        this.beforeCapture = this.pending;
        this.pending = KeyCombo.of();
        this.capturing = true;
        refreshKeyButton();
    }

    /** Ends the capture, keeping what was pressed: this is the "click elsewhere to finish" path. */
    private void endCapture() {
        this.capturing = false;
        refreshKeyButton();
    }

    /** Ends the capture and restores the combination it started from. */
    private void cancelCapture() {
        this.pending = this.beforeCapture;
        this.capturing = false;
        refreshKeyButton();
    }

    private Component keyLabel() {
        if (this.capturing) {
            return Component.translatable("gui.merlinlib.macro.capturing",
                    HotkeyRegistry.comboName(this.pending));
        }
        if (this.pending.isEmpty()) {
            return Component.translatable("gui.merlinlib.macro.press_key");
        }
        return Component.literal(HotkeyRegistry.comboName(this.pending));
    }

    private int rowAt(double mouseX, double mouseY) {
        if (this.list == null || mouseX < this.list.x() || mouseX >= this.list.x() + this.list.width()) {
            return -1;
        }
        int index = this.list.firstRow() + ((int) mouseY - this.list.y()) / ROW_HEIGHT;
        boolean inside = mouseY >= this.list.y() && mouseY < this.list.y() + this.list.height();
        return inside && index >= 0 && index < this.macros.size() ? index : -1;
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.capturing) {
            // Escape is the way out; Enter is an ordinary key here, so a combination may contain it. The
            // capture ends on a click elsewhere (see mouseClicked), which is what the button says.
            if (event.key() == MerlinUi.KEY_ESCAPE) {
                cancelCapture();
                return true;
            }
            // Toggling is what makes a combination correctable: press a key to add it, press it again to drop
            // it. The key is released right away, so closing the screen does not immediately run the macro.
            this.pending = this.pending.toggle(event.key());
            HotkeyRegistry.release(event.key());
            refreshKeyButton();
            return true;
        }
        return super.keyPressed(event);
    }

    /**
     * While a combination is being captured, Escape belongs to the capture and not to the screen.
     *
     * <p>This is the fix for a real complaint. The button promises "Esc 取消", but Escape reaches the engine's
     * own "close this screen" path, so the capture never saw the key: pressing Escape closed the whole macro
     * screen with the half captured combination still in it, and the player read that as "Escape did not
     * cancel". Both doors are shut here - the key handler cancels the capture, and this makes the screen refuse
     * to close on Escape while that capture is running.
     *
     * @return whether Escape may close this screen
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return !this.capturing;
    }

    @Override
    public void onClose() {
        if (this.capturing) {
            // The second door: a close that arrived anyway - from the engine, or from another mod's key
            // handling - cancels the capture instead of throwing the screen away.
            cancelCapture();
            return;
        }
        super.onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // While capturing, clicking the key button binds the mouse button that was clicked; a click anywhere
        // else ends the capture, so the text fields become usable again. The end click is consumed so it
        // cannot hit a control the player was not aiming at.
        if (this.capturing) {
            if (this.keyButton != null && this.keyButton.isMouseOver(event.x(), event.y())) {
                this.pending = this.pending.toggle(event.button());
                HotkeyRegistry.release(event.button());
                refreshKeyButton();
                return true;
            }
            endCapture();
            return true;
        }
        // The per row delete buttons live inside the list column, so they have to be offered the click before
        // the list turns it into a row selection: the row branch used to swallow it, so X did nothing.
        if (overDeleteButton(event.x(), event.y())) {
            return super.mouseClicked(event, doubleClick);
        }
        int row = rowAt(event.x(), event.y());
        if (row >= 0) {
            commit();
            this.selected = row;
            loadSelection();
            return true;
        }
        if (this.list != null && this.list.mouseClicked(event)) {
            this.hovered = -1;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /**
     * Whether a point is over one of the per row delete buttons.
     *
     * @param mouseX the cursor x
     * @param mouseY the cursor y
     * @return {@code true} when a visible delete button is under the cursor
     */
    private boolean overDeleteButton(double mouseX, double mouseY) {
        for (Button button : this.deleteButtons) {
            if (button.visible && button.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.list != null && this.list.mouseDragged(event)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.list != null && this.list.mouseReleased(event)) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.list != null && this.list.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.hovered = rowAt(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    // ---------------------------------------------------------------- drawing

    /** The list is recessed: this has to happen before the widgets, or it covers the delete buttons. */
    @Override
    protected void drawContentBackground(GuiGraphicsExtractor graphics) {
        if (this.list == null) {
            return;
        }
        VanillaUi.listArea(graphics, this.list.x(), this.list.y(), this.list.width(), this.list.height());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        PanelLayout layout = layout();
        int left = layout.left();
        int editorX = this.list.x() + this.list.width() + VanillaUi.GAP * 2;
        int editorWidth = layout.right() - editorX;

        VanillaUi.text(graphics, this.font, this.title, left, layout.top(), VanillaUi.TEXT, layout.contentWidth());
        VanillaUi.separator(graphics, left, layout.right(), layout.top() + 12);
        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.macro.list"),
                left, this.listLabelY, VanillaUi.TEXT_HINT, this.list.width());
        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.macro.name"),
                editorX, this.nameLabelY, VanillaUi.TEXT_HINT, editorWidth);
        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.macro.commands"),
                editorX, this.commandsLabelY, VanillaUi.TEXT_HINT, editorWidth);
        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.macro.key"),
                editorX, this.keyLabelY, VanillaUi.TEXT_HINT, editorWidth);

        if (this.macros.isEmpty()) {
            VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.macro.empty"),
                    this.list.x() + 4, this.list.y() + 6, VanillaUi.TEXT_HINT, this.list.width() - 8);
        }

        Set<Set<Integer>> clashes = duplicatedCombos();
        int firstRow = this.list.firstRow();
        int nameWidth = this.list.rowWidth() - DELETE_WIDTH - 6;
        for (int slot = 0; slot < visibleRows(); slot++) {
            int index = firstRow + slot;
            if (index >= this.macros.size()) {
                break;
            }
            MacroStorage.Macro macro = this.macros.get(index);
            int rowY = this.list.y() + slot * ROW_HEIGHT;
            if (index == this.selected) {
                VanillaUi.rowOutline(graphics, this.list.x(), rowY, this.list.width(), ROW_HEIGHT, true);
            } else if (index == this.hovered) {
                VanillaUi.rowOutline(graphics, this.list.x(), rowY, this.list.width(), ROW_HEIGHT, false);
            }
            boolean conflict = clashes.contains(Set.copyOf(macro.keys()));
            // The name and the combination it is bound to, so what a macro does is visible without clicking it.
            String text = macro.name() + "  " + HotkeyRegistry.comboName(macro.combo());
            VanillaUi.text(graphics, this.font, Component.literal(text), this.list.x() + 3, rowY + 6,
                    conflict ? VanillaUi.TEXT_ERROR : VanillaUi.TEXT, nameWidth);
        }

        this.list.render(graphics);

        // Wrapped, never clipped: this line explains the whole screen, and the room for it is measured when the
        // panel is laid out.
        int hintLine = 0;
        for (String line : VanillaUi.wrap(this.font, bottomLine().getString(), layout.contentWidth())) {
            VanillaUi.text(graphics, this.font, Component.literal(line), left, this.hintY + hintLine * 10,
                    VanillaUi.TEXT_HINT, layout.contentWidth());
            hintLine++;
        }
    }

    /**
     * The combinations that more than one macro uses, down to the exact key set.
     *
     * <p>Only an identical combination is a clash. Two macros that merely share a key - both with left control
     * held, say - are different bindings and can both be used, so marking them was wrong: the shared key is
     * the whole point of a combination. The clash is a hint on the row (the name turns red) and nothing more;
     * it never blocks a macro from being saved or fired.
     *
     * @return the key sets used by at least two macros
     */
    private Set<Set<Integer>> duplicatedCombos() {
        java.util.Map<Set<Integer>, Integer> counts = new java.util.HashMap<>();
        for (MacroStorage.Macro macro : this.macros) {
            counts.merge(Set.copyOf(macro.keys()), 1, Integer::sum);
        }
        Set<Set<Integer>> duplicated = new java.util.HashSet<>();
        counts.forEach((combo, count) -> {
            if (count > 1) {
                duplicated.add(combo);
            }
        });
        return duplicated;
    }

    /** @return the line under the list */
    private Component bottomLine() {
        return Component.translatable("gui.merlinlib.macro.hint");
    }
}
