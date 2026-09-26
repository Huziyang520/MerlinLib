package com.huziyang520.merlinlib.content;

/**
 * Where a piece of content came from. The declaration order is also the arbitration order: a value
 * with a higher priority overrides the same id declared by a lower one.
 *
 * <p>Overriding is never silent: every override is logged with both sources.
 */
public enum ContentSource {

    /** Content shipped by an external datapack. Lowest priority, always overridable. */
    DATAPACK(0, "external datapack"),

    /** Content registered through the MerlinLib java API. */
    API(1, "code API"),

    /** Content defined in {@code config/MerlinLib/*.json}. Highest priority. */
    CONFIG(2, "config file");

    private final int priority;
    private final String displayName;

    ContentSource(int priority, String displayName) {
        this.priority = priority;
        this.displayName = displayName;
    }

    public int priority() {
        return this.priority;
    }

    public String displayName() {
        return this.displayName;
    }

    /**
     * @return {@code true} when {@code this} is allowed to override {@code other}.
     */
    public boolean overrides(ContentSource other) {
        return this.priority > other.priority;
    }
}
