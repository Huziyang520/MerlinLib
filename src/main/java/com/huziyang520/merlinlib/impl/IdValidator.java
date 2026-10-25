// Validates identifiers early so a broken id fails with a readable message instead of an obscure registry error later on.

package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;
import java.util.regex.Pattern;

public final class IdValidator {
    private static final Pattern MOD_ID = Pattern.compile("^(?=.{2,64}$)[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$");
    private static final Pattern PATH = Pattern.compile("^[a-z0-9_.\\-/]+$");

    private IdValidator() {
    }

    public static boolean isValidNamespace(String namespace) {
        return namespace != null && MOD_ID.matcher(namespace).matches();
    }

    public static boolean isValidPath(String path) {
        return path != null && !path.isEmpty() && PATH.matcher(path).matches();
    }

    public static boolean isValid(ResourceLocation id) {
        return id != null && isValidNamespace(id.getNamespace()) && isValidPath(id.getPath());
    }

    public static Optional<ResourceLocation> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        ResourceLocation id = trimmed.indexOf(ResourceLocation.NAMESPACE_SEPARATOR) >= 0
                ? ResourceLocation.tryParse(trimmed)
                : ResourceLocation.tryBuild(Constants.MOD_ID, trimmed);
        if (id == null || !isValid(id)) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    public static Optional<String> describeProblem(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.of("id must not be empty");
        }
        String trimmed = raw.trim();
        if (trimmed.indexOf(ResourceLocation.NAMESPACE_SEPARATOR) >= 0) {
            ResourceLocation id = ResourceLocation.tryParse(trimmed);
            if (id == null) {
                return Optional.of("not a well formed identifier, expected <namespace>:<path>");
            }
            if (!isValidNamespace(id.getNamespace())) {
                return Optional.of("illegal namespace '" + id.getNamespace() + "', expected ^(?=.{2,64}$)[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$");
            }
            if (!isValidPath(id.getPath())) {
                return Optional.of("illegal path '" + id.getPath() + "', allowed: a-z 0-9 _ - . /");
            }
            return Optional.empty();
        }
        if (!isValidPath(trimmed)) {
            return Optional.of("illegal path '" + trimmed + "', allowed: a-z 0-9 _ - . /");
        }
        return Optional.empty();
    }
}
