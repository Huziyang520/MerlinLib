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

/**
 * The health editor: edits the current and maximum health of one living entity.
 *
 * <p>Opened by sneak-using an entity, or yourself while holding the health editor item when the server
 * asks for it. Values are whole numbers from 0 to the integer limit; a field that does not hold one is
 * outlined in red, its reason is printed under the fields, and the confirm button is switched off, so an
 * invalid edit cannot be sent at all. The write still happens on the server, which re-validates permission
 * and clamps the values before touching the entity.
 */
public class HealthEditorScreen extends VanillaScreen {

    /** Narrowest content that still lays out sensibly. */
    private static final int MIN_CONTENT_WIDTH = 208;
    /** Width of a value field. */
    private static final int FIELD_WIDTH = 100;
    /** Label block: label line, gap, field. */
    private static final int FIELD_BLOCK = 10 + 2 + VanillaUi.WIDGET_HEIGHT + 6;

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

    /**
     * @param parent the screen to return to
     * @param target the entity to edit; only this entity is touched
     */
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
        return 22                        // header: title and target
                + FIELD_BLOCK * 2        // current and maximum
                + 10 + 10 + 10           // limit, hint and error lines
                + 6 + VanillaUi.WIDGET_HEIGHT;
    }

    @Override
    protected void build(PanelLayout layout) {
        int left = layout.left();
        int content = layout.contentWidth();
        int fieldX = layout.right() - FIELD_WIDTH;

        layout.gap(22);

        this.currentLabelY = layout.cursor();
        int currentY = layout.row(10 + 2 + VanillaUi.WIDGET_HEIGHT);
        this.currentBox = VanillaUi.field(this.font, fieldX, currentY, FIELD_WIDTH,
                Component.translatable("gui.merlinlib.health.current"),
                Integer.toString(Math.round(this.target.getHealth())), 10);
        this.currentBox.setResponder(text -> validate());
        this.addRenderableWidget(this.currentBox);
        layout.gap(6);

        this.maxLabelY = layout.cursor();
        int maxY = layout.row(10 + 2 + VanillaUi.WIDGET_HEIGHT);
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

    /** Re-reads both fields and switches the confirm button on only when both hold a valid value. */
    private void validate() {
        boolean currentOk = parseHealth(this.currentBox == null ? null : this.currentBox.getValue()) != null;
        boolean maxOk = parseHealth(this.maxBox == null ? null : this.maxBox.getValue()) != null;
        this.valid = currentOk && maxOk;
        if (this.confirm != null) {
            this.confirm.active = this.valid;
        }
    }

    /**
     * Parses a health value: whole numbers only, from 0 to the integer limit.
     *
     * @param text the field text, possibly {@code null} while the screen is being built
     * @return the value, or {@code null} when the text is not acceptable
     */
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
            // Refused silently: the reason is already in the log, and a chat message on every attempt is noise.
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

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
            outline(graphics, this.currentBox, parseHealth(this.currentBox.getValue()) != null);
            outline(graphics, this.maxBox, parseHealth(this.maxBox.getValue()) != null);
        }
    }

    /**
     * Draws a red outline around a field that does not hold a valid value.
     *
     * @param graphics the render state extractor
     * @param box      the field
     * @param ok       whether the field is acceptable
     */
    private static void outline(GuiGraphicsExtractor graphics, EditBox box, boolean ok) {
        if (ok || box == null) {
            return;
        }
        int x = box.getX();
        int y = box.getY();
        int width = box.getWidth();
        int height = box.getHeight();
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, VanillaUi.TEXT_ERROR);
    }
}
