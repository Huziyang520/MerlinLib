package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.client.HudRowPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the armour bar back against the health bar once the health row is folded.
 *
 * <p>See {@link HudRowPolicy} for why this is done at the drawing call rather than in the health row
 * itself: on 1.20.1 every hook aimed at {@code Gui.renderPlayerHealth} is found by Mixin and then never
 * runs, so the row that method would have moved has to be caught where it is actually drawn. An armour
 * icon is the only use of the vanilla icon sheet with {@code v = 9}, so it can be recognised from the
 * arguments alone.
 *
 * <p>The replacement draw re-enters this handler once. That is intentional and terminates: the second
 * pass sees the icon already at the adapted y, returns without cancelling, and the original draw
 * proceeds.
 */
@Mixin(GuiGraphics.class)
public abstract class MixinGuiGraphics {

    /**
     * Draws an armour icon at the adapted y instead of the y vanilla asked for, and cancels the original.
     *
     * @param texture the sheet being drawn from
     * @param x       the destination x, unchanged
     * @param y       the destination y vanilla computed from the unfolded maximum health
     * @param u       the source x on the sheet
     * @param v       the source y on the sheet
     * @param width   the icon width
     * @param height  the icon height
     * @param info    the injection callback
     */
    @Inject(method = "blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V", at = @At("HEAD"),
            require = 1, cancellable = true)
    private void merlinlib$addressArmourRow(ResourceLocation texture, int x, int y, int u, int v,
                                            int width, int height, CallbackInfo info) {
        if (!HudRowPolicy.adaptingArmour() || !HudRowPolicy.isArmourIcon(texture, v, width, height)) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !HudRowPolicy.folding(player.getMaxHealth())) {
            return;
        }
        int adapted = HudRowPolicy.adaptedArmourY();
        if (adapted == y) {
            return;
        }
        ((GuiGraphics) (Object) this).blit(texture, x, adapted, u, v, width, height);
        info.cancel();
    }
}
