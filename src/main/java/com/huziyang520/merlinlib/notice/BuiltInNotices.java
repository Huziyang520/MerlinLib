package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.api.NoticeMode;
import com.huziyang520.merlinlib.config.ConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The join notices MerlinLib registers for itself.
 */
public final class BuiltInNotices {

    /**
     * The notice about the rebuilt configuration screen.
     *
     * <p>Shown once per save and per player: it is an announcement, not a warning, and repeating it on
     * every join would be the fastest way to make players switch all notices off.
 */
    public static final ResourceLocation CONFIG_UI = new ResourceLocation(Constants.MOD_ID, "config_ui");

    private BuiltInNotices() {
    }

    /**
     * Registers the library's own notices.
     *
     * <p>Called from the mod constructor after the switches have been read, because the default it declares
     * depends on them.
 */
    public static void register() {
        boolean enabled = ConfigManager.client().noticeEnabled();
        MerlinApi.notices().register(
                CONFIG_UI,
                NoticeMode.ONCE_PER_SAVE,
                Component.translatable("merlinlib.notice.config_ui"),
                enabled);
        if (!enabled) {
            Constants.LOG.info("[MerlinLib] the notice about the rebuilt configuration screen is switched "
                    + "off (client.toml: general.notice_enabled = false)");
        }
    }
}
