package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.notice.NoticeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Join notices, reached through {@link MerlinApi#notices()}.
 */
public final class NoticeApi {

    /** Single instance, handed out by {@link MerlinApi#notices()}. */
    public static final NoticeApi INSTANCE = new NoticeApi();

    private NoticeApi() {
    }

    /**
     * Registers a one line notice that is on by default.
     *
     * @param id   the notice id, whose namespace is the owning mod
     * @param mode when to show it
     * @param line the line to send
     */
    public void register(Identifier id, NoticeMode mode, Component line) {
        register(id, mode, List.of(line), true);
    }

    /**
     * Registers a one line notice.
     *
     * @param id               the notice id, whose namespace is the owning mod
     * @param mode             when to show it
     * @param line             the line to send
     * @param enabledByDefault the default the per mod switch starts from
     */
    public void register(Identifier id, NoticeMode mode, Component line, boolean enabledByDefault) {
        register(id, mode, List.of(line), enabledByDefault);
    }

    /**
     * Registers a notice of several lines.
     *
     * @param id               the notice id, whose namespace is the owning mod
     * @param mode             when to show it
     * @param lines            the lines to send, in order
     * @param enabledByDefault the default the per mod switch starts from
     */
    public void register(Identifier id, NoticeMode mode, List<Component> lines, boolean enabledByDefault) {
        NoticeRegistry.register(id, mode, lines, enabledByDefault);
    }
}
