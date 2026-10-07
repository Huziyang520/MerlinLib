package com.huziyang520.merlinlib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project wide constants.
 *
 * <p>This project targets a single loader (Forge for Minecraft 1.20.1), so unlike the 26.3 line
 * there is no common/fabric/neoforge split. The class is kept free of loader types anyway: it is
 * read from the config layer and from the Forge bridges alike.
 */
public final class Constants {

    public static final String MOD_ID = "merlinlib";
    public static final String MOD_NAME = "MerlinLib";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    /** Folder under the game {@code config} directory holding all MerlinLib configuration. */
    public static final String CONFIG_DIRECTORY = "MerlinLib";

    private Constants() {
    }
}
