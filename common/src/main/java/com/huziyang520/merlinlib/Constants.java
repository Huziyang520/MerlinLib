package com.huziyang520.merlinlib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project wide constants. Deliberately free of any loader specific type so the common, fabric and
 * neoforge projects can all use it.
 */
public final class Constants {

    public static final String MOD_ID = "merlinlib";
    public static final String MOD_NAME = "MerlinLib";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    /** Folder under the game {@code config} directory holding all MerlinLib configuration. */
    public static final String CONFIG_DIRECTORY = "MerlinLib";

    /** Id of the in-memory datapack generated from the configuration files. */
    public static final String GENERATED_PACK_ID = "merlinlib_generated";

    private Constants() {
    }
}
