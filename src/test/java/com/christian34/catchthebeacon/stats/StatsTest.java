package com.christian34.catchthebeacon.stats;

import com.christian34.catchthebeacon.game.GameTestBase;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kills, deaths, wins, games and beacons are counted, saved in the database and shown (command and placeholders).
 *
 * @author Christian34
 */
class StatsTest extends GameTestBase {

    /**
     * waits for the queued database queries and runs the tasks they scheduled
     */
    private void waitForDatabase() {
        plugin.getStatsManager().flush().join();
        server.getScheduler().performOneTick();
    }

    private StatsManager.PlayerStats stats(org.bukkit.entity.Player player) {
        waitForDatabase();
        return plugin.getStatsManager().get(player.getUniqueId());
    }

    private void kill() {
        blue.setKiller(red);
        blue.damage(100, red);
    }

    @Test
    void killsAndDeathsAreCounted() {
        startGame();
        kill();
        assertEquals(1, stats(red).kills());
        assertEquals(1, stats(blue).deaths());
        assertEquals(0, stats(red).deaths());
    }

    @Test
    void winsGamesAndBeaconsAreCountedAndSaved() {
        startGame();
        for (Location location : blueBeacons()) {
            Block beacon = location.getBlock();
            beacon.setType(Material.BEACON);
            breakBlock(red, beacon);
        }
        assertEquals(new StatsManager.PlayerStats(0, 0, 1, 1, 2), stats(red));
        assertEquals(new StatsManager.PlayerStats(0, 0, 0, 1, 0), stats(blue));
        assertTrue(new File(plugin.getDataFolder(), "database.db").isFile());

        // loaded again after a restart
        server.getPluginManager().disablePlugin(plugin);
        server.getPluginManager().enablePlugin(plugin);
        assertEquals(1, stats(red).wins());
    }

    @Test
    void statsCanBeDisabled() {
        plugin.getFileManager().getConfigFile().set("stats.enabled", false);
        startGame();
        kill();
        assertEquals(0, stats(red).kills());
        execute(red, "ctb stats");
        assertContains(messages(red), "Stats are disabled");
    }

    @Test
    void statsCommand() {
        startGame();
        kill();
        waitForDatabase();
        messages(red);
        execute(red, "ctb stats");
        waitForDatabase();
        assertContains(messages(red), "Kills: 1 | Deaths: 0 | K/D: 1.0");
        execute(red, "ctb stats blue1");
        waitForDatabase();
        assertContains(messages(red), "Stats of Blue1");
        execute(red, "ctb stats Nobody");
        waitForDatabase();
        assertContains(messages(red), "Player was not found!");
    }

    @Test
    void statsOfOfflinePlayers() {
        startGame();
        kill();
        blue.disconnect();
        waitForDatabase();
        messages(red);
        // found by the name saved in the database
        execute(red, "ctb stats BLUE1");
        waitForDatabase();
        List<String> messages = messages(red);
        assertContains(messages, "Stats of Blue1");
        assertContains(messages, "Deaths: 1");
    }

    @Test
    void oldStatsFileIsImported() throws IOException {
        YamlConfiguration old = new YamlConfiguration();
        old.set(red.getUniqueId() + ".name", "Red1");
        old.set(red.getUniqueId() + ".kills", 7);
        old.set(red.getUniqueId() + ".wins", 2);
        File file = new File(plugin.getDataFolder(), "stats.yml");
        old.save(file);
        server.getPluginManager().disablePlugin(plugin);
        server.getPluginManager().enablePlugin(plugin);
        assertEquals(new StatsManager.PlayerStats(7, 0, 2, 0, 0), stats(red));
        assertFalse(file.exists());
        assertTrue(new File(plugin.getDataFolder(), "stats.yml.imported").isFile());
    }

    @Test
    void unreachableDatabaseDisablesStats() {
        plugin.getFileManager().getConfigFile().set("sql.enabled", true);
        plugin.getFileManager().getConfigFile().set("sql.host", "127.0.0.1");
        plugin.getFileManager().getConfigFile().set("sql.port", 1);
        plugin.getFileManager().getConfigFile().save();
        server.getPluginManager().disablePlugin(plugin);
        server.getPluginManager().enablePlugin(plugin);
        assertFalse(plugin.getStatsManager().isEnabled());
        execute(red, "ctb stats");
        assertContains(messages(red), "Stats are disabled");
    }

    @Test
    void killDeathRatio() {
        assertEquals(2.5, new StatsManager.PlayerStats(5, 2, 0, 0, 0).killDeathRatio());
        assertEquals(3.0, new StatsManager.PlayerStats(3, 0, 0, 0, 0).killDeathRatio());
        assertEquals(0.33, new StatsManager.PlayerStats(1, 3, 0, 0, 0).killDeathRatio());
    }

    private void addStat(org.bukkit.entity.Player player, StatsManager.Stat stat, int times) {
        for (int i = 0; i < times; i++) {
            plugin.getStatsManager().add(player, stat);
        }
    }

    @Test
    void topCommand() {
        messages(red);
        execute(red, "ctb top");
        waitForDatabase();
        assertContains(messages(red), "Nobody is on the list yet");

        addStat(red, StatsManager.Stat.WINS, 3);
        addStat(blue, StatsManager.Stat.WINS, 1);
        addStat(addPlayer("Steve"), StatsManager.Stat.WINS, 1);
        addStat(blue, StatsManager.Stat.KILLS, 5);
        waitForDatabase();
        execute(red, "ctb top");
        waitForDatabase();
        // the same wins share the place
        assertEquals(List.of("---- Top 10 (Wins) ----", "#1 Red1 - 3", "#2 Blue1 - 1", "#2 Steve - 1"), messages(red));

        execute(red, "ctb top kills");
        waitForDatabase();
        assertEquals(List.of("---- Top 10 (Kills) ----", "#1 Blue1 - 5"), messages(red));
    }

    @Test
    void ownRankBelowTheTop() {
        messages(red);
        messages(blue);
        var best = addPlayer("Pro1");
        addStat(best, StatsManager.Stat.BEACONS, 2);
        for (int i = 2; i <= 11; i++) {
            addStat(addPlayer("Pro" + i), StatsManager.Stat.BEACONS, 2);
        }
        addStat(red, StatsManager.Stat.BEACONS, 1);
        waitForDatabase();
        execute(red, "ctb top beacons");
        waitForDatabase();
        List<String> messages = messages(red);
        assertEquals(12, messages.size());
        assertEquals("Your rank: #12 - 1", messages.getLast());

        // not shown twice if he is in the list
        execute(best, "ctb top beacons");
        waitForDatabase();
        assertEquals(11, messages(best).size());
        // nothing without the stat
        execute(blue, "ctb top beacons");
        waitForDatabase();
        assertEquals(11, messages(blue).size());
    }

    @Test
    void topWithoutStats() {
        plugin.getFileManager().getConfigFile().set("stats.enabled", false);
        execute(red, "ctb top");
        assertContains(messages(red), "Stats are disabled");
    }

    @Test
    void topPlaceholders() {
        addStat(red, StatsManager.Stat.WINS, 2);
        addStat(blue, StatsManager.Stat.WINS, 4);
        waitForDatabase();
        StatsManager stats = plugin.getStatsManager();
        // loaded in the background
        assertEquals("", StatsExpansion.top(stats, "top_wins_1_name"));
        waitForDatabase();
        assertEquals("Blue1", StatsExpansion.top(stats, "top_wins_1_name"));
        assertEquals("4", StatsExpansion.top(stats, "TOP_WINS_1_VALUE"));
        assertEquals("Red1", StatsExpansion.top(stats, "top_wins_2_name"));
        assertEquals("", StatsExpansion.top(stats, "top_wins_3_name"));
        assertNull(StatsExpansion.top(stats, "top_wins_11_name"));
        assertNull(StatsExpansion.top(stats, "top_unknown_1_name"));
        assertNull(StatsExpansion.top(stats, "top_wins_x_name"));
        assertNull(StatsExpansion.top(stats, "top_wins_1_uuid"));
    }

    @Test
    void placeholders() {
        StatsManager.PlayerStats stats = new StatsManager.PlayerStats(4, 2, 1, 3, 5);
        assertEquals("4", StatsExpansion.value(stats, "kills"));
        assertEquals("2", StatsExpansion.value(stats, "deaths"));
        assertEquals("2.0", StatsExpansion.value(stats, "kd"));
        assertEquals("1", StatsExpansion.value(stats, "wins"));
        assertEquals("3", StatsExpansion.value(stats, "games"));
        assertEquals("5", StatsExpansion.value(stats, "beacons"));
        assertNull(StatsExpansion.value(stats, "unknown"));
    }

}
