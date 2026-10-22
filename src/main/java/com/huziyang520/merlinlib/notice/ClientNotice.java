package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.NoticeMode;
import com.huziyang520.merlinlib.config.ConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MerlinLib's own join notice: the one that explains why the configuration screen is no longer hand written.
 *
 * <h2>Why this one is delivered by the client</h2>
 *
 * <p>Every other notice is sent by the server, because it is the server that decides what its players are told
 * and its switches live in the common file. This notice is different: it is about this computer's own
 * configuration screen, it has to appear on every server (including one that runs no mods at all), and its
 * switch is {@code general.notice_enabled} in the client file - the same file the player opens when they want
 * to silence it. Sending it from the server would mean the one player who turned it off still saw it, because
 * a dedicated server never reads a player's client file.
 *
 * <p>It still uses the notices feature's own vocabulary - a {@link NoticeEntry} with an id in the
 * {@code merlinlib} namespace and a {@link NoticeMode} - so it is described, and behaves, like every other
 * notice; only the delivery point differs.
 */
public final class ClientNotice {

    /** The notice's id; the namespace is the owning mod, as for every other notice. */
    public static final ResourceLocation CONFIG_UI =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "config_ui");

    /** Notices already shown in this game session. */
    private static final Set<ResourceLocation> SHOWN = new HashSet<>();

    private ClientNotice() {
    }

    /**
     * @return the notice about the rebuilt configuration screen
     */
    public static NoticeEntry configUi() {
        return new NoticeEntry(CONFIG_UI, NoticeMode.ONCE_PER_SAVE,
                List.of(Component.translatable("merlinlib.notice.config_ui")), true);
    }

    /**
     * Shows the library's own notices to a player who has just joined, if they are switched on.
     *
     * <p>The switch is read at the moment of sending, so turning it off takes effect on the next join instead
     * of on the next restart.
     *
     * @param player the local player
     */
    public static synchronized void showOnJoin(Player player) {
        if (player == null || !ConfigManager.client().noticeEnabled()) {
            return;
        }
        NoticeEntry entry = configUi();
        if (!SHOWN.add(entry.id())) {
            return;
        }
        for (Component line : entry.lines()) {
            player.displayClientMessage(line, false);
        }
    }
}
