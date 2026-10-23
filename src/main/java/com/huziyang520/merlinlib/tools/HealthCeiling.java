package com.huziyang520.merlinlib.tools;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * The health ceiling the health editor writes, and how it survives a death.
 */
public final class HealthCeiling {

    /** The modifier id shared by the editor, the servers and the respawn copy. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "health_editor");

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
