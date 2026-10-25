// The join notices MerlinLib registers for itself.

package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.api.NoticeMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class BuiltInNotices {
    public static final ResourceLocation CONFIG_UI = new ResourceLocation(Constants.MOD_ID, "config_ui");

    private BuiltInNotices() {
    }

    public static void register() {
        MerlinApi.notices().register(
                CONFIG_UI,
                NoticeMode.EVERY_JOIN,
                Component.translatable("merlinlib.notice.config_ui"),
                true);
    }
}
