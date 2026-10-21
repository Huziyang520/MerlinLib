package com.huziyang520.merlinlib.platform.services;

/**
 * Loader specific bridge for looking up a mod's display name.
 *
 * <p>The join notice settings screen lists the mods that registered a notice, and a mod id such as
 * {@code practical_enchantments} is not something to put in front of a player when both loaders already know
 * the name the mod ships with. The two lookups have nothing in common beyond the answer, which is exactly what
 * a bridge is for.
 *
 * <p>There is no hook to install for this one, so {@link #bootstrap(Object)} does nothing.
 */
public interface IModNameBridge extends ILoaderBridge {

    /**
     * Looks up the name a mod shows in the mod list.
     *
     * @param modId the mod id
     * @return the display name, or the id itself when the mod is unknown to the loader
     */
    String modName(String modId);
}
