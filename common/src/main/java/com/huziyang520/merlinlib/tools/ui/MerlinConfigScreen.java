package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.config.ClientConfig;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.ServerConfig;
import com.huziyang520.merlinlib.tools.ui.vanilla.PanelLayout;
import com.huziyang520.merlinlib.tools.ui.vanilla.ScrollArea;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaScreen;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The configuration screen: every switch of the library, grouped by the module it belongs to.
 *
 * <p>It is deliberately a vanilla options screen and nothing more: a row of module tabs, one row per
 * setting, a label on the left and a vanilla control on the right, and no panel behind it, because the
 * controls already carry the vanilla frame. Every line of text is drawn in plain white with a shadow, which
 * is the only colour that stays readable over an arbitrary world - the dark label grey used on a panel is
 * invisible here.
 *
 * <h2>It has to fit the smallest window</h2>
 *
 * <p>At GUI scale 4 the usable window is a quarter of the resolution, so both dimensions are clamped to what
 * the window actually has, labels are clipped to the space left of their control, and the settings rows
 * scroll when they cannot all be shown. Without that, a long label or a tall module simply ran off the
 * screen.
 *
 * <h2>Switching a tab is deferred</h2>
 *
 * <p>The tab buttons do not rebuild the screen while the click is being dispatched: they record the request,
 * and it is applied at the start of the next frame, when no widget list is being iterated. Clearing widgets
 * from inside a button callback corrupted the dispatch and could empty the screen entirely.
 *
 * <p>Saving writes only the values that changed, into {@code client.toml} or {@code server.toml} depending
 * on which side owns the option, and reloads immediately.
 */
public class MerlinConfigScreen extends VanillaScreen {

    private static final String[] TABS = {
            "gui.merlinlib.config.tab.general",
            "gui.merlinlib.config.tab.editor",
            "gui.merlinlib.config.tab.health",
            "gui.merlinlib.config.tab.hud",
            "gui.merlinlib.config.tab.macro",
    };

    private static final String[][] HINTS = {
            {"gui.merlinlib.config.hint.general"},
            {"gui.merlinlib.config.hint.editor", "gui.merlinlib.config.hint.editor_shift"},
            {"gui.merlinlib.config.hint.health"},
            {"gui.merlinlib.config.hint.hud"},
            {"gui.merlinlib.config.hint.macro"},
    };

    /** Width of the control column. */
    private static final int CONTROL_WIDTH = 96;
    /** Height of one setting row. */
    private static final int ROW = 22;
    /** Narrowest content width; smaller windows clip instead. */
    private static final int MIN_WIDTH = 300;
    /** Smallest number of setting rows shown at once, however small the window is. */
    private static final int MIN_VISIBLE_ROWS = 3;
    /** Margin kept between the content and the window edge. */
    private static final int SCREEN_MARGIN = 8;
    /** The rows above the settings: the title and the tab row. */
    private static final int HEADER = 12 + 4 + VanillaUi.WIDGET_HEIGHT + 8;

    /**
     * Text colour of this screen.
     *
     * <p>Plain white, because there is no panel behind it: the dark grey that reads well on the vanilla
     * dialog body disappears against grass, sand or stone.
     */
    private static final int TEXT = 0xFFFFFFFF;
    /** The hint lines use the same white; a shadow keeps them apart from the labels. */
    private static final int TEXT_HINT = 0xFFFFFFFF;
    /** Colour of a field that cannot be accepted. */
    private static final int TEXT_ERROR = 0xFFFF5555;
    /** Colour of the warning shown while the toolkit master switch is off. */
    private static final int TEXT_WARNING = 0xFFFF5555;

    /** What a setting's value looks like, which decides how it is validated. */
    private enum Kind { TOGGLE, INTEGER, DECIMAL }

    /**
     * One row of the screen.
     *
     * @param key        the {@code section.key} this row writes
     * @param clientSide whether the key lives in {@code client.toml} rather than {@code server.toml}
     * @param kind       the value type
     * @param label      the row label
     * @param min        smallest accepted value, ignored for toggles
     * @param max        largest accepted value, ignored for toggles
     * @param current    the value currently on disk, as text
     */
    private record Setting(String key, boolean clientSide, Kind kind, Component label,
                           double min, double max, String current) {
    }

    private final Screen parent;
    private final List<Runnable> collectors = new ArrayList<>();
    private final Map<String, String> clientChanges = new LinkedHashMap<>();
    private final Map<String, String> serverChanges = new LinkedHashMap<>();
    /** The toggle buttons, in row order; the n-th belongs to the n-th toggle row. */
    private final List<Button> toggleButtons = new ArrayList<>();
    /** The number fields, in row order; the n-th belongs to the n-th numeric row. */
    private final List<EditBox> fields = new ArrayList<>();

    private int tab;
    /** The tab a button asked for; applied on the next frame instead of during the click. */
    private int pendingTab = -1;
    private int titleY;
    private int warningY;
    private int hintY;
    private List<Setting> rows = List.of();
    private ScrollArea settingsArea;
    private int[] rowY = new int[0];
    private boolean invalid;
    private Button save;

    /**
     * @param parent the screen to return to, or {@code null} to resume the game
     */
    public MerlinConfigScreen(Screen parent) {
        super(Component.translatable("gui.merlinlib.config.title"));
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

    // ---------------------------------------------------------------- the settings

    private static final String T = "gui.merlinlib.config.";

    private List<Setting> settings(int tab) {
        ServerConfig server = ConfigManager.server();
        ClientConfig client = ConfigManager.client();
        return switch (tab) {
            case 0 -> List.of(
                    toggle("tools.enable_testing_toolkit", false, T + "toolkit", server.testingToolkitEnabled()),
                    toggle("security.restrict_tools_to_operators", false, T + "operators",
                            server.restrictToolsToOperators()),
                    toggle("content.disable_generated_enchantments", false, T + "generated",
                            server.disableGeneratedEnchantments()));
            case 1 -> List.of(
                    toggle("editor.hotkey_enabled", true, T + "hotkey", client.editorHotkeyEnabled()),
                    toggle("security.editor_requires_permission", false, T + "editor_permission",
                            server.editorRequiresPermission()),
                    integer("editor.shift_step", true, T + "shift_step", client.editorShiftStep(), 1, 100000),
                    integer("editor.scroll_step", true, T + "scroll_step", client.editorScrollStep(), 1, 1000),
                    integer("editor.scroll_step_fast", true, T + "scroll_step_fast",
                            client.editorScrollStepFast(), 1, 100000),
                    toggle("tools.allow_mismatched_enchantments", false, T + "mismatched",
                            server.allowMismatchedEnchantments()),
                    toggle("tools.allow_levels_above_max", false, T + "above_max", server.allowLevelsAboveMax()),
                    toggle("tools.allow_editing_all_items", false, T + "all_items",
                            server.allowEditingAllItems()),
                    integer("tools.max_enchantment_level", false, T + "enchant_level",
                            server.maxEnchantmentLevel(), 1, Integer.MAX_VALUE),
                    integer("tools.max_test_weapon_damage", false, T + "weapon_damage",
                            server.maxTestWeaponDamage(), 1, Integer.MAX_VALUE));
            case 2 -> List.of(
                    toggle("tools.health_editor_requires_item", false, T + "health_item",
                            server.healthEditorRequiresItem()),
                    toggle("security.health_editor_requires_permission", false, T + "health_permission",
                            server.healthEditorRequiresPermission()));
            case 3 -> List.of(
                    toggle("floating_text.enabled", true, T + "floating_text", client.floatingTextEnabled()),
                    integer("floating_text.duration_ticks", true, T + "floating_duration",
                            client.floatingTextDurationTicks(), 1, 400),
                    decimal("floating_text.base_scale", true, T + "floating_scale",
                            client.floatingTextBaseScale(), 0.1D, 8.0D),
                    toggle("hud.crosshair_damage", true, T + "crosshair", client.crosshairDamage()),
                    integer("hud.crosshair_damage_ticks", true, T + "crosshair_ticks",
                            client.crosshairDamageTicks(), 1, 400));
            default -> List.of(
                    toggle("macros.enabled", true, T + "macros", client.macrosEnabled()));
        };
    }

    private static Setting toggle(String key, boolean clientSide, String label, boolean value) {
        return new Setting(key, clientSide, Kind.TOGGLE, Component.translatable(label), 0, 0,
                Boolean.toString(value));
    }

    private static Setting integer(String key, boolean clientSide, String label, int value, int min, int max) {
        return new Setting(key, clientSide, Kind.INTEGER, Component.translatable(label), min, max,
                Integer.toString(value));
    }

    private static Setting decimal(String key, boolean clientSide, String label, double value, double min,
                                   double max) {
        return new Setting(key, clientSide, Kind.DECIMAL, Component.translatable(label), min, max,
                Double.toString(value));
    }

    // ---------------------------------------------------------------- measured size

    /** @return true when the toolkit master switch is off, which disables everything else. */
    private static boolean toolkitOff() {
        return !ConfigManager.server().testingToolkitEnabled();
    }

    @Override
    protected int contentWidth() {
        int widest = this.font.width(Component.translatable("gui.merlinlib.config.title"));
        if (toolkitOff()) {
            widest = Math.max(widest, this.font.width(Component.translatable(T + "warning.toolkit_off")));
        }
        for (int index = 0; index < TABS.length; index++) {
            for (Setting setting : settings(index)) {
                widest = Math.max(widest, this.font.width(setting.label()) + 12 + CONTROL_WIDTH);
            }
            for (String hint : HINTS[index]) {
                widest = Math.max(widest, this.font.width(Component.translatable(hint)));
            }
        }
        // Clamp to the window: at GUI scale 4 the usable width is a quarter of the resolution, and a screen
        // wider than the window simply runs off it. Long labels and hints are clipped instead.
        return Math.max(MIN_WIDTH, Math.min(widest, this.width - SCREEN_MARGIN));
    }

    @Override
    protected int contentHeight() {
        int natural = 0;
        for (int index = 0; index < TABS.length; index++) {
            natural = Math.max(natural, HEADER + settings(index).size() * ROW
                    + (index == 0 && toolkitOff() ? 12 : 0)
                    + HINTS[index].length * 10 + 6 + VanillaUi.WIDGET_HEIGHT);
        }
        return Math.min(natural, this.height - SCREEN_MARGIN);
    }

    // ---------------------------------------------------------------- building

    @Override
    protected void build(PanelLayout layout) {
        this.collectors.clear();
        this.fields.clear();
        this.toggleButtons.clear();
        // The pending changes are deliberately kept: a tab switch rebuilds this screen, and clearing them here
        // threw away every edit made on the previous tab, which is what made the tabs feel like they reset.
        this.invalid = false;

        int left = layout.left();
        int content = layout.contentWidth();

        this.titleY = layout.row(12);
        layout.gap(4);

        int tabWidth = layout.sliceWidth(TABS.length);
        int tabY = layout.row(VanillaUi.WIDGET_HEIGHT);
        for (int index = 0; index < TABS.length; index++) {
            final int target = index;
            Button button = VanillaUi.button(Component.translatable(TABS[index]),
                    pressed -> this.pendingTab = target, layout.sliceX(index, TABS.length), tabY, tabWidth);
            button.active = index != this.tab;
            this.addRenderableWidget(button);
        }
        layout.gap(8);

        if (toolkitOff()) {
            this.warningY = layout.row(12);
        }

        // The rows get whatever height is left after the title, the tabs, the hints and the buttons, so the
        // bottom of the screen is never pushed off it.
        int reservedBottom = HINTS[this.tab].length * 10 + 6 + VanillaUi.WIDGET_HEIGHT;
        int available = Math.max(MIN_VISIBLE_ROWS * ROW, layout.contentBottom() - layout.cursor() - reservedBottom);
        int visibleRows = Math.max(MIN_VISIBLE_ROWS, available / ROW);
        int top = layout.row(visibleRows * ROW);
        this.settingsArea = new ScrollArea(left, top, content, visibleRows * ROW, ROW);

        this.rows = settings(this.tab);
        this.rowY = new int[this.rows.size()];
        this.settingsArea.setContentHeight(this.rows.size() * ROW);

        int controlX = left + content - CONTROL_WIDTH;
        for (int index = 0; index < this.rows.size(); index++) {
            Setting setting = this.rows.get(index);
            int y = this.settingsArea.rowTop(index);
            this.rowY[index] = y;
            if (setting.kind() == Kind.TOGGLE) {
                boolean[] state = {Boolean.parseBoolean(valueOf(setting))};
                this.toggleButtons.add(this.addRenderableWidget(VanillaUi.button(stateLabel(state[0]),
                        button -> {
                            state[0] = !state[0];
                            button.setMessage(stateLabel(state[0]));
                        }, controlX, y, CONTROL_WIDTH)));
                this.collectors.add(() -> record(setting, Boolean.toString(state[0])));
            } else {
                EditBox box = VanillaUi.field(this.font, controlX, y, CONTROL_WIDTH, setting.label(),
                        valueOf(setting), 12);
                box.setResponder(text -> validate());
                this.addRenderableWidget(box);
                this.fields.add(box);
                this.collectors.add(() -> record(setting, box.getValue().trim()));
            }
        }
        layout.cursorTo(this.settingsArea.y() + this.settingsArea.height());
        layoutRows();

        this.hintY = layout.cursor() + 6;
        layout.gap(6 + HINTS[this.tab].length * 10 + 6);

        int bottom = layout.row(VanillaUi.WIDGET_HEIGHT);
        int half = layout.sliceWidth(2);
        this.save = this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.config.save"),
                button -> this.save(), layout.sliceX(0, 2), bottom, half));
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.cancel"), button -> this.onClose(),
                layout.sliceX(1, 2), bottom, half));

        validate();
    }

    /** Moves the setting widgets to follow the scroll position and hides the rows beyond the viewport. */
    private void layoutRows() {
        if (this.settingsArea == null) {
            return;
        }
        int toggles = 0;
        int edits = 0;
        for (int index = 0; index < this.rows.size(); index++) {
            int y = this.settingsArea.rowTop(index);
            this.rowY[index] = y;
            boolean visible = this.settingsArea.rowVisible(index);
            if (this.rows.get(index).kind() == Kind.TOGGLE) {
                Button button = this.toggleButtons.get(toggles++);
                button.setY(y);
                button.visible = visible;
                button.active = visible;
            } else {
                EditBox box = this.fields.get(edits++);
                box.setY(y);
                box.visible = visible;
                box.active = visible;
            }
        }
    }

    /**
     * The value a row has to start from.
     *
     * <p>An edit made on a tab that is not shown right now wins over what is on disk, so switching tabs back
     * and forth never loses work: the pending value is the one the player last saw.
     *
     * @param setting the row
     * @return the value to show and to seed the control with
     */
    private String valueOf(Setting setting) {
        Map<String, String> changes = setting.clientSide() ? this.clientChanges : this.serverChanges;
        return changes.getOrDefault(setting.key(), setting.current());
    }

    private static Component stateLabel(boolean enabled) {
        return Component.translatable(enabled ? "gui.merlinlib.config.on" : "gui.merlinlib.config.off");
    }

    /**
     * Applies a tab switch requested by a button.
     *
     * <p>Called at the start of a frame, never from the button callback: rebuilding the widget list while a
     * click is being dispatched leaves the screen in a broken state (widgets vanish and the event is handled
     * by a list that no longer exists).
     */
    private void applyPendingTab() {
        if (this.pendingTab < 0 || this.pendingTab == this.tab) {
            this.pendingTab = -1;
            return;
        }
        // Collected before the rebuild, while the widgets of the old tab still exist: without this the values
        // typed on a tab were only read when Save was pressed, so switching tabs - or saving from another tab
        // - silently dropped them.
        collect();
        this.tab = this.pendingTab;
        this.pendingTab = -1;
        this.rebuildWidgets();
    }

    /** Runs every row's collector, so the pending maps hold what is on screen right now. */
    private void collect() {
        for (Runnable collector : this.collectors) {
            collector.run();
        }
    }

    private void record(Setting setting, String value) {
        (setting.clientSide() ? this.clientChanges : this.serverChanges).put(setting.key(), value);
    }

    /** Checks every numeric field and switches saving off while one of them cannot be accepted. */
    private void validate() {
        this.invalid = false;
        for (int index = 0; index < this.rows.size() && !this.invalid; index++) {
            Setting setting = this.rows.get(index);
            if (setting.kind() == Kind.TOGGLE) {
                continue;
            }
            String text = this.fields.get(fieldIndex(index)).getValue();
            this.invalid = !valid(setting, text);
        }
        if (this.save != null) {
            this.save.active = !this.invalid;
        }
    }

    private int fieldIndex(int rowIndex) {
        int fields = 0;
        for (int index = 0; index < rowIndex; index++) {
            if (this.rows.get(index).kind() != Kind.TOGGLE) {
                fields++;
            }
        }
        return fields;
    }

    private static boolean valid(Setting setting, String text) {
        try {
            if (setting.kind() == Kind.INTEGER) {
                long value = Long.parseLong(text.trim());
                return value >= setting.min() && value <= setting.max();
            }
            double value = Double.parseDouble(text.trim());
            return value >= setting.min() && value <= setting.max();
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void save() {
        validate();
        if (this.invalid) {
            return;
        }
        collect();
        if (!this.clientChanges.isEmpty()) {
            ConfigManager.saveClient(this.clientChanges);
        }
        if (!this.serverChanges.isEmpty()) {
            ConfigManager.saveServer(this.serverChanges);
        }
        this.onClose();
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.settingsArea != null && this.settingsArea.mouseClicked(event)) {
            layoutRows();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.settingsArea != null && this.settingsArea.mouseDragged(event)) {
            layoutRows();
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.settingsArea != null && this.settingsArea.mouseReleased(event)) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.settingsArea != null && this.settingsArea.mouseScrolled(mouseX, mouseY, scrollY)) {
            layoutRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ---------------------------------------------------------------- drawing

    /**
     * Outlines a field that cannot be accepted with a one pixel red frame.
     *
     * <p>Four thin lines rather than a filled rectangle: a filled one would cover the field and its text,
     * which hides the very value the player has to correct.
     *
     * @param graphics the render state extractor
     * @param box      the field to frame
     */
    private static void outline(GuiGraphicsExtractor graphics, EditBox box) {
        int x = box.getX();
        int y = box.getY();
        int width = box.getWidth();
        int height = box.getHeight();
        graphics.fill(x - 1, y - 1, x + width + 1, y, TEXT_ERROR);
        graphics.fill(x - 1, y + height, x + width + 1, y + height + 1, TEXT_ERROR);
        graphics.fill(x - 1, y, x, y + height, TEXT_ERROR);
        graphics.fill(x + width, y, x + width + 1, y + height, TEXT_ERROR);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        applyPendingTab();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        PanelLayout layout = layout();
        int left = layout.left();
        int content = layout.contentWidth();

        // No panel behind this screen, so every line is white with a shadow to stay readable anywhere.
        graphics.text(this.font, Component.translatable("gui.merlinlib.config.title"), left, this.titleY,
                TEXT, true);

        if (toolkitOff()) {
            graphics.text(this.font, Component.translatable(T + "warning.toolkit_off"), left, this.warningY,
                    TEXT_WARNING, true);
        }

        int labelWidth = Math.max(40, content - CONTROL_WIDTH - 8);
        for (int index = 0; index < this.rows.size(); index++) {
            Setting setting = this.rows.get(index);
            if (!this.settingsArea.rowVisible(index)) {
                continue;
            }
            boolean bad = setting.kind() != Kind.TOGGLE
                    && !valid(setting, this.fields.get(fieldIndex(index)).getValue());
            graphics.text(this.font, Component.literal(
                            VanillaUi.clip(this.font, setting.label().getString(), labelWidth)),
                    left, this.rowY[index] + 6, bad ? TEXT_ERROR : TEXT, true);
            if (bad) {
                outline(graphics, this.fields.get(fieldIndex(index)));
            }
        }
        this.settingsArea.render(graphics);

        String[] hints = HINTS[this.tab];
        for (int index = 0; index < hints.length; index++) {
            String clipped = VanillaUi.clip(this.font, Component.translatable(hints[index]).getString(), content);
            graphics.text(this.font, Component.literal(clipped), left, this.hintY + index * 10,
                    TEXT_HINT, true);
        }
    }
}
