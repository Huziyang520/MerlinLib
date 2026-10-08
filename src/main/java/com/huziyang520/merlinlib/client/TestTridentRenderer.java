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
 *
 * <h2>What this class reproduces, and where it was read from</h2>
 *
 * <p>1.20.1's {@code BlockEntityWithoutLevelRenderer#renderByItem} contains a branch for the vanilla
 * trident - {@code if (stack.is(Items.TRIDENT))} - and this class is a copy of that branch, bytecode for
 * bytecode, read with {@code javap -c} rather than reconstructed from memory:
 *
 * <pre>
 * 613: aload_1
 * 614: getstatic     // Field net/minecraft/world/item/Items.TRIDENT
 * 617: invokevirtual // Method ItemStack.is:(Lnet/minecraft/world/item/Item;)Z
 * 623: pushPose
 * 627: fconst_1 / -1.0f / -1.0f            // scale(1, -1, -1)
 * 640-655: ItemRenderer.getFoilBufferDirect(buffer, model.renderType(TridentModel.TEXTURE), false, stack.hasFoil())
 * 661: getfield tridentModel
 * 675: TridentModel.renderToBuffer(pose, consumer, light, overlay, 1, 1, 1, 1)
 * 678: popPose
 * </pre>
 *
 * <p>Only the {@code Items.TRIDENT} test is gone: for the vanilla renderer it selects the branch, for
 * this renderer the branch <em>is</em> the whole class, and the item that reaches it was already selected
 * by {@code getCustomRenderer()}.
 *
 * <h2>Why the instance is created lazily</h2>
 *
 * <p>The model needs {@code Minecraft.getInstance()} for the block entity dispatcher and the entity model
 * set, and this class is reachable from {@code initializeClient} - which asks for the renderer as soon as
 * an {@code ItemRenderer} exists, but the ordering there is not something this class should rely on.
 * Building it on first request keeps the class loadable even if something asks early, and the game's own
 * {@code BlockEntityWithoutLevelRenderer} is built exactly the same way.
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
