package com.huziyang520.merlinlib.tools;

import com.huziyang520.merlinlib.Constants;

/**
 * Records how many particles the cap has dropped, and says so in the log.
 */
public final class ParticleDiagnostics {

    /** How long a summary line is held back, in milliseconds. */
    private static final long SUMMARY_INTERVAL_MILLIS = 1000L;

    /** Whether the first drop has been logged; the one after it switches to summaries. */
    private static boolean firstDropLogged;

    /** When the last summary was logged. */
    private static long lastSummary;

    /** How many particles have been dropped since the last summary. */
    private static long droppedSinceSummary;

    private ParticleDiagnostics() {
    }

    /**
     * Counts the particles a limit dropped and reports them.
     *
     * @param source  where the drop happened, for the log line
     * @param dropped how many particles were dropped by this one decision
     */
    public static synchronized void recordDrop(String source, int dropped) {
        if (dropped <= 0) {
            return;
        }
        if (!firstDropLogged) {
            firstDropLogged = true;
            Constants.LOG.info("[MerlinLib] the particle limit is working: the first {} particle(s) were "
                    + "dropped at the {} (client.toml: particles.limit_enabled = true)", dropped, source);
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
