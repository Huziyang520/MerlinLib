// Sends join notices.

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

public final class NoticeManager {
    private NoticeManager() {
    }

    public static void install() {
        Services.LIFECYCLE.onPlayerJoin(NoticeManager::onJoin);
    }

    public static List<NoticeEntry> all(MinecraftServer server) {
        List<NoticeEntry> entries = new ArrayList<>(code());
        entries.addAll(NoticeReader.read(server));
        return entries;
    }

    public static List<NoticeEntry> code() {
        return NoticeRegistry.codeNotices();
    }

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
            if (!NoticePreferences.isEnabled(entry)) {
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

    private static boolean shouldSend(ServerLevel level, ServerPlayer player, NoticeEntry entry) {
        return switch (entry.mode()) {
            case EVERY_JOIN -> true;
            case ONCE_PER_SAVE -> remember(level, "once/" + entry.id() + '/' + player.getUUID());
            case FIRST_JOIN -> remember(level, "first/" + entry.id());
        };
    }

    private static boolean remember(ServerLevel level, String key) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return false;
        }
        NoticeStore store = server.overworld().getDataStorage()
                .computeIfAbsent(NoticeStore.TYPE, NoticeStore.STORAGE_KEY);
        if (!store.remember(key)) {
            return false;
        }
        store.setDirty();
        return true;
    }
}
