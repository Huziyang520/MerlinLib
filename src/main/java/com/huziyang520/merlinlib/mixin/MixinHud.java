package com.huziyang520.merlinlib.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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
 * <h2>Why the two hooks are variables, not call sites</h2>
 *
 * <p>The first 1.20.1 version of this class used a {@code @Redirect} on the
 * {@code getAttributeValue} call and a {@code @ModifyArg} on the {@code renderHearts} call - the
 * direct translation of the 26.3 source. On 26.3 that works. On 1.20.1 it was measured not to: a
 * player with 102 health still drew all fifty-one hearts, which proves the value that reached
 * {@code renderHearts} was the raw 102 - i.e. the argument hook did not run - while the text hook in
 * the very same class did run. Both of the dead hooks are <em>call site</em> hookpoints
 * ({@code @At(INVOKE)}), the live one is not.
 *
 * <p>So both hooks now target <b>local variables</b> instead, which needs no member lookup at all:
 * {@code javap -c -p net.minecraft.client.gui.Gui} shows the layout value and the heart count are the
 * same slot, read once for the armour row and once to build the call:
 *
 * <pre>
 *      241: invokevirtual #1430   // Player.getAttributeValue:(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D
 *      244: d2f
 *      ...  fstore 13                                   // the row's own copy of the maximum
 *      514: fload         13                            // ... read back as renderHearts' argument 6
 *      523: invokevirtual  renderHearts:(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V
 * </pre>
 *
 * <p>Slot 13 is therefore both "how much room the row takes" (the armour, food and air rows are
 * placed from it) and "how many hearts are drawn", and rewriting it once fixes both. The armour row
 * adapting is not a separate feature - it follows from the same value, which is why the 26.3 source
 * needed no armour code either.
 *
 * <p>The second hook is the belt to that braces: it sits on {@code renderHearts}' own
 * {@code maxHealth} parameter, the same method whose {@code TAIL} injection demonstrably runs, so the
 * heart count is held to one even if the layout hook is lost again. It is a no-op whenever the first
 * hook did its job, because the value it then sees is already {@link #ONE_HEART: two}, which is below
 * the threshold.
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

    /** Health above which the row becomes one heart and the numbers. */
    private static final float COMPACT_THRESHOLD = 100.0F;

    /** The value that makes the vanilla loop draw a single heart. */
    private static final float ONE_HEART = 2.0F;

    /**
     * Slot of the row's own copy of the maximum health inside {@code renderPlayerHealth}.
     *
     * <p>Written as a named constant because the number itself is meaningless: it is the float local
     * that the disassembly above shows being stored once after the attribute read and read back for
     * both the armour row and the {@code renderHearts} call. Vanilla's own compiler output fixes this
     * slot for 1.20.1.
     */
    private static final int LAYOUT_LOCAL = 13;

    /**
     * Slot of the {@code float maxHealth} parameter of {@code renderHearts}.
     *
     * <p>Argument slots start after {@code this}, so over
     * {@code GuiGraphics 0, Player 1, int xLeft 2, int yLineBase 3, int rowHeight 4, int offset 5,
     * float maxHealth 6, ...} in <em>argument list</em> terms this is argument 6, and it is local slot
     * 7 - the two numbers differ by one and mixing them up is exactly the kind of mistake that would
     * silently draw ten hearts instead of one, so both are written down here.
     */
    private static final int MAX_HEALTH_ARG = 7;

    /** Gap between the heart and the numbers, and the text's vertical nudge. */
    private static final int TEXT_OFFSET_X = 12;
    private static final int TEXT_OFFSET_Y = 1;

    /**
     * Keeps the heart row to a single row of space, but only once the health is really large.
     *
     * <p>This is the layout copy of the maximum: it decides how much room the row takes, how many
     * hearts are drawn and where the armour, food and air rows sit. It is therefore the value the
     * threshold has to be applied to - clamping it unconditionally would leave every player between
     * twenty-one and a hundred health with a single row of hearts where vanilla would have drawn two
     * to five. Up to the threshold the real value is passed through untouched.
     *
     * @param maxHealth the value the row is about to be built from
     * @return the real value, brought down to one heart only when it is large
     */
    @ModifyVariable(method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("STORE"), index = LAYOUT_LOCAL, require = 1)
    private float merlinlib$oneRowWhenHuge(float maxHealth) {
        return maxHealth > COMPACT_THRESHOLD ? ONE_HEART : maxHealth;
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
        return maxHealth > COMPACT_THRESHOLD ? ONE_HEART : maxHealth;
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
        if (real <= COMPACT_THRESHOLD) {
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
