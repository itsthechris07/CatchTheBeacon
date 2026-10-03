package com.christian34.catchthebeacon.stats;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.database.Database;
import com.christian34.catchthebeacon.lib.lang.LangText;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The statistics of all players in the database (see {@link Database}), shown with /ctb stats and the placeholders
 * of PlaceholderAPI. The stats of online players are cached for the placeholders; everything else is loaded
 * asynchronously.
 *
 * @author Christian34
 */
public class StatsManager implements Listener {
    /**
     * the ranking placeholders show the first 10 places
     */
    public static final int TOP_CACHE_SIZE = 10;
    private static final long TOP_CACHE_MILLIS = 60_000;
    private final CatchTheBeacon plugin;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    @Nullable
    private Database database;
    private String table;
    private final Map<Stat, CachedTop> topCache = new ConcurrentHashMap<>();

    public StatsManager(CatchTheBeacon plugin) {
        this.plugin = plugin;
        try {
            this.database = new Database(plugin);
            this.table = database.table("stats");
            database.update(this::createTable).join();
            importYaml();
            Debug.info("Player data is saved in " + (database.getType() == Database.Type.MYSQL
                    ? "MySQL (table " + table + ")" : "SQLite (database.db)"));
        } catch (SQLException | CompletionException ex) {
            Debug.warn("Couldn't connect to the database, stats are disabled: " + ex.getMessage());
            if (database != null) database.close();
            this.database = null;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadIntoCache(player.getUniqueId());
        }
    }

    private void createTable(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + table + " ("
                + "uuid CHAR(36) NOT NULL PRIMARY KEY, name VARCHAR(16), "
                + "kills INT NOT NULL DEFAULT 0, deaths INT NOT NULL DEFAULT 0, wins INT NOT NULL DEFAULT 0, "
                + "games INT NOT NULL DEFAULT 0, beacons INT NOT NULL DEFAULT 0)")) {
            statement.executeUpdate();
        }
    }

    /**
     * imports the stats.yml of older versions once (renamed to stats.yml.imported afterwards)
     */
    private void importYaml() {
        File file = new File(plugin.getDataFolder(), "stats.yml");
        if (!file.isFile() || database == null) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        database.update(connection -> {
            for (String key : data.getKeys(false)) {
                ConfigurationSection section = data.getConfigurationSection(key);
                if (section == null) continue;
                UUID uuid;
                try {
                    uuid = UUID.fromString(key);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                insert(connection, uuid, section.getString("name"));
                StringBuilder sql = new StringBuilder("UPDATE " + table + " SET ");
                Stat[] stats = Stat.values();
                for (int i = 0; i < stats.length; i++) {
                    sql.append(i == 0 ? "" : ", ").append(stats[i].key).append(" = ").append(stats[i].key).append(" + ?");
                }
                try (PreparedStatement statement = connection.prepareStatement(sql + " WHERE uuid = ?")) {
                    for (int i = 0; i < stats.length; i++) {
                        statement.setInt(i + 1, section.getInt(stats[i].key));
                    }
                    statement.setString(stats.length + 1, uuid.toString());
                    statement.executeUpdate();
                }
            }
        }).join();
        if (file.renameTo(new File(plugin.getDataFolder(), "stats.yml.imported"))) {
            Debug.info("Imported stats.yml into the database");
        }
    }

    /**
     * @return false if stats.enabled is false in config.yml or there is no database (nothing is counted then)
     */
    public boolean isEnabled() {
        return database != null && plugin.getFileManager().getConfigFile().getBoolean("stats.enabled");
    }

    /**
     * @return the cached stats of an online player (for placeholders), empty stats if they aren't loaded (yet)
     */
    @NotNull
    public PlayerStats get(@NotNull UUID uuid) {
        return cache.getOrDefault(uuid, PlayerStats.EMPTY);
    }

    /**
     * @return the cached stats of an online player, null if they aren't loaded (yet) or there is no database
     */
    @Nullable
    public PlayerStats getCached(@NotNull UUID uuid) {
        return cache.get(uuid);
    }

    /**
     * @return the database of the player data, null if it couldn't be reached
     */
    @Nullable
    public Database getDatabase() {
        return database;
    }

    /**
     * @return the stats from the database, empty ones if the player has none
     */
    public CompletableFuture<PlayerStats> load(@NotNull UUID uuid) {
        if (database == null) return CompletableFuture.completedFuture(PlayerStats.EMPTY);
        return database.query(connection -> read(connection, uuid));
    }

    /**
     * @return the stats of the player with that name (who has played at least once), null if there is none
     */
    public CompletableFuture<@Nullable NamedStats> find(@NotNull String name) {
        if (database == null) return CompletableFuture.completedFuture(null);
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM " + table + " WHERE LOWER(name) = LOWER(?) LIMIT 1")) {
                statement.setString(1, name);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? new NamedStats(result.getString("name"), read(result)) : null;
                }
            }
        });
    }

    /**
     * @return the uuid of the player with that name (who has played at least once), null if there is none
     */
    public CompletableFuture<@Nullable UUID> findUuid(@NotNull String name) {
        if (database == null) return CompletableFuture.completedFuture(null);
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT uuid FROM " + table + " WHERE LOWER(name) = LOWER(?) LIMIT 1")) {
                statement.setString(1, name);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? UUID.fromString(result.getString(1)) : null;
                }
            }
        });
    }

    /**
     * @return the players with the highest value of the stat (only players who have it at least once), the best first
     */
    public CompletableFuture<List<RankedStat>> top(@NotNull Stat stat, int limit) {
        if (database == null) return CompletableFuture.completedFuture(List.of());
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT name, " + stat.key + " FROM " + table
                    + " WHERE " + stat.key + " > 0 ORDER BY " + stat.key + " DESC, name LIMIT ?")) {
                statement.setInt(1, limit);
                List<RankedStat> top = new ArrayList<>();
                try (ResultSet result = statement.executeQuery()) {
                    int rank = 0;
                    int previous = -1;
                    for (int position = 1; result.next(); position++) {
                        int value = result.getInt(2);
                        // players with the same value share the rank
                        if (value != previous) rank = position;
                        previous = value;
                        top.add(new RankedStat(rank, result.getString(1), value));
                    }
                }
                return top;
            }
        });
    }

    /**
     * @return the best players of the stat for placeholders (without waiting for the database): refreshed in the
     * background at most every minute, empty until they are loaded the first time
     */
    @NotNull
    public List<RankedStat> getTop(@NotNull Stat stat) {
        CachedTop cached = topCache.get(stat);
        long now = System.currentTimeMillis();
        if (cached == null || now - cached.time() > TOP_CACHE_MILLIS) {
            List<RankedStat> entries = cached == null ? List.of() : cached.entries();
            // only one refresh at a time
            topCache.put(stat, new CachedTop(entries, now));
            top(stat, TOP_CACHE_SIZE).thenAccept(top -> topCache.put(stat, new CachedTop(top, System.currentTimeMillis())));
            return entries;
        }
        return cached.entries();
    }

    /**
     * @return the rank of the player in the ranking of the stat, null if he doesn't have the stat (yet)
     */
    public CompletableFuture<@Nullable RankedStat> rank(@NotNull UUID uuid, @NotNull Stat stat) {
        if (database == null) return CompletableFuture.completedFuture(null);
        return database.query(connection -> {
            String name;
            int value;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT name, " + stat.key + " FROM " + table + " WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next() || result.getInt(2) <= 0) return null;
                    name = result.getString(1);
                    value = result.getInt(2);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM " + table + " WHERE " + stat.key + " > ?")) {
                statement.setInt(1, value);
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    return new RankedStat(result.getInt(1) + 1, name, value);
                }
            }
        });
    }

    private PlayerStats read(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table + " WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? read(result) : PlayerStats.EMPTY;
            }
        }
    }

    private static PlayerStats read(ResultSet result) throws SQLException {
        return new PlayerStats(result.getInt(Stat.KILLS.key), result.getInt(Stat.DEATHS.key), result.getInt(Stat.WINS.key),
                result.getInt(Stat.GAMES.key), result.getInt(Stat.BEACONS.key));
    }

    private void insert(Connection connection, UUID uuid, @Nullable String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                database.insertIgnore() + " INTO " + table + " (uuid, name) VALUES (?, ?)")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.executeUpdate();
        }
    }

    /**
     * counts the stat of the player (asynchronously)
     */
    public void add(@NotNull OfflinePlayer player, @NotNull Stat stat) {
        if (!isEnabled()) return;
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        database.update(connection -> {
            insert(connection, uuid, name);
            try (PreparedStatement statement = connection.prepareStatement("UPDATE " + table + " SET name = COALESCE(?, name), "
                    + stat.key + " = " + stat.key + " + 1 WHERE uuid = ?")) {
                statement.setString(1, name);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            }
            cache.computeIfPresent(uuid, (key, stats) -> stats.plus(stat));
        });
    }

    private void loadIntoCache(UUID uuid) {
        if (database == null) return;
        database.update(connection -> cache.put(uuid, read(connection, uuid)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        loadIntoCache(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        // after the queued updates of the player
        if (database != null) database.update(connection -> cache.remove(uuid));
    }

    /**
     * @return completes when all queued queries are done
     */
    public CompletableFuture<Void> flush() {
        return database == null ? CompletableFuture.completedFuture(null) : database.update(connection -> {
        });
    }

    public void close() {
        if (database != null) database.close();
    }

    public enum Stat {
        KILLS("kills", LangText.STAT_KILLS), DEATHS("deaths", LangText.STAT_DEATHS), WINS("wins", LangText.STAT_WINS),
        GAMES("games", LangText.STAT_GAMES), BEACONS("beacons", LangText.STAT_BEACONS);

        /**
         * the column in the database
         */
        private final String key;
        private final LangText displayName;

        Stat(String key, LangText displayName) {
            this.key = key;
            this.displayName = displayName;
        }

        public LangText getDisplayName() {
            return displayName;
        }
    }

    public record PlayerStats(int kills, int deaths, int wins, int games, int beacons) {
        public static final PlayerStats EMPTY = new PlayerStats(0, 0, 0, 0, 0);

        /**
         * @return kills per death, rounded to two decimals
         */
        public double killDeathRatio() {
            return Math.round((double) kills / Math.max(1, deaths) * 100) / 100.0;
        }

        /**
         * @return the stats with the stat counted once more
         */
        public PlayerStats plus(Stat stat) {
            return new PlayerStats(kills + (stat == Stat.KILLS ? 1 : 0), deaths + (stat == Stat.DEATHS ? 1 : 0),
                    wins + (stat == Stat.WINS ? 1 : 0), games + (stat == Stat.GAMES ? 1 : 0),
                    beacons + (stat == Stat.BEACONS ? 1 : 0));
        }
    }

    public record NamedStats(String name, PlayerStats stats) {
    }

    private record CachedTop(List<RankedStat> entries, long time) {
    }

    /**
     * @param rank  the position in the ranking, starting with 1 (players with the same value share it)
     * @param name  the last known name of the player
     * @param value the value of the stat
     */
    public record RankedStat(int rank, String name, int value) {
    }

}
