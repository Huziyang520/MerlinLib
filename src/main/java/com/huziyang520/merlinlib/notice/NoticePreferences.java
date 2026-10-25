// Per mod switches for join notices.

package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.config.NoticeSettings;

public final class NoticePreferences {
    private NoticePreferences() {
    }

    public static boolean isEnabled(NoticeEntry entry) {
        return NoticeSettings.isEnabled(entry.owner(), entry.enabledByDefault());
    }
}
