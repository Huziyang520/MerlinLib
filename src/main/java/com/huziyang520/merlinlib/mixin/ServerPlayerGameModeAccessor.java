package com.huziyang520.merlinlib.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the player a {@link ServerPlayerGameMode} belongs to.
 *
 * <p>The game mode keeps the player as a field and offers no getter, and a mixin cannot reach
 * another class's field by casting alone, so the access is generated. This interface is added to
 * {@link ServerPlayerGameMode} itself, which is what lets {@link MixinPlayerDestroyBlock} cast
 * itself to this type and call the method below.
 *
 * <h2>Why an {@code @Accessor} and not a {@code @Shadow} field</h2>
 *
 * <p>{@code MixinPlayerDestroyBlock} first carried a {@code @Shadow ServerPlayer player} field, and
 * the game refused to open a world with
 *
 * <pre>
 * InvalidMixinException @Shadow field player was not located in the target class
 *   net.minecraft.server.level.ServerPlayerGameMode. Using refmap merlinlib.refmap.json
 * </pre>
 *
 * <p>The field itself is not at fault - it exists with exactly this name and type (see below) and
 * the build's TSRG table maps it. The entry is missing from the generated refmap, which is the file
 * the runtime consults when it cannot find the plain name, and a {@code @Shadow} member does not get
 * an entry written into this project's refmap on this line. An {@code @Accessor} interface mixin
 * takes a different route through the annotation processor and its reference <b>does</b> appear in
 * the refmap - the same route {@link AvoidEntityGoalAccessor} and {@link RangedAttributeAccessor}
 * already take on this line.
 *
 * <h2>1.20.1 evidence</h2>
 *
 * <p>Verified with {@code javap -p -s net.minecraft.server.level.ServerPlayerGameMode}:
 *
 * <pre>
 * protected final net.minecraft.server.level.ServerPlayer player;
 *   descriptor: Lnet/minecraft/server/level/ServerPlayer;
 * </pre>
 *
 * <p>The field is {@code final}, so this interface exposes a read-only accessor; asking for a setter
 * would be a mixin that refuses to apply.
 */
@Mixin(ServerPlayerGameMode.class)
public interface ServerPlayerGameModeAccessor {

    /** @return the player this game mode belongs to */
    @Accessor("player")
    ServerPlayer merlinlib$player();
}
