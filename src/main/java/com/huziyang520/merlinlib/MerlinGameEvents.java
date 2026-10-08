package com.huziyang520.merlinlib;

import com.huziyang520.merlinlib.command.MerlinCommand;
import com.huziyang520.merlinlib.network.DamageFeedbackPayload;
import com.huziyang520.merlinlib.network.ServerSender;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Forge events MerlinLib subscribes to on the <b>game</b> event bus.
 *
 * <p>See {@link MerlinModEvents} for why the two buses are split across two classes rather than
 * merged: a handler on the wrong bus never runs and says nothing about it.
 *
 * <p>The bus is named explicitly even though {@code Bus.FORGE} is the annotation's default. Being
 * explicit costs one word and removes the need to remember which of the two is the default - which
 * is exactly the knowledge whose absence produced the bug that split these classes apart.
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MerlinGameEvents {

    private MerlinGameEvents() {
    }

    /**
     * Registers {@code /merlinlib}.
     *
     * <p>The 26.3 line registered this from a per loader callback that handed over the same three
     * arguments, so the command tree itself is unchanged.
     *
     * @param event the command registration
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MerlinCommand.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    /**
     * Publishes the resolved damage of a player dealt hit to the attacking client.
     *
     * <p>This is the 1.20.1 form of the 26.3 call site ({@code MerlinLibNeoForge#onIncomingDamage},
     * driven by NeoForge's {@code LivingIncomingDamageEvent}): {@code LivingHurtEvent} is the same
     * "damage about to be applied, not yet capped by the target's remaining health" moment, which is
     * the number the overlay is meant to show.
     *
     * <p>Without this handler nothing on the server ever constructs a
     * {@link DamageFeedbackPayload}, so the crosshair number silently never appears - the client cannot
     * compute the value on its own, because the enchantment pipeline needs a server level. The floating
     * numbers were unaffected (they are sampled from entity health on the client), which is why only
     * the crosshair read as broken.
     *
     * @param event the incoming damage event
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            ServerSender.sendToPlayer(attacker, new DamageFeedbackPayload(
                    event.getAmount(), false, event.getEntity().getId()));
        }
    }
}
