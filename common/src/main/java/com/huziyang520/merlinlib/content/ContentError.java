package com.huziyang520.merlinlib.content;

import java.util.Objects;

/**
 * A single, actionable problem found while reading content definitions.
 */
public record ContentError(
        String file,
        String jsonPath,
        String entryId,
        String expected,
        String message
) {

    public ContentError {
        Objects.requireNonNull(file, "file");
        jsonPath = jsonPath == null || jsonPath.isBlank() ? "<root>" : jsonPath;
        entryId = entryId == null ? "-" : entryId;
        expected = expected == null ? "-" : expected;
        message = message == null ? "unknown error" : message;
    }

    /**
     * Renders a single line suitable for the log and for the post-reload report.
     */
    public String format() {
        return "[%s] %s -> %s (expected %s): %s".formatted(this.file, this.jsonPath, this.entryId, this.expected, this.message);
    }
}
