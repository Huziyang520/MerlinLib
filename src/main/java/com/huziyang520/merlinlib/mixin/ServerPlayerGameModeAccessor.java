package com.huziyang520.merlinlib.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the player a {@link ServerPlayerGameMode} belongs to.
 */
@Mixin(ServerPlayerGameMode.class)
public interface ServerPlayerGameModeAccessor {

    /** @return the player this game mode belongs to */
    @Accessor("player")
    ServerPlayer merlinlib$player();
}
