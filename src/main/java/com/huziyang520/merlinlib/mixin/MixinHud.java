package com.huziyang520.merlinlib.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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
 * {@code extractHearts} / {@code GuiGraphicsExtractor}) that is four independent renames, so the three
 * injection points are re-derived from the bytecode below rather than translated by pattern.
 *
 * <h3>Injection 1 - the layout value</h3>
 *
 * <p>{@code javap -p -c net.minecraft.client.gui.Gui} locates the attribute read inside
 * {@code renderPlayerHealth}:
 *
 * <pre>
 *      237: aload_2
 *      238: getstatic     #1426   // Field net/minecraft/world/entity/ai/attributes/Attributes.MAX_HEALTH:Lnet/minecraft/world/entity/ai/attributes/Attribute;
 *      241: invokevirtual #1430   // Method net/minecraft/world/entity/player/Player.getAttributeValue:(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D
 *      244: d2f
 * </pre>
 *
 * <p>The redirect target is therefore
 * {@code Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D},
 * and the handler takes an {@link Attribute}, <b>not</b> a {@code Holder<Attribute>} as the 26.3 source
 * does. Both overloads exist on this version -
 * {@code getAttributeValue(Holder)} and {@code getAttributeValue(Attribute)} - and the bytecode above
 * proves the one actually called is the {@code Attribute} form. Redirecting the {@code Holder} form
 * would find no call site and, with {@code require = 1}, abort startup.
 *
 * <p>The 26.3 source names two methods for this redirect ({@code extractPlayerHealth} and
 * {@code extractHealthLevel}) because the two loaders had split the code differently. On 1.20.1 the
 * method is a single vanilla one, {@code renderPlayerHealth}, so the multi-name form collapses to one
 * name. Keeping a name that does not exist would be exactly the mistake {@code require = 1} is there
 * to catch.
 *
 * <h3>Injection 2 - the heart count</h3>
 *
 * <p>The same disassembly shows the one call into {@code renderHearts}:
 *
 * <pre>
 *      514: fload         13
 *      516: iload_3
 *      517: iload         7
 *      519: iload         14
 *      521: iload         4
 *      523: invokevirtual #1454   // Method renderHearts:(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V
 * </pre>
 *
 * <p>The argument indices are readable directly off this listing: {@code aload_1} is the
 * {@code GuiGraphics} at index 0 and {@code aload_2} the {@code Player} at index 1, so the float at
 * index 6 is the heart-count value and the two ints at 7 and 8 are the current and old health. Those
 * are the same indices the 26.3 source uses, so the handler body ports unchanged once the descriptor
 * is corrected to the 1.20.1 one.
 *
 * <h3>Injection 3 - the numbers</h3>
 *
 * <p>{@code renderHearts} keeps its tail free for a plain {@code @Inject} at {@code RETURN}, and the
 * text is drawn through {@code GuiGraphics#drawString}, which is the 1.20.1 name for what 26.3 calls
 * {@code text}. The {@code Minecraft#font} field still exists, so the readout needs no accessor.
 *
 * <h3>The armour row</h3>
 *
 * <p>The 26.3 source speaks of the armour row as a separate drawing step. On 1.20.1 the armour row is
 * drawn <b>inline inside {@code renderPlayerHealth}</b>; there is no separate method to hook, and this
 * mixin does not try to hook one. That is not a missing feature: the change this mixin makes is to the
 * heart row and to the layout value that decides where the rows below it sit, so moving the armour row
 * correctly follows from {@code renderHearts} being drawn in one row's worth of space. Nothing else in
 * the armour path needs to know.
 */
@Mixin(Gui.class)
public class MixinHud {

    /** Health above which the row becomes one heart and the numbers. */
    private static final float COMPACT_THRESHOLD = 100.0F;

    /** The value that makes the vanilla loop draw a single heart. */
    private static final float ONE_HEART = 2.0F;

    /** The same, as one row's worth of health, for the layout calculation. */
    private static final double ONE_ROW_OF_HEALTH = 20.0D;

    /**
     * Index of the {@code maxHealth} argument in the {@code Gui#renderHearts} call.
     *
     * <p>Zero-based, counted over the call's own argument list:
     * {@code GuiGraphics 0, Player 1, int xLeft 2, int yLineBase 3, int rowHeight 4, int offset 5,
     * float maxHealth 6, int current 7, int old 8, int absorption 9, boolean blink 10}. Named rather
     * than written as a bare {@code 6} so that the thing that matters - which argument this is - is
     * stated where it is used.
     */
    private static final int PARAM_MAX_HEALTH = 6;

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
     * @param player    the player the row belongs to
     * @param attribute the attribute being read, always the maximum health
     * @return the real value, brought down to one vanilla row only when it is large
     */
    @Redirect(method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V", require = 1,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D"))
    private double merlinlib$layoutOneRow(Player player, Attribute attribute) {
        double real = player.getAttributeValue(attribute);
        return real > COMPACT_THRESHOLD ? ONE_ROW_OF_HEALTH : real;
    }

    /**
     * Reduces the heart count to one once the health is large.
     *
     * <h2>Why this is a {@code @ModifyArg} on call argument 6</h2>
     *
     * <p>{@code renderPlayerHealth} calls
     * {@code renderHearts(graphics, player, xLeft, yLineBase, rowHeight, offset, maxHealth, ...)}.
     * Argument 6 is that {@code float maxHealth} - the value the loop uses as the heart row's width -
     * and it is the same value the 26.3 line set through {@code args.set(6, ONE_HEART)}. Replacing it
     * before the call reaches {@code renderHearts} is therefore equivalent, and the rest of the call
     * is untouched.
     *
     * <p>The player is recovered from the call's own argument 1 rather than from a field, which means
     * the real maximum can be re-read: the value being passed has already been brought down for the
     * layout by the {@code getAttributeValue} redirect above, so it can never answer this question.
     *
     * <h2>Why not the 26.3 line's {@code @ModifyArgs}, and why not {@code @Redirect} either</h2>
     *
     * <p>Both alternatives were tried on this project and both broke the game at startup, so the
     * reasons are recorded rather than left to be rediscovered:
     *
     * <p><b>{@code @ModifyArgs}</b> - which is what 26.3 used - has Mixin <b>generate a class at
     * runtime</b>: {@code ArgsClassGenerator}'s {@code CLASS_NAME_BASE} is
     * {@code "org.spongepowered.asm.synthetic.args.Args$"}, so it synthesises {@code Args$1},
     * {@code Args$2} ... per call site. On the Mixin 0.8.5 that Forge 1.20.1 bundles that generation
     * does not happen, and the game dies with:
     * <pre>
     * java.lang.NoClassDefFoundError: org/spongepowered/asm/synthetic/args/Args$1
     *     at net.minecraft.client.Minecraft.&lt;init&gt;(Minecraft.java:521)
     * </pre>
     * That stack trace never names the offending mixin, and the class it cannot find is supposed to be
     * generated - it exists in no jar on the machine. It is a genuinely misleading failure.
     *
     * <p><b>{@code @Redirect}</b> - replacing the whole call - avoids the generated class, but it then
     * has to re-invoke {@code renderHearts} to draw anything, and that method is {@code protected} on
     * {@link Gui}. Reaching it needs an {@code @Shadow}, and the shadow failed to resolve:
     * <pre>
     * InvalidMixinException: @Shadow method renderHearts in merlinlib.mixins.json:MixinHud was not
     * located in the target class net.minecraft.client.gui.Gui. Using refmap merlinlib.refmap.json
     * </pre>
     * The target does exist - {@code protected void m_168688_(...)} is present in the runtime jar with
     * exactly that descriptor, and the refmap maps it correctly - so the failure is in how Mixin 0.8.5
     * attaches a shadow that the mixin itself introduced. That is not worth fighting when a simpler
     * injector exists.
     *
     * <p>{@code @ModifyArg} needs neither a generated class nor a shadow, which is why it is the one
     * that works here.
     *
     * @param graphics         the draw context, unused beyond context
     * @param player           the player the row belongs to
     * @param xLeft            the left edge of the heart row
     * @param yLineBase        the baseline of the heart row
     * @param healthRowHeight  the vanilla row spacing
     * @param heartOffsetIndex the vanilla regeneration heartbeat offset
     * @param maxHealth        the maximum health the call would use - the value being replaced
     * @param currentHealth    the health as a whole number
     * @param oldHealth        the previously displayed health
     * @param absorption       the absorption amount
     * @param blink            whether the row is blinking after a change
     * @return the value to pass as {@code maxHealth}
     */
    @ModifyArg(method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V", require = 1,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V"),
            index = PARAM_MAX_HEALTH)
    private float merlinlib$oneHeartWhenHuge(GuiGraphics graphics, Player player, int xLeft, int yLineBase,
                                             int healthRowHeight, int heartOffsetIndex, float maxHealth,
                                             int currentHealth, int oldHealth, int absorption, boolean blink) {
        // The player is taken from the call's own argument rather than a field, because the value that
        // arrives in maxHealth has already been reduced by the getAttributeValue redirect above when
        // the health is large - so it cannot be used to decide whether the health is large.
        if (realHealth(player, currentHealth, oldHealth) > COMPACT_THRESHOLD) {
            return ONE_HEART;
        }
        return maxHealth;
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
        if (real <= COMPACT_THRESHOLD) {
            return;
        }
        Component text = Component.literal(currentHealth + "/" + Math.round(real));
        int color = absorption > 0 ? 0xFFFFD700 : 0xFFFFFFFF;
        // drawString is the 1.20.1 name for the 26.3 source's GuiGraphicsExtractor#text. It takes the
        // same arguments, including the drop shadow flag, so the readout is drawn identically.
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
