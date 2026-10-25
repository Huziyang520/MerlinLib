// Puts the armour bar back against the health bar once the health row is folded.

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

@Mixin(GuiGraphics.class)
public abstract class MixinGuiGraphics {
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
