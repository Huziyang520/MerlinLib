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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The item editor: rename any item, edit the enchantments of any item, and edit the base damage of a
 * MerlinLib test weapon.
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
    /**
     * The enchantment rows, sorted by name.
 */
    private final List<Enchantment> rows = new ArrayList<>();

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
                    button -> stepDamage(-1, hasShiftDown()),
                    left, damageY, VanillaUi.STEP_BUTTON));
            this.addRenderableWidget(VanillaUi.button(Component.literal("+"),
                    button -> stepDamage(1, hasShiftDown()),
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
        int actionY = bottom - VanillaUi.GAP - VanillaUi.WIDGET_HEIGHT;
        int listTop = layout.cursor();
        int listHeight = Math.max(LIST_ROW, Math.min(visibleRows() * LIST_ROW, actionY - VanillaUi.GAP - listTop));

        this.list = new ScrollArea(left, listTop, content, listHeight, LIST_ROW);
        this.list.setContentHeight(this.rows.size() * LIST_ROW);

        // Two rows of two full width buttons: the left column starts at the content's left edge and the right
        // column at the second half, so both rows are the same size and symmetric about the panel's centre.
        // A capped 120 px pair against the right edge left half the panel empty between them.
        int wide = layout.sliceWidth(2);
        int rightColumn = layout.sliceX(1, 2);
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.editor.add_enchant"),
                button -> Minecraft.getInstance().setScreen(new EnchantSelectionScreen(this,
                        this::addEnchantment, this.working)),
                left, actionY, wide));
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.editor.clear_enchant"),
                button -> Minecraft.getInstance().setScreen(new DeleteEnchantsConfirmScreen(this,
                        this::clearEnchantments)),
                rightColumn, actionY, wide));

        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.done"), button -> this.save(),
                left, bottom, wide));
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.cancel"), button -> this.onClose(),
                rightColumn, bottom, wide));

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
                    button -> stepLevel(index, -1, hasShiftDown()), buttonX, 0)));
            this.rowButtons.add(this.addRenderableWidget(VanillaUi.compact(Component.literal("+"),
                    button -> stepLevel(index, 1, hasShiftDown()),
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
                String level = Integer.toString(levelOf(this.rows.get(index)));
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
     * The enchantments of the working stack, as a live map from enchantment to level.
 *
     * <p>The one behavioural difference is that this version's helper drops entries whose id no longer
     * resolves in the registry. On 1.21 a stale entry survived in the component until the player removed
     * it; here it is simply not reported. That is a strictly safer outcome - a row the player cannot see
     * is a row the editor cannot rewrite - and it cannot be reproduced without hand walking the NBT list,
     * which would trade a registry lookup for a raw tag dependency.
 *
     * @return the current enchantments, never {@code null}
 */
    private Map<Enchantment, Integer> currentEnchantments() {
        return EnchantmentHelper.getEnchantments(this.working);
    }

    /**
     * Writes the enchantments of the working stack back to the item.
 *
     * @param value the enchantments to store, possibly empty
 */
    private void writeEnchantments(Map<Enchantment, Integer> value) {
        if (this.working.is(Items.ENCHANTED_BOOK)) {
            this.working.removeTagKey(EnchantedBookItem.TAG_STORED_ENCHANTMENTS);
            for (Map.Entry<Enchantment, Integer> entry : value.entrySet()) {
                EnchantedBookItem.addEnchantment(this.working,
                        new EnchantmentInstance(entry.getKey(), entry.getValue()));
            }
        } else {
            EnchantmentHelper.setEnchantments(value, this.working);
        }
        if (this.list != null) {
            this.list.setContentHeight(this.rows.size() * LIST_ROW);
        }
        layoutRows();
    }

    /**
     * Sorts a set of enchantments by their translated name.
 *
     * @param enchantments the enchantments to sort
     * @return a new list, sorted by name
 */
    private static List<Enchantment> sorted(java.util.Collection<Enchantment> enchantments) {
        List<Enchantment> copy = new ArrayList<>(enchantments);
        copy.sort(java.util.Comparator.comparing(
                enchantment -> Component.translatable(enchantment.getDescriptionId()).getString()));
        return copy;
    }

    /**
     * Resolves a visible slot to the enchantment it currently shows.
     *
     * @param slot the visible row index
     * @return the enchantment, or {@code null} when the slot is past the end of the list
 */
    private Enchantment resolve(int slot) {
        int index = this.list.firstRow() + slot;
        return index >= 0 && index < this.rows.size() ? this.rows.get(index) : null;
    }

    /** @return the level currently stored for an enchantment, 0 when it is not on the item. */
    private int levelOf(Enchantment enchantment) {
        return currentEnchantments().getOrDefault(enchantment, 0);
    }

    private void onLevelTyped(int slot, String text) {
        if (this.updatingFields) {
            return;
        }
        Enchantment enchantment = resolve(slot);
        if (enchantment == null) {
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        // The same rule as the damage field: a level typed above everything is the largest allowed level, not a
        // number to throw away. clampLevel() then applies the enchantment's own maximum on top of it.
        int wanted = VanillaUi.parseClamped(trimmed, levelOf(enchantment), 1, Integer.MAX_VALUE);
        int level = clampLevel(enchantment, wanted);
        if (level != levelOf(enchantment)) {
            setLevel(enchantment, level);
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
        Enchantment enchantment = resolve(slot);
        if (enchantment == null) {
            return;
        }
        int wanted = levelOf(enchantment) + direction * Math.max(1, step);
        setLevel(enchantment, Math.max(1, wanted));
    }

    private void setLevel(Enchantment enchantment, int level) {
        Map<Enchantment, Integer> updated = new LinkedHashMap<>(currentEnchantments());
        updated.put(enchantment, clampLevel(enchantment, level));
        writeEnchantments(updated);
    }

    private void remove(Enchantment enchantment) {
        Map<Enchantment, Integer> updated = new LinkedHashMap<>(currentEnchantments());
        updated.remove(enchantment);
        this.rows.remove(enchantment);
        writeEnchantments(updated);
    }

    private void addEnchantment(EnchantmentLevel selection) {
        Enchantment enchantment = selection.enchantment();
        Map<Enchantment, Integer> updated = new LinkedHashMap<>(currentEnchantments());
        updated.put(enchantment, clampLevel(enchantment, selection.level()));
        if (!this.rows.contains(enchantment)) {
            // Appended, never re-sorted: sorting on every add made the rows already on screen jump to new
            // places, which reads as "the order changed by itself" while editing.
            this.rows.add(enchantment);
        }
        writeEnchantments(updated);
    }

    private void deleteRow(int slot) {
        Enchantment enchantment = resolve(slot);
        if (enchantment != null) {
            remove(enchantment);
        }
    }

    private void clearEnchantments() {
        this.rows.clear();
        writeEnchantments(new LinkedHashMap<>());
    }

    /**
     * The highest level the given enchantment may be set to.
     *
     * @param enchantment the enchantment
     * @param level       the requested level
     * @return the allowed level, at least 1
 */
    private int clampLevel(Enchantment enchantment, int level) {
        int cap = Math.max(1, ConfigManager.server().maxEnchantmentLevel());
        int maximum = ConfigManager.server().allowLevelsAboveMax()
                ? cap
                : Math.min(cap, Math.max(1, enchantment.getMaxLevel()));
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

    /**
     * The name to seed the field with.
 *
     * @return the custom name, or the item's own name when it has none
 */
    private String displayName() {
        return this.working.getHoverName().getString();
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
        // Vanilla's own rule from the anvil: a name equal to the item's original name is not a custom name at
        // all, so it is cleared rather than stored. The original is read off a throwaway copy with the custom
        // name removed, because getHoverName() on this very stack reports the custom name when there is one -
        ItemStack bare = this.working.copy();
        bare.resetHoverName();
        if (typed.isEmpty() || typed.equals(bare.getHoverName().getString())) {
            this.working.resetHoverName();
        } else {
            this.working.setHoverName(Component.literal(typed));
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
        minecraft.setScreen(this.parent);
    }

    // ---------------------------------------------------------------- input

    /**
     * Confirms the edit on Enter, the way a vanilla dialog does.
 *
     * @param keyCode   the GLFW key code the event carries
     * @param scanCode  the platform scan code, unused
     * @param modifiers the modifier mask, unused
     * @return {@code true} when the key was consumed
 */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == MerlinUi.KEY_ENTER || keyCode == MerlinUi.KEY_KP_ENTER) {
            this.save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Offers a click to the scrollbar before the widgets see it.
 *
     * @param mouseX the cursor x
     * @param mouseY the cursor y
     * @param button the mouse button
     * @return {@code true} when the click was consumed
 */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.list != null && this.list.mouseClicked(mouseX, mouseY, button)) {
            layoutRows();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Continues a scrollbar drag.
 *
     * @param mouseX the cursor x
     * @param mouseY the cursor y
     * @param button the mouse button
     * @param dragX  the x delta, forwarded but unused
     * @param dragY  the y delta, forwarded but unused
     * @return {@code true} when the drag was consumed
 */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.list != null && this.list.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            layoutRows();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * Ends a scrollbar drag.
 *
     * @param mouseX the cursor x
     * @param mouseY the cursor y
     * @param button the mouse button
     * @return {@code true} when the release was consumed
 */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.list != null && this.list.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
     * @param scrollY the wheel delta, positive when scrolling up
     * @return true when the scroll was handled
 */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        // The damage row answers the wheel too, and it is checked first because the box sits above the list:
        // without this the wheel over the damage number did nothing at all, which is exactly what the hint line
        // next to the row already promised it would do.
        if (scrollY != 0.0D && this.damageBox != null && this.damageBox.visible
                && !hasShiftDown()
                && this.damageBox.isMouseOver(mouseX, mouseY)) {
            int step = hasControlDown()
                    ? ConfigManager.client().editorScrollStepFast()
                    : ConfigManager.client().editorScrollStep();
            int direction = scrollY > 0.0D ? 1 : -1;
            for (int index = 0; index < Math.max(1, step); index++) {
                stepDamage(direction, false);
            }
            return true;
        }
        if (this.list != null && scrollY != 0.0D
                && !hasShiftDown()
                && this.list.contains(mouseX, mouseY)
                && mouseX < this.list.x() + this.list.rowWidth()) {
            int slot = (int) ((mouseY - this.list.y()) / LIST_ROW);
            if (slot >= 0 && slot < visibleRows() && resolve(slot) != null) {
                int step = hasControlDown()
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
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    // ---------------------------------------------------------------- drawing

    /** The list is recessed: this has to happen before the widgets, or it covers the row controls. */
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
        int content = layout.contentWidth();

        // operation is GuiGraphics#renderItem(ItemStack, int, int); there is no "item" method on this
        // version's GuiGraphics at all, which is why this is a rename rather than an overload choice.
        graphics.renderItem(this.working, left, layout.top() + 2);
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
            Enchantment enchantment = this.rows.get(index);
            int rowY = this.list.y() + slot * LIST_ROW + (LIST_ROW - 8) / 2;
            VanillaUi.text(graphics, this.font,
                    Component.translatable(enchantment.getDescriptionId()), left + 4, rowY,
                    VanillaUi.TEXT, nameWidth);
        }

        this.list.render(graphics);
    }
}
