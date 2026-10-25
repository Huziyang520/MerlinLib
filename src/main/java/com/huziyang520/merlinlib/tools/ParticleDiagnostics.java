// Records how many particles the cap has dropped, and says so in the log.

package com.huziyang520.merlinlib.tools;

import com.huziyang520.merlinlib.Constants;

public final class ParticleDiagnostics {
    private static final long SUMMARY_INTERVAL_MILLIS = 1000L;
    private static boolean firstDropLogged;
    private static long lastSummary;
    private static long droppedSinceSummary;

    private ParticleDiagnostics() {
    }

    public static synchronized void recordDrop(String source, int dropped) {
        if (dropped <= 0) {
            return;
        }
        if (!firstDropLogged) {
            firstDropLogged = true;
            Constants.LOG.info("[MerlinLib] the particle limit is working: the first {} particle(s) were "
                    + "dropped at the {} (merlinlib-client.toml: particles.limit_enabled = true)", dropped, source);
            lastSummary = System.currentTimeMillis();
            return;
        }
        droppedSinceSummary += dropped;
        long now = System.currentTimeMillis();
        if (now - lastSummary >= SUMMARY_INTERVAL_MILLIS) {
            Constants.LOG.info("[MerlinLib] the particle limit dropped {} more particle(s) at the {}",
                    droppedSinceSummary, source);
            lastSummary = now;
            droppedSinceSummary = 0L;
        }
    }
}
