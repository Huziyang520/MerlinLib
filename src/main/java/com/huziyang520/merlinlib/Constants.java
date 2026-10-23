package com.huziyang520.merlinlib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project wide constants.
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
