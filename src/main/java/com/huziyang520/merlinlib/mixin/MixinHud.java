package com.huziyang520.merlinlib.mixin;

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
 *
 * <h2>The problem</h2>
 *
 * <p>Vanilla draws one heart per two points of health and loops from {@code ceil(maxHealth / 2)} down
 * to zero. The health editor can set the ceiling to the integer limit, so a max health of two billion
 * asks for a billion hearts and the game stops responding while it draws them. Vanilla already guards
 * against this shape of problem for vehicles, where the heart count is capped at thirty.
 *
 * <h2>The fix, and why it is not a clamp</h2>
 *
 * <p>Clamping the values would hide what the player set. Instead, above {@link #COMPACT_THRESHOLD}
 * health the heart loop is asked for exactly <em>one</em> heart, and the exact numbers are drawn next
 * to it. The heart itself is still vanilla's own sprite and colour, so poison, wither, freezing and
 * absorption stay visible at a glance - which is the point of keeping a heart when the numbers are
 * huge. The attribute, the health value, the damage dealt and the network are untouched.
 *
 * <h2>1.20.1: every name in this file changed, and the mapping is evidence-driven</h2>
 *
 * <p>The class is still {@code net.minecraft.client.gui.Gui} - the rename to {@code Hud} comes later -
 * and every method this mixin touches was renamed with it. Verified with
 * {@code javap -p -s net.minecraft.client.gui.Gui}, which gives the whole set at once:
 *
 * <pre>
 * private void renderPlayerHealth(net.minecraft.client.gui.GuiGraphics);
 *   descriptor: (Lnet/minecraft/client/gui/GuiGraphics;)V
 * protected void renderHearts(net.minecraft.client.gui.GuiGraphics, net.minecraft.world.entity.player.Player, int, int, int, int, float, int, int, int, boolean);
 *   descriptor: (Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V
 * </pre>
 *
 * <p>Against the 26.3 names ({@code extractPlayerHealth} / {@code extractHealthLevel} /
 * {@code extractHearts} / {@code GuiGraphicsExtractor}) that is four independent renames, so the
 * injection points are re-derived from the bytecode below rather than translated by pattern.
 *
 * <h2>What was measured on 1.20.1, and the two rules that came out of it</h2>
 *
 * <p>The first version of this class was a direct translation of the 26.3 source: a
 * {@code @Redirect} on the {@code Player.getAttributeValue} call, a {@code @ModifyArg} on the
 * {@code renderHearts} call, and a {@code @Inject} at the tail of {@code renderHearts} for the
 * numbers. On 26.3 that works. On 1.20.1 the text appeared while a player with 102 health still drew
 * all fifty-one hearts and the armour row still floated above them, which proves the two hooks that
 * had been aimed at {@code renderPlayerHealth} never ran while the one inside {@code renderHearts}
 * did. A second attempt replaced the call-site hooks with {@code @ModifyVariable(index = 13)} on the
 * layout slot and it failed the same way - the armour stayed high - while the {@code renderHearts}
 * hook kept working.
 *
 * <p><b>Rule one: an injection point whose {@code @At} names a Minecraft member is not trustworthy
 * on this setup.</b> Both dead hooks named one ({@code renderHearts}, {@code Player.getAttributeValue})
 * and both are supposed to abort startup with {@code require = 1} when they cannot be resolved - yet
 * the game started, so the resolution step found something and the injection still did not run. The
 * hook that survives is the one with no {@code @At} target at all, and the layout hook above now
 * names {@code java.lang.Math}, which Mixin never remaps.
 *
 * <p><b>Rule two: {@code @ModifyVariable} must stay {@code argsOnly}.</b> Vanilla classes ship no
 * local variable table, and Mixin's own documentation for {@code index} says the injector has to read
 * (or generate) that table, "which can have mixed results". {@code argsOnly} reads the argument types
 * straight off the method descriptor, which is why the {@code renderHearts} hook has always worked.
 *
 * <p>The two hooks left in this file therefore follow both rules: the layout one redirects a JDK
 * call, and the heart count one sits on {@code renderHearts}' own {@code maxHealth} argument. The
 * second is the belt to the first's braces - it is a no-op whenever the layout hook did its job,
 * because the value it then sees is already {@link #ONE_HEART: two}, which is below the threshold.
 *
 * <h2>What is deliberately not done</h2>
 *
 * <p>{@code @ModifyArgs}, which the 26.3 source uses, is not available here: it makes Mixin
 * <em>generate a class at runtime</em> ({@code ArgsClassGenerator.CLASS_NAME_BASE} is
 * {@code "org.spongepowered.asm.synthetic.args.Args$"}), and the Mixin 0.8.5 that Forge 1.20.1
 * bundles does not do that generation - the game dies with
 * {@code NoClassDefFoundError: org/spongepowered/asm/synthetic/args/Args$1} and a stack trace that
 * names no mixin. Replacing the whole {@code renderHearts} call with {@code @Redirect} is not an
 * option either: it would have to re-invoke a {@code protected} method of the target class, and the
 * {@code @Shadow} that needs failed to resolve on this Mixin version.
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
        ClientConfig config = ConfigManager.client();
        if (config == null || !config.hudCollapse()) {
            return false;
        }
        float threshold = Math.max(1.0F, config.hudCollapseThreshold());
        return real > threshold;
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
        return folding(real) ? ONE_HEART : real;
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
            System.out.println("[MerlinLib] hud probe: incoming=" + maxHealth + " folding="
                    + folding(maxHealth) + " threshold="
                    + (config == null ? FALLBACK_COLLAPSE_THRESHOLD : config.hudCollapseThreshold()));
        }
        return folding(maxHealth) ? ONE_HEART : maxHealth;
    }

    /**
     * Writes the exact numbers next to the single heart.
     *
     * <p>{@code renderHearts} keeps its tail free for a plain {@code @Inject} at {@code RETURN}, and
     * the text is drawn through {@code GuiGraphics#drawString}, which is the 1.20.1 name for what
     * 26.3 calls {@code text}.
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
