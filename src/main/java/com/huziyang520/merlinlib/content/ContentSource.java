package com.huziyang520.merlinlib.content;

/**
 * Where a piece of content came from. The declaration order is also the arbitration order: a value
 * with a higher priority overrides the same id declared by a lower one.
 *
 * <p>Overriding is never silent: every override is logged with both sources.
 *
 * <p>{@link #DATAPACK} no longer has a producer on 1.20.1: enchantments are a code registry here, and
 * there is no data driven enchantment registry to read a datapack entry from. The constant is kept so
 * that the priority order, the diagnostic output and every string the 26.3 line printed stay
 * identical, and so a future target that regains a data driven registry does not have to reintroduce
 * it.
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
