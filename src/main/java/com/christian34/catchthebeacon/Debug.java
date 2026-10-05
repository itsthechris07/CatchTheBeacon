package com.christian34.catchthebeacon;


import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logs to the plugin's logger. Unexpected errors are logged with their exception: the server shows the stack trace
 * and {@link Telemetry} reports them (System.err/printStackTrace bypasses both). Expected problems (database
 * unreachable, invalid config, ...) are logged without the exception, so they aren't reported.
 */
public class Debug {

    public static void warn(String message) {
        log(Level.WARNING, message, null);
    }

    /**
     * an unexpected error that the plugin survives (reported to Sentry)
     */
    public static void warn(String message, Throwable exception) {
        log(Level.WARNING, message, exception);
    }

    public static void info(String message) {
        log(Level.INFO, message, null);
    }

    public static void severe(String message) {
        log(Level.SEVERE, message, null);
    }

    /**
     * a serious unexpected error (reported to Sentry)
     */
    public static void severe(String message, Throwable exception) {
        log(Level.SEVERE, message, exception);
    }

    private static void log(Level level, String message, Throwable exception) {
        logger().log(level, message, exception);
    }

    /**
     * the logger of the plugin, where {@link Telemetry} listens
     */
    private static Logger logger() {
        CatchTheBeacon plugin = CatchTheBeacon.getInstance();
        return plugin != null ? plugin.getLogger() : Logger.getLogger("CatchTheBeacon");
    }

}
