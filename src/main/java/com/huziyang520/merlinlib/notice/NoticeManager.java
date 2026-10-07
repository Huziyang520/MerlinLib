package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.ServerConfig;
import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends join notices.
 *
 * <p>Installed once at startup; from then on every player who joins is offered every notice they have not
 * already seen, in registration order for code notices and resource order for pack notices.
 *
 * <p>Every switch is read at the moment of sending - the global one, the per mod one, and the recorded ones
 * in the save - which is what makes a settings change take effect on the next join instead of on the next
 * restart.
 *
 * <p>1.20.1 note: the only real divergence is in {@link #remember}, which reaches the save's store through the
 * older three argument {@code DimensionDataStorage#computeIfAbsent(load, create, name)} instead of the single
 * {@code SavedDataType} argument the 26.3 line passes. Everything else in this class is a rename.
 */
public final class NoticeManager {

    private NoticeManager() {
    }

    /** Hooks the join moment. Safe to call from the mod constructor. */
    public static void install() {
        Services.LIFECYCLE.onPlayerJoin(NoticeManager::onJoin);
    }

    /**
     * Every notice that could be sent in this server, code and data pack together.
     *
     * @param server the running server
     * @return the notices, in a stable order
     */
    public static List<NoticeEntry> all(MinecraftServer server) {
        List<NoticeEntry> entries = new ArrayList<>(code());
        entries.addAll(NoticeReader.read(server));
        return entries;
    }

    /**
     * The notices registered from code, which both sides can see, because the same mods run on both.
     *
     * <p>What the settings screen lists on a dedicated server: a client cannot ask that server what its packs
     * contain, so the switches are named by the mods rather than by the packs, and they still work.
     *
     * @return the code notices
     */
    public static List<NoticeEntry> code() {
        return NoticeRegistry.codeNotices();
    }

    /**
     * Offers every unseen notice to a player who just joined.
     *
     * @param player the player
     */
    private static void onJoin(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        ServerConfig config = ConfigManager.server();
        if (!config.noticesEnabled()) {
            return;
        }
        int sent = 0;
        for (NoticeEntry entry : all(server)) {
            if (!NoticePreferences.isEnabled(config, entry)) {
                continue;
            }
            if (!shouldSend(level, player, entry)) {
                continue;
            }
            for (Component line : entry.lines()) {
                player.sendSystemMessage(line);
            }
            sent++;
        }
        if (sent > 0) {
            Constants.LOG.info("[MerlinLib] sent {} join notice(s) to {}", sent, player.getScoreboardName());
        }
    }

    /**
     * Decides whether one notice is due, recording it when the mode says it may be shown only once.
     *
     * @param level  the level the player is in
     * @param player the player
     * @param entry  the notice
     * @return true when it should be sent
     */
    private static boolean shouldSend(ServerLevel level, ServerPlayer player, NoticeEntry entry) {
        return switch (entry.mode()) {
            case EVERY_JOIN -> true;
            case ONCE_PER_SAVE -> remember(level, "once/" + entry.id() + '/' + player.getUUID());
            case FIRST_JOIN -> remember(level, "first/" + entry.id());
        };
    }

    /**
     * Records a key in the save's store, reporting whether it is new.
     *
     * <p>The store lives on the overworld's data storage whether or not the player is in the overworld, because
     * a world has one save's worth of "already shown" and not one per dimension - a player who walks through a
     * nether portal must not be handed the notice again.
     *
     * @param level the level, used to reach the save's storage
     * @param key   the key to record
     * @return true when the key had not been recorded before
     */
    private static boolean remember(ServerLevel level, String key) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return false;
        }
        NoticeStore store = server.overworld().getDataStorage()
                .computeIfAbsent(NoticeStore::load, NoticeStore::new, NoticeStore.STORAGE_KEY);
        if (!store.remember(key)) {
            return false;
        }
        store.setDirty();
        return true;
    }
}
