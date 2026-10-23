package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prints the client banner once the game object is fully constructed.
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {

    /**
     * Logs the banner once the client exists.
     *
     * @param info the injection callback
 */
    @Inject(method = "<init>", at = @At("TAIL"), require = 1)
    private void merlinlib$logClientBanner(CallbackInfo info) {
        Constants.LOG.info("This line is printed by a MerlinLib client mixin!");
        Constants.LOG.info("MC Version: {}", SharedConstants.getCurrentVersion().getName());
    }
}
