package com.huziyang520.merlinlib.content;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Outcome of one content reload. Printed in full after every startup and every {@code /reload} so
 * that authors can see exactly what changed instead of guessing.
 *
 * @param added   ids that did not exist before this reload
 * @param updated ids whose generated definition changed
 * @param removed ids that no longer exist
 * @param failed  ids or files that could not be loaded
 * @param errors  detailed, locatable problems
 */
public record ContentReport(
        List<Identifier> added,
        List<Identifier> updated,
        List<Identifier> removed,
        List<Identifier> failed,
        List<ContentError> errors
) {

    public static final ContentReport EMPTY = new ContentReport(List.of(), List.of(), List.of(), List.of(), List.of());

    public boolean hasChanges() {
        return !this.added.isEmpty() || !this.updated.isEmpty() || !this.removed.isEmpty();
    }

    public boolean hasErrors() {
        return !this.failed.isEmpty() || !this.errors.isEmpty();
    }

    /**
     * @return a multi-line, human readable summary. Never empty, so callers can always log it.
     */
    public String describe() {
        StringBuilder builder = new StringBuilder();
        builder.append("added=").append(this.added.size())
                .append(" updated=").append(this.updated.size())
                .append(" removed=").append(this.removed.size())
                .append(" failed=").append(this.failed.size());
        if (!this.added.isEmpty() || !this.updated.isEmpty() || !this.removed.isEmpty()) {
            builder.append('\n').append("  + ").append(names(this.added));
            builder.append('\n').append("  ~ ").append(names(this.updated));
            builder.append('\n').append("  - ").append(names(this.removed));
        }
        for (ContentError error : this.errors) {
            builder.append('\n').append("  ! ").append(error.format());
        }
        return builder.toString();
    }

    private static String names(List<Identifier> ids) {
        return ids.isEmpty() ? "(none)" : ids.stream().map(Identifier::toString).collect(Collectors.joining(", "));
    }
}
