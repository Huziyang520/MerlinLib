package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validates identifiers early so a broken id fails with a readable message instead of an obscure
 * registry error later on.
 *
 * <p>{@link net.minecraft.resources.ResourceLocation} validates its own path on construction and
 * throws, which is why {@link #parse} below catches that instead of returning a null id the way the
 * 26.3 line did: on 1.20.1 there is no {@code ResourceLocation.tryParse} that reports failure with a
 * {@code null} result, and an id read out of a hand written config file must not be able to crash the
 * game during startup.
 */
public final class IdValidator {

    /** Same rule the game applies to mod ids: 2..64 chars, lower snake case, optional dot segments. */
    private static final Pattern MOD_ID = Pattern.compile("^(?=.{2,64}$)[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$");

    /** Vanilla path rule: lower case, digits, {@code _ - . /}. */
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

    /**
     * Parses an id, adding the MerlinLib namespace when the value has none.
     *
     * @param raw value as written by the user, e.g. {@code unbreakable} or {@code othermod:thing}
     * @return the parsed id, or empty when the value is not a legal identifier
     */
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

    /**
     * @return a readable reason why {@code raw} is not a legal id, or empty when it is legal.
     */
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
