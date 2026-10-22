package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.notice.NoticeEntry;
import com.huziyang520.merlinlib.notice.NoticeRegistry;
import com.huziyang520.merlinlib.notice.NoticeSettings;
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
 * The join notice switches: one master switch and one switch per mod that registered a notice.
 *
 * <h2>Why a screen and not a configuration line</h2>
 *
 * <p>The per mod switches used to be a single {@code notices.per_mod} line holding
 * {@code modid=true;other=false}. Every player who wanted one mod's notice switched off had to type that
 * format, and a typo silently did nothing. The list of mods is not known until they have registered, so no
 * configuration schema could declare it either - which is why this screen builds itself from
 * {@link NoticeRegistry#codeNotices()} at the moment it opens, and why the values live in
 * {@code config/MerlinLib/notices.toml} rather than in {@code server.toml}.
 *
 * <h2>The order of the rows</h2>
 *
 * <p>MerlinLib's own notices come first, then the business mods sorted by mod id. The library is first
 * because it is the one row every installation has: a list whose first line moves around as mods are added
 * is a list where a player re-reads every row to find the one they changed last time.
 *
 * <p>When no business mod has registered a notice, the list is not left empty - an empty list reads as
 * "this screen is broken". One grey line says what the situation actually is.
 *
 * <h2>When it is written</h2>
 *
 * <p>Flipping a switch only changes what this screen shows. Confirm writes both stores once - the master
 * switch to {@code server.toml} through {@link ConfigManager#saveServer}, the per mod switches to
 * {@code notices.toml} through {@link NoticeSettings#save()} - and closes, so a screen abandoned with Escape
 * leaves the files alone. Both are read at the moment a notice would be sent, so the change applies to the
 * next player who joins rather than to the next restart.
 *
 * <h2>The list scrolls</h2>
 *
 * <p>An installation can register more notices than fit at a high GUI scale. The rows are real buttons
 * repositioned by {@link ScrollArea} as it scrolls, not painted text, so the wheel, the scrollbar and the
 * clicks all keep working while the list moves.
 */
public class NoticesScreen extends VanillaScreen {

    /** Narrowest content that still lays out a label and its switch comfortably. */
    private static final int MIN_CONTENT_WIDTH = 260;
    /** Width of a switch. */
    private static final int TOGGLE_WIDTH = 64;
    /** Height of the title and the separator under it. */
    private static final int HEADER_HEIGHT = 28;
    /** Height of one switch row, which is also the scroll step. */
    private static final int ROW_HEIGHT = VanillaUi.WIDGET_HEIGHT + 4;
    /** Gap between the master switch and the list. */
    private static final int LIST_GAP_ABOVE = 8;
    /** Gap between the list and the confirm button. */
    private static final int LIST_GAP_BELOW = 8;

    private final Screen parent;
    /** The mods with a registered notice: MerlinLib first, then by mod id. */
    private final List<String> owners;
    /** {@code true} when no mod other than MerlinLib registered a notice. */
    private final boolean noBusinessMods;
    /** The label of every owner, built once: a mod's display name does not change while the screen is open. */
    private final Map<String, Component> labels = new LinkedHashMap<>();

    private final Map<String, Boolean> switches = new LinkedHashMap<>();
    private boolean master;
    /** The per mod switch buttons, in the order the rows are drawn. */
    private final Map<String, Button> modButtons = new LinkedHashMap<>();

    private ScrollArea list;
    private int masterLabelY;

    /**
     * @param parent the screen to return to, or {@code null} to resume the game
     */
    public NoticesScreen(Screen parent) {
        super(Component.translatable("gui.merlinlib.notices.title"));
        this.parent = parent;
        this.owners = owners();
        boolean business = false;
        for (String owner : this.owners) {
            if (!Constants.MOD_ID.equals(owner)) {
                business = true;
                break;
            }
        }
        // A mod that registered a notice but is unknown to the loader still gets a row: the notice exists, so
        // the switch that decides whether it is sent has to exist too.
        for (String owner : this.owners) {
            this.labels.put(owner, rowLabel(owner));
        }
        this.noBusinessMods = !business;
        this.master = ConfigManager.server().noticesEnabled();
        for (NoticeEntry entry : NoticeRegistry.codeNotices()) {
            this.switches.putIfAbsent(entry.owner(), NoticeSettings.isEnabled(entry));
        }
    }

    /**
     * @return the mods that registered a notice, MerlinLib first and the rest by mod id
     */
    private static List<String> owners() {
        Set<String> distinct = new LinkedHashSet<>();
        for (NoticeEntry entry : NoticeRegistry.codeNotices()) {
            distinct.add(entry.owner());
        }
        boolean library = distinct.remove(Constants.MOD_ID);
        List<String> others = new ArrayList<>(distinct);
        others.sort(String::compareTo);
        List<String> ordered = new ArrayList<>();
        if (library) {
            ordered.add(Constants.MOD_ID);
        }
        ordered.addAll(others);
        return List.copyOf(ordered);
    }

    /**
     * @param owner the mod id
     * @return "Display Name / modid" when the loader knows a name, the id alone otherwise
     */
    private static String ownerName(String owner) {
        String name = Services.MOD_NAMES.modName(owner);
        if (name == null || name.isEmpty() || name.equalsIgnoreCase(owner)) {
            return owner;
        }
        return name + " / " + owner;
    }

    /**
     * @param owner the mod id
     * @return the row's label
     */
    private static Component rowLabel(String owner) {
        return Component.translatable("gui.merlinlib.notices.entry", ownerName(owner));
    }

    @Override
    protected Screen parentScreen() {
        return this.parent;
    }

    @Override
    protected int contentWidth() {
        List<Component> labels = new ArrayList<>();
        labels.add(this.title);
        labels.add(Component.translatable("merlinlib.config.notices.enabled"));
        labels.add(Component.translatable("gui.merlinlib.notices.empty"));
        labels.addAll(this.labels.values());
        return Math.max(MIN_CONTENT_WIDTH, VanillaUi.measureWidth(this.font, labels, TOGGLE_WIDTH,
                MIN_CONTENT_WIDTH));
    }

    /** @return how many rows the list holds, the grey line included */
    private int rowCount() {
        return Math.max(1, this.owners.size() + (this.noBusinessMods ? 1 : 0));
    }

    /**
     * @return how many rows fit at the current window size, so the panel never runs off the bottom
     */
    private int visibleRows() {
        int reserved = HEADER_HEIGHT + ROW_HEIGHT + LIST_GAP_ABOVE + LIST_GAP_BELOW + VanillaUi.WIDGET_HEIGHT;
        int fits = (contentHeightLimit() - reserved) / ROW_HEIGHT;
        return VanillaUi.clamp(fits, 1, rowCount());
    }

    @Override
    protected int contentHeight() {
        return HEADER_HEIGHT
                + ROW_HEIGHT                            // the master switch
                + LIST_GAP_ABOVE
                + visibleRows() * ROW_HEIGHT           // the per mod switches
                + LIST_GAP_BELOW
                + VanillaUi.WIDGET_HEIGHT;              // confirm
    }

    @Override
    protected void build(PanelLayout layout) {
        int left = layout.left();
        int toggleX = layout.right() - TOGGLE_WIDTH;

        layout.gap(HEADER_HEIGHT);

        int masterTop = layout.row(ROW_HEIGHT);
        this.masterLabelY = masterTop + 6;
        this.addRenderableWidget(VanillaUi.button(valueLabel(this.master), button -> {
            this.master = !this.master;
            button.setMessage(valueLabel(this.master));
        }, toggleX, masterTop + (ROW_HEIGHT - VanillaUi.WIDGET_HEIGHT) / 2, TOGGLE_WIDTH));

        layout.gap(LIST_GAP_ABOVE);
        int listTop = layout.cursor();
        int listHeight = visibleRows() * ROW_HEIGHT;
        this.list = new ScrollArea(left, listTop, layout.contentWidth(), listHeight, ROW_HEIGHT);
        this.list.setContentHeight(rowCount() * ROW_HEIGHT);
        layout.gap(listHeight);
        layout.gap(LIST_GAP_BELOW);

        // The rows are built here and positioned by applyScroll: a row that is scrolled out is hidden rather
        // than removed, so its state, its hover and its click target all come back in one piece.
        for (String owner : this.owners) {
            int rowX = this.list.x() + this.list.rowWidth() - TOGGLE_WIDTH;
            Button button = this.addRenderableWidget(VanillaUi.button(valueLabel(isOn(owner)),
                    pressed -> {
                        boolean now = !isOn(owner);
                        this.switches.put(owner, now);
                        pressed.setMessage(valueLabel(now));
                    }, rowX, listTop, TOGGLE_WIDTH));
            this.modButtons.put(owner, button);
        }
        applyScroll();

        int footer = layout.row(VanillaUi.WIDGET_HEIGHT);
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.done"),
                button -> save(), left, footer, layout.contentWidth()));
    }

    /**
     * @param owner the mod id
     * @return whether that mod's notices are on in this screen
     */
    private boolean isOn(String owner) {
        return Boolean.TRUE.equals(this.switches.get(owner));
    }

    /**
     * @param enabled the state
     * @return the text of a switch showing it
     */
    private static Component valueLabel(boolean enabled) {
        return Component.translatable(enabled ? "gui.merlinlib.notices.on" : "gui.merlinlib.notices.off");
    }

    /** Moves every row button to its scrolled position and hides the rows outside the viewport. */
    private void applyScroll() {
        if (this.list == null) {
            return;
        }
        int index = 0;
        for (Button button : this.modButtons.values()) {
            button.visible = this.list.rowVisible(index);
            button.setY(this.list.rowTop(index) + (ROW_HEIGHT - VanillaUi.WIDGET_HEIGHT) / 2);
            index++;
        }
    }

    /** Writes both stores and closes. */
    private void save() {
        Map<String, String> server = new LinkedHashMap<>();
        server.put("notices.enabled", Boolean.toString(this.master));
        ConfigManager.saveServer(server);
        for (Map.Entry<String, Boolean> entry : this.switches.entrySet()) {
            NoticeSettings.set(entry.getKey(), entry.getValue());
        }
        NoticeSettings.save();
        this.onClose();
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PanelLayout layout = layout();
        int left = layout.left();
        int content = layout.contentWidth();

        VanillaUi.text(graphics, this.font, this.title, left, layout.top(), VanillaUi.TEXT, content);
        VanillaUi.separator(graphics, left, layout.right(), layout.top() + 18);
        VanillaUi.text(graphics, this.font, Component.translatable("merlinlib.config.notices.enabled"),
                left, this.masterLabelY, VanillaUi.TEXT_HINT, content - TOGGLE_WIDTH - VanillaUi.GAP);

        if (this.list == null) {
            return;
        }
        int textLeft = this.list.x();
        int textWidth = this.list.rowWidth() - TOGGLE_WIDTH - VanillaUi.GAP;
        int index = 0;
        for (String owner : this.owners) {
            if (this.list.rowVisible(index)) {
                int y = this.list.rowTop(index) + (ROW_HEIGHT - this.font.lineHeight) / 2;
                VanillaUi.text(graphics, this.font, this.labels.get(owner), textLeft, y,
                        isOn(owner) ? VanillaUi.TEXT_HINT : VanillaUi.TEXT_DISABLED, textWidth);
            }
            index++;
        }
        if (this.noBusinessMods && this.list.rowVisible(index)) {
            int y = this.list.rowTop(index) + (ROW_HEIGHT - this.font.lineHeight) / 2;
            VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.notices.empty"),
                    textLeft, y, VanillaUi.TEXT_DISABLED, this.list.rowWidth());
        }
        this.list.render(graphics);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.list != null && this.list.mouseScrolled(mouseX, mouseY, amount)) {
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.list != null && this.list.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.list != null && this.list.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            applyScroll();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.list != null) {
            this.list.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
