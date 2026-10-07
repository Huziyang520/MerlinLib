package com.huziyang520.merlinlib.api;

/**
 * When a join notice should be shown.
 *
 * <p>Read this alongside {@link NoticeApi}: the mode is the only thing a mod has to decide, and the three
 * answers cover the three things a message like "this mod changed its dependency" can mean.
 */
public enum NoticeMode {

    /**
     * Every time the player joins the world.
     *
     * <p>The mode for something the player has to read now - a warning, a pending change, a migration they
     * are about to walk into. This is what a plain join message has always done.
     */
    EVERY_JOIN,

    /**
     * Once per player per world save.
     *
     * <p>The mode for an announcement: the player sees it the first time they enter this world, and never
     * again in that world. Entering a different world shows it once more, because the player is starting
     * over there and nothing recorded that they already know.
     */
    ONCE_PER_SAVE,

    /**
     * Once per world save, for whoever arrives first.
     *
     * <p>The mode for something only one arrival needs to see - "the world was updated, tell the next player
     * in". Every later join in that save is silent.
     */
    FIRST_JOIN
}
