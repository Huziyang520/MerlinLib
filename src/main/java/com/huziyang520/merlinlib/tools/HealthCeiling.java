package com.huziyang520.merlinlib.tools;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
 *
 * <h2>What changed / 1.20.1 note</h2>
 *
 * <p>An attribute modifier is addressed differently on this version, and it is not a naming difference - the
 * whole identity of a modifier changed:
 *
 * <ul>
 *   <li>26.3 ({@code ResourceLocation} ids, 1.21+): {@code AttributeModifier(ResourceLocation, double,
 *       Operation)} and {@code AttributeInstance#removeModifier(ResourceLocation)} /
 *       {@code getModifier(ResourceLocation)}. The id is a namespaced, human readable string.</li>
 *   <li>1.20.1 ({@code UUID} ids): {@code AttributeModifier(UUID, String, double, Operation)} and
 *       {@code AttributeInstance#removeModifier(UUID)} / {@code getModifier(UUID)}. The id is a
 *       {@link UUID} and the name is decoration.</li>
 * </ul>
 *
 * <p>The 1.21 change renamed the vanilla operation constants at the same time - {@code ADDITION} became
 * {@code ADD_VALUE} - so the operation below is spelled {@link AttributeModifier.Operation#ADDITION}, which is
 * what this version calls the same thing.
 *
 * <p><b>Deterministic id.</b> The {@link UUID} is derived from the constant name rather than drawn at
 * random. This matters for the same reason the shared {@code ResourceLocation} did: the id is the only handle
 * a modifier can be found and removed by, and it has to be the same one in every session and on both sides -
 * a random id would make a ceiling written before a restart impossible to replace, and a second edit would
 * stack a second modifier instead of overriding the first.
 */
public final class HealthCeiling {

    /**
     * The name the modifier carries, so it is recognisable in a debug screen.
     *
     * <p>Decoration on this version: the game addresses the modifier by {@link #ID} alone. It is still set
     * because the tooltip in a vanilla attribute dump shows it.
     */
    private static final String MODIFIER_NAME = Constants.MOD_ID + ":health_editor";

    /**
     * The modifier id shared by the editor, the servers and the respawn copy.
     *
     * <p><b>What changed / 1.20.1 note:</b> this field is a {@link UUID}, not the {@code ResourceLocation} that
     * 26.3 used. It is derived from the fixed string {@code "merlinlib:health_editor"} through the Java name
     * based UUID algorithm, so it is stable across sessions and machines and does not have to be stored
     * anywhere. The field is public and was public in 26.3, so a dependent mod may still read it; the type is
     * the only thing that moved, and it had to, because the 1.20.1 attribute API has no
     * {@code ResourceLocation} overload to pass it to.
     */
    public static final UUID ID = UUID.nameUUIDFromBytes(
            (Constants.MOD_ID + ":health_editor").getBytes(StandardCharsets.UTF_8));

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
            attribute.addPermanentModifier(new AttributeModifier(ID, MODIFIER_NAME,
                    max - attribute.getBaseValue(), AttributeModifier.Operation.ADDITION));
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
        target.addPermanentModifier(new AttributeModifier(ID, MODIFIER_NAME, ceiling.getAmount(),
                ceiling.getOperation()));
    }
}
