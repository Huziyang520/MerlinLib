package com.huziyang520.merlinlib.platform.services;

/**
 * Loader specific bridge for looking up a mod's display name.
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
