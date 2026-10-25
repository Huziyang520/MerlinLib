// Draws very large health as one vanilla heart plus the numbers, instead of one heart per two points.

package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.client.HudRowPolicy;
import com.huziyang520.merlinlib.config.ClientConfig;
import com.huziyang520.merlinlib.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinHud {
    private static final float ONE_HEART = 2.0F;
    private static final float FALLBACK_COLLAPSE_THRESHOLD = 99.0F;
    private static long probeAt;

    private static boolean folding(float real) {
        return HudRowPolicy.folding(real);
    }

    private static final int MAX_HEALTH_ARG = 7;
    private static final int TEXT_OFFSET_X = 12;
    private static final int TEXT_OFFSET_Y = 1;

    @Redirect(method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V", require = 1,
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float merlinlib$oneRowWhenHuge(float a, float b) {
        ClientConfig config = ConfigManager.client();
        float real = Math.max(a, b);
        if (config == null || !config.hudCollapse() || !config.hudArmourDodge()) {
            return real;
        }
        boolean fold = folding(real);
        long now = System.currentTimeMillis();
        if (now - probeAt >= 1000L) {
            probeAt = now;
            Constants.LOG.info("[hud] layout hook RAN: real={} fold={}", real, fold);
        }
        return fold ? ONE_HEART : real;
    }

    @ModifyVariable(method = "renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V",
            at = @At("HEAD"), argsOnly = true, index = MAX_HEALTH_ARG, require = 1)
    private float merlinlib$oneHeartWhenHuge(float maxHealth) {
        long now = System.currentTimeMillis();
        if (now - probeAt >= 1000L) {
            probeAt = now;
            ClientConfig config = ConfigManager.client();
            StackTraceElement[] trace = new Throwable().getStackTrace();
            StringBuilder line = new StringBuilder("[MerlinLib] hud probe: incoming=" + maxHealth
                    + " threshold=" + (config == null ? FALLBACK_COLLAPSE_THRESHOLD
                    : config.hudCollapseThreshold()) + " caller=");
            for (int i = 1; i < Math.min(6, trace.length); i++) {
                line.append(trace[i].getClassName()).append('#').append(trace[i].getMethodName())
                        .append(" <- ");
            }
            Constants.LOG.info("[hud] probe: {}", line);
        }
        return folding(maxHealth) ? ONE_HEART : maxHealth;
    }

    @Inject(method = "renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V",
            at = @At("TAIL"), require = 1)
    private void merlinlib$healthNumbers(GuiGraphics graphics, Player player, int xLeft, int yLineBase,
                                         int healthRowHeight, int heartOffsetIndex, float maxHealth,
                                         int currentHealth, int oldHealth, int absorption, boolean blink,
                                         CallbackInfo info) {
        float real = realHealth(player, currentHealth, oldHealth);
        if (!folding(real)) {
            return;
        }
        Component text = Component.literal(currentHealth + "/" + Math.round(real));
        int color = absorption > 0 ? 0xFFFFD700 : 0xFFFFFFFF;
        graphics.drawString(Minecraft.getInstance().font, text, xLeft + TEXT_OFFSET_X,
                yLineBase + TEXT_OFFSET_Y, color, true);
    }

    private static float realHealth(Player player, int currentHealth, int oldHealth) {
        return Math.max(player.getMaxHealth(), Math.max(currentHealth, oldHealth));
    }
}
