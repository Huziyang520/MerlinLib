package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.ServerConfig;
import com.huziyang520.merlinlib.notice.NoticeEntry;
import com.huziyang520.merlinlib.notice.NoticeManager;
import com.huziyang520.merlinlib.notice.NoticePreferences;
import com.huziyang520.merlinlib.platform.Services;
import com.huziyang520.merlinlib.tools.ui.vanilla.PanelLayout;
import com.huziyang520.merlinlib.tools.ui.vanilla.ScrollArea;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaScreen;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
 * The per mod switches for join notices, opened from the general tab.
 *
 * <h2>Who appears in the list</h2>
 *
 * <p>Only mods that registered a notice through {@link com.huziyang520.merlinlib.api.NoticeApi}: the list is
 * built from what was registered at runtime, never from a list of names kept in the library. A mod that does
 * not use the feature has nothing to show here, which is also why an empty list saying so is a normal state
 * and not an error.
 *
 * <p>MerlinLib's own notices come first, under their own heading, because they belong to the library rather
 * than to the mods being served; every other owner follows under a centred heading.
 */
public final class NoticeSettingsScreen extends VanillaScreen {

    private static final String T = "gui.merlinlib.notice.";
    private static final int CONTROL_WIDTH = 72;
    private static final int ROW = 24;
    private static final int MIN_VISIBLE_ROWS = 3;
    private static final int SCREEN_MARGIN = 40;

    /** What a row is. */
    private enum Kind { HEADING, CENTERED, TOGGLE, MUTED, SPACER }

    /**
     * One row of the list.
     *
     * @param kind  what the row is
     * @param label what it says, empty for a spacer
     * @param owner the mod the row switches, only for toggle rows
     */
    private record Row(Kind kind, Component label, String owner) {
    }

    private final Screen parent;
    /** Choices made on this screen, by mod id; only mods the player touched appear here. */
    private final Map<String, Boolean> pending = new LinkedHashMap<>();
    private final List<Row> rows = new ArrayList<>();
    /** The toggle buttons, in toggle row order. */
    private final List<Button> buttons = new ArrayList<>();

    private ScrollArea area;
    private int titleY;
    private int hintY;
    private Button save;
    /** Whether a button asked to leave; acted on the next frame instead of during the click. */
    private boolean pendingExit;
    /** Whether the pending exit should write the switches first. */
    private boolean exitSaves;

    /**
     * @param parent the screen to return to
     */
    public NoticeSettingsScreen(Screen parent) {
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

    // ---------------------------------------------------------------- the list

    /**
     * Builds the rows: the library's own notices, then one row per business mod that signed up.
     */
    private void buildRows() {
        List<NoticeEntry> entries = notices();
        Set<String> libraryOwners = new LinkedHashSet<>();
        Set<String> modOwners = new LinkedHashSet<>();
        for (NoticeEntry entry : entries) {
            (entry.ownedByLibrary() ? libraryOwners : modOwners).add(entry.owner());
        }

        this.rows.add(new Row(Kind.HEADING, Component.translatable(T + "library"), ""));
        if (libraryOwners.isEmpty()) {
            this.rows.add(new Row(Kind.MUTED, Component.translatable(T + "library_empty"), ""));
        } else {
            for (String owner : libraryOwners) {
                this.rows.add(toggleRow(owner));
            }
        }
        this.rows.add(new Row(Kind.SPACER, Component.empty(), ""));
        this.rows.add(new Row(Kind.CENTERED, Component.translatable(T + "mods"), ""));
        if (modOwners.isEmpty()) {
            this.rows.add(new Row(Kind.MUTED, Component.translatable(T + "empty"), ""));
        } else {
            for (String owner : modOwners) {
                this.rows.add(toggleRow(owner));
            }
        }
    }

    private Row toggleRow(String owner) {
        return new Row(Kind.TOGGLE, Component.literal(Services.MOD_NAMES.modName(owner)), owner);
    }

    /**
     * Reads the notices that can be seen from this side.
     *
     * <p>Code registrations are the same on both sides, because the same mods run on both. Data pack notices
     * only exist where the packs were read, so they are listed when this client runs the world itself and are
     * left out on a dedicated server - the switches work either way, they are simply named by the mods the
     * client can see.
     *
     * @return the notices known to this client
     */
    private static List<NoticeEntry> notices() {
        return NoticeManager.code();
    }

    /** @return whether a mod's notices are on before the player touches anything */
    private static boolean defaultEnabled(String owner) {
        ServerConfig config = ConfigManager.server();
        for (NoticeEntry entry : NoticeManager.code()) {
            if (entry.owner().equals(owner)) {
                return NoticePreferences.isEnabled(config, entry);
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- size

    @Override
    protected int contentWidth() {
        buildRowsIfNeeded();
        int widest = this.font.width(Component.translatable(T + "title"));
        for (Row row : this.rows) {
            widest = Math.max(widest, this.font.width(row.label())
                    + (row.kind() == Kind.TOGGLE ? 12 + CONTROL_WIDTH : 0));
        }
        widest = Math.max(widest, this.font.width(Component.translatable(T + "hint")));
        return Math.max(240, Math.min(widest + 24, this.width - SCREEN_MARGIN));
    }

    @Override
    protected int contentHeight() {
        buildRowsIfNeeded();
        int natural = 12 + 4 + this.rows.size() * ROW + 10 + 6 + VanillaUi.WIDGET_HEIGHT;
        return Math.min(natural, this.height - SCREEN_MARGIN);
    }

    /** The rows are read for measuring before the screen is built, so they are made once. */
    private void buildRowsIfNeeded() {
        if (this.rows.isEmpty()) {
            buildRows();
        }
    }

    // ---------------------------------------------------------------- building

    @Override
    protected void build(PanelLayout layout) {
        this.buttons.clear();
        this.rows.clear();
        buildRows();

        int left = layout.left();
        int content = layout.contentWidth();
        this.titleY = layout.row(12);
        layout.gap(4);

        int reservedBottom = 10 + 6 + VanillaUi.WIDGET_HEIGHT;
        int available = Math.max(MIN_VISIBLE_ROWS * ROW, layout.contentBottom() - layout.cursor() - reservedBottom);
        int visibleRows = Math.max(MIN_VISIBLE_ROWS, available / ROW);
        int top = layout.row(visibleRows * ROW);
        this.area = new ScrollArea(left, top, content, visibleRows * ROW, ROW);
        this.area.setContentHeight(this.rows.size() * ROW);

        int controlX = left + this.area.rowWidth() - CONTROL_WIDTH;
        for (int index = 0; index < this.rows.size(); index++) {
            Row row = this.rows.get(index);
            if (row.kind() != Kind.TOGGLE) {
                continue;
            }
            String owner = row.owner();
            boolean[] state = {this.pending.getOrDefault(owner, defaultEnabled(owner))};
            this.buttons.add(this.addRenderableWidget(VanillaUi.button(stateLabel(state[0]),
                    button -> {
                        state[0] = !state[0];
                        button.setMessage(stateLabel(state[0]));
                        this.pending.put(owner, state[0]);
                    }, controlX, this.area.rowTop(index), CONTROL_WIDTH)));
        }
        layout.cursorTo(this.area.y() + this.area.height());

        int bottom = layout.contentBottom() - VanillaUi.WIDGET_HEIGHT;
        this.hintY = bottom - 6 - 10;
        int half = layout.sliceWidth(2);
        this.save = this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.config.save"),
                button -> this.exit(true), layout.sliceX(0, 2), bottom, half));
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.cancel"),
                button -> this.exit(false), layout.sliceX(1, 2), bottom, half));
    }

    private static Component stateLabel(boolean enabled) {
        return Component.translatable(enabled ? "gui.merlinlib.config.on" : "gui.merlinlib.config.off");
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        applyPendingExit();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        PanelLayout layout = layout();
        int left = layout.left();
        int content = layout.contentWidth();
        graphics.text(this.font, Component.translatable(T + "title"), left, this.titleY,
                VanillaUi.TEXT, true);

        if (this.area != null) {
            int width = this.area.rowWidth() - CONTROL_WIDTH - 8;
            for (int index = 0; index < this.rows.size(); index++) {
                if (!this.area.rowVisible(index)) {
                    continue;
                }
                Row row = this.rows.get(index);
                int y = this.area.rowTop(index) + 6;
                switch (row.kind()) {
                    case HEADING -> graphics.text(this.font, row.label(), left, y, VanillaUi.TEXT, true);
                    case MUTED -> graphics.text(this.font, row.label(), left, y, VanillaUi.TEXT_DISABLED, true);
                    case CENTERED -> {
                        int x = left + Math.max(0, (content - this.font.width(row.label())) / 2);
                        graphics.text(this.font, row.label(), x, y, VanillaUi.TEXT, true);
                    }
                    case TOGGLE -> graphics.text(this.font,
                            Component.literal(VanillaUi.clip(this.font, row.label().getString(), width)),
                            left, y, VanillaUi.TEXT, true);
                    case SPACER -> {
                        // Nothing to draw: the row is there to leave a gap between the two headings.
                    }
                }
            }
            this.area.render(graphics);
        }

        graphics.text(this.font, Component.literal(VanillaUi.clip(this.font,
                Component.translatable(T + "hint").getString(), content)), left, this.hintY,
                VanillaUi.TEXT_HINT, true);
    }

    // ---------------------------------------------------------------- saving

    /**
     * Asks to leave, either saving first or not.
     *
     * <p>Deferred to the next frame rather than done here: switching screens from inside a click leaves the
     * screen half built while it is still being drawn, which the player sees as a flicker.
     *
     * @param saves whether the switches should be written before leaving
     */
    private void exit(boolean saves) {
        this.pendingExit = true;
        this.exitSaves = saves;
    }

    /** Carries out a requested exit. */
    private void applyPendingExit() {
        if (!this.pendingExit) {
            return;
        }
        this.pendingExit = false;
        if (this.exitSaves) {
            writeChanges();
        }
        this.onClose();
    }

    /** Writes the switches the player touched, leaving the untouched ones at their declared defaults. */
    private void writeChanges() {
        if (this.pending.isEmpty()) {
            return;
        }
        String value = ConfigManager.server().noticePerMod();
        for (Map.Entry<String, Boolean> entry : this.pending.entrySet()) {
            value = NoticePreferences.with(value, entry.getKey(), entry.getValue());
        }
        ConfigManager.saveServer(Map.of(NoticePreferences.KEY, value));
    }
}
