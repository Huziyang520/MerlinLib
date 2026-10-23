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

/**
 * Draws very large health as one vanilla heart plus the numbers, instead of one heart per two points.
 */
@Mixin(Gui.class)
public class MixinHud {

    /** The value that makes the vanilla loop draw a single heart. */
    private static final float ONE_HEART = 2.0F;

    /**
     * Fallback threshold, used only while no client configuration is loaded.
     *
     * <p>99 folds from a ceiling of 100 upwards, which is the documented default.
 */
    private static final float FALLBACK_COLLAPSE_THRESHOLD = 99.0F;

    /** When the next layout probe may print, in milliseconds; the HUD asks every frame. */
    private static long probeAt;

    /**
     * Whether the row should be folded, according to the client's own configuration.
     *
     * @param real the health the row was built from
     * @return true when the row collapses to one heart plus the numbers
 */
    private static boolean folding(float real) {
        return HudRowPolicy.folding(real);
    }

    /**
     * Slot of the {@code float maxHealth} parameter of {@code renderHearts}.
     *
     * <p>Argument slots start after {@code this}, so over
     * {@code GuiGraphics 0, Player 1, int xLeft 2, int yLineBase 3, int rowHeight 4, int offset 5,
     * float maxHealth 6, ...} in <em>argument list</em> terms this is argument 6, and it is local slot
     * 7 - the two numbers differ by one and mixing them up is exactly the kind of mistake that would
     * silently draw ten hearts instead of one, so both are written down here. {@code argsOnly} is what
     * makes this hook reliable: it reads the arguments straight off the method descriptor and never
     * touches the local variable table, which vanilla classes do not ship (see the class comment).
 */
    private static final int MAX_HEALTH_ARG = 7;

    /** Gap between the heart and the numbers, and the text's vertical nudge. */
    private static final int TEXT_OFFSET_X = 12;
    private static final int TEXT_OFFSET_Y = 1;

    /**
     * Keeps the heart row to a single row of space, but only once the health is really large.
     *
     * <p>This is the layout copy of the maximum: it decides how much room the row takes, how many
     * hearts are drawn and where the armour, food and air rows sit, so it is the value the threshold
     * has to be applied to. Clamping it unconditionally would leave every player between twenty-one
     * and a hundred health with a single row of hearts where vanilla would have drawn two to five;
     * up to the threshold the real value is passed through untouched. Above it the armour, food and
     * air rows all slide down on their own, because every one of them is positioned from this value:
     *
     * <pre>
     *      252: invokestatic  java/lang/Math.max:(FF)F   // m = max(maxHealth, max(current, old))
     *      255: fstore        13                         // the row's own copy of that value
     *      ...  rows    = ceil((m + absorption) / 2 / 10)                 // istore 15
     *      ...  armourY = yLineBase - (rows - 1) * rowHeight - 10         // istore 17
     * </pre>
     *
     * <p>The call is the only {@code Math.max(float, float)} in the method, and
     * {@code java.lang.Math} is not a Minecraft class, so this injection point needs no name
     * remapping at all. That is not a detail: the two earlier versions of this class redirected
     * {@code Player.getAttributeValue} and replaced the {@code renderHearts} call argument, both of
     * which are remapped Minecraft members, and both of which were measured on 1.20.1 to apply
     * without error yet never run. The {@code renderHearts} hooks kept working. Targeting a JDK
     * method removes that whole failure mode - with it, the injection is found and the only thing
     * left to get right is the handler's own signature.
     *
     * <p><b>The handler must not be {@code static}</b>, even though the call it replaces is a static
     * one. Mixin matches the handler's modifier against the <em>target method</em>, and
     * {@code renderPlayerHealth} is an instance method; writing {@code static} aborts startup with
     * {@code InvalidInjectionException: 'static' modifier of handler method does not match target}
     * from {@code Injector.checkTargetModifiers}. That is a hard failure at class-transform time,
     * before any of this class's other injections get a chance to run.
     *
     * @param a the maximum health the row was built from
     * @param b the displayed health
     * @return the larger of the two, brought down to one heart only when it is large
 */
    @Redirect(method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V", require = 1,
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float merlinlib$oneRowWhenHuge(float a, float b) {
        ClientConfig config = ConfigManager.client();
        float real = Math.max(a, b);
        if (config == null || !config.hudCollapse() || !config.hudArmourDodge()) {
            // Not folding at all, or the player asked for vanilla's placement: the layout value is
            // passed through untouched, so the armour row keeps whatever spot vanilla gives it.
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

    /**
     * The same rule applied to the heart loop's own argument, as a second line of defence.
     *
     * <p>{@code renderHearts} is the one place this class is proven to reach - the numbers below are
     * drawn from an injection into the same method - so the heart count is pinned here rather than
     * only at the call site. When the layout hook above has already reduced the value this is a
     * no-op, because {@code 2.0} is below the threshold.
     *
     * @param maxHealth the value the loop would use as the row width
     * @return one heart's worth of health once the value is large
 */
    @ModifyVariable(method = "renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V",
            at = @At("HEAD"), argsOnly = true, index = MAX_HEALTH_ARG, require = 1)
    private float merlinlib$oneHeartWhenHuge(float maxHealth) {
        // TEMPORARY diagnostic, once a second: this is the only injection point in this class proven
        // to run on this setup, so it is where the value the rest of the HUD sees can be read.
        // Remove once the layout hook above is confirmed to be reached.
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

    /**
     * Writes the exact numbers next to the single heart.
 *
     * @param graphics         the draw context
     * @param player           the player the row belongs to
     * @param xLeft            the left edge of the heart row
     * @param yLineBase        the baseline of the heart row
     * @param healthRowHeight  the vanilla row spacing, unused here
     * @param heartOffsetIndex the vanilla regeneration heartbeat offset, unused here
     * @param maxHealth        the value the loop used, already reduced when the health is large
     * @param currentHealth    the health as a whole number
     * @param oldHealth        the previously displayed health
     * @param absorption       the absorption amount
     * @param blink            whether the row is blinking after a change
     * @param info             the injection callback
 */
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

    /**
     * The health the readout should show.
     *
     * <p>The player's own maximum wins over what the row was built from, and the displayed health is
     * taken into account as well: vanilla does the same, so that a heart row never shows less than the
     * number next to it while a health change is still animating.
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
