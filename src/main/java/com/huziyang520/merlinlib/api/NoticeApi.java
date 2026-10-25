// Join notices, reached through MerlinApi#notices().

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.notice.NoticeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public final class NoticeApi {
    public static final NoticeApi INSTANCE = new NoticeApi();

    private NoticeApi() {
    }

    public void register(ResourceLocation id, NoticeMode mode, Component line) {
        register(id, mode, List.of(line), true);
    }

    public void register(ResourceLocation id, NoticeMode mode, Component line, boolean enabledByDefault) {
        register(id, mode, List.of(line), enabledByDefault);
    }

    public void register(ResourceLocation id, NoticeMode mode, List<Component> lines, boolean enabledByDefault) {
        NoticeRegistry.register(id, mode, lines, enabledByDefault);
    }
}
