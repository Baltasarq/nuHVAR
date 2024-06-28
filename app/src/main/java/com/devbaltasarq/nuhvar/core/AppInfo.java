// Nuhvar (c) 2023/24 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.nuhvar.core;


/** The version information for this app. */
public class AppInfo {
    public static final String NAME = "nuHVAR";
    public static final String VERSION = "v1.0.2 20240711";
    public static final String AUTHOR = "MILE Group";
    public static final String EDITION = "Friendly Dolphin";

    public static String asShortString()
    {
        return NAME + ' ' + VERSION;
    }

    public static String asString()
    {
        return NAME + ' ' + VERSION
                + " \"" + EDITION + "\" - " + AUTHOR;
    }
}
