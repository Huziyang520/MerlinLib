// Where a piece of content came from.

package com.huziyang520.merlinlib.content;

public enum ContentSource {
    DATAPACK(0, "external datapack"),

    API(1, "code API"),

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

    public boolean overrides(ContentSource other) {
        return this.priority > other.priority;
    }
}
