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
 */
public class DeleteEnchantsConfirmScreen extends VanillaScreen {

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
        // The two buttons share the content width in equal halves, so there is no separate "two buttons plus a
        // gap" term any more: the panel only has to be wide enough for the warning above them and for two
        // usable halves.
        return Math.max(MIN_CONTENT_WIDTH, VanillaUi.labelWidth(this.font, labels));
    }

    @Override
    protected int contentHeight() {
        return 12 + 4 + 12 + 10 + VanillaUi.WIDGET_HEIGHT;
    }

    @Override
    protected void build(PanelLayout layout) {
        layout.gap(12 + 4 + 12 + 10);
        int y = layout.row(VanillaUi.WIDGET_HEIGHT);
        // Equal halves, flush with both edges of the content: the same treatment every screen's footer gets.
        int wide = layout.sliceWidth(2);
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.confirm.delete"),
                button -> {
                    this.onConfirm.run();
                    this.onClose();
                }, layout.left(), y, wide));
        Button cancel = this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.cancel"),
                button -> this.onClose(), layout.sliceX(1, 2), y, wide));
        this.setInitialFocus(cancel);
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PanelLayout layout = layout();
        int left = layout.left();
        int content = layout.contentWidth();

        VanillaUi.text(graphics, this.font, this.title, left, layout.top(), VanillaUi.TEXT_ERROR, content);
        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.confirm.warning"),
                left, layout.top() + 16, VanillaUi.TEXT, content);
        VanillaUi.separator(graphics, left, layout.right(), layout.top() + 32);
    }
}
