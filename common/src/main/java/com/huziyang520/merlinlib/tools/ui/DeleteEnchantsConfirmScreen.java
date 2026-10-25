// Confirmation dialog for "remove every enchantment".

package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.tools.ui.vanilla.PanelLayout;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaScreen;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public class DeleteEnchantsConfirmScreen extends VanillaScreen {
    private static final int MIN_CONTENT_WIDTH = 200;
    private final Screen parent;
    private final Runnable onConfirm;

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
        return Math.max(MIN_CONTENT_WIDTH, VanillaUi.labelWidth(this.font, labels) + VanillaUi.PADDING);
    }

    @Override
    protected int contentHeight() {
        return 12 + 4 + 12 + 10 + VanillaUi.WIDGET_HEIGHT;
    }

    @Override
    protected void build(PanelLayout layout) {
        layout.gap(12 + 4 + 12 + 10);
        int y = layout.row(VanillaUi.WIDGET_HEIGHT);
        int half = layout.sliceWidth(2);
        this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.merlinlib.confirm.delete"),
                button -> {
                    this.onConfirm.run();
                    this.onClose();
                }, layout.left(), y, half));
        Button cancel = this.addRenderableWidget(VanillaUi.button(Component.translatable("gui.cancel"),
                button -> this.onClose(), layout.sliceX(1, 2), y, half));
        this.setInitialFocus(cancel);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        PanelLayout layout = layout();
        int left = layout.left();
        int content = layout.contentWidth();
        VanillaUi.text(graphics, this.font, this.title, left, layout.top(), VanillaUi.TEXT_ERROR, content);
        VanillaUi.text(graphics, this.font, Component.translatable("gui.merlinlib.confirm.warning"),
                left, layout.top() + 16, VanillaUi.TEXT, content);
        VanillaUi.separator(graphics, left, layout.right(), layout.top() + 32);
    }
}
