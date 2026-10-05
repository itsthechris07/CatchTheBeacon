package com.christian34.catchthebeacon;

import io.sentry.SentryEvent;
import io.sentry.protocol.Message;
import io.sentry.protocol.SentryException;
import io.sentry.protocol.User;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Error reports only contain errors of CatchTheBeacon and nothing about the server or its players.
 *
 * @author Christian34
 */
class TelemetryTest extends PluginTestBase {

    private static Throwable thrownAt(String className) {
        RuntimeException exception = new RuntimeException("test");
        exception.setStackTrace(new StackTraceElement[]{
                new StackTraceElement(className, "run", "Test.java", 1),
                new StackTraceElement("org.bukkit.Server", "tick", "Server.java", 2)});
        return exception;
    }

    @Test
    void testsAreOffline() {
        assertTrue(Telemetry.isOffline(), "the tests must not send statistics or error reports");
    }

    @Test
    void onlyOwnErrors() {
        assertTrue(Telemetry.isOwnError(thrownAt("com.christian34.catchthebeacon.game.Game")));
        assertFalse(Telemetry.isOwnError(thrownAt("com.example.OtherPlugin")));
        assertFalse(Telemetry.isOwnError(thrownAt("com.christian34.catchthebeacon.libs.cloud.CommandManager")),
                "errors of the bundled libraries alone aren't ours");
        // the cause counts too
        RuntimeException wrapped = new RuntimeException("wrapper", thrownAt("com.christian34.catchthebeacon.game.Game"));
        wrapped.setStackTrace(new StackTraceElement[]{new StackTraceElement("org.bukkit.Server", "tick", "Server.java", 1)});
        assertTrue(Telemetry.isOwnError(wrapped));
    }

    @Test
    void sameErrorHasSameSignature() {
        Throwable first = thrownAt("com.christian34.catchthebeacon.game.Game");
        Throwable second = thrownAt("com.christian34.catchthebeacon.game.Game");
        assertEquals(Telemetry.signature(first), Telemetry.signature(second));
        assertNotEquals(Telemetry.signature(first), Telemetry.signature(thrownAt("com.christian34.catchthebeacon.game.Beacon")));
    }

    @Test
    void playersAndUuidsAreRemoved() {
        PlayerMock steve = addPlayer("Steve");
        String text = "Couldn't teleport Steve (" + steve.getUniqueId() + ")";
        assertEquals("Couldn't teleport <player> (<uuid>)", Telemetry.anonymize(text));
    }

    @Test
    void eventsAreMinimized() {
        PlayerMock steve = addPlayer("Steve");
        SentryEvent event = new SentryEvent();
        User user = new User();
        user.setIpAddress("1.2.3.4");
        event.setUser(user);
        event.setServerName("my-server.example.com");
        SentryException exception = new SentryException();
        exception.setValue("Steve has no team");
        event.setExceptions(List.of(exception));
        Message message = new Message();
        message.setFormatted("player " + steve.getUniqueId());
        event.setMessage(message);

        SentryEvent minimized = new Telemetry(plugin).minimize(event);
        assertNull(minimized.getUser());
        assertNull(minimized.getServerName());
        assertEquals("<player> has no team", minimized.getExceptions().getFirst().getValue());
        assertEquals("player <uuid>", minimized.getMessage().getFormatted());
    }

    @Test
    void debugErrorsReachTheReportHandler() {
        // Telemetry listens on the plugin's logger, Debug must log there (printStackTrace would bypass it)
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        plugin.getLogger().addHandler(handler);
        try {
            Debug.warn("Couldn't do something", new IllegalStateException("test"));
            Debug.warn("Expected problem");
        } finally {
            plugin.getLogger().removeHandler(handler);
        }
        assertEquals(2, records.size());
        Telemetry telemetry = new Telemetry(plugin);
        assertTrue(telemetry.shouldReport(records.get(0)));
        assertFalse(telemetry.shouldReport(records.get(0)), "every error is reported once");
        assertFalse(telemetry.shouldReport(records.get(1)), "warnings without an exception aren't reported");
    }

    @Test
    void roundCountersStartAgainAfterSubmission() {
        Map<String, AtomicInteger> counters = new HashMap<>();
        counters.put("rush", new AtomicInteger(2));
        counters.put("normal", new AtomicInteger(0));
        assertEquals(Map.of("rush", 2), Telemetry.drain(counters), "empty values aren't sent");
        assertEquals(Map.of(), Telemetry.drain(counters));
        assertEquals("2", Telemetry.playerBucket(2));
        assertEquals("5-8", Telemetry.playerBucket(8));
        assertEquals("17+", Telemetry.playerBucket(20));
    }

}
