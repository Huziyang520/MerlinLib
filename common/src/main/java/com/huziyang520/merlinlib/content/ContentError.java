package com.huziyang520.merlinlib.content;

import java.util.Objects;

/**
 * A single, actionable problem found while reading content definitions.
 *
 * <p>Never report a bare "parse failed": every entry carries the file, the JSON path and what was
 * expected, so the author can fix it without guessing.
 *
 * @param file     file the problem was found in, relative to the config directory when possible
 * @param jsonPath dotted path inside the file, {@code <root>} for whole-file problems
 * @param entryId  id of the content entry, when it could be determined
 * @param expected human readable expectation, e.g. {@code integer in [1, 255]}
 * @param message  what actually went wrong
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
