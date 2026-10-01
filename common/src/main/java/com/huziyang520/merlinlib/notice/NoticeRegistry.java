package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.api.NoticeMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The notices registered from code, keyed by id.
 *
 * <p>Registration is a plain map write, deliberately: it happens while mod constructors run, before any
 * server exists, and the notices are only read when a player joins. Registering the same id twice replaces
 * the earlier entry, so a mod that re-registers on every server start does not end up with duplicates.
 *
 * <p>The mod the notice belongs to is never passed in by hand - it is the id's namespace. A mod therefore
 * cannot accidentally claim another mod's notices, and the settings screen can group by owner without
 * keeping a second list in step with this one.
 */
public final class NoticeRegistry {

    /** Notices registered from code, in registration order. */
    private static final Map<Identifier, NoticeEntry> CODE = new LinkedHashMap<>();

    private NoticeRegistry() {
    }

    /**
     * Registers or replaces a code notice.
     *
     * @param id               the notice id, whose namespace is the owning mod
     * @param mode             when it is shown
     * @param lines            the lines to send
     * @param enabledByDefault the owner's default for the settings screen
     */
    public static synchronized void register(Identifier id, NoticeMode mode, List<Component> lines,
                                             boolean enabledByDefault) {
        CODE.put(id, new NoticeEntry(id, mode, lines, enabledByDefault));
    }

    /** @return the code notices, in registration order */
    public static synchronized List<NoticeEntry> codeNotices() {
        return new ArrayList<>(CODE.values());
    }
}
