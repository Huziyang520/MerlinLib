package com.huziyang520.merlinlib;

import com.huziyang520.merlinlib.command.MerlinCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
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
}
