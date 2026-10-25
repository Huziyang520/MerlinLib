// The join notice switches: one global switch, then one switch per mod that registered a notice, MerlinLib first and the ...

package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.NoticeSettings;
import com.huziyang520.merlinlib.notice.NoticeEntry;
import com.huziyang520.merlinlib.notice.NoticeRegistry;
import com.huziyang520.merlinlib.tools.ui.vanilla.PanelLayout;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaScreen;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class NoticesScreen extends VanillaScreen {
    private static final int MIN_CONTENT_WIDTH = 208;
    private static final int CONTROL_WIDTH = 96;
    private static final int HEADER_HEIGHT = 28;
    private static final int HINT_HEIGHT = 20;
    private static final int ROW = VanillaUi.ROW_STEP;
    private final Screen parent;
    private final List<String> owners = new ArrayList<>();
    private final Map<String, Boolean> perMod = new LinkedHashMap<>();
    private final List<String> rowLabels = new ArrayList<>();
    private final List<Integer> rowYs = new ArrayList<>();
    private boolean enabled;
    private int hintY;
    private int globalLabelY;
    private int hidden;
    private boolean noBusinessMods;
    private int emptyY;

    public NoticesScreen(Screen parent) {
        super(Component.translatable("gui.merlinlib.notices.title"));
        this.parent = parent;
        this.enabled = ConfigManager.server().noticesEnabled();
        Map<String, Boolean> defaults = new LinkedHashMap<>();
        for (NoticeEntry entry : NoticeRegistry.codeNotices()) {
            defaults.putIfAbsent(entry.owner(), entry.enabledByDefault());
        }
        this.owners.addAll(defaults.keySet());
        this.owners.sort((left, right) -> {
            boolean leftIsLibrary = Constants.MOD_ID.equals(left);
            boolean rightIsLibrary = Constants.MOD_ID.equals(right);
            if (leftIsLibrary != rightIsLibrary) {
                return leftIsLibrary ? -1 : 1;
            }
            return left.compareTo(right);
        });
        Map<String, Boolean> stored = NoticeSettings.values();
        for (String owner : this.owners) {
            this.perMod.put(owner, stored.getOrDefault(owner, defaults.get(owner)));
        }
        this.noBusinessMods = this.owners.stream().allMatch(Constants.MOD_ID::equals);
    }

    @Override
    protected Screen parentScreen() {
        return this.parent;
    }

    private int capacity() {
        int reserved = HEADER_HEIGHT + ROW + HINT_HEIGHT + 6 + VanillaUi.WIDGET_HEIGHT
                + (this.noBusinessMods ? ROW : 0);
        return Math.max(0, (contentHeightLimit() - reserved) / ROW);
    }

    private int visible() {
        return Math.min(this.owners.size(), capacity());
    }

    @Override
    protected int contentWidth() {
        List<Component> labels = new ArrayList<>();
        labels.add(this.title);
        labels.add(Component.translatable("gui.merlinlib.notices.enabled"));
        labels.add(Component.translatable("gui.merlinlib.notices.empty"));
        for (String owner : this.owners) {
            labels.add(label(owner));
        }
        return Math.max(MIN_CONTENT_WIDTH,
                VanillaUi.measureWidth(this.font, labels, CONTROL_WIDTH, MIN_CONTENT_WIDTH));
    }

    @Override
    protected int contentHeight() {
        int rows = HEADER_HEIGHT + (1 + visible()) * ROW;
        return rows + (this.noBusinessMods ? ROW : 0) + HINT_HEIGHT + 6 + VanillaUi.WIDGET_HEIGHT;
    }

    @Override
    protected void build(PanelLayout layout) {
        int controlX = layout.right() - CONTROL_WIDTH;
        layout.gap(HEADER_HEIGHT);
        int globalY = layout.row();
        this.globalLabelY = globalY + 6;
        this.addRenderableWidget(toggle(controlX, globalY, CONTROL_WIDTH, this.enabled,
                value -> this.enabled = value));
        layout.gap(ROW - VanillaUi.WIDGET_HEIGHT);
        int shown = visible();
        this.hidden = this.owners.size() - shown;
        this.rowLabels.clear();
        this.rowYs.clear();
        for (int index = 0; index < shown; index++) {
            String owner = this.owners.get(index);
            int y = layout.row();
            this.addRenderableWidget(toggle(controlX, y, CONTROL_WIDTH,
                    Boolean.TRUE.equals(this.perMod.get(owner)), value -> this.perMod.put(owner, value)));
            this.rowLabels.add(owner);
            this.rowYs.add(y + 6);
            layout.gap(ROW - VanillaUi.WIDGET_HEIGHT);
        }

        this.emptyY = layout.cursor();
        if (this.noBusinessMods) {
            layout.gap(ROW);
        }

        this.hintY = layout.cursor();
        layout.gap(HINT_HEIGHT);
        layout.gap(6);
        int footerY = layout.row(VanillaUi.WIDGET_HEIGHT);
        int half = layout.sliceWidth(2);
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.notices.save"),
                button -> this.save(), layout.left(), footerY, half));
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.cancel"),
                button -> this.onClose(), layout.sliceX(1, 2), footerY, half));
    }

    private void save() {
        NoticeSettings.save(new LinkedHashMap<>(this.perMod));
        ConfigManager.saveServer(Map.of("notices.enabled", Boolean.toString(this.enabled)));
        this.onClose();
    }

    private static Button toggle(int x, int y, int width, boolean value, Consumer<Boolean> sink) {
        final boolean[] state = {value};
        final Button[] holder = new Button[1];
        Button button = Button.builder(stateLabel(state[0]), pressed -> {
            state[0] = !state[0];
            holder[0].setMessage(stateLabel(state[0]));
            sink.accept(state[0]);
        }).bounds(x, y, width, VanillaUi.WIDGET_HEIGHT).build();
        holder[0] = button;
        return button;
    }

    private static Component stateLabel(boolean on) {
        return Component.translatable(on ? "gui.merlinlib.notices.on" : "gui.merlinlib.notices.off");
    }

    private static Component label(String owner) {
        return Constants.MOD_ID.equals(owner) ? Component.literal("MerlinLib") : Component.literal(owner);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        PanelLayout layout = layout();
        int left = layout.left();
        int content = layout.contentWidth();
        VanillaUi.text(graphics, this.font, this.title, left, layout.top(), VanillaUi.TEXT, content);
        VanillaUi.separator(graphics, left, layout.right(), layout.top() + 24);

        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.notices.enabled"),
                left, this.globalLabelY, VanillaUi.TEXT, content - CONTROL_WIDTH - VanillaUi.GAP);

        for (int index = 0; index < this.rowYs.size(); index++) {
            VanillaUi.text(graphics, this.font, label(this.rowLabels.get(index)),
                    left, this.rowYs.get(index), VanillaUi.TEXT, content - CONTROL_WIDTH - VanillaUi.GAP);
        }
        if (this.noBusinessMods) {
            VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.notices.empty"),
                    left, this.emptyY, VanillaUi.TEXT_DISABLED, content);
        }

        int hintTop = this.hintY;
        List<String> hint = VanillaUi.wrap(this.font,
                Component.translatable("gui.merlinlib.notices.hint").getString(), content);
        for (int index = 0; index < Math.min(2, hint.size()); index++) {
            VanillaUi.text(graphics, this.font, Component.literal(hint.get(index)),
                    left, hintTop + index * 10, VanillaUi.TEXT_HINT, content);
        }
        if (this.hidden > 0) {
            VanillaUi.textRight(graphics, this.font,
                    Component.translatable("gui.merlinlib.notices.more", this.hidden),
                    layout.right(), hintTop, VanillaUi.TEXT_DISABLED, content);
        }
    }
}
