package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.config.ServerConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per mod switches for join notices, stored in {@code server.toml}.
 *
 * <h2>Why the server holds them</h2>
 *
 * <p>The notices are sent by the server, so the decision of whether to send one has to live where the sending
 * happens. A client side switch could only hide a message that had already arrived, and on a dedicated server
 * the players cannot see each other's settings anyway.
 *
 * <h2>Why one string and not a section per mod</h2>
 *
 * <p>The list of mods is only known at runtime, and the configuration writer only knows how to put a value back
 * into a line the template already contains - a key invented at runtime would be dropped the next time the file
 * is written. One documented key holding {@code modid=true;other=false} therefore keeps every mod's choice
 * writable, and a mod that is absent from the string simply uses the default it declared.
 */
public final class NoticePreferences {

    /** The {@code server.toml} key holding every override. */
    public static final String KEY = "notices.per_mod";

    private NoticePreferences() {
    }

    /**
     * @param config the server configuration
     * @param entry  the notice
     * @return whether the notice should be sent
     */
    public static boolean isEnabled(ServerConfig config, NoticeEntry entry) {
        String override = override(config, entry.owner());
        return override == null ? entry.enabledByDefault() : Boolean.parseBoolean(override);
    }

    /**
     * @param config the server configuration
     * @param owner  the mod id
     * @return the stored override, or {@code null} when the mod's own default stands
     */
    public static String override(ServerConfig config, String owner) {
        return parse(config.noticePerMod()).get(owner);
    }

    /**
     * @param config  the server configuration
     * @param owner   the mod id
     * @param enabled the choice to store
     * @return the value the configuration key should be set to
     */
    public static String with(ServerConfig config, String owner, boolean enabled) {
        return with(config.noticePerMod(), owner, enabled);
    }

    /**
     * Applies one choice to a raw value, for a screen that is changing several at once.
     *
     * @param raw     the raw configuration value to start from
     * @param owner   the mod id
     * @param enabled the choice to store
     * @return the value the configuration key should be set to
     */
    public static String with(String raw, String owner, boolean enabled) {
        Map<String, String> values = parse(raw);
        values.put(owner, Boolean.toString(enabled));
        return join(values);
    }

    /**
     * Reads the stored overrides.
     *
     * @param raw the raw configuration value
     * @return the overrides, in a stable order
     */
    public static Map<String, String> parse(String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String part : raw.split("[;,]")) {
            int equals = part.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String owner = part.substring(0, equals).trim();
            String value = part.substring(equals + 1).trim();
            if (!owner.isEmpty() && !value.isEmpty()) {
                values.put(owner, value);
            }
        }
        return values;
    }

    private static String join(Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append(';');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }
}
