package com.huziyang520.merlinlib.tools.ui;

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
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Second level picker: every enchantment of the running game, one row per level.
 *
 * <p>The list is built when the screen opens and released when it closes, so a world with hundreds of
 * enchantments costs nothing while the player is not picking one.
 *
 * <p>Three behaviours are deliberate:
 * <ul>
 *     <li><b>the filter follows the server switch.</b> With mismatched enchantments disabled the picker only
 *     offers enchantments the target item can really carry, so the screen cannot add an enchantment that
 *     would silently do nothing. With it enabled every registry entry is offered.</li>
 *     <li><b>scrolling never recreates widgets.</b> The row buttons are bound to a slot and resolve their
 *     target at click time, which keeps typing in the search box from losing focus.</li>
 *     <li><b>the list area is painted before the widgets.</b> Painting it afterwards covered the "add"
 *     buttons of every row: they were clickable but invisible.</li>
 * </ul>
 *
 * <h2>What changed / 1.20.1 note</h2>
 *
 * <p>The registry walk survives almost unchanged - {@code RegistryAccess#lookup} and
 * {@code RegistryLookup#listElements()} exist on this version with the same shapes 26.3 used, because the
 * holder lookup layer was introduced before it - but four call sites had to move:
 *
 * <ul>
 *   <li><b>the four mouse methods lost their event object.</b> 26.3 received a {@code MouseButtonEvent}
 *       with {@code x()}, {@code y()} and {@code button()} accessors and hand-wrote the {@code doubleClick}
 *       flag. This version passes the coordinates and the button as separate arguments, and
 *       {@code mouseClicked(double, double, int)} has <b>no</b> double-click argument at all, so the flag
 *       is simply gone rather than reimplemented - the list does not use it. The forwarding into
 *       {@link ScrollArea} is direct, since that class was ported to the same unpacked shape;</li>
 *   <li><b>{@code mouseScrolled} is three arguments, not four.</b> 1.20.1 has no horizontal wheel, so
 *       {@code scrollX} does not exist and the single wheel delta is passed through as {@code scrollY} was
 *       on 26.3;</li>
 *   <li><b>the render pass</b> is {@link #render(GuiGraphics, int, int, float)} rather than
 *       {@code extractRenderState(GuiGraphicsExtractor, ...)}, and {@code drawContentBackground} takes a
 *       {@link GuiGraphics}. Both are pure renames of the parameter type;</li>
 *   <li><b>a registry key is addressed by {@code location()}.</b> 26.3's {@code ResourceKey#identifier()}
 *       is this version's {@code ResourceKey#location()}; the search's id matching therefore reads
 *       {@code key.location().toString()}. The values are identical.</li>
 * </ul>
 *
 * <p>One visible behaviour is version specific and worth naming: the enchantment registry is the plain
 * static one here, <em>not</em> a synced dynamic registry. Every enchantment that exists is therefore
 * visible from this side, which is what the filter below then narrows down.
 */
public class EnchantSelectionScreen extends VanillaScreen {

    /** Rows visible at once on a window tall enough. */
    private static final int MAX_ROWS = 6;
    /** Fewest rows a window too short for {@link #MAX_ROWS} still shows. */
    private static final int MIN_ROWS = 1;
    /** Height of one row. */
    private static final int ROW_HEIGHT = VanillaUi.LIST_ROW_HEIGHT;
    /** Width of the per row add button. */
    private static final int ADD_WIDTH = 46;
    /** Narrowest content that still lays out sensibly. */
    private static final int MIN_CONTENT_WIDTH = 230;
    /** Height of a label plus the control under it. */
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

    /**
     * @param parent the screen to return to
     * @param onPick receives the chosen enchantment and level
     * @param target the item the enchantment will be added to, used for the applicability filter;
     *               {@code null} disables the filter
     */
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
        return Math.min(12 + 4                                       // title and separator
                + LABEL + VanillaUi.WIDGET_HEIGHT + 6              // search label and field
                + visibleRows() * ROW_HEIGHT + 6                   // list
                + 10 + 6                                           // hint
                + VanillaUi.WIDGET_HEIGHT,                         // close button
                contentHeightLimit());
    }

    /**
     * How many rows fit at the current window height.
     *
     * @return the number of rows to show, between {@link #MIN_ROWS} and {@link #MAX_ROWS}
     */
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
            this.rowButtons.add(this.addRenderableWidget(VanillaUi.button(
                    Component.translatable("gui.merlinlib.select.add"),
                    button -> pick(index), this.list.x() + this.list.rowWidth() - ADD_WIDTH, 0, ADD_WIDTH)));
        }
        layoutRows();
    }

    /** Moves the row buttons to follow the scroll position and hides the unused ones. */
    private void layoutRows() {
        if (this.list == null) {
            return;
        }
        int firstRow = this.list.firstRow();
        for (int slot = 0; slot < visibleRows(); slot++) {
            boolean visible = firstRow + slot < this.filtered.size();
            Button button = this.rowButtons.get(slot);
            button.setY(this.list.y() + slot * ROW_HEIGHT);
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

    /**
     * Enumerates the registry once, sorted by name and level so the picker stays stable between openings.
     *
     * <p>Enchantments the target cannot carry are skipped unless the server allows mismatched ones, which is
     * the switch that decides whether this list shows "everything" or "everything that works here".
     *
     * <p><b>What changed / 1.20.1 note:</b> {@code RegistryAccess#lookup} and
     * {@code RegistryLookup#listElements()} are the same calls as on 26.3 and yield the same
     * {@code Holder.Reference} stream, so only the name lookup differs: 1.21's
     * {@code Enchantment#description()} does not exist here, and the display name is built from
     * {@code getDescriptionId()} instead - the same substitution {@link EnchantmentLevel#label()} makes.
     */
    private void buildList() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var lookup = minecraft.level.registryAccess().lookup(Registries.ENCHANTMENT);
        if (lookup.isEmpty()) {
            return;
        }
        boolean onlyApplicable = !ConfigManager.server().allowMismatchedEnchantments() && this.target != null;
        lookup.get().listElements()
                .sorted(Comparator.comparing(holder -> Component.translatable(
                        holder.value().getDescriptionId()).getString()))
                .forEach(holder -> {
                    if (onlyApplicable && !holder.value().canEnchant(this.target)) {
                        return;
                    }
                    int max = Math.max(1, holder.value().getMaxLevel());
                    for (int level = 1; level <= max; level++) {
                        this.all.add(new EnchantmentLevel(holder.value(), level));
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
            if (entry.label().toLowerCase(Locale.ROOT).contains(query)
                    || idOf(enchantment).toLowerCase(Locale.ROOT).contains(query)) {
                this.filtered.add(entry);
            }
        }
    }

    /**
     * The registry id of an enchantment, for the search box.
     *
     * <p><b>What changed / 1.20.1 note:</b> 26.3 read the id off the holder's key
     * ({@code entry.enchantment().unwrapKey().map(key -> key.identifier())}) because the row carried a
     * {@code Holder}. This version's row carries the enchantment itself, so the id has to be looked up.
     *
     * <p>The lookup goes through {@code ForgeRegistries.ENCHANTMENTS} rather than
     * {@code BuiltInRegistries.ENCHANTMENT}: both answer the same question, but Forge deprecated the
     * latter precisely so that modded lookup goes through its own registry, and a deprecation warning
     * from a class that is still perfectly functional is worse than using the one Forge asks for. The
     * two are backed by the same map, so the id returned is identical.
     *
     * @param enchantment the enchantment
     * @return the id as text, or an empty string when the enchantment is not in the registry
     */
    private static String idOf(Enchantment enchantment) {
        net.minecraft.resources.ResourceLocation id =
                net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        return id == null ? "" : id.toString();
    }

    /**
     * Forwards a click to the scrollbar before the widgets see it.
     *
     * <p><b>What changed / 1.20.1 note:</b> the 26.3 signature took one {@code MouseButtonEvent} plus the
     * double-click flag; this version takes the cursor and the button separately, and has no double-click
     * flag to forward.
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
     * Forwards a drag to the scrollbar.
     *
     * <p><b>What changed / 1.20.1 note:</b> the cursor, the button and the two drag deltas are separate
     * arguments here; {@link ScrollArea} was ported to the same shape, so they are passed straight through.
     *
     * @param mouseX the cursor x
     * @param mouseY the cursor y
     * @param button the mouse button
     * @param dragX  the x delta, forwarded but unused by the region
     * @param dragY  the y delta, forwarded but unused by the region
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
     * <p><b>What changed / 1.20.1 note:</b> three separate arguments instead of one event object.
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
     * Scrolls the list with the wheel.
     *
     * <p><b>What changed / 1.20.1 note:</b> this is the three argument overload, not the four argument one
     * this project's mapping table warns about - 1.20.1 has no horizontal wheel, so the single delta is what
     * 26.3 passed as {@code scrollY}.
     *
     * @param mouseX  the cursor x
     * @param mouseY  the cursor y
     * @param scrollY the wheel delta
     * @return {@code true} when the scroll was consumed
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (this.list != null && this.list.mouseScrolled(mouseX, mouseY, scrollY)) {
            layoutRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    /** The list is recessed: this has to happen before the widgets, or it covers the add buttons. */
    @Override
    protected void drawContentBackground(GuiGraphics graphics) {
        if (this.list == null) {
            return;
        }
        VanillaUi.listArea(graphics, this.list.x(), this.list.y(), this.list.width(), this.list.height());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

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
