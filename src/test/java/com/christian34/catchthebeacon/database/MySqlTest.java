package com.christian34.catchthebeacon.database;

import com.christian34.catchthebeacon.PluginTestBase;
import com.christian34.catchthebeacon.files.PluginFile;
import com.christian34.catchthebeacon.stats.StatsManager;
import com.christian34.catchthebeacon.stats.StatsManager.PlayerStats;
import com.christian34.catchthebeacon.stats.StatsManager.Stat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The stats against a real MySQL/MariaDB server (not part of the normal build):
 * {@code .\gradlew.bat mysqlTest} with mysql.host, mysql.port, mysql.database, mysql.username and mysql.password in
 * the (git-ignored) gradle.properties. Every run uses own tables, which are dropped afterwards.
 *
 * @author Christian34
 */
@Tag("mysql")
class MySqlTest extends PluginTestBase {
    private String prefix;

    private static String property(String key) {
        String value = System.getProperty("mysql." + key);
        if (value == null) fail("set mysql." + key + " in gradle.properties (or -Pmysql." + key + "=...)");
        return value;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://" + property("host") + ":" + property("port") + "/"
                + property("database") + "?useSSL=false&allowPublicKeyRetrieval=true", property("username"), property("password"));
    }

    private StatsManager stats() {
        return plugin.getStatsManager();
    }

    private void restart() {
        server.getPluginManager().disablePlugin(plugin);
        server.getPluginManager().enablePlugin(plugin);
    }

    @BeforeEach
    void useMySql() {
        this.prefix = "ctbtest" + Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000, 0xFFFFF));
        PluginFile config = plugin.getFileManager().getConfigFile();
        config.set("sql.enabled", true);
        config.set("sql.host", property("host"));
        config.set("sql.port", Integer.parseInt(property("port")));
        config.set("sql.database", property("database"));
        config.set("sql.username", property("username"));
        config.set("sql.password", property("password"));
        config.set("sql.table-prefix", prefix);
        config.save();
        restart();
        assertTrue(stats().isEnabled(), "couldn't connect to MySQL, see the log");
    }

    @AfterEach
    void dropTables() throws SQLException {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("DROP TABLE IF EXISTS " + prefix + "_stats")) {
            statement.executeUpdate();
        }
    }

    private PlayerStats cached(PlayerMock player) {
        stats().flush().join();
        return stats().get(player.getUniqueId());
    }

    @Test
    void statsAreSavedInMySql() throws SQLException {
        PlayerMock player = addPlayer("Steve");
        stats().add(player, Stat.KILLS);
        stats().add(player, Stat.KILLS);
        stats().add(player, Stat.WINS);
        assertEquals(new PlayerStats(2, 0, 1, 0, 0), cached(player));

        // another server of the network reads the same table
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + prefix + "_stats WHERE uuid = ?")) {
            statement.setString(1, player.getUniqueId().toString());
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals("Steve", result.getString("name"));
                assertEquals(2, result.getInt("kills"));
                assertEquals(1, result.getInt("wins"));
            }
        }
    }

    @Test
    void statsSurviveRestarts() {
        PlayerMock player = addPlayer("Steve");
        stats().add(player, Stat.BEACONS);
        stats().flush().join();
        restart();
        assertEquals(1, cached(player).beacons());
        assertEquals(1, stats().load(player.getUniqueId()).join().beacons());
    }

    @Test
    void findByName() {
        PlayerMock player = addPlayer("Steve");
        stats().add(player, Stat.DEATHS);
        StatsManager.NamedStats found = stats().find("STEVE").join();
        assertNotNull(found);
        assertEquals("Steve", found.name());
        assertEquals(1, found.stats().deaths());
        assertNull(stats().find("Nobody").join());
        assertEquals(PlayerStats.EMPTY, stats().load(java.util.UUID.randomUUID()).join());
    }

    @Test
    void statsChangedByAnotherServer() throws SQLException {
        PlayerMock player = addPlayer("Steve");
        stats().add(player, Stat.GAMES);
        stats().flush().join();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("UPDATE " + prefix + "_stats SET games = games + 5 WHERE uuid = ?")) {
            statement.setString(1, player.getUniqueId().toString());
            statement.executeUpdate();
        }
        // /ctb stats loads from the database
        assertEquals(6, stats().load(player.getUniqueId()).join().games());
    }

    @Test
    void statsCommand() {
        PlayerMock player = addPlayer("Steve");
        stats().add(player, Stat.KILLS);
        stats().flush().join();
        messages(player);
        execute(player, "ctb stats");
        stats().flush().join();
        server.getScheduler().performOneTick();
        assertContains(messages(player), "Kills: 1 | Deaths: 0");
    }

}
