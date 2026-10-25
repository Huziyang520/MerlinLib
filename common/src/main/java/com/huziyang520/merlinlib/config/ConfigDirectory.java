// Locates and fingerprints the MerlinLib configuration directory.

package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class ConfigDirectory {
    private ConfigDirectory() {
    }

    public static Path root() {
        return Path.of("config", Constants.CONFIG_DIRECTORY).toAbsolutePath().normalize();
    }

    public static List<Path> jsonFiles() {
        Path root = root();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(root)) {
            List<Path> files = new ArrayList<>();
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(files::add);
            return files;
        } catch (IOException exception) {
            Constants.LOG.error("Could not list {}", root, exception);
            return List.of();
        }
    }

    public static String fingerprint() {
        StringBuilder builder = new StringBuilder();
        for (Path file : jsonFiles()) {
            builder.append(file.getFileName()).append(':');
            try {
                builder.append(Files.size(file)).append(':').append(Files.getLastModifiedTime(file).toMillis());
            } catch (IOException exception) {
                builder.append("unreadable");
            }
            builder.append(';');
        }
        return builder.toString();
    }
}
