package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.tools.HealthEditorItem;
import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opens the health editor for the player themselves on a sneak right click into thin air.
 *
 * <h2>Why the existing entry points cannot cover this</h2>
 *
 * <p>The health editor already opens from two places: using it on an entity, and sneak using an entity.
 * Both need an entity under the crosshair, so "edit myself while nothing is in front of me" had no path at
 * all - vanilla only calls the use item callback when there is something to use it on.
 *
 * <p>{@code Minecraft.startUseItem} is the method the game runs for the use key, which is why the hook sits
 * there: it respects the player's own key binding, works with the key bound to a mouse button, and runs
 * before vanilla decides what to do with the click.
 *
 * <h2>Why only thin air</h2>
 *
 * <p>The hook stays out of the way unless the crosshair points at nothing. Sneak right clicking a block is
 * how a player opens a chest or places a block, and sneak right clicking an entity already belongs to the
 * entity path; hijacking either would break vanilla behaviour the player relies on.
 */
@Mixin(Minecraft.class)
public class MixinMinecraftUseItem {

    /**
     * Handles the sneak right click into thin air.
     *
     * @param info the injection callback, cancelled when the screen is opened
     */
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void merlinlib$healthEditorOnSneakUse(CallbackInfo info) {
        Minecraft minecraft = (Minecraft) (Object) this;
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isShiftKeyDown()) {
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            return;
        }
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean holdingItem = held.getItem() instanceof HealthEditorItem;
        if (MerlinScreens.openHealthEditor(null, player, holdingItem)) {
            info.cancel();
        }
    }
}
