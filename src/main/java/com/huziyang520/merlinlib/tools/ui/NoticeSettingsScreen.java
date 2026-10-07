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
 *
 * <h2>What changed / 1.20.1 note</h2>
 *
 * <p>The body is a direct port; four call sites moved:
 *
 * <ul>
 *   <li>the render pass is {@link #render(GuiGraphics, int, int, float)}, and every
 *       {@code graphics.text(...)} became {@code graphics.drawString(..., shadow)} - 1.21 renamed that
 *       method;</li>
 *   <li>the four mouse methods lost their {@code MouseButtonEvent}. They now take the cursor and the button
 *       as separate arguments, and {@code mouseClicked} has no double-click flag on this version, so that
 *       flag is dropped rather than reimplemented. {@link ScrollArea} was ported to the same unpacked shape,
 *       which is what makes the forwarding below a straight pass-through;</li>
 *   <li>{@code mouseScrolled} is the three argument overload here; 1.20.1 has no horizontal wheel, so the
 *       single delta is what 26.3 passed as {@code scrollY};</li>
 *   <li>{@code Services.MOD_NAMES.modName(owner)} is unchanged - the 1.20.1 bridge resolves it through
 *       Forge's mod list, which answers the same question.</li>
 * </ul>
 *
 * <p>One thing worth stating for this screen specifically: the switches are written through
 * {@code ConfigManager.saveServer(Map)} into {@code server.toml}, and the {@code notices.per_mod} key that
 * {@link NoticePreferences#KEY} names exists in this project's 1.20.1 template as well. The screen therefore
 * writes the same key on both lines.
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

    @Override
    protected boolean subScreen() {
        return true;
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
     * client can see. On 1.20.1 that split is unchanged: {@code NoticeManager#code()} is the client-visible
     * half, exactly as it was on 26.3.
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        applyPendingExit();
        beginIntro(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        PanelLayout layout = layout();
        int left = layout.left();
        int content = layout.contentWidth();
        graphics.drawString(this.font, Component.translatable(T + "title"), left, this.titleY,
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
                    case HEADING -> graphics.drawString(this.font, row.label(), left, y, VanillaUi.TEXT, true);
                    case MUTED -> graphics.drawString(this.font, row.label(), left, y,
                            VanillaUi.TEXT_DISABLED, true);
                    case CENTERED -> {
                        int x = left + Math.max(0, (content - this.font.width(row.label())) / 2);
                        graphics.drawString(this.font, row.label(), x, y, VanillaUi.TEXT, true);
                    }
                    case TOGGLE -> graphics.drawString(this.font,
                            Component.literal(VanillaUi.clip(this.font, row.label().getString(), width)),
                            left, y, VanillaUi.TEXT, true);
                    case SPACER -> {
                        // Nothing to draw: the row is there to leave a gap between the two headings.
                    }
                }
            }
            this.area.render(graphics);
        }

        // Wrapped rather than clipped, for the same reason as the settings screen: the hint has to be readable.
        int hintLine = 0;
        for (String line : VanillaUi.wrap(this.font, Component.translatable(T + "hint").getString(), content)) {
            graphics.drawString(this.font, Component.literal(line), left, this.hintY + hintLine * 10,
                    VanillaUi.TEXT_HINT, true);
            hintLine++;
        }
        endIntro(graphics);
        drawIntroVeil(graphics);
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
    // The list's scrollbar is the vanilla one and it drags, but only if the mouse events reach it: a screen
    // that does not forward them leaves a bar that scrolls with the wheel and cannot be grabbed.

    /**
     * Forwards a click to the scrollbar before the widgets see it.
     *
     * <p><b>What changed / 1.20.1 note:</b> the 26.3 parameter was one {@code MouseButtonEvent}; this version
     * takes the cursor and the button as separate arguments and has no double-click flag, so the flag is
     * dropped. {@link ScrollArea} was ported to the same shape, so the call below is a direct pass-through.
     *
     * @param mouseX the cursor x
     * @param mouseY the cursor y
     * @param button the mouse button
     * @return {@code true} when the click was consumed
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.area != null && this.area.mouseClicked(mouseX, mouseY, button)) {
            layoutRows();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Forwards a drag to the scrollbar.
     *
     * <p><b>What changed / 1.20.1 note:</b> the cursor, the button and the two drag deltas are separate
     * arguments here, which is exactly what {@link ScrollArea#mouseDragged} takes.
     *
     * @param mouseX the cursor x
     * @param mouseY the cursor y
     * @param button the mouse button
     * @param dragX  the x delta
     * @param dragY  the y delta
     * @return {@code true} when the drag was consumed
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.area != null && this.area.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            layoutRows();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * Ends a scrollbar drag.
     *
     * <p><b>What changed / 1.20.1 note:</b> three separate arguments instead of one event object.
     *
     * @param mouseX the cursor x
     * @param mouseY the cursor y
     * @param button the mouse button
     * @return {@code true} when the release was consumed
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.area != null && this.area.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Scrolls the list with the wheel.
     *
     * <p><b>What changed / 1.20.1 note:</b> the three argument overload, not the four argument one: this
     * version has no horizontal wheel, so the single delta plays the part 26.3 gave to {@code scrollY}.
     *
     * @param mouseX  the cursor x
     * @param mouseY  the cursor y
     * @param scrollY the wheel delta
     * @return {@code true} when the scroll was consumed
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (this.area != null && this.area.mouseScrolled(mouseX, mouseY, scrollY)) {
            layoutRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    /** Moves the toggle buttons to follow the scroll position and hides the rows beyond the viewport. */
    private void layoutRows() {
        if (this.area == null) {
            return;
        }
        int slot = 0;
        for (int index = 0; index < this.rows.size(); index++) {
            if (this.rows.get(index).kind() != Kind.TOGGLE) {
                continue;
            }
            Button button = this.buttons.get(slot++);
            int y = this.area.rowTop(index);
            boolean visible = this.area.rowVisible(index);
            button.setY(y);
            button.visible = visible;
            button.active = visible;
        }
    }

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
