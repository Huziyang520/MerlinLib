// The notices registered from code, keyed by id.

package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.api.NoticeMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NoticeRegistry {
    private static final Map<Identifier, NoticeEntry> CODE = new LinkedHashMap<>();

    private NoticeRegistry() {
    }

    public static synchronized void register(Identifier id, NoticeMode mode, List<Component> lines,
                                             boolean enabledByDefault) {
        CODE.put(id, new NoticeEntry(id, mode, lines, enabledByDefault));
    }

    public static synchronized List<NoticeEntry> codeNotices() {
        return new ArrayList<>(CODE.values());
    }
}
