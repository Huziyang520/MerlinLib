package com.huziyang520.merlinlib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the testing trident the way the vanilla renderer draws {@code Items.TRIDENT}.
 */
public final class TestTridentRenderer extends BlockEntityWithoutLevelRenderer {

    /** The shared instance; built on first use, see the class comment. */
    private static TestTridentRenderer instance;

    private final TridentModel model;

    private TestTridentRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.model = new TridentModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.TRIDENT));
    }

    /**
     * The renderer instance, built on first request.
     *
     * @return the shared renderer
 */
    public static TestTridentRenderer instance() {
        TestTridentRenderer active = instance;
        if (active == null) {
            active = new TestTridentRenderer();
            instance = active;
        }
        return active;
    }

    /**
     * Paints one trident, in whatever display context the item renderer asked for.
     *
     * @param stack   the stack being rendered
     * @param context where it is being drawn
     * @param pose    the pose stack
     * @param buffer  the buffer source
     * @param light   packed light
     * @param overlay packed overlay
 */
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();
        pose.scale(1.0F, -1.0F, -1.0F);
        VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(
                buffer, this.model.renderType(TridentModel.TEXTURE), false, stack.hasFoil());
        this.model.renderToBuffer(pose, consumer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }
}
