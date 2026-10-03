package com.christian34.catchthebeacon.game.map;

import com.christian34.catchthebeacon.PluginTestBase;
import com.christian34.catchthebeacon.files.ArenasFile;
import com.christian34.catchthebeacon.files.FileManager;
import com.christian34.catchthebeacon.game.Beacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.Team;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The map that comes with the plugin is installed on a new server, so it can be played right away.
 */
class DefaultMapTest extends PluginTestBase {

    @Override
    protected boolean installDefaultMap() {
        return true;
    }

    private File maps() {
        return new File(plugin.getDataFolder(), "maps");
    }

    /**
     * the server has no arenas and the map has never been installed
     */
    private void resetToNewServer() {
        ArenasFile arenas = plugin.getFileManager().getArenasFile();
        arenas.getData().set("arenas", null);
        arenas.getData().set("default-map-checked", null);
        arenas.save();
        plugin.getGameManager().removeGames(plugin.getMapHandler().getArena(DefaultMap.NAME));
        FileManager.deleteDirectory(new File(maps(), "islands"));
        FileManager.deleteDirectory(new File(maps(), "islands_lobby"));
    }

    @Test
    void newServerGetsAPlayableArenaWithGame() {
        Arena arena = plugin.getMapHandler().getArena(DefaultMap.NAME);
        assertNotNull(arena);
        assertTrue(arena.getMissingSetup().isEmpty(), () -> "missing: " + arena.getMissingSetup());
        assertTrue(arena.getLobbyMap().isPlayable());
        assertTrue(arena.hasAutoGame());
        assertTrue(new File(maps(), "islands/region/r.0.0.mca").isFile());
        assertTrue(new File(maps(), "islands_lobby/region/r.2.0.mca").isFile());

        for (Team team : Team.getTeams()) {
            Map<Beacon.Position, Location> beacons = arena.getBeaconLocations(team);
            assertEquals(2, beacons.size(), team + " beacons");
        }
        // point-symmetric: red's left beacon is opposite of blue's left one
        Location redLeft = arena.getBeaconLocations(Team.RED).get(Beacon.Position.LEFT);
        Location blueLeft = arena.getBeaconLocations(Team.BLUE).get(Beacon.Position.LEFT);
        assertEquals(-redLeft.getX(), blueLeft.getX());
        assertEquals(-redLeft.getZ(), blueLeft.getZ());
        assertTrue(arena.getSpawnLocation(Team.RED).getZ() < 0 && arena.getSpawnLocation(Team.BLUE).getZ() > 0);

        assertEquals(1, plugin.getGameManager().getGames().size());
        Game game = plugin.getGameManager().getGames().iterator().next();
        assertEquals(arena, game.getArena());
    }

    @Test
    void isOnlyInstalledOnce() {
        ArenasFile arenas = plugin.getFileManager().getArenasFile();
        plugin.getGameManager().removeGames(plugin.getMapHandler().getArena(DefaultMap.NAME));
        arenas.getData().set("arenas", null);
        arenas.save();
        FileManager.deleteDirectory(new File(maps(), "islands"));

        DefaultMap.installIfNew(plugin, maps());

        assertFalse(new File(maps(), "islands").exists());
    }

    @Test
    void serverWithArenasDoesNotGetIt() {
        resetToNewServer();
        ArenasFile arenas = plugin.getFileManager().getArenasFile();
        arenas.set("castle.min-players", 2);

        DefaultMap.installIfNew(plugin, maps());

        assertFalse(new File(maps(), "islands").exists());
        assertTrue(arenas.getData().getBoolean("default-map-checked"));
        assertFalse(arenas.getData().contains("arenas.islands"));
    }

    @Test
    void lobbyServerOfANetworkDoesNotGetIt() {
        resetToNewServer();
        plugin.getFileManager().getConfigFile().set("network.mode", "lobby");

        DefaultMap.installIfNew(plugin, maps());

        assertFalse(new File(maps(), "islands").exists());
        // switching to a game server later still installs it
        assertFalse(plugin.getFileManager().getArenasFile().getData().contains("default-map-checked"));
    }

    @Test
    void existingFoldersAreNotOverwritten() {
        resetToNewServer();
        File own = new File(maps(), "islands_lobby");
        assertTrue(own.mkdirs());

        DefaultMap.installIfNew(plugin, maps());

        assertFalse(new File(maps(), "islands").exists());
        assertTrue(own.isDirectory());
        assertFalse(plugin.getFileManager().getArenasFile().getData().contains("arenas.islands"));
    }

}
