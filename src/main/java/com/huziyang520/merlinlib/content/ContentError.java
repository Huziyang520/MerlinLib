// A single, actionable problem found while reading content definitions.

package com.huziyang520.merlinlib.content;

import java.util.Objects;

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

    public String format() {
        return "[" + this.file + "] " + this.jsonPath + " -> " + this.entryId
                + " (expected " + this.expected + "): " + this.message;
    }
}
