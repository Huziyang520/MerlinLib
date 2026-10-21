package com.huziyang520.merlinlib.tools;

import com.huziyang520.merlinlib.Constants;

/**
 * Records how many particles the cap has dropped, and says so in the log.
 *
 * <h2>Why it lives outside the mixin package</h2>
 *
 * <p>It is a plain helper, not a mixin. A class in the package named by {@code merlinlib.mixins.json} is
 * treated as part of the mixin configuration, and referencing one of those directly from game code fails at
 * the worst possible moment - the first time the game loads it - with
 * {@code is in a defined mixin package ... and cannot be referenced directly}. A mixin may reference other
 * classes freely; what it may not do is live next to a class that the mixin config owns.
 *
 * <h2>Why a log line at all</h2>
 *
 * <p>The cap lives in client side code, which a headless smoke test never loads, so "it is registered" was
 * the most that could be said about it - and that is not the same as "it actually dropped something". The
 * first drop is therefore logged once at {@code INFO}, and later drops are summarised at most once a second so
 * a pathological hit cannot flood the log. A run that never logs means the cap never had to do anything.
 *
 * <p>Deliberately reached from both ends: the server side cap stops an absurd count from being put on the
 * wire, and the client side caps stop whatever arrives. Both report through here, so one line in the log
 * tells which of the two was needed.
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
