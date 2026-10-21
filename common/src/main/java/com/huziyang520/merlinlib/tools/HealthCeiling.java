package com.huziyang520.merlinlib.tools;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * The health ceiling the health editor writes, and how it survives a death.
 *
 * <h2>Why a permanent modifier is not enough on its own</h2>
 *
 * <p>{@code ServerPlayer#restoreFrom} copies attribute <em>base values</em> always, but copies the
 * <em>modifiers</em> only when it is called with {@code restoreAll = true} - and a death respawn calls it
 * with {@code false}. A permanent modifier is therefore saved to disk and survives a restart, yet is dropped
 * the moment the player dies and respawns, which is exactly the behaviour that looked like "the edit is not
 * kept". Both loaders hand us a clone/copy-from event on respawn, and {@link #copyTo} is what they call.
 *
 * <h2>Setting it to zero</h2>
 *
 * <p>A maximum of zero removes the modifier instead of writing it, and the zero current health then kills the
 * holder. Nothing is left to copy on respawn, so the player comes back with the default maximum - the safety
 * net this switch exists for. Any other value is written as a permanent modifier and copied across respawns.
 *
 * <p>One id is used for everything, so a second edit replaces the first and the modifier can always be found
 * again to remove it.
 */
public final class HealthCeiling {

    /** The modifier id shared by the editor, the servers and the respawn copy. */
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "health_editor");

    private HealthCeiling() {
    }

    /**
     * Writes a new maximum health onto a living entity.
     *
     * @param target the entity to edit
     * @param max    the new maximum, at least 0; {@code 0} removes the ceiling instead of writing it
     */
    public static void apply(LivingEntity target, int max) {
        AttributeInstance attribute = target.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(ID);
        if (max > 0) {
            attribute.addPermanentModifier(new AttributeModifier(ID, max - attribute.getBaseValue(),
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /**
     * Carries the ceiling over to a player that replaced another one.
     *
     * <p>Called from the respawn/clone hook of each loader. The amount is copied verbatim rather than
     * recomputed, because the base value it was measured against does not change.
     *
     * @param oldPlayer the player that is going away, may be {@code null}
     * @param newPlayer the player taking its place, may be {@code null}
     */
    public static void copyTo(Player oldPlayer, Player newPlayer) {
        if (oldPlayer == null || newPlayer == null || oldPlayer == newPlayer) {
            return;
        }
        AttributeInstance source = oldPlayer.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance target = newPlayer.getAttribute(Attributes.MAX_HEALTH);
        if (source == null || target == null) {
            return;
        }
        AttributeModifier ceiling = source.getModifier(ID);
        if (ceiling == null) {
            return;
        }
        target.removeModifier(ID);
        target.addPermanentModifier(new AttributeModifier(ID, ceiling.amount(), ceiling.operation()));
    }
}
