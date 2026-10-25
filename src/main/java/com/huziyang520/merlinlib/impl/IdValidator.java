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
        int colon = trimmed.indexOf(':');
        String namespace = colon >= 0 ? trimmed.substring(0, colon) : Constants.MOD_ID;
        String path = colon >= 0 ? trimmed.substring(colon + 1) : trimmed;
        if (!isValidNamespace(namespace) || !isValidPath(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ResourceLocation(namespace, path));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public static Optional<String> describeProblem(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.of("id must not be empty");
        }
        String trimmed = raw.trim();
        int colon = trimmed.indexOf(':');
        if (colon >= 0) {
            String namespace = trimmed.substring(0, colon);
            String path = trimmed.substring(colon + 1);
            if (!isValidNamespace(namespace)) {
                return Optional.of("illegal namespace '" + namespace + "', expected ^(?=.{2,64}$)[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$");
            }
            if (!isValidPath(path)) {
                return Optional.of("illegal path '" + path + "', allowed: a-z 0-9 _ - . /");
            }
            return Optional.empty();
        }
        if (!isValidPath(trimmed)) {
            return Optional.of("illegal path '" + trimmed + "', allowed: a-z 0-9 _ - . /");
        }
        return Optional.empty();
    }
}
