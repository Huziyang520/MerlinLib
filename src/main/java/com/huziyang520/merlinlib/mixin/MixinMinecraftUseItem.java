// Reserved hook for opening the health editor on a sneak right click into thin air.

package com.huziyang520.merlinlib.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraftUseItem {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void merlinlib$healthEditorOnSneakUse(CallbackInfo info) {
        Minecraft minecraft = (Minecraft) (Object) this;
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isShiftKeyDown()) {
            return;
        }

        if (minecraft.screen != null) {
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            return;
        }
    }
}
