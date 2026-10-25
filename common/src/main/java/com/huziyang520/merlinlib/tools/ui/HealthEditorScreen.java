// The health editor: edits the current and maximum health of one living entity.

package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.network.HealthEditPayload;
import com.huziyang520.merlinlib.network.ServerSender;
import com.huziyang520.merlinlib.tools.ui.vanilla.PanelLayout;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaScreen;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class HealthEditorScreen extends VanillaScreen {
    private static final int MIN_CONTENT_WIDTH = 208;
    private static final int FIELD_WIDTH = 100;
    private static final int HEADER_HEIGHT = 28;
    private static final int FIELD_ROW = VanillaUi.WIDGET_HEIGHT + 8;
    private final Screen parent;
    private final LivingEntity target;
    private EditBox currentBox;
    private EditBox maxBox;
    private Button confirm;
    private int currentLabelY;
    private int maxLabelY;
    private int hintY;
    private int errorY;
    private boolean valid = true;

    public HealthEditorScreen(Screen parent, LivingEntity target) {
        super(Component.translatable("gui.merlinlib.health.title"));
        this.parent = parent;
        this.target = target;
    }

    @Override
    protected Screen parentScreen() {
        return this.parent;
    }

    @Override
    protected int contentWidth() {
        return Math.max(MIN_CONTENT_WIDTH, Math.max(
                VanillaUi.labelWidth(this.font, java.util.List.of(
                        Component.translatable("gui.merlinlib.health.hint"),
                        Component.translatable("gui.merlinlib.health.limit"),
                        Component.translatable("gui.merlinlib.health.invalid"))),
                FIELD_WIDTH + Math.max(this.font.width("999999"), this.font.width("0")) + 8));
    }

    @Override
    protected int contentHeight() {
        return HEADER_HEIGHT
                + FIELD_ROW * 2
                + 10 + 10 + 10
                + 6 + VanillaUi.WIDGET_HEIGHT;
    }

    @Override
    protected void build(PanelLayout layout) {
        int left = layout.left();
        int content = layout.contentWidth();
        int fieldX = layout.right() - FIELD_WIDTH;
        layout.gap(HEADER_HEIGHT);
        this.currentLabelY = layout.cursor() + 6;
        int currentY = layout.row(VanillaUi.WIDGET_HEIGHT);
        this.currentBox = VanillaUi.field(this.font, fieldX, currentY, FIELD_WIDTH,
                Component.translatable("gui.merlinlib.health.current"),
                Integer.toString(Math.round(this.target.getHealth())), 10);
        this.currentBox.setResponder(text -> validate());
        this.addRenderableWidget(this.currentBox);
        layout.gap(6);
        this.maxLabelY = layout.cursor() + 6;
        int maxY = layout.row(VanillaUi.WIDGET_HEIGHT);
        this.maxBox = VanillaUi.field(this.font, fieldX, maxY, FIELD_WIDTH,
                Component.translatable("gui.merlinlib.health.max"),
                Integer.toString(Math.round(this.target.getMaxHealth())), 10);
        this.maxBox.setResponder(text -> validate());
        this.addRenderableWidget(this.maxBox);
        layout.gap(6);
        this.hintY = layout.cursor();
        this.errorY = this.hintY + 10;
        layout.gap(10 + 10 + 10);
        int bottom = layout.row(VanillaUi.WIDGET_HEIGHT);
        int half = layout.sliceWidth(2);
        this.confirm = this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.health.confirm"),
                button -> this.send(), left, bottom, half));
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.cancel"), button -> this.onClose(),
                layout.sliceX(1, 2), bottom, half));

        validate();
    }

    private void validate() {
        boolean currentOk = parseHealth(this.currentBox == null ? null : this.currentBox.getValue()) != null;
        boolean maxOk = parseHealth(this.maxBox == null ? null : this.maxBox.getValue()) != null;
        this.valid = currentOk && maxOk;
        if (this.confirm != null) {
            this.confirm.active = this.valid;
        }
    }

    private static Integer parseHealth(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (!trimmed.matches("\\d{1,10}")) {
            return null;
        }
        try {
            long value = Long.parseLong(trimmed);
            return value >= 0 && value <= Integer.MAX_VALUE ? (int) value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void send() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !this.valid) {
            return;
        }
        if (!MerlinScreens.mayUseToolsLocally(player)) {
            this.onClose();
            return;
        }
        Integer current = parseHealth(this.currentBox.getValue());
        Integer max = parseHealth(this.maxBox.getValue());
        if (current == null || max == null) {
            return;
        }
        ServerSender.send(new HealthEditPayload(this.target.getId(), current, max));
        this.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((event.key() == MerlinUi.KEY_ENTER || event.key() == MerlinUi.KEY_KP_ENTER) && this.valid) {
            this.send();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        PanelLayout layout = layout();
        int left = layout.left();
        int content = layout.contentWidth();
        VanillaUi.text(graphics, this.font, this.title, left, layout.top(), VanillaUi.TEXT, content);
        VanillaUi.text(graphics, this.font,
                Component.translatable("gui.merlinlib.health.target", this.target.getName().getString()),
                left, layout.top() + 12, VanillaUi.TEXT_HINT, content);
        VanillaUi.separator(graphics, left, layout.right(), layout.top() + 24);

        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.health.current"),
                left, this.currentLabelY, VanillaUi.TEXT_HINT, content);
        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.health.max"),
                left, this.maxLabelY, VanillaUi.TEXT_HINT, content);

        VanillaUi.text(graphics, this.font,
                Component.translatable("gui.merlinlib.health.limit", Integer.MAX_VALUE),
                left, this.hintY, VanillaUi.TEXT_HINT, content);
        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.health.hint"),
                left, this.errorY, VanillaUi.TEXT_HINT, content);

        if (!this.valid) {
            VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.health.invalid"),
                    left, this.errorY + 10, VanillaUi.TEXT_ERROR, content);
        }

        tint(this.currentBox, parseHealth(this.currentBox.getValue()) != null);
        tint(this.maxBox, parseHealth(this.maxBox.getValue()) != null);
    }

    private static void tint(EditBox box, boolean ok) {
        if (box != null) {
            box.setTextColor(ok ? EditBox.DEFAULT_TEXT_COLOR : VanillaUi.TEXT_ERROR);
        }
    }
}
