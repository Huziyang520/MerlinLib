package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.config.NoticeSettings;

/**
 * Per mod switches for join notices.
 *
 * <h2>Why the server holds them</h2>
 *
 * <p>The notices are sent by the server, so the decision of whether to send one has to live where the sending
 * happens. A client side switch could only hide a message that had already arrived, and on a dedicated server
 * the players cannot see each other's settings anyway.
 *
 * <h2>Where they are stored</h2>
 *
 * <p>In {@code config/MerlinLib/notices.toml}, one line per mod, owned by {@link NoticeSettings}. They used to
 * be one hand edited string in the common configuration file, {@code notices.per_mod}, which was drawn as a
 * text box on the configuration screen: the string was the only shape the loader's file could hold for keys
 * that exist only at runtime, and typing {@code modid=true;other=false} by hand was the price of that. The page
 * {@code /merlinlib notices} opens replaces both the string and the text box, and the old value is carried over
 * the first time this build starts - see {@link NoticeSettings#captureLegacy()}.
 *
 * <p>This class stays as the one question the notices feature asks, so the notice code itself does not have to
 * know where its switches came from.
 */
public final class NoticePreferences {

    private NoticePreferences() {
    }

    /**
     * @param entry the notice
     * @return whether the notice should be sent
     */
    public static boolean isEnabled(NoticeEntry entry) {
        return NoticeSettings.isEnabled(entry.owner(), entry.enabledByDefault());
    }
}
