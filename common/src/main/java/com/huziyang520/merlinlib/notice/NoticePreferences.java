package com.huziyang520.merlinlib.notice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parser for the retired {@code notices.per_mod} string.
 *
 * <h2>What this used to be</h2>
 *
 * <p>Every per mod notice switch lived in one {@code server.toml} line,
 * {@code per_mod = "modid=true;other=false"}, because the configuration writers of both loaders can only write a
 * key they were told about at build time while the set of mods is only known at runtime. It worked, but it was a
 * text field in the settings screen that no player could reasonably edit, and a mod id typed with one typo
 * silently changed nothing.
 *
 * <h2>What it is now</h2>
 *
 * <p>Only a reader. The switches themselves live in {@code config/MerlinLib/notices.toml}, one key per mod - see
 * {@code com.huziyang520.merlinlib.config.NoticeSettings}, which calls {@link #parseLegacy(String)} once to bring
 * an old string forward and never looks at one again. This class stays because the string a previous build wrote
 * is still on players' disks, and it has to be read rather than ignored.
 */
public final class NoticePreferences {

    private NoticePreferences() {
    }

    /**
     * Reads the overrides out of a raw {@code notices.per_mod} value.
     *
     * @param raw the string as the retired key held it, {@code modid=true;other=false}
     * @return mod id to the stored value, in the order the string listed them
     */
    public static Map<String, String> parseLegacy(String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        if (raw == null) {
            return values;
        }
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
}
