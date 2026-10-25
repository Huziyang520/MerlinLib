// Draws very large health as one vanilla heart plus the numbers, instead of one heart per two points.

package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.config.ClientConfig;
import com.huziyang520.merlinlib.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Gui.class)
public class MixinHud {
    private static final float DEFAULT_THRESHOLD = 99.0F;

    private static final float ONE_HEART = 2.0F;

    private static final double ONE_ROW_OF_HEALTH = 20.0D;

    private static final int TEXT_OFFSET_X = 12;
    private static final int TEXT_OFFSET_Y = 1;

    @Redirect(method = {"extractPlayerHealth", "extractHealthLevel"}, require = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double merlinlib$layoutOneRow(Player player, Holder<Attribute> attribute) {
        double real = player.getAttributeValue(attribute);
        ClientConfig config = ConfigManager.client();
        if (!config.hudCollapse()) {
            return real;
        }
        if (!config.hudArmourDodge()) {
            return real;
        }
        return real > threshold(config) ? ONE_ROW_OF_HEALTH : real;
    }

    @ModifyArgs(method = {"extractPlayerHealth", "extractHealthLevel"}, require = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;extractHearts(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V"))
    private void merlinlib$oneHeartWhenHuge(Args args) {
        Player player = args.get(1);
        int currentHealth = args.get(7);
        int oldHealth = args.get(8);
        ClientConfig config = ConfigManager.client();
        if (config.hudCollapse() && realHealth(player, currentHealth, oldHealth) > threshold(config)) {
            args.set(6, ONE_HEART);
        }
    }

    @Inject(method = "extractHearts", at = @At("TAIL"))
    private void merlinlib$healthNumbers(GuiGraphicsExtractor graphics, Player player, int xLeft, int yLineBase,
                                         int healthRowHeight, int heartOffsetIndex, float maxHealth,
                                         int currentHealth, int oldHealth, int absorption, boolean blink,
                                         CallbackInfo info) {
        ClientConfig config = ConfigManager.client();
        float real = realHealth(player, currentHealth, oldHealth);
        if (!config.hudCollapse() || real <= threshold(config)) {
            return;
        }
        Component text = Component.literal(currentHealth + "/" + Math.round(real));
        int color = absorption > 0 ? 0xFFFFD700 : 0xFFFFFFFF;
        graphics.text(Minecraft.getInstance().font, text, xLeft + TEXT_OFFSET_X, yLineBase + TEXT_OFFSET_Y,
                color, true);
    }

    private static float threshold(ClientConfig config) {
        int configured = config.hudCollapseThreshold();
        return configured > 0 ? (float) configured : DEFAULT_THRESHOLD;
    }

    private static float realHealth(Player player, int currentHealth, int oldHealth) {
        return Math.max(player.getMaxHealth(), Math.max(currentHealth, oldHealth));
    }
}
