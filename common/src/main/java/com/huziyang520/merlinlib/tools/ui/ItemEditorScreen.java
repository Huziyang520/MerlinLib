package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.network.ItemEditPayload;
import com.huziyang520.merlinlib.network.ServerSender;
import com.huziyang520.merlinlib.tools.TestWeapons;
import com.huziyang520.merlinlib.tools.ui.vanilla.PanelLayout;
import com.huziyang520.merlinlib.tools.ui.vanilla.ScrollArea;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaScreen;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * The item editor: rename any item, edit the enchantments of any item, and edit the base damage of a
 * MerlinLib test weapon.
 *
 * <h2>How a row is laid out</h2>
 *
 * <p>Every enchantment row is the same four things, left to right: the enchantment name, a number field
 * holding its level, a {@code -} and a {@code +} button, and an {@code X} that removes the row. The name is
 * clipped to the room that is actually left, so a long translation cannot push the controls off the panel.
 *
 * <p>The level can be changed four ways, which is what players expect from a number in a game:
 * <ul>
 *     <li>type a number into the field;</li>
 *     <li>click {@code -} / {@code +} (with shift, by the configured step);</li>
 *     <li>put the wheel over a row's name, which changes that row by {@code editor.scroll_step}, or by
 *     {@code editor.scroll_step_fast} while control is held;</li>
 *     <li>shift and wheel to scroll the list instead, or drag the scrollbar, or wheel over the scrollbar.</li>
 * </ul>
 *
 * <h2>Why nothing is rebuilt while editing</h2>
 *
 * <p>The visible rows are a fixed set of widgets; a click resolves its target from the row index at click
 * time. Rebuilding the screen on every change made the - / + buttons act on the wrong row and reset the
 * order, so that never happens now.
 */
public class ItemEditorScreen extends VanillaScreen {

    /** Enchantment rows visible at once on a window tall enough; the rest is reached by scrolling. */
    private static final int MAX_ROWS = 5;
    /** Fewest enchantment rows a window too short for {@link #MAX_ROWS} still shows. */
    private static final int MIN_ROWS = 1;
    /** Height of one enchantment row. */
    private static final int LIST_ROW = VanillaUi.LIST_ROW_HEIGHT;
    /** Space the header (icon, title, separator) occupies. */
    private static final int HEADER = 26;
    /** Height of a label plus the control under it. */
    private static final int LABEL = 12;
    /** Width of the level number field in a row. */
    private static final int LEVEL_FIELD = 34;
    /** Width of the damage number field. */
    private static final int DAMAGE_FIELD = 64;
    /** Narrowest content that still lays out sensibly. */
    private static final int MIN_CONTENT_WIDTH = 240;

    private final Screen parent;
    private final ItemStack working;
    private final boolean testWeapon;
    private final List<Holder<Enchantment>> rows = new ArrayList<>();

    /** Per visible row: the level field, the - / + / X buttons. */
    private final List<EditBox> levelFields = new ArrayList<>();
    private final List<Button> rowButtons = new ArrayList<>();

    private EditBox nameBox;
    private EditBox damageBox;
    private ScrollArea list;

    private int nameLabelY;
    private int damageLabelY;
    private int listLabelY;
    /** Guards the level fields against being rewritten while they are being typed into. */
    private boolean updatingFields;

    /**
     * @param parent the screen to return to, or {@code null} to resume the game
     * @param stack  the item to edit; it is copied, so cancelling changes nothing
     */
    public ItemEditorScreen(Screen parent, ItemStack stack) {
        super(Component.translatable("gui.merlinlib.editor.title"));
        this.parent = parent;
        this.working = stack.copy();
        this.testWeapon = TestWeapons.isTestWeapon(this.working);
        this.rows.addAll(sorted(currentEnchantments().keySet()));
    }

    // ---------------------------------------------------------------- measured size

    /**
     * Whether the base damage row is shown.
     *
     * <p>A testing weapon always has one - that is what it is for. Any other weapon has one only while
     * {@code tools.allow_non_test_item_damage} is on, because editing the damage of an ordinary weapon
     * changes what vanilla balance and other mods expect.
     *
     * @return {@code true} when the row belongs on this screen
     */
    private boolean showsDamageRow() {
        return TestWeapons.hasEditableDamage(this.working)
                && (this.testWeapon || ConfigManager.server().allowNonTestItemDamage());
    }

    private List<Component> labels() {
        List<Component> labels = new ArrayList<>();
        labels.add(Component.translatable("gui.merlinlib.editor.name"));
        if (this.testWeapon) {
            labels.add(Component.translatable("gui.merlinlib.editor.damage"));
        }
        labels.add(Component.translatable("gui.merlinlib.editor.enchants"));
        labels.add(Component.translatable("gui.merlinlib.editor.hint",
                ConfigManager.client().editorShiftStep(), ConfigManager.client().editorScrollStep(),
                ConfigManager.client().editorScrollStepFast()));
        return labels;
    }

    /** @return the width the right hand controls of an enchantment row need. */
    private static int rowControlsWidth() {
        return LEVEL_FIELD + VanillaUi.GAP + (VanillaUi.STEP_BUTTON + 2) * 3;
    }

    @Override
    protected int contentWidth() {
        Font font = this.font;
        int enchantRow = 90 + rowControlsWidth();
        int damageRow = VanillaUi.STEP_BUTTON * 2 + VanillaUi.GAP * 2 + DAMAGE_FIELD;
        int widestLabel = VanillaUi.labelWidth(font, labels());
        return Math.max(MIN_CONTENT_WIDTH, Math.max(widestLabel + 8, Math.max(enchantRow, damageRow)));
    }

    @Override
    protected int contentHeight() {
        int height = HEADER;
        height += LABEL + VanillaUi.WIDGET_HEIGHT + 6;                 // name
        if (showsDamageRow()) {
            height += LABEL + VanillaUi.WIDGET_HEIGHT + 6;             // damage
        }
        height += LABEL + visibleRows() * LIST_ROW + 6;                // enchantment list
        height += VanillaUi.WIDGET_HEIGHT + 6;                         // add / clear
        height += VanillaUi.WIDGET_HEIGHT;                             // done / cancel
        return Math.min(height, contentHeightLimit());
    }

    /**
     * How many enchantment rows fit at the current window height.
     *
     * <p>Five is what the screen is designed for, but a high GUI scale leaves less room than that, and a list
     * taller than the window pushed the hint line and the bottom buttons off the screen. The list shrinks
     * instead, and the scroll area reaches whatever rows are left.
     *
     * @return the number of rows to show, between {@link #MIN_ROWS} and {@link #MAX_ROWS}
     */
    private int visibleRows() {
        int fixed = HEADER
                + LABEL + VanillaUi.WIDGET_HEIGHT + 6
                + (showsDamageRow() ? LABEL + VanillaUi.WIDGET_HEIGHT + 6 : 0)
                + LABEL + 6
                + VanillaUi.WIDGET_HEIGHT + 6
                + 10 + 6
                + VanillaUi.WIDGET_HEIGHT;
        int room = contentHeightLimit() - fixed;
        return VanillaUi.clamp(room / LIST_ROW, MIN_ROWS, MAX_ROWS);
    }

    @Override
    protected Screen parentScreen() {
        return this.parent;
    }

    // ---------------------------------------------------------------- build

    @Override
    protected void build(PanelLayout layout) {
        int left = layout.left();
        int content = layout.contentWidth();

        this.levelFields.clear();
        this.rowButtons.clear();

        layout.gap(HEADER);

        this.nameLabelY = layout.cursor();
        layout.gap(LABEL);
        int nameY = layout.row(VanillaUi.WIDGET_HEIGHT);
        this.nameBox = VanillaUi.field(this.font, left, nameY, content,
                Component.translatable("gui.merlinlib.editor.name"), displayName(), 64);
        this.addRenderableWidget(this.nameBox);
        layout.gap(6);

        if (showsDamageRow()) {
            this.damageLabelY = layout.cursor();
            layout.gap(LABEL);
            int damageY = layout.row(VanillaUi.WIDGET_HEIGHT);
            this.damageBox = VanillaUi.field(this.font, left + VanillaUi.STEP_BUTTON + 4, damageY,
                    content - (VanillaUi.STEP_BUTTON + 4) * 2,
                    Component.translatable("gui.merlinlib.editor.damage"),
                    Integer.toString(TestWeapons.weaponDamage(this.working).orElse(1)), 10);
            this.addRenderableWidget(this.damageBox);
            this.addRenderableWidget(VanillaUi.button(Component.literal("-"),
                    button -> stepDamage(-1, Minecraft.getInstance().hasShiftDown()),
                    left, damageY, VanillaUi.STEP_BUTTON));
            this.addRenderableWidget(VanillaUi.button(Component.literal("+"),
                    button -> stepDamage(1, Minecraft.getInstance().hasShiftDown()),
                    left + content - VanillaUi.STEP_BUTTON, damageY, VanillaUi.STEP_BUTTON));
            layout.gap(6);
        }

        this.listLabelY = layout.cursor();
        layout.gap(LABEL);

        // The bottom of the panel is laid out first and the list keeps what is left between the label and it.
        // Walking the cursor down instead put the hint and the two buttons below the panel's own bottom edge
        // whenever the window could not hold the whole layout, which is what made them overlap each other and
        // stick out of the panel at a high GUI scale. The hint's height is measured, not assumed: it wraps, and
        // a language whose hint needs two lines gets room for two lines.
        // The hint line that used to sit above these buttons is gone: it explained the wheel and the step keys,
        // which the screen answers to directly anyway, and it cost a row of height on every layout. The buttons
        // stay anchored to the panel's own bottom edge and the list takes the room above them.
        int bottom = layout.contentBottom() - VanillaUi.WIDGET_HEIGHT;
        int actionY = bottom - 6 - VanillaUi.WIDGET_HEIGHT;
        int listTop = layout.cursor();
        int listHeight = Math.max(LIST_ROW, Math.min(visibleRows() * LIST_ROW, actionY - 6 - listTop));

        this.list = new ScrollArea(left, listTop, content, listHeight, LIST_ROW);
        this.list.setContentHeight(this.rows.size() * LIST_ROW);

        int half = layout.sliceWidth(2);
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.editor.add_enchant"),
                button -> Minecraft.getInstance().setScreenAndShow(new EnchantSelectionScreen(this,
                        this::addEnchantment, this.working)),
                left, actionY, half));
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.editor.clear_enchant"),
                button -> Minecraft.getInstance().setScreenAndShow(new DeleteEnchantsConfirmScreen(this,
                        this::clearEnchantments)),
                layout.sliceX(1, 2), actionY, half));

        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.done"), button -> this.save(),
                left, bottom, half));
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.cancel"), button -> this.onClose(),
                layout.sliceX(1, 2), bottom, half));

        buildRowControls();
        layoutRows();
    }

    /**
     * Creates the level field and the three buttons of every visible row, once.
     *
     * <p>Each control is bound to a <em>slot</em>, not to an enchantment: the slot is turned into a row index
     * when it is used, which is what keeps scrolling from corrupting the targets.
     */
    private void buildRowControls() {
        int x = this.list.x() + this.list.rowWidth() - rowControlsWidth();
        for (int slot = 0; slot < visibleRows(); slot++) {
            final int index = slot;
            int fieldX = x;
            EditBox field = new EditBox(this.font, fieldX, 0, LEVEL_FIELD, 16,
                    Component.translatable("gui.merlinlib.editor.level"));
            field.setMaxLength(10);
            field.setResponder(text -> onLevelTyped(index, text));
            this.levelFields.add(this.addRenderableWidget(field));

            int buttonX = fieldX + LEVEL_FIELD + VanillaUi.GAP;
            this.rowButtons.add(this.addRenderableWidget(VanillaUi.compact(Component.literal("-"),
                    button -> stepLevel(index, -1, Minecraft.getInstance().hasShiftDown()), buttonX, 0)));
            this.rowButtons.add(this.addRenderableWidget(VanillaUi.compact(Component.literal("+"),
                    button -> stepLevel(index, 1, Minecraft.getInstance().hasShiftDown()),
                    buttonX + VanillaUi.STEP_BUTTON + 2, 0)));
            this.rowButtons.add(this.addRenderableWidget(VanillaUi.compact(Component.literal("X"),
                    button -> deleteRow(index),
                    buttonX + (VanillaUi.STEP_BUTTON + 2) * 2, 0)));
        }
    }

    /** Moves the row controls to follow the scroll position and hides the rows beyond the viewport. */
    private void layoutRows() {
        if (this.list == null) {
            return;
        }
        int firstRow = this.list.firstRow();
        this.updatingFields = true;
        for (int slot = 0; slot < visibleRows(); slot++) {
            int index = firstRow + slot;
            // Visible through the list's own test, not just "is there such a row": the list is shortened when
            // the window cannot hold the whole layout, and a row that the list no longer shows must take its
            // controls with it - otherwise the extra controls sat on top of the buttons underneath the list.
            boolean visible = index < this.rows.size() && this.list.rowVisible(index);
            int rowY = this.list.y() + slot * LIST_ROW;
            // Centred in the row: the field and the buttons next to it have different heights, so a shared
            // top edge made the row look crooked.
            int y = rowY + (LIST_ROW - 16) / 2;
            EditBox field = this.levelFields.get(slot);
            field.setY(y);
            field.setHeight(16);
            field.visible = visible;
            field.active = visible;
            if (visible) {
                String level = Integer.toString(currentEnchantments().getLevel(this.rows.get(index)));
                if (!level.equals(field.getValue())) {
                    field.setValue(level);
                }
            } else {
                field.setValue("");
            }
            for (int column = 0; column < 3; column++) {
                Button button = this.rowButtons.get(slot * 3 + column);
                button.setY(rowY + (LIST_ROW - VanillaUi.STEP_BUTTON) / 2);
                button.visible = visible;
                button.active = visible;
            }
        }
        this.updatingFields = false;
    }

    // ---------------------------------------------------------------- data

    /**
     * The enchantments of the working stack, taken from the component that carries them.
     *
     * @return the current enchantments, never {@code null}
     */
    private ItemEnchantments currentEnchantments() {
        ItemEnchantments stored = this.working.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null) {
            return stored;
        }
        ItemEnchantments applied = this.working.get(DataComponents.ENCHANTMENTS);
        return applied == null ? ItemEnchantments.EMPTY : applied;
    }

    private void writeEnchantments(ItemEnchantments value) {
        if (this.working.has(DataComponents.STORED_ENCHANTMENTS)) {
            this.working.set(DataComponents.STORED_ENCHANTMENTS, value);
        } else {
            this.working.set(DataComponents.ENCHANTMENTS, value);
        }
        if (this.list != null) {
            this.list.setContentHeight(this.rows.size() * LIST_ROW);
        }
        layoutRows();
    }

    private static List<Holder<Enchantment>> sorted(Collection<Holder<Enchantment>> holders) {
        List<Holder<Enchantment>> copy = new ArrayList<>(holders);
        copy.sort(Comparator.comparing(holder -> holder.value().description().getString()));
        return copy;
    }

    /**
     * Resolves a visible slot to the enchantment it currently shows.
     *
     * @param slot the visible row index
     * @return the holder, or {@code null} when the slot is past the end of the list
     */
    private Holder<Enchantment> resolve(int slot) {
        int index = this.list.firstRow() + slot;
        return index >= 0 && index < this.rows.size() ? this.rows.get(index) : null;
    }

    private void onLevelTyped(int slot, String text) {
        if (this.updatingFields) {
            return;
        }
        Holder<Enchantment> holder = resolve(slot);
        if (holder == null) {
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        // The same rule as the damage field: a level typed above everything is the largest allowed level, not a
        // number to throw away. clampLevel() then applies the enchantment's own maximum on top of it.
        int wanted = VanillaUi.parseClamped(trimmed, currentEnchantments().getLevel(holder), 1, Integer.MAX_VALUE);
        int level = clampLevel(holder, wanted);
        if (level != currentEnchantments().getLevel(holder)) {
            setLevel(holder, level);
        }
    }

    private void stepLevel(int slot, int direction, boolean shift) {
        int step = shift ? Math.max(1, ConfigManager.client().editorShiftStep()) : 1;
        stepLevelBy(slot, direction, step);
    }

    /**
     * Changes the level of the enchantment in one visible row.
     *
     * <p>The result is held at 1: a level control that silently deletes the row once it reaches the bottom
     * is a trap, and removing a row is what the X button and "remove all" are for.
     *
     * @param slot      the visible row
     * @param direction {@code +1} to raise, {@code -1} to lower
     * @param step      how much to change, at least 1
     */
    private void stepLevelBy(int slot, int direction, int step) {
        Holder<Enchantment> holder = resolve(slot);
        if (holder == null) {
            return;
        }
        int wanted = currentEnchantments().getLevel(holder) + direction * Math.max(1, step);
        setLevel(holder, Math.max(1, wanted));
    }

    private void setLevel(Holder<Enchantment> holder, int level) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(currentEnchantments());
        mutable.set(holder, clampLevel(holder, level));
        writeEnchantments(mutable.toImmutable());
    }

    private void remove(Holder<Enchantment> holder) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(currentEnchantments());
        mutable.removeIf(candidate -> candidate.equals(holder));
        this.rows.remove(holder);
        writeEnchantments(mutable.toImmutable());
    }

    private void addEnchantment(EnchantmentLevel selection) {
        Holder<Enchantment> holder = selection.enchantment();
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(currentEnchantments());
        mutable.set(holder, clampLevel(holder, selection.level()));
        if (!this.rows.contains(holder)) {
            // Appended, never re-sorted: sorting on every add made the rows already on screen jump to new
            // places, which reads as "the order changed by itself" while editing.
            this.rows.add(holder);
        }
        writeEnchantments(mutable.toImmutable());
    }

    private void deleteRow(int slot) {
        Holder<Enchantment> holder = resolve(slot);
        if (holder != null) {
            remove(holder);
        }
    }

    private void clearEnchantments() {
        this.rows.clear();
        writeEnchantments(ItemEnchantments.EMPTY);
    }

    /**
     * The highest level the given enchantment may be set to.
     *
     * @param holder the enchantment
     * @param level  the requested level
     * @return the allowed level, at least 1
     */
    private int clampLevel(Holder<Enchantment> holder, int level) {
        int cap = Math.max(1, ConfigManager.server().maxEnchantmentLevel());
        int maximum = ConfigManager.server().allowLevelsAboveMax()
                ? cap
                : Math.min(cap, Math.max(1, holder.value().getMaxLevel()));
        return VanillaUi.clamp(level, 1, maximum);
    }

    private void stepDamage(int direction, boolean shift) {
        if (this.damageBox == null) {
            return;
        }
        int step = shift ? Math.max(1, ConfigManager.client().editorShiftStep()) : 1;
        int maximum = Math.max(1, ConfigManager.server().maxTestWeaponDamage());
        // Read with the ceiling in hand: a value typed above it is the maximum, not an unreadable number that
        // would otherwise be replaced by the fallback of 1.
        int current = VanillaUi.parseClamped(this.damageBox.getValue(), 1, 0, maximum);
        this.damageBox.setValue(Integer.toString(VanillaUi.clamp(current + direction * step, 0, maximum)));
    }

    private String displayName() {
        Component custom = this.working.get(DataComponents.CUSTOM_NAME);
        return custom != null ? custom.getString() : this.working.getHoverName().getString();
    }

    private void save() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            this.onClose();
            return;
        }
        if (!MerlinScreens.mayUseToolsLocally(player)) {
            // Refused silently: the reason is already in the log, and a chat message on every attempt is noise.
            this.onClose();
            return;
        }
        String typed = this.nameBox == null ? "" : this.nameBox.getValue().trim();
        if (typed.isEmpty() || typed.equals(this.working.getHoverName().getString())) {
            this.working.remove(DataComponents.CUSTOM_NAME);
        } else {
            this.working.set(DataComponents.CUSTOM_NAME, Component.literal(typed));
        }
        if (showsDamageRow() && this.damageBox != null) {
            // The typed value is clamped here as well as in the - / + buttons: typing bypasses the buttons,
            // and a value above the ceiling would be a way around the limit the server advertises. The
            // ceiling itself is whatever server.toml says, up to the integer limit.
            int maximum = Math.max(1, ConfigManager.server().maxTestWeaponDamage());
            // Typing 2147483648 used to end up as 1 point of damage, because the ceiling is the integer limit and
            // anything above it failed to parse. One more than the maximum is a request for the maximum.
            int damage = VanillaUi.parseClamped(this.damageBox.getValue(), 1, 0, maximum);
            TestWeapons.setWeaponDamage(this.working, damage);
        }
        // The stack is applied locally so the hand updates immediately, and sent to the server, which is the
        // only side that can persist it: a client side change alone is replaced by the next inventory sync.
        player.setItemInHand(InteractionHand.MAIN_HAND, this.working);
        ServerSender.send(new ItemEditPayload(this.working));
        minecraft.setScreenAndShow(this.parent);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == MerlinUi.KEY_ENTER || event.key() == MerlinUi.KEY_KP_ENTER) {
            this.save();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.list != null && this.list.mouseClicked(event)) {
            layoutRows();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.list != null && this.list.mouseDragged(event)) {
            layoutRows();
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

    /**
     * The wheel changes the level of the row under the cursor, or scrolls the list.
     *
     * <p>Over a row's name the wheel changes that row's level by {@code editor.scroll_step}, or by
     * {@code editor.scroll_step_fast} while control is held - the two switches shown in the hint line. Over
     * the scrollbar, over the empty part below the rows, and with shift held anywhere in the region the
     * wheel scrolls the list instead, so a long list is still reached with the wheel alone.
     *
     * @param mouseX  the cursor x
     * @param mouseY  the cursor y
     * @param scrollX horizontal wheel delta, unused
     * @param scrollY vertical wheel delta, positive when scrolling up
     * @return true when the scroll was handled
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // The damage row answers the wheel too, and it is checked first because the box sits above the list:
        // without this the wheel over the damage number did nothing at all, which is exactly what the hint line
        // next to the row already promised it would do.
        if (scrollY != 0.0D && this.damageBox != null && this.damageBox.visible
                && !Minecraft.getInstance().hasShiftDown()
                && this.damageBox.isMouseOver(mouseX, mouseY)) {
            int step = Minecraft.getInstance().hasControlDown()
                    ? ConfigManager.client().editorScrollStepFast()
                    : ConfigManager.client().editorScrollStep();
            int direction = scrollY > 0.0D ? 1 : -1;
            for (int index = 0; index < Math.max(1, step); index++) {
                stepDamage(direction, false);
            }
            return true;
        }
        if (this.list != null && scrollY != 0.0D
                && !Minecraft.getInstance().hasShiftDown()
                && this.list.contains(mouseX, mouseY)
                && mouseX < this.list.x() + this.list.rowWidth()) {
            int slot = (int) ((mouseY - this.list.y()) / LIST_ROW);
            if (slot >= 0 && slot < visibleRows() && resolve(slot) != null) {
                int step = Minecraft.getInstance().hasControlDown()
                        ? ConfigManager.client().editorScrollStepFast()
                        : ConfigManager.client().editorScrollStep();
                stepLevelBy(slot, scrollY > 0.0D ? 1 : -1, step);
                layoutRows();
                return true;
            }
        }
        if (this.list != null && this.list.mouseScrolled(mouseX, mouseY, scrollY)) {
            layoutRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ---------------------------------------------------------------- drawing

    /** The list is recessed: this has to happen before the widgets, or it covers the row controls. */
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
        int content = layout.contentWidth();

        graphics.item(this.working, left, layout.top() + 2);
        VanillaUi.text(graphics, this.font, this.title, left + 20, layout.top() + 6, VanillaUi.TEXT, content - 20);
        VanillaUi.separator(graphics, left, layout.right(), layout.top() + 20);

        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.editor.name"),
                left, this.nameLabelY, VanillaUi.TEXT_HINT, content);
        if (showsDamageRow()) {
            VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.editor.damage"),
                    left, this.damageLabelY, VanillaUi.TEXT_HINT, content);
        }
        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.editor.enchants"),
                left, this.listLabelY, VanillaUi.TEXT_HINT, content);

        if (this.rows.isEmpty()) {
            VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.editor.no_enchants"),
                    left + 4, this.list.y() + 6, VanillaUi.TEXT_HINT,
                    this.list.rowWidth() - rowControlsWidth() - 8);
        }

        int nameWidth = this.list.rowWidth() - rowControlsWidth() - 8;
        int firstRow = this.list.firstRow();
        for (int slot = 0; slot < visibleRows(); slot++) {
            int index = firstRow + slot;
            if (index >= this.rows.size()) {
                break;
            }
            Holder<Enchantment> holder = this.rows.get(index);
            int rowY = this.list.y() + slot * LIST_ROW + (LIST_ROW - 8) / 2;
            VanillaUi.text(graphics, this.font, holder.value().description(), left + 4, rowY,
                    VanillaUi.TEXT, nameWidth);
        }

        this.list.render(graphics);
    }
}
