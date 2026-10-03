package com.christian34.catchthebeacon;

import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.Variant;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.SentryException;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

/**
 * Anonymous statistics (bStats, opt-out for all plugins in plugins/bStats/config.yml) and error reports (Sentry,
 * opt-out with sentry.enabled in config.yml).
 * <p>
 * The error reports are minimized: only exceptions with CatchTheBeacon in their stack trace, no server name, no
 * player names or uuids (replaced), no user data; the Sentry project must not store
 * IP addresses (project settings - Security &amp; Privacy).
 *
 * @author Christian34
 */
public class Telemetry {
    /**
     * the plugin on bstats.org
     */
    static final int BSTATS_ID = 34381;
    /**
     * the Sentry project (region US, Sentry is certified under the EU-U.S. Data Privacy Framework), empty: no error reports
     */
    static final String SENTRY_DSN = "https://8ac212d7f167bfe8280feee32de51599@o393387.ingest.us.sentry.io/4512168500330496";
    /**
     * at most this many error reports per server start (a broken loop mustn't send thousands)
     */
    static final int MAX_REPORTS = 25;
    static final String PACKAGE = "com.christian34.catchthebeacon.";
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private final CatchTheBeacon plugin;
    private final Set<String> reported = new HashSet<>();
    /**
     * finished rounds since the last bStats submission (every 30 minutes), per variant and per player count
     */
    private final AtomicInteger rounds = new AtomicInteger();
    private final Map<String, AtomicInteger> variants = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundSizes = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> variantVotes = new ConcurrentHashMap<>();
    @Nullable
    private Handler handler;

    public Telemetry(CatchTheBeacon plugin) {
        this.plugin = plugin;
    }

    /**
     * -Dctb.offline=true: nothing is sent (tests)
     */
    public static boolean isOffline() {
        return Boolean.getBoolean("ctb.offline");
    }

    public void start() {
        if (isOffline()) return;
        startMetrics();
        startErrorReports();
    }

    private void startMetrics() {
        Metrics metrics = new Metrics(plugin, BSTATS_ID);
        var config = plugin.getFileManager().getConfigFile();
        metrics.addCustomChart(new SimplePie("network_mode", () -> plugin.getNetworkManager().getMode().name().toLowerCase()));
        metrics.addCustomChart(new SimplePie("database", () -> config.getBoolean("sql.enabled") ? "mysql" : "sqlite"));
        metrics.addCustomChart(new SimplePie("combat", () -> String.valueOf(config.getString("game.combat")).toLowerCase()));
        metrics.addCustomChart(new SimplePie("variant_voting", () -> String.valueOf(config.getBoolean("variants.voting"))));
        metrics.addCustomChart(new SimplePie("arenas", () -> bucket(plugin.getMapHandler().getGameMaps().size())));
        metrics.addCustomChart(new SimplePie("placeholderapi", () ->
                Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? "installed" : "not installed"));
        metrics.addCustomChart(new SimplePie("team_balancing", () -> String.valueOf(config.getBoolean("game.balance-teams"))));
        metrics.addCustomChart(new SimplePie("achievements", () -> String.valueOf(config.getBoolean("achievements.enabled"))));
        metrics.addCustomChart(new SingleLineChart("rounds_played", () -> rounds.getAndSet(0)));
        metrics.addCustomChart(new AdvancedPie("variants_played", () -> drain(variants)));
        metrics.addCustomChart(new AdvancedPie("players_per_round", () -> drain(roundSizes)));
        metrics.addCustomChart(new AdvancedPie("variant_votes", () -> drain(variantVotes)));
    }

    /**
     * a round ended with a winner or a draw (EndingState)
     */
    public void roundPlayed(Game game) {
        rounds.incrementAndGet();
        variants.computeIfAbsent(game.getVariant().name().toLowerCase(), key -> new AtomicInteger()).incrementAndGet();
        roundSizes.computeIfAbsent(playerBucket(game.getGamePlayers().size()), key -> new AtomicInteger()).incrementAndGet();
    }

    /**
     * the players' votes at the end of the lobby (the most popular variant, not only the winner of each vote)
     */
    public void votesCast(Game game) {
        for (Variant variant : Variant.getVotable()) {
            int votes = game.getVotes(variant);
            if (votes > 0) {
                variantVotes.computeIfAbsent(variant.name().toLowerCase(), key -> new AtomicInteger()).addAndGet(votes);
            }
        }
    }

    /**
     * @return the counts since the last submission, the counters start again at 0
     */
    static Map<String, Integer> drain(Map<String, AtomicInteger> counters) {
        Map<String, Integer> values = new HashMap<>();
        counters.forEach((key, count) -> {
            int value = count.getAndSet(0);
            if (value > 0) values.put(key, value);
        });
        return values;
    }

    static String playerBucket(int players) {
        if (players <= 2) return "2";
        if (players <= 4) return "3-4";
        if (players <= 8) return "5-8";
        if (players <= 16) return "9-16";
        return "17+";
    }

    private static String bucket(int amount) {
        if (amount <= 3) return Integer.toString(amount);
        if (amount <= 10) return "4-10";
        return "10+";
    }

    private void startErrorReports() {
        if (SENTRY_DSN.isBlank() || !plugin.getFileManager().getConfigFile().getBoolean("sentry.enabled")) return;
        Sentry.init(options -> {
            options.setDsn(SENTRY_DSN);
            options.setRelease("catchthebeacon@" + plugin.getPluginMeta().getVersion());
            options.setEnvironment("production");
            // no data about the server or its players
            options.setSendDefaultPii(false);
            options.setAttachServerName(false);
            options.setServerName(null);
            options.setMaxBreadcrumbs(0);
            // only what this plugin reports, not the errors of other plugins
            options.setEnableUncaughtExceptionHandler(false);
            options.setEnableShutdownHook(false);
            options.setEnableExternalConfiguration(false);
            options.setSendClientReports(false);
            options.setBeforeSend((event, hint) -> minimize(event));
            // -Dctb.sentry.debug=true: Sentry logs what it sends and what the server answers
            options.setDebug(Boolean.getBoolean("ctb.sentry.debug"));
        });
        Sentry.configureScope(scope -> {
            scope.setTag("server", Bukkit.getName() + " " + Bukkit.getMinecraftVersion());
            scope.setTag("java", System.getProperty("java.version"));
            scope.setTag("network_mode", plugin.getNetworkManager().getMode().name().toLowerCase());
        });
        // errors of the plugin are logged by the server (event handlers, tasks) or the plugin itself
        this.handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                report(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Bukkit.getLogger().addHandler(handler);
        // the same error from both loggers is only reported once (signature)
        plugin.getLogger().addHandler(handler);
        Debug.info("Anonymous error reports are sent to Sentry to find bugs (disable: sentry.enabled in config.yml)");
    }

    private void report(LogRecord record) {
        Throwable thrown = record.getThrown();
        if (thrown == null || record.getLevel().intValue() < Level.WARNING.intValue() || !isOwnError(thrown)) return;
        // every error once, and not too many
        String signature = signature(thrown);
        synchronized (reported) {
            if (reported.size() >= MAX_REPORTS || !reported.add(signature)) return;
        }
        Sentry.captureException(thrown);
    }

    /**
     * @return true if CatchTheBeacon is in the stack trace (not its bundled libraries)
     */
    static boolean isOwnError(Throwable thrown) {
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            for (StackTraceElement element : cause.getStackTrace()) {
                String className = element.getClassName();
                if (className.startsWith(PACKAGE) && !className.startsWith(PACKAGE + "libs.")) return true;
            }
            if (cause.getCause() == cause) break;
        }
        return false;
    }

    /**
     * @return the type and the first line of the plugin in the stack trace
     */
    static String signature(Throwable thrown) {
        for (StackTraceElement element : thrown.getStackTrace()) {
            if (element.getClassName().startsWith(PACKAGE)) return thrown.getClass().getName() + "@" + element;
        }
        return thrown.getClass().getName();
    }

    /**
     * removes everything that could identify the server or its players
     */
    SentryEvent minimize(SentryEvent event) {
        event.setUser(null);
        event.setServerName(null);
        if (event.getExceptions() != null) {
            for (SentryException exception : event.getExceptions()) {
                if (exception.getValue() != null) exception.setValue(anonymize(exception.getValue()));
            }
        }
        if (event.getMessage() != null && event.getMessage().getFormatted() != null) {
            event.getMessage().setFormatted(anonymize(event.getMessage().getFormatted()));
        }
        return event;
    }

    /**
     * @return the text without uuids and names of online players
     */
    static String anonymize(String text) {
        String result = UUID_PATTERN.matcher(text).replaceAll("<uuid>");
        for (Player player : Bukkit.getOnlinePlayers()) {
            result = result.replace(player.getName(), "<player>");
        }
        return result;
    }

    /**
     * /ctb debug sentry: checks that error reports arrive
     *
     * @return false if error reports are disabled
     */
    public boolean sendTestReport() {
        if (!Sentry.isEnabled()) return false;
        Sentry.captureException(new IllegalStateException("Test report of /ctb debug sentry"));
        Sentry.flush(5000);
        return true;
    }

    public void stop() {
        if (handler != null) {
            Bukkit.getLogger().removeHandler(handler);
            plugin.getLogger().removeHandler(handler);
            handler = null;
        }
        if (Sentry.isEnabled()) Sentry.close();
    }

}
