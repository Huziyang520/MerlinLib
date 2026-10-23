package com.huziyang520.merlinlib.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reserved hook for opening the health editor on a sneak right click into thin air.
 */
@Mixin(Minecraft.class)
public class MixinMinecraftUseItem {

    /**
     * Handles the sneak right click into thin air.
     *
     * @param info the injection callback, cancelled when the screen is opened
 */
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void merlinlib$healthEditorOnSneakUse(CallbackInfo info) {
        Minecraft minecraft = (Minecraft) (Object) this;
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isShiftKeyDown()) {
            return;
        }
        // 1.20.1 keeps the current screen on Minecraft itself. See the class Javadoc: this line is the
        if (minecraft.screen != null) {
            // A screen is already up. Never stack another one on top: the use key is polled while it is
            // held, so without this the editor would reopen as soon as it was closed.
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            return;
        }
        // TODO: restore the opening call once the tools package is ported:
        //
        //     if (MerlinScreens.openHealthEditor(null, player)) {
        //         info.cancel();
        //     }
        //
        // Every guard above it is in place and evaluates exactly as it does upstream, so restoring the
        // three lines is the whole of the remaining work.
    }
}
