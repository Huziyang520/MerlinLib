// Content report.

package com.huziyang520.merlinlib.content;

import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.stream.Collectors;

public record ContentReport(
        List<ResourceLocation> added,
        List<ResourceLocation> updated,
        List<ResourceLocation> removed,
        List<ResourceLocation> failed,
        List<ContentError> errors
) {
    public static final ContentReport EMPTY = new ContentReport(List.of(), List.of(), List.of(), List.of(), List.of());

    public boolean hasChanges() {
        return !this.added.isEmpty() || !this.updated.isEmpty() || !this.removed.isEmpty();
    }

    public boolean hasErrors() {
        return !this.failed.isEmpty() || !this.errors.isEmpty();
    }

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

    private static String names(List<ResourceLocation> ids) {
        return ids.isEmpty() ? "(none)" : ids.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));
    }
}
