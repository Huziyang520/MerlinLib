// Loader specific bridge for looking up a mod's display name.

package com.huziyang520.merlinlib.platform.services;

public interface IModNameBridge extends ILoaderBridge {
    String modName(String modId);
}
