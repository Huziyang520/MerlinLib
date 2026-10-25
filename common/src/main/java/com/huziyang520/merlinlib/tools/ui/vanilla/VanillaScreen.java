// Base class of the MerlinLib toolkit screens.

package com.huziyang520.merlinlib.tools.ui.vanilla;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class VanillaScreen extends Screen {
    private static final int WORLD_DIM = 0xC0101010;
    private PanelLayout layout;

    protected VanillaScreen(Component title) {
        super(title);
    }

    protected int panelWidth() {
        return Math.min(contentWidth(), Math.min(VanillaUi.MAX_PANEL_WIDTH, this.width - VanillaUi.PADDING));
    }

    protected abstract int contentWidth();
    protected abstract int contentHeight();

    protected final int contentHeightLimit() {
        return Math.max(60, this.height - 2 * (VanillaUi.PADDING + 4));
    }

    protected abstract void build(PanelLayout layout);

    @Override
    protected void init() {
        super.init();
        this.layout = PanelLayout.centered(this, panelWidth(), contentHeight());
        this.build(this.layout);
    }

    protected final PanelLayout layout() {
        if (this.layout == null) {
            this.layout = PanelLayout.centered(this, panelWidth(), contentHeight());
        }
        return this.layout;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawContentBackground(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderContent(graphics, mouseX, mouseY, partialTick);
    }

    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        if (this.width > 0 && this.height > 0) {
            graphics.fill(0, 0, this.width, this.height, WORLD_DIM);
        }

        if (drawsPanel() && this.width > 0 && this.height > 0) {
            layout().draw(graphics);
        }
    }

    protected void drawContentBackground(GuiGraphicsExtractor graphics) {
    }

    protected boolean drawsPanel() {
        return true;
    }

    protected Screen parentScreen() {
        return null;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parentScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
