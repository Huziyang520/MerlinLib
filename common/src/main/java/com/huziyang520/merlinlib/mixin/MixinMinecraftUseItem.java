// Opens the health editor for the player themselves on a sneak right click into thin air.

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

@Mixin(Minecraft.class)
public class MixinMinecraftUseItem {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void merlinlib$healthEditorOnSneakUse(CallbackInfo info) {
        Minecraft minecraft = (Minecraft) (Object) this;
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isShiftKeyDown()) {
            return;
        }

        if (minecraft.gui.screen() != null) {
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            return;
        }
        if (MerlinScreens.openHealthEditor(null, player)) {
            info.cancel();
        }
    }
}
