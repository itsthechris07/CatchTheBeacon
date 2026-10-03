package com.christian34.catchthebeacon;


import java.util.logging.Level;
import java.util.logging.Logger;

public class Debug {
    private static final Logger logger;

    static {
        logger = Logger.getLogger("CatchTheBeacon");
    }

    public static void catchException(Exception exception) {
        exception.printStackTrace();
    }

    public static void handleException(Exception exception) {
        exception.printStackTrace();
    }

    public static void warn(String message) {
        log(Level.WARNING, message);
    }

    public static void info(String message) {
        log(Level.INFO, message);
    }

    public static void severe(String message) {
        log(Level.SEVERE, message);
    }

    private static void log(Level level, String message) {
        logger.log(level, message);
    }

}
