package com.huziyang520.merlinlib.config;

/**
 * Which of the two configuration files a value belongs to.
 *
 * <p>MerlinLib keeps one file for this computer and one for the game, and they are stored by different
 * backends ({@code CLIENT} and {@code COMMON}). Naming the file at every call site instead of guessing from
 * the key keeps the two apart: the same key name could otherwise be asked of the wrong file and silently
 * answered with a default.
 */
public enum ConfigScope {

    /** This computer's switches: display options, particle cap, editor steps, macros. */
    CLIENT,

    /** The authoritative switches of the game: permissions, toolkit limits, join notices. */
    SERVER
}
