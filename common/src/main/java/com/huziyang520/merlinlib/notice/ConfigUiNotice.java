package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.api.NoticeMode;
import com.huziyang520.merlinlib.config.ConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * MerlinLib's own join notice: the configuration screen was rebuilt for this version, and on the two loaders
 * that have no built-in screen it needs another mod to be shown.
 *
 * <p>Registered through the same notice api a business mod uses, so it obeys the two switches that already
 * exist - the global {@code notices.enabled} and the per mod override - and it is listed with every other
 * notice. Its own on/off switch is {@code general.notice_enabled} in the client configuration, which is also
 * what the note tells the player about.
 *
 * <p>Registered again on every configuration reload so flipping the switch takes effect on the next join
 * instead of on the next restart.
 */
public final class ConfigUiNotice {

    /** Translation key of the notice, and the setting the player can turn it off with. */
    public static final String KEY = "merlinlib.notice.config_ui";

    private static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "config_ui");

    private ConfigUiNotice() {
    }

    /** Registers or replaces the notice, following {@code general.notice_enabled}. */
    public static void register() {
        MerlinApi.notices().register(ID, NoticeMode.ONCE_PER_SAVE,
                Component.translatable(KEY), ConfigManager.client().noticeEnabled());
    }
}
