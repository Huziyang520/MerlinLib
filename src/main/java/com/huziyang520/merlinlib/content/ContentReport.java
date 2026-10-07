package com.huziyang520.merlinlib.content;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Outcome of one content refresh. Printed in full after every startup so that authors can see exactly
 * what was registered instead of guessing.
 *
 * <p>The 26.3 line ran this after every {@code /reload} as well, because a data driven registry could
 * gain and lose entries while the game ran. On 1.20.1 an enchantment cannot be added to the registry
 * after the registry event, so this is produced once per launch and the "updated"/"removed" lists are
 * always empty in practice. They are kept because the report is also what {@code /merlinlib list}
 * prints, and because a future target that regains a dynamic registry should not have to reintroduce
 * the vocabulary.
 *
 * @param added   ids that were registered by this refresh
 * @param updated ids whose definition changed
 * @param removed ids that no longer exist
 * @param failed  ids or files that could not be loaded
 * @param errors  detailed, locatable problems
 */
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

    private static String names(List<ResourceLocation> ids) {
        return ids.isEmpty() ? "(none)" : ids.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));
    }
}
