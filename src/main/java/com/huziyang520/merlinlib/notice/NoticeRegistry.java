package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.api.NoticeMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The notices registered from code, keyed by id.
 */
public final class NoticeRegistry {

    /** Notices registered from code, in registration order. */
    private static final Map<ResourceLocation, NoticeEntry> CODE = new LinkedHashMap<>();

    private NoticeRegistry() {
    }

    /**
     * Registers or replaces a code notice.
     *
     * @param id               the notice id, whose namespace is the owning mod
     * @param mode             when it is shown
     * @param lines            the lines to send
     * @param enabledByDefault the owner's default, used while the per mod switch says nothing
     */
    public static synchronized void register(ResourceLocation id, NoticeMode mode, List<Component> lines,
                                             boolean enabledByDefault) {
        CODE.put(id, new NoticeEntry(id, mode, lines, enabledByDefault));
    }

    /** @return the code notices, in registration order */
    public static synchronized List<NoticeEntry> codeNotices() {
        return new ArrayList<>(CODE.values());
    }
}
