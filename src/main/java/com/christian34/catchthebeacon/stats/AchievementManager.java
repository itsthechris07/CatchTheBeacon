package com.christian34.catchthebeacon.stats;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.database.Database;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

/**
 * The achievements of the players in the database of the stats (table achievements), see {@link Achievement}.
 *
 * @author Christian34
 */
public class AchievementManager implements Listener {
    private final CatchTheBeacon plugin;
    private final Map<UUID, Set<Achievement>> cache = new ConcurrentHashMap<>();
    @Nullable
    private final Database database;
    private final String table;

    public AchievementManager(CatchTheBeacon plugin) {
        this.plugin = plugin;
        Database database = plugin.getStatsManager().getDatabase();
        this.table = database == null ? "" : database.table("achievements");
        if (database != null) {
            try {
                database.update(this::createTable).join();
            } catch (CompletionException ex) {
                Debug.warn("Couldn't create the table of the achievements: " + ex.getMessage());
                database = null;
            }
        }
        this.database = database;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadIntoCache(player.getUniqueId());
        }
    }

    private void createTable(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + table + " ("
                + "uuid CHAR(36) NOT NULL, achievement VARCHAR(32) NOT NULL, achieved_at BIGINT NOT NULL, "
                + "PRIMARY KEY (uuid, achievement))")) {
            statement.executeUpdate();
        }
    }

    /**
     * @return false if achievements.enabled is false or there is no database
     */
    public boolean isEnabled() {
        return database != null && plugin.getFileManager().getConfigFile().getBoolean("achievements.enabled");
    }

    /**
     * gives the player the achievement (tells him, if he hasn't had it yet)
     */
    public void unlock(@NotNull Player player, @NotNull Achievement achievement) {
        if (!isEnabled()) return;
        UUID uuid = player.getUniqueId();
        Set<Achievement> owned = cache.get(uuid);
        if (owned != null && owned.contains(achievement)) return;
        database.update(connection -> {
            int inserted;
            try (PreparedStatement statement = connection.prepareStatement(database.insertIgnore() + " INTO " + table
                    + " (uuid, achievement, achieved_at) VALUES (?, ?, ?)")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, achievement.getKey());
                statement.setLong(3, System.currentTimeMillis());
                inserted = statement.executeUpdate();
            }
            cache.computeIfPresent(uuid, (key, achievements) -> {
                achievements.add(achievement);
                return achievements;
            });
            // only the first time (e.g. not again on another server of the network)
            if (inserted > 0) Bukkit.getScheduler().runTask(plugin, () -> announce(player, achievement));
        });
    }

    private void announce(Player player, Achievement achievement) {
        if (!player.isOnline()) return;
        player.sendMessage(I.prefixed(LangText.ACHIEVEMENT_UNLOCKED, achievement.getName(),
                achievement.getDescription()));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    /**
     * @return the cached achievements of an online player (empty if they aren't loaded)
     */
    public Set<Achievement> get(@NotNull UUID uuid) {
        Set<Achievement> achievements = cache.get(uuid);
        Set<Achievement> copy = EnumSet.noneOf(Achievement.class);
        if (achievements != null) copy.addAll(achievements);
        return copy;
    }

    /**
     * @return the achievements from the database
     */
    public CompletableFuture<Set<Achievement>> load(@NotNull UUID uuid) {
        if (database == null) return CompletableFuture.completedFuture(EnumSet.noneOf(Achievement.class));
        return database.query(connection -> read(connection, uuid));
    }

    private Set<Achievement> read(Connection connection, UUID uuid) throws SQLException {
        Set<Achievement> achievements = EnumSet.noneOf(Achievement.class);
        try (PreparedStatement statement = connection.prepareStatement("SELECT achievement FROM " + table + " WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    for (Achievement achievement : Achievement.values()) {
                        if (achievement.getKey().equals(result.getString(1))) achievements.add(achievement);
                    }
                }
            }
        }
        return achievements;
    }

    private void loadIntoCache(UUID uuid) {
        if (database == null) return;
        database.update(connection -> {
            // changed on the database thread, read on the main thread
            Set<Achievement> achievements = ConcurrentHashMap.newKeySet();
            achievements.addAll(read(connection, uuid));
            cache.put(uuid, achievements);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        loadIntoCache(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (database != null) database.update(connection -> cache.remove(uuid));
    }

}
