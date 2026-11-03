// The health ceiling the health editor writes, and how it survives a death.

package com.huziyang520.merlinlib.tools;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class HealthCeiling {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "health_editor");

    private HealthCeiling() {
    }

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
