// The item editor: rename any item, edit the enchantments of any item, and edit the base damage of a MerlinLib test ...

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

public class ItemEditorScreen extends VanillaScreen {
    private static final int MAX_ROWS = 5;
    private static final int MIN_ROWS = 1;
    private static final int LIST_ROW = VanillaUi.LIST_ROW_HEIGHT;
    private static final int HEADER = 26;
    private static final int LABEL = 12;
    private static final int LEVEL_FIELD = 34;
    private static final int DAMAGE_FIELD = 64;
    private static final int MIN_CONTENT_WIDTH = 240;
    private final Screen parent;
    private final ItemStack working;
    private final boolean testWeapon;
    private final List<Holder<Enchantment>> rows = new ArrayList<>();
    private final List<EditBox> levelFields = new ArrayList<>();
    private final List<Button> rowButtons = new ArrayList<>();
    private EditBox nameBox;
    private EditBox damageBox;
    private ScrollArea list;
    private int nameLabelY;
    private int damageLabelY;
    private int listLabelY;
    private boolean updatingFields;

    public ItemEditorScreen(Screen parent, ItemStack stack) {
        super(Component.translatable("gui.merlinlib.editor.title"));
        this.parent = parent;
        this.working = stack.copy();
        this.testWeapon = TestWeapons.isTestWeapon(this.working);
        this.rows.addAll(sorted(currentEnchantments().keySet()));
    }

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
        height += LABEL + VanillaUi.WIDGET_HEIGHT + 6;
        if (showsDamageRow()) {
            height += LABEL + VanillaUi.WIDGET_HEIGHT + 6;
        }
        height += LABEL + visibleRows() * LIST_ROW + 6;
        height += VanillaUi.WIDGET_HEIGHT + 6;
        height += VanillaUi.WIDGET_HEIGHT;
        return Math.min(height, contentHeightLimit());
    }

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

    private void layoutRows() {
        if (this.list == null) {
            return;
        }
        int firstRow = this.list.firstRow();
        this.updatingFields = true;
        for (int slot = 0; slot < visibleRows(); slot++) {
            int index = firstRow + slot;
            boolean visible = index < this.rows.size() && this.list.rowVisible(index);
            int rowY = this.list.y() + slot * LIST_ROW;
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
            int maximum = Math.max(1, ConfigManager.server().maxTestWeaponDamage());
            int damage = VanillaUi.parseClamped(this.damageBox.getValue(), 1, 0, maximum);
            TestWeapons.setWeaponDamage(this.working, damage);
        }

        player.setItemInHand(InteractionHand.MAIN_HAND, this.working);
        ServerSender.send(new ItemEditPayload(this.working));
        minecraft.setScreenAndShow(this.parent);
    }

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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
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

    @Override
    protected void drawContentBackground(GuiGraphicsExtractor graphics) {
        if (this.list == null) {
            return;
        }
        VanillaUi.listArea(graphics, this.list.x(), this.list.y(), this.list.width(), this.list.height());
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
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
