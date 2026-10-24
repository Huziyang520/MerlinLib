package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.api.NoticeMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The join notices MerlinLib registers for itself.
 */
public final class BuiltInNotices {

    /**
     * The notice about the rebuilt configuration screen.
     *
     * <p>Shown on every entry into a save, like a plain join message: it explains where the configuration
     * screen went, which a player looks for more than once.
 */
    public static final ResourceLocation CONFIG_UI = new ResourceLocation(Constants.MOD_ID, "config_ui");

    private BuiltInNotices() {
    }

    /**
     * Registers the library's own notices.
     *
     * <p>Registered as a notice rather than as a plain chat message, so the server's switches - the global
     * one in {@code server.toml} and MerlinLib's own row in {@code notices.toml}, which the join notice
     * page edits - apply to it exactly like they apply to a business mod's notice. Nothing is checked here:
     * the switches are read when the notice would be sent, so a change takes effect on the next join.
 */
    public static void register() {
        MerlinApi.notices().register(
                CONFIG_UI,
                NoticeMode.EVERY_JOIN,
                Component.translatable("merlinlib.notice.config_ui"),
                true);
    }
}
