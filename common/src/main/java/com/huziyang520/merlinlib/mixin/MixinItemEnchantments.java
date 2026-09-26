package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Raises the upper bound of an enchantment level from 255 to the integer limit.
 *
 * <h2>Why 255 is a wall that has to be moved this early</h2>
 *
 * <p>Vanilla builds the level codec as {@code Codec.intRange(1, 255)} and then captures it into
 * {@code ItemEnchantments.CODEC}, which in turn is captured by the item component that stores
 * enchantments. Anything above 255 therefore survives in memory and on the network (that path uses
 * {@code VAR_INT}, so it has no bound) but fails when the world is written: the codec refuses the value
 * and the enchantment is lost on the next load. That is the "vanilla cannot save it" wall.
 *
 * <p>Because both codecs are built in the static initialiser, replacing a field afterwards is too late -
 * the wider range has to be in place while the initialiser runs. That is exactly what this injection does:
 * it rewrites the upper bound of the {@code intRange} call inside {@code <clinit>}, so every codec built
 * from it - and every component that captured it - is widened by construction.
 */
@Mixin(ItemEnchantments.class)
public class MixinItemEnchantments {

    /** Set once so the log does not repeat for every class load attempt. */
    private static boolean merlinlib$logged;

    /**
     * Widens the level range to the integer limit.
     *
     * @param args the arguments of {@code Codec.intRange(min, max)}; index 1 is the upper bound
     */
    @ModifyArgs(method = "<clinit>", at = @At(value = "INVOKE",
            target = "Lcom/mojang/serialization/Codec;intRange(II)Lcom/mojang/serialization/Codec;"))
    private static void merlinlib$widenLevelRange(Args args) {
        args.set(1, Integer.MAX_VALUE);
        if (!merlinlib$logged) {
            merlinlib$logged = true;
            Constants.LOG.info("[MerlinLib] enchantment levels are no longer capped at 255 when an item is saved");
        }
    }
}
