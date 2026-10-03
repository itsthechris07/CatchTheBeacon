package com.christian34.catchthebeacon.commands;

import com.christian34.catchthebeacon.PluginTestBase;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.game.map.Arena;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.suggestion.Suggestion;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the arena setup commands.
 *
 * @author Christian34
 */
class CommandArenaTest extends PluginTestBase {

    private List<String> suggestions(CommandSender sender, String input) {
        return commands.suggestionFactory().suggestImmediately(sender, input).list().stream()
                .map(Suggestion::suggestion).toList();
    }

    @Test
    void needsAdminPermission() {
        PlayerMock player = addPlayer("Steve");
        addMapFolder("castle");
        execute(player, "ctb arena create castle");
        assertContains(messages(player), "You do not have permission");
        assertNull(plugin.getMapHandler().getArena("castle"));
    }

    @Test
    void createNeedsMapFolder() {
        PlayerMock admin = addAdmin("Admin");
        execute(admin, "ctb arena create castle");
        assertContains(messages(admin), "There is no map 'castle'");
        assertNull(plugin.getMapHandler().getArena("castle"));
    }

    @Test
    void createArena() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        execute(admin, "ctb arena create castle");
        assertContains(messages(admin), "has been created!");

        Arena arena = plugin.getMapHandler().getArena("castle");
        assertNotNull(arena);
        assertEquals(2, arena.getMinPlayers());
        assertEquals(8, arena.getMaxPlayers());
    }

    @Test
    void createdArenaIsLoadedAfterRestart() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        execute(admin, "ctb arena create castle");

        server.getPluginManager().disablePlugin(plugin);
        server.getPluginManager().enablePlugin(plugin);

        Arena arena = plugin.getMapHandler().getArena("castle");
        assertNotNull(arena, "arena was not saved in arenas.yml");
        assertEquals(8, arena.getMaxPlayers());
    }

    @Test
    void createArenaTwice() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        execute(admin, "ctb arena create castle");
        messages(admin);
        execute(admin, "ctb arena create castle");
        assertContains(messages(admin), "This arena already exists!");
    }

    @Test
    void unknownArena() {
        PlayerMock admin = addAdmin("Admin");
        execute(admin, "ctb arena nowhere save");
        assertContains(messages(admin), "Couldn't find the arena 'nowhere'!");
    }

    @Test
    void suggestsArenas() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        addMapFolder("desert");
        execute(admin, "ctb arena create castle");
        execute(admin, "ctb arena create desert");

        assertEquals(List.of("castle", "create", "desert", "list"), suggestions(admin, "ctb arena ").stream().sorted().toList());
        assertEquals(List.of("desert"), suggestions(admin, "ctb arena d"));
    }

    @Test
    void suggestsTeamsAndBeaconPositions() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        execute(admin, "ctb arena create castle");

        assertEquals(List.of("blue", "red"), suggestions(admin, "ctb arena castle team ").stream().sorted().toList());
        assertEquals(List.of("left", "right"),
                suggestions(admin, "ctb arena castle team red setbeacon ").stream().sorted().toList());
    }

    @Test
    void invalidTeam() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        execute(admin, "ctb arena create castle");
        messages(admin);
        execute(admin, "ctb arena castle team green setspawn");
        assertContains(messages(admin), "Invalid team!");
    }

    @Test
    void setTeamSpawn() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        execute(admin, "ctb arena create castle");
        admin.setLocation(new Location(admin.getWorld(), 10.5, 64, -3.5, 90, 0));

        execute(admin, "ctb arena castle team red setspawn");
        assertContains(messages(admin), "Spawn for team Red has been set");

        Location spawn = plugin.getMapHandler().getArena("castle").getSpawnLocation(Team.RED);
        assertEquals(10.5, spawn.getX());
        assertEquals(64, spawn.getY());
        assertEquals(-3.5, spawn.getZ());
        assertEquals(90, spawn.getYaw());
    }

    @Test
    void setLobbyWorld() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        addMapFolder("lobby");
        execute(admin, "ctb arena create castle");
        messages(admin);

        execute(admin, "ctb arena castle lobby setworld lobby");
        assertContains(messages(admin), "The lobby for arena 'castle' has been set to 'lobby'!");
        assertEquals("lobby", plugin.getMapHandler().getArena("castle").getLobbyMap().getName());
    }

    @Test
    void setLobbyWorldNeedsMapFolder() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        execute(admin, "ctb arena create castle");
        messages(admin);

        execute(admin, "ctb arena castle lobby setworld lobby");
        assertContains(messages(admin), "There is neither a map nor a world 'lobby'");
    }

    @Test
    void setupCommandsWorkInConsole() {
        addMapFolder("castle");
        addMapFolder("lobby");
        var console = server.getConsoleSender();
        execute(console, "ctb arena create castle");
        execute(console, "ctb arena castle lobby setworld lobby");
        List<String> messages = messages(console);
        assertContains(messages, "has been created!");
        assertContains(messages, "has been set to 'lobby'");
    }

    @Test
    void checkListsMissingSetup() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        execute(admin, "ctb arena create castle");
        messages(admin);

        execute(admin, "ctb arena castle check");
        List<String> messages = messages(admin);
        assertContains(messages, "the lobby world");
        assertContains(messages, "the spawn of team Red");
        assertContains(messages, "the left beacon of team Blue");
    }

    @Test
    void createGameNeedsSetup() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        execute(admin, "ctb arena create castle");
        messages(admin);

        execute(admin, "ctb arena castle creategame --force");
        assertContains(messages(admin), "the lobby world");
        assertTrue(plugin.getGameManager().getGames().isEmpty());
    }

    @Test
    void lobbySpawnAtCoordinateZeroIsSet() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        addMapFolder("lobby");
        execute(admin, "ctb arena create castle");
        execute(admin, "ctb arena castle lobby setworld lobby");
        admin.setLocation(new Location(admin.getWorld(), 0, 64, 12));
        execute(admin, "ctb arena castle lobby setspawn");
        messages(admin);

        server.getPluginManager().disablePlugin(plugin);
        server.getPluginManager().enablePlugin(plugin);

        Location spawn = plugin.getMapHandler().getArena("castle").getLobbyMap().getSpawnLocation();
        assertNotNull(spawn, "a spawn with x = 0 was treated as not set");
        assertEquals(12, spawn.getZ());
    }

    @Test
    void setBeaconIsUsedWithoutRestart() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        execute(admin, "ctb arena create castle");
        admin.getWorld().getBlockAt(3, 64, 3).setType(org.bukkit.Material.BEACON);
        admin.setLocation(new Location(admin.getWorld(), 2, 64, 2));

        execute(admin, "ctb arena castle team red setbeacon left");
        assertContains(messages(admin), "The left beacon for team Red has been set!");

        Location beacon = plugin.getMapHandler().getArena("castle").getBeaconLocations(Team.RED)
                .get(com.christian34.catchthebeacon.game.Beacon.Position.LEFT);
        assertNotNull(beacon);
        assertEquals(3, beacon.getBlockX());
        assertEquals(64, beacon.getBlockY());
    }

    @Test
    void setBeaconNeedsBeaconNearby() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        execute(admin, "ctb arena create castle");
        messages(admin);
        execute(admin, "ctb arena castle team red setbeacon left");
        assertContains(messages(admin), "Couldn't find any beacons near you!");
    }

    @Test
    void reservedNamesCantBeArenas() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("list");
        execute(admin, "ctb arena create list");
        assertContains(messages(admin), "Invalid name!");
        assertNull(plugin.getMapHandler().getArena("list"));
    }

    /**
     * the files of the mocked world "world" (key minecraft:overworld), as MockBukkit doesn't store worlds
     */
    private void addWorldFiles() throws java.io.IOException {
        java.io.File region = new java.io.File(worldsFolder, "dimensions/minecraft/overworld/region/r.0.0.mca");
        assertTrue(region.getParentFile().mkdirs());
        assertTrue(region.createNewFile());
    }

    @Test
    void createImportsAWorldOfTheServer() throws java.io.IOException {
        addWorldFiles();
        PlayerMock admin = addAdmin("Admin");
        execute(admin, "ctb arena create desert --world world");
        assertContains(messages(admin), "The arena 'desert' has been created!");
        assertTrue(new java.io.File(plugin.getDataFolder(), "maps/desert/region/r.0.0.mca").isFile());
        assertNotNull(plugin.getMapHandler().getArena("desert"));
    }

    @Test
    void importSkipsTheFilesOfTheRunningWorld() throws java.io.IOException {
        addWorldFiles();
        java.io.File data = new java.io.File(worldsFolder, "dimensions/minecraft/overworld/data/minecraft");
        assertTrue(data.mkdirs());
        for (String file : List.of("chunk_tickets.dat", "raids.dat", "session.lock", "world_border.dat")) {
            assertTrue(new java.io.File(data, file).createNewFile());
        }
        PlayerMock admin = addAdmin("Admin");
        execute(admin, "ctb arena create desert --world world");
        java.io.File copy = new java.io.File(plugin.getDataFolder(), "maps/desert/data/minecraft");
        assertTrue(new java.io.File(copy, "world_border.dat").isFile());
        // the forced chunks and raids of the server world aren't part of the map (Paper writes them while copying)
        assertFalse(new java.io.File(copy, "chunk_tickets.dat").exists());
        assertFalse(new java.io.File(copy, "raids.dat").exists());
        assertFalse(new java.io.File(copy, "session.lock").exists());
    }

    @Test
    void createWithUnknownWorld() {
        PlayerMock admin = addAdmin("Admin");
        execute(admin, "ctb arena create desert --world nowhere");
        assertContains(messages(admin), "The server has no world 'nowhere'!");
        assertNull(plugin.getMapHandler().getArena("desert"));
    }

    @Test
    void setLobbyWorldImportsAWorldOfTheServer() throws java.io.IOException {
        addWorldFiles();
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        execute(admin, "ctb arena create castle");
        messages(admin);
        execute(admin, "ctb arena castle lobby setworld world");
        assertContains(messages(admin), "has been set to 'world'");
        assertTrue(new java.io.File(plugin.getDataFolder(), "maps/world").isDirectory());
    }

    @Test
    void suggestsMapsAndWorldsForTheLobby() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        addMapFolder("lobby");
        execute(admin, "ctb arena create castle");
        assertEquals(List.of("castle", "lobby", "world"),
                suggestions(admin, "ctb arena castle lobby setworld ").stream().sorted().toList());
    }

    @Test
    void listShowsTheSetupState() {
        PlayerMock admin = addAdmin("Admin");
        addMapFolder("castle");
        execute(admin, "ctb arena create castle");
        messages(admin);
        execute(admin, "ctb arena list");
        assertContains(messages(admin), "castle 8 steps missing");
    }

    @Test
    void getItemsNeedsConfirmation() {
        PlayerMock admin = addAdmin("Admin");
        execute(admin, "ctb config items get");
        assertContains(messages(admin), "--confirm");
    }

}
