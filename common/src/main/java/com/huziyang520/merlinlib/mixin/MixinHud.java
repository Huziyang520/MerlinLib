package com.huziyang520.merlinlib.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
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

/**
 * Draws very large health as one vanilla heart plus the numbers, instead of one heart per two points.
 *
 * <h2>The problem</h2>
 *
 * <p>Vanilla draws one heart per two points of health and loops from {@code ceil(maxHealth / 2)} down to
 * zero. The health editor can set the ceiling to the integer limit, so a max health of two billion asks for
 * a billion hearts and the game stops responding while it draws them. Vanilla already guards against this
 * shape of problem for vehicles, where the heart count is capped at thirty.
 *
 * <h2>The fix, and why it is not a clamp</h2>
 *
 * <p>Clamping the values would hide what the player set. Instead, above {@link #COMPACT_THRESHOLD} health the
 * heart loop is asked for exactly <em>one</em> heart, and the exact numbers are drawn next to it. The heart
 * itself is still vanilla's own sprite and colour, so poison, wither, freezing and absorption stay visible at
 * a glance - which is the point of keeping a heart when the numbers are huge. The attribute, the health
 * value, the damage dealt and the network are untouched.
 *
 * <h2>The three injection points, and why each names two methods</h2>
 *
 * <p>The health row is drawn by {@code extractPlayerHealth} on Fabric, while NeoForge's patches split the same
 * code into {@code extractHealthLevel} (with a deprecated {@code extractPlayerHealth} that merely calls it).
 * A hook that names only one of them finds no target on the other loader and aborts the whole mixin - which is
 * exactly how the first version of this class crashed both clients. Naming both, with {@code require = 1},
 * means the injection lands wherever the call really is.
 */
@Mixin(Hud.class)
public class MixinHud {

    /** Health above which the row becomes one heart and the numbers. */
    private static final float COMPACT_THRESHOLD = 100.0F;

    /** The value that makes the vanilla loop draw a single heart. */
    private static final float ONE_HEART = 2.0F;

    /** The same, as one row's worth of health, for the layout calculation. */
    private static final double ONE_ROW_OF_HEALTH = 20.0D;

    /** Gap between the heart and the numbers, and the text's vertical nudge. */
    private static final int TEXT_OFFSET_X = 12;
    private static final int TEXT_OFFSET_Y = 1;

    /**
     * Keeps the heart row to a single row of space.
     *
     * <p>This is the layout copy of the maximum only: it decides how much room the row takes and where the
     * armour, food and air rows sit. What is drawn comes from the player's real values, so nothing is hidden
     * by it.
     *
     * @param player    the player the row belongs to
     * @param attribute the attribute being read, always the maximum health
     * @return the same value, brought down to one vanilla row
     */
    @Redirect(method = {"extractPlayerHealth", "extractHealthLevel"}, require = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double merlinlib$layoutOneRow(Player player, Holder<Attribute> attribute) {
        return Math.min(player.getAttributeValue(attribute), ONE_ROW_OF_HEALTH);
    }

    /**
     * Reduces the heart count to one once the health is large.
     *
     * <p>The player argument of the call is what tells us the real maximum: the value being passed has
     * already been brought down for the layout, so it can never answer this question.
     *
     * @param args the arguments of the {@code extractHearts} call
     */
    @ModifyArgs(method = {"extractPlayerHealth", "extractHealthLevel"}, require = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;extractHearts(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V"))
    private void merlinlib$oneHeartWhenHuge(Args args) {
        Player player = args.get(1);
        int currentHealth = args.get(7);
        int oldHealth = args.get(8);
        if (realHealth(player, currentHealth, oldHealth) > COMPACT_THRESHOLD) {
            args.set(6, ONE_HEART);
        }
    }

    /**
     * Writes the exact numbers next to the single heart.
     *
     * @param graphics        the render state extractor
     * @param player          the player the row belongs to
     * @param xLeft           the left edge of the heart row
     * @param yLineBase       the baseline of the heart row
     * @param healthRowHeight the vanilla row spacing, unused here
     * @param heartOffsetIndex the vanilla regeneration heartbeat offset, unused here
     * @param maxHealth       the value the loop used, already reduced when the health is large
     * @param currentHealth   the health as a whole number
     * @param oldHealth       the previously displayed health
     * @param absorption      the absorption amount
     * @param blink           whether the row is blinking after a change
     * @param info            the injection callback
     */
    @Inject(method = "extractHearts", at = @At("TAIL"))
    private void merlinlib$healthNumbers(GuiGraphicsExtractor graphics, Player player, int xLeft, int yLineBase,
                                         int healthRowHeight, int heartOffsetIndex, float maxHealth,
                                         int currentHealth, int oldHealth, int absorption, boolean blink,
                                         CallbackInfo info) {
        float real = realHealth(player, currentHealth, oldHealth);
        if (real <= COMPACT_THRESHOLD) {
            return;
        }
        Component text = Component.literal(currentHealth + "/" + Math.round(real));
        int color = absorption > 0 ? 0xFFFFD700 : 0xFFFFFFFF;
        graphics.text(Minecraft.getInstance().font, text, xLeft + TEXT_OFFSET_X, yLineBase + TEXT_OFFSET_Y,
                color, true);
    }

    /**
     * The health the readout should show.
     *
     * <p>The player's own maximum wins over what the row was built from, and the displayed health is taken
     * into account as well: vanilla does the same, so that a heart row never shows less than the number next
     * to it while a health change is still animating.
     *
     * @param player        the player
     * @param currentHealth the health as a whole number
     * @param oldHealth     the previously displayed health
     * @return the largest of the three, as a float
     */
    private static float realHealth(Player player, int currentHealth, int oldHealth) {
        return Math.max(player.getMaxHealth(), Math.max(currentHealth, oldHealth));
    }
}
