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
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MerlinGameEvents {

    private MerlinGameEvents() {
    }

    /**
     * Registers {@code /merlinlib}.
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
