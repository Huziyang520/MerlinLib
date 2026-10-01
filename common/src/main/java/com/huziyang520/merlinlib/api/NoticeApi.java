package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.notice.NoticeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Join notices, reached through {@link MerlinApi#notices()}.
 *
 * <h2>What it is for</h2>
 *
 * <p>A message a mod wants to show in the chat when a player arrives - a warning, an announcement, a note
 * about something that changed. The mod says what to show and when, and the library handles the rest: the
 * three once/every modes, the player's and the server's switches for it, and the settings screen that lists
 * every mod that signed up.
 *
 * <pre>{@code
 * MerlinApi.notices().register(
 *         Identifier.fromNamespaceAndPath("mymod", "dependency_change"),
 *         NoticeMode.ONCE_PER_SAVE,
 *         Component.translatable("mymod.notice.dependency_change"));
 * }</pre>
 *
 * <h2>The rules that make it work</h2>
 *
 * <ul>
 *   <li>The <b>id's namespace is the owning mod</b>. The settings screen groups by it and the per mod switch
 *       is stored under it, so registering under someone else's namespace would put your notice in their
 *       group.</li>
 *   <li>Registration happens <b>before a player joins</b>. Registering from {@code onServerStarting} is the
 *       usual place, because that is where a mod has usually read its own configuration and knows what its
 *       default should be.</li>
 *   <li>Colours come from the component. One line may carry as many colours as it has styles, so a message
 *       can highlight its own first words - see the README for the data pack form, which spells segments
 *       out in JSON instead.</li>
 * </ul>
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
     * @param enabledByDefault the default the settings screen starts from
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
     * @param enabledByDefault the default the settings screen starts from
     */
    public void register(Identifier id, NoticeMode mode, List<Component> lines, boolean enabledByDefault) {
        NoticeRegistry.register(id, mode, lines, enabledByDefault);
    }
}
