// The command macro screen.

package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.tools.hotkey.HotkeyRegistry;
import com.huziyang520.merlinlib.tools.hotkey.KeyCombo;
import com.huziyang520.merlinlib.tools.macro.MacroStorage;
import com.huziyang520.merlinlib.tools.ui.vanilla.PanelLayout;
import com.huziyang520.merlinlib.tools.ui.vanilla.ScrollArea;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaScreen;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MacroScreen extends VanillaScreen {
    private static final int MAX_ROWS = 6;
    private static final int MIN_ROWS = 1;
    private static final int ROW_HEIGHT = VanillaUi.LIST_ROW_HEIGHT;
    private static final int DELETE_WIDTH = VanillaUi.STEP_BUTTON;
    private static final int MIN_LIST_WIDTH = 150;
    private static final int MIN_EDITOR_WIDTH = 230;
    private static final int COMMANDS_HEIGHT = 90;
    private static final int KEY_WIDTH = 170;
    private static final int LABEL = 12;
    private static final int COMMANDS_CHARACTER_LIMIT = 4096;
    private final Screen parent;
    private final List<MacroStorage.Macro> macros = new ArrayList<>(MacroStorage.macros());
    private int selected = -1;
    private KeyCombo pending = KeyCombo.of();
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
        int bottom = layout.contentBottom() - VanillaUi.WIDGET_HEIGHT;
        int hintLines = VanillaUi.wrap(this.font,
                Component.translatable("gui.merlinlib.macro.hint").getString(),
                layout.contentWidth()).size();
        this.hintY = bottom - 6 - hintLines * 10;
        layout.gap(12 + 4);
        int bodyTop = layout.cursor();
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

        this.nameLabelY = bodyTop;
        int nameY = bodyTop + LABEL;
        this.nameBox = VanillaUi.field(this.font, editorX, nameY, editorWidth,
                Component.translatable("gui.merlinlib.macro.name"),
                this.selected >= 0 ? this.macros.get(this.selected).name() : "", 48);
        this.addRenderableWidget(this.nameBox);
        this.commandsLabelY = nameY + VanillaUi.WIDGET_HEIGHT + 6;
        int commandsY = this.commandsLabelY + LABEL;

        int commandsHeight = Math.max(30, Math.min(COMMANDS_HEIGHT,
                this.hintY - 6 - LABEL - 6 - VanillaUi.WIDGET_HEIGHT - commandsY));

        this.commandsBox = new MultiLineEditBox(this.font, editorX, commandsY, editorWidth, commandsHeight,
                Component.translatable("gui.merlinlib.macro.commands_hint"),
                Component.translatable("gui.merlinlib.macro.commands"));
        this.commandsBox.setCharacterLimit(COMMANDS_CHARACTER_LIMIT);
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

    private void beginCapture() {
        if (this.selected < 0) {
            return;
        }

        this.beforeCapture = this.pending;
        this.pending = KeyCombo.of();
        this.capturing = true;
        refreshKeyButton();
    }

    private void endCapture() {
        this.capturing = false;
        refreshKeyButton();
    }

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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.capturing) {
            if (keyCode == MerlinUi.KEY_ESCAPE) {
                cancelCapture();
                return true;
            }

            this.pending = this.pending.toggle(keyCode);
            HotkeyRegistry.release(keyCode);
            refreshKeyButton();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.capturing;
    }

    @Override
    public void onClose() {
        if (this.capturing) {
            cancelCapture();
            return;
        }
        super.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.capturing) {
            if (this.keyButton != null && this.keyButton.isMouseOver(mouseX, mouseY)) {
                this.pending = this.pending.toggle(button);
                HotkeyRegistry.release(button);
                refreshKeyButton();
                return true;
            }
            endCapture();
            return true;
        }

        if (overDeleteButton(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int row = rowAt(mouseX, mouseY);
        if (row >= 0) {
            commit();
            this.selected = row;
            loadSelection();
            return true;
        }
        if (this.list != null && this.list.mouseClicked(mouseX, mouseY, button)) {
            this.hovered = -1;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean overDeleteButton(double mouseX, double mouseY) {
        for (Button button : this.deleteButtons) {
            if (button.visible && button.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.list != null && this.list.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
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

    @Override
    protected void drawContentBackground(GuiGraphics graphics) {
        if (this.list == null) {
            return;
        }
        VanillaUi.listArea(graphics, this.list.x(), this.list.y(), this.list.width(), this.list.height());
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
            String text = macro.name() + "  " + HotkeyRegistry.comboName(macro.combo());
            VanillaUi.text(graphics, this.font, Component.literal(text), this.list.x() + 3, rowY + 6,
                    conflict ? VanillaUi.TEXT_ERROR : VanillaUi.TEXT, nameWidth);
        }

        this.list.render(graphics);
        int hintLine = 0;
        for (String line : VanillaUi.wrap(this.font, bottomLine().getString(), layout.contentWidth())) {
            VanillaUi.text(graphics, this.font, Component.literal(line), left, this.hintY + hintLine * 10,
                    VanillaUi.TEXT_HINT, layout.contentWidth());
            hintLine++;
        }
    }

    private Set<Set<Integer>> duplicatedCombos() {
        Map<Set<Integer>, Integer> counts = new HashMap<>();
        for (MacroStorage.Macro macro : this.macros) {
            counts.merge(Set.copyOf(macro.keys()), 1, Integer::sum);
        }
        Set<Set<Integer>> duplicated = new HashSet<>();
        counts.forEach((combo, count) -> {
            if (count > 1) {
                duplicated.add(combo);
            }
        });
        return duplicated;
    }

    private Component bottomLine() {
        return Component.translatable("gui.merlinlib.macro.hint");
    }
}
