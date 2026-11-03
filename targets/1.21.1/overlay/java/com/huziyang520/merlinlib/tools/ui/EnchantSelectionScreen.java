// Second level picker: every enchantment of the running game, one row per level.

package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.api.SearchApi;
import com.huziyang520.merlinlib.config.ConfigManager;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public class EnchantSelectionScreen extends VanillaScreen {
    private static final int MAX_ROWS = 6;
    private static final int MIN_ROWS = 1;
    private static final int ROW_HEIGHT = VanillaUi.LIST_ROW_HEIGHT;
    private static final int ADD_WIDTH = 46;
    private static final int ADD_HEIGHT = ROW_HEIGHT - 4;
    private static final int MIN_CONTENT_WIDTH = 230;
    private static final int LABEL = 12;
    private final Screen parent;
    private final Consumer<EnchantmentLevel> onPick;
    private final ItemStack target;
    private final List<EnchantmentLevel> all = new ArrayList<>();
    private final List<EnchantmentLevel> filtered = new ArrayList<>();
    private final List<Button> rowButtons = new ArrayList<>();
    private EditBox search;
    private ScrollArea list;
    private int titleY;
    private int searchLabelY;
    private int hintY;

    public EnchantSelectionScreen(Screen parent, Consumer<EnchantmentLevel> onPick, ItemStack target) {
        super(Component.translatable("gui.merlinlib.select.title"));
        this.parent = parent;
        this.onPick = onPick;
        this.target = target;
        buildList();
        refilter();
    }

    @Override
    protected Screen parentScreen() {
        return this.parent;
    }

    @Override
    protected int contentWidth() {
        Font font = this.font;
        int widest = Math.max(font.width(this.title), font.width(Component.translatable("gui.merlinlib.select.search")));
        return Math.max(MIN_CONTENT_WIDTH, Math.max(widest + 8, 150 + ADD_WIDTH));
    }

    @Override
    protected int contentHeight() {
        return Math.min(12 + 4
                + LABEL + VanillaUi.WIDGET_HEIGHT + 6
                + visibleRows() * ROW_HEIGHT + 6
                + 10 + 6
                + VanillaUi.WIDGET_HEIGHT,
                contentHeightLimit());
    }

    private int visibleRows() {
        int fixed = 12 + 4 + LABEL + VanillaUi.WIDGET_HEIGHT + 6 + 6 + 10 + 6 + VanillaUi.WIDGET_HEIGHT;
        return VanillaUi.clamp((contentHeightLimit() - fixed) / ROW_HEIGHT, MIN_ROWS, MAX_ROWS);
    }

    @Override
    protected void build(PanelLayout layout) {
        int left = layout.left();
        int content = layout.contentWidth();
        this.titleY = layout.row(12);
        layout.gap(4);
        this.searchLabelY = layout.cursor();
        layout.gap(LABEL);
        int searchY = layout.row(VanillaUi.WIDGET_HEIGHT);
        this.search = VanillaUi.field(this.font, left, searchY, content,
                Component.translatable("gui.merlinlib.select.search"),
                this.search == null ? "" : this.search.getValue(), 64);
        this.search.setResponder(text -> {
            refilter();
            if (this.list != null) {
                this.list.setContentHeight(this.filtered.size() * ROW_HEIGHT);
                layoutRows();
            }
        });
        this.addRenderableWidget(this.search);
        layout.gap(6);
        int listY = layout.row(visibleRows() * ROW_HEIGHT);
        this.list = new ScrollArea(left, listY, content, visibleRows() * ROW_HEIGHT, ROW_HEIGHT);
        this.list.setContentHeight(this.filtered.size() * ROW_HEIGHT);
        layout.gap(6);
        this.hintY = layout.cursor();
        layout.gap(10 + 6);
        int bottom = layout.row(VanillaUi.WIDGET_HEIGHT);
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.cancel"), button -> this.onClose(),
                layout.sliceX(1, 2), bottom, layout.sliceWidth(2)));

        this.rowButtons.clear();
        for (int slot = 0; slot < visibleRows(); slot++) {
            final int index = slot;

            this.rowButtons.add(this.addRenderableWidget(VanillaUi.rowButton(
                    Component.translatable("gui.merlinlib.select.add"),
                    button -> pick(index), this.list.x() + this.list.rowWidth() - ADD_WIDTH, 0,
                    ADD_WIDTH, ADD_HEIGHT)));
        }
        layoutRows();
    }

    private void layoutRows() {
        if (this.list == null) {
            return;
        }
        int firstRow = this.list.firstRow();
        for (int slot = 0; slot < visibleRows(); slot++) {
            boolean visible = firstRow + slot < this.filtered.size();
            Button button = this.rowButtons.get(slot);
            button.setY(this.list.y() + slot * ROW_HEIGHT + (ROW_HEIGHT - ADD_HEIGHT) / 2);
            button.visible = visible;
            button.active = visible;
        }
    }

    private void pick(int slot) {
        int index = this.list.firstRow() + slot;
        if (index < 0 || index >= this.filtered.size()) {
            return;
        }
        this.onPick.accept(this.filtered.get(index));
        Minecraft.getInstance().setScreen(this.parent);
    }

    private void buildList() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var lookup = minecraft.level.registryAccess().lookup(Registries.ENCHANTMENT);
        if (lookup.isEmpty()) {
            return;
        }
        this.all.clear();
        Set<String> seen = new HashSet<>();
        boolean onlyApplicable = !ConfigManager.server().allowMismatchedEnchantments() && this.target != null;
        lookup.get().listElements()
                .sorted(Comparator.comparing(holder -> holder.value().description().getString()))
                .forEach(holder -> {
                    if (onlyApplicable && !holder.value().canEnchant(this.target)) {
                        return;
                    }
                    int max = Math.max(1, holder.value().getMaxLevel());
                    for (int level = 1; level <= max; level++) {
                        String enchantmentId = idOf(holder.value());
                        if (enchantmentId.isEmpty()) {
                            enchantmentId = "unnamed:" + holder.value().description().getString();
                        }
                        if (seen.add(enchantmentId + "#" + level)) {
                            this.all.add(new EnchantmentLevel(holder.value(), level));
                        }
                    }
                });
    }

    private void refilter() {
        String query = this.search == null ? "" : this.search.getValue().trim().toLowerCase(Locale.ROOT);
        this.filtered.clear();
        if (query.isEmpty()) {
            this.filtered.addAll(this.all);
            return;
        }
        for (EnchantmentLevel entry : this.all) {
            Enchantment enchantment = entry.enchantment();
            if (SearchApi.matches(entry.label(), query)
                    || idOf(enchantment).toLowerCase(Locale.ROOT).contains(query)) {
                this.filtered.add(entry);
            }
        }
    }

    private static String idOf(Enchantment enchantment) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) {
            return "";
        }
        net.minecraft.resources.ResourceLocation id = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getKey(enchantment);
        return id == null ? "" : id.toString();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.list != null && this.list.mouseClicked(mouseX, mouseY, button)) {
            layoutRows();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.list != null && this.list.mouseScrolled(mouseX, mouseY, scrollY)) {
            layoutRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        int content = layout.contentWidth();
        VanillaUi.text(graphics, this.font, this.title, left, this.titleY, VanillaUi.TEXT, content);
        VanillaUi.separator(graphics, left, layout.right(), this.titleY + 12);
        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.select.search"),
                left, this.searchLabelY, VanillaUi.TEXT_HINT, content);

        if (this.filtered.isEmpty()) {
            Component empty = Component.translatable("gui.merlinlib.select.empty");
            VanillaUi.text(graphics, this.font, empty,
                    left + Math.max(4, (content - this.font.width(empty)) / 2), this.list.y() + 12,
                    VanillaUi.TEXT_HINT, content - 8);
        }

        int nameWidth = this.list.rowWidth() - ADD_WIDTH - 8;
        int firstRow = this.list.firstRow();
        for (int slot = 0; slot < visibleRows(); slot++) {
            int index = firstRow + slot;
            if (index >= this.filtered.size()) {
                break;
            }
            EnchantmentLevel entry = this.filtered.get(index);
            VanillaUi.text(graphics, this.font, Component.literal(entry.label()), left + 4,
                    this.list.y() + slot * ROW_HEIGHT + 6, VanillaUi.TEXT, nameWidth);
        }

        this.list.render(graphics);
        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.select.hint"),
                left, this.hintY, VanillaUi.TEXT_HINT, content);
    }
}
