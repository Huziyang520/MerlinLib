package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.tools.ui.vanilla.PanelLayout;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaScreen;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Confirmation dialog for "remove every enchantment".
 *
 * <p>Small on purpose: one warning line and two buttons. The destructive button is on the left and the
 * cancel button is on the right <em>and</em> focused, so the default action of a stray Enter or Escape is
 * always the harmless one.
 *
 * <h2>What changed / 1.20.1 note</h2>
 *
 * <p>A direct port with the two renames the whole port needs and nothing else:
 *
 * <ul>
 *   <li>the drawing type is {@link GuiGraphics}, not the 26.3 {@code GuiGraphicsExtractor}, and the pass
 *       itself is {@link #render(GuiGraphics, int, int, float)} rather than
 *       {@code extractRenderState}. The body is unchanged, because every helper this screen calls lives in
 *       {@link VanillaUi} and only its first parameter moved;</li>
 *   <li>{@code Component.translatable} and {@code setInitialFocus} are the same calls here. The latter is
 *       still {@code protected} on this version's {@code Screen}, which is what lets the cancel button be
 *       focused from inside {@link #build(PanelLayout)}.</li>
 * </ul>
 */
public class DeleteEnchantsConfirmScreen extends VanillaScreen {

    private static final int BUTTON_WIDTH = 90;
    private static final int MIN_CONTENT_WIDTH = 200;

    private final Screen parent;
    private final Runnable onConfirm;

    /**
     * @param parent    the screen to return to, both after confirming and after cancelling
     * @param onConfirm run when the player confirms; the screen closes itself afterwards
     */
    public DeleteEnchantsConfirmScreen(Screen parent, Runnable onConfirm) {
        super(Component.translatable("gui.merlinlib.confirm.title"));
        this.parent = parent;
        this.onConfirm = onConfirm;
    }

    @Override
    protected Screen parentScreen() {
        return this.parent;
    }

    @Override
    protected int contentWidth() {
        List<Component> labels = List.of(this.title, Component.translatable("gui.merlinlib.confirm.warning"));
        return Math.max(MIN_CONTENT_WIDTH,
                VanillaUi.labelWidth(this.font, labels) + VanillaUi.PADDING + BUTTON_WIDTH * 2 + VanillaUi.GAP);
    }

    @Override
    protected int contentHeight() {
        return 12 + 4 + 12 + 10 + VanillaUi.WIDGET_HEIGHT;
    }

    @Override
    protected void build(PanelLayout layout) {
        layout.gap(12 + 4 + 12 + 10);
        int y = layout.row(VanillaUi.WIDGET_HEIGHT);
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.confirm.delete"),
                button -> {
                    this.onConfirm.run();
                    this.onClose();
                }, layout.left(), y, BUTTON_WIDTH));
        Button cancel = this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.cancel"),
                button -> this.onClose(), layout.right() - BUTTON_WIDTH, y, BUTTON_WIDTH));
        this.setInitialFocus(cancel);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        PanelLayout layout = layout();
        int left = layout.left();
        int content = layout.contentWidth();

        VanillaUi.text(graphics, this.font, this.title, left, layout.top(), VanillaUi.TEXT_ERROR, content);
        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.confirm.warning"),
                left, layout.top() + 16, VanillaUi.TEXT, content);
        VanillaUi.separator(graphics, left, layout.right(), layout.top() + 32);
    }
}
