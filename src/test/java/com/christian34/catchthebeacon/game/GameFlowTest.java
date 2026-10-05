package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.game.states.EndingState;
import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.user.GamePlayer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plays a whole round with two mocked players: join, start, the kit, protection, destroying the beacons and the end.
 *
 * @author Christian34
 */
class GameFlowTest extends GameTestBase {

    @Test
    void playersWaitInTheLobby() {
        assertEquals(GameState.LOBBY, game.getGameState());
        assertEquals(game.getLobbyWorld().getWorld(), red.getWorld());
        assertEquals(2, game.getGamePlayers().size());
    }

    @Test
    void scoreboardShowsTheLobby() {
        server.getScheduler().performTicks(20);
        Objective sidebar = red.getScoreboard().getObjective(DisplaySlot.SIDEBAR);
        assertNotNull(sidebar, "no sidebar");
        assertNotSame(server.getScoreboardManager().getMainScoreboard(), red.getScoreboard());
    }

    @Test
    void playersGetTheKitInTheTeamColor() {
        startGame();
        assertEquals(gameWorld(), red.getWorld());
        assertTrue(red.getInventory().contains(Material.STONE_SWORD));
        assertTrue(red.getInventory().contains(Material.RED_WOOL), "wool not in the team color");
        assertTrue(blue.getInventory().contains(Material.BLUE_WOOL));
        ItemStack chestplate = red.getInventory().getChestplate();
        assertNotNull(chestplate);
        assertEquals(Team.RED.getLeatherColor(), ((LeatherArmorMeta) chestplate.getItemMeta()).getColor());
        assertEquals(GameMode.SURVIVAL, red.getGameMode());
    }

    @Test
    void blocksNearTheSpawnAreProtected() {
        startGame();
        Block nearSpawn = at(RED_SPAWN, gameWorld()).add(2, 0, 0).getBlock();
        BlockPlaceEvent place = placeBlock(red, Material.RED_WOOL, nearSpawn.getLocation());
        assertTrue(place == null || place.isCancelled(), "could place a block next to the spawn");

        Block farAway = at(RED_SPAWN, gameWorld()).add(15, 0, 15).getBlock();
        BlockPlaceEvent allowed = placeBlock(red, Material.RED_WOOL, farAway.getLocation());
        assertNotNull(allowed);
        assertFalse(allowed.isCancelled());
    }

    @Test
    void ownBeaconCantBeDestroyed() {
        startGame();
        Block beacon = at(RED_BEACONS.getFirst(), gameWorld()).getBlock();
        beacon.setType(Material.BEACON);
        breakBlock(red, beacon);
        assertEquals(Material.BEACON, beacon.getType());
        assertEquals(GameState.INGAME, game.getGameState());
    }

    @Test
    void destroyingAllBeaconsWinsAndEndsNicely() {
        startGame();
        for (Location location : BLUE_BEACONS) {
            Block beacon = at(location, gameWorld()).getBlock();
            beacon.setType(Material.BEACON);
            BlockBreakEvent event = breakBlock(red, beacon);
            assertTrue(event == null || event.isCancelled());
            assertEquals(Material.AIR, beacon.getType());
        }
        assertEquals(GameState.ENDING, game.getGameState());
        assertEquals(Team.RED, game.getWinner());
        // the players stay in the game world for the celebration
        assertEquals(gameWorld(), blue.getWorld());
        assertEquals(GameMode.ADVENTURE, blue.getGameMode());
        assertContains(messages(blue), "has won the game!");

        server.getScheduler().performTicks(20 * 17);
        assertNotEquals(game, plugin.getUser(red).getGame());
        assertEquals("world", red.getWorld().getName());
        assertContains(messages(red), "[Play again]");
        assertSame(server.getScoreboardManager().getMainScoreboard(), red.getScoreboard());

        // the next round is ready
        assertFalse(plugin.getGameManager().getGames().contains(game));
        assertEquals(1, plugin.getGameManager().getGames().size());
        Game next = plugin.getGameManager().getGames().iterator().next();
        assertTrue(next.isJoinable());
        execute(red, "ctb join");
        assertEquals(next, plugin.getUser(red).getGame());
    }

    @Test
    void lastPlayerStandingWins() {
        startGame();
        execute(blue, "ctb quit");
        assertEquals(GameState.ENDING, game.getGameState());
        assertEquals(Team.RED, game.getWinner());
    }

    @Test
    void joinWatchesRunningGames() {
        startGame();
        PlayerMock late = addPlayer("Late");
        execute(late, "ctb join");
        assertFalse(game.getGamePlayers().contains(plugin.getUser(late)), "joined a running game");
        assertTrue(plugin.getUser(late).isSpectator());
    }

    @Test
    void joinWithoutGameToWatch() {
        plugin.getFileManager().getConfigFile().set("spectators.enabled", false);
        startGame();
        PlayerMock late = addPlayer("Late");
        execute(late, "ctb join");
        assertNull(plugin.getUser(late).getGame());
        assertContains(messages(late), "There is no game you could join right now!");
    }

    @Test
    void noMobsInTheGameWorld() {
        startGame();
        Zombie zombie = (Zombie) gameWorld().spawnEntity(at(RED_SPAWN, gameWorld()), EntityType.ZOMBIE);
        CreatureSpawnEvent natural = new CreatureSpawnEvent(zombie, CreatureSpawnEvent.SpawnReason.NATURAL);
        server.getPluginManager().callEvent(natural);
        assertTrue(natural.isCancelled(), "natural spawns are allowed");

        CreatureSpawnEvent plugin = new CreatureSpawnEvent(zombie, CreatureSpawnEvent.SpawnReason.CUSTOM);
        server.getPluginManager().callEvent(plugin);
        assertFalse(plugin.isCancelled(), "plugins can't spawn mobs");
    }

    @Test
    void mobsOfTheMapAreRemoved() {
        startGame();
        Zombie zombie = (Zombie) gameWorld().spawnEntity(at(RED_SPAWN, gameWorld()), EntityType.ZOMBIE);
        server.getPluginManager().callEvent(new org.bukkit.event.world.EntitiesLoadEvent(
                zombie.getLocation().getChunk(), List.of(zombie)));
        assertFalse(zombie.isValid(), "the zombie of the map is still there");
    }

    /**
     * the event asks for the progress, which MockBukkit doesn't implement
     */
    private PlayerMock advancementPlayer(String name, Location location) {
        PlayerMock player = new PlayerMock(server, name) {
            @Override
            public org.bukkit.advancement.AdvancementProgress getAdvancementProgress(org.bukkit.advancement.Advancement advancement) {
                return null;
            }
        };
        server.addPlayer(player);
        player.setLocation(location);
        return player;
    }

    @Test
    void noAdvancementsInTheGame() {
        startGame();
        PlayerMock player = advancementPlayer("Player", red.getLocation());
        var event = new com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent(player, null, "mined_stone");
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled(), "advancements are granted in the game");
        assertEquals(Boolean.FALSE, gameWorld().getGameRuleValue(org.bukkit.GameRules.SHOW_ADVANCEMENT_MESSAGES));
    }

    @Test
    void advancementsOutsideOfGames() {
        PlayerMock outside = advancementPlayer("Outside", server.getWorld("world").getSpawnLocation());
        var event = new com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent(outside, null, "mined_stone");
        server.getPluginManager().callEvent(event);
        assertFalse(event.isCancelled());
    }

    @Test
    void oldLobbyCountdownDoesntStartTheRoundAgain() {
        execute(red, "ctb quit");
        execute(blue, "ctb quit");
        assertEquals(GameState.PENDING, game.getGameState());
        server.getScheduler().performTicks(20 * 5);
        execute(red, "ctb join");
        execute(blue, "ctb join");
        plugin.getUser(red).setTeam(Team.RED);
        plugin.getUser(blue).setTeam(Team.BLUE);
        startGame();
        // the countdown of the first lobby (lobby.countdown: 180 s) must not start the round a second time
        server.getScheduler().performTicks(20 * 190);
        assertEquals(GameState.INGAME, game.getGameState());
        assertTrue(game.getRoundSeconds() >= 190, "the round was started again: " + game.getRoundSeconds() + " s");
    }

    @Test
    void playersInTheGameDontSeeAdvancementsOfOthers() {
        startGame();
        PlayerMock outside = addPlayer("Outside");
        PlayerMock other = addPlayer("Other");
        messages(red);
        messages(other);
        var event = new org.bukkit.event.player.PlayerAdvancementDoneEvent(outside, null,
                net.kyori.adventure.text.Component.text("Outside has made the advancement [Stone Age]"));
        server.getPluginManager().callEvent(event);
        assertNull(event.message());
        assertNotContains(messages(red), "Stone Age");
        assertContains(messages(other), "Outside has made the advancement [Stone Age]");
    }

    @Test
    void enemiesNearABeaconWarnTheTeam() {
        startGame();
        messages(red);
        blue.setLocation(at(RED_BEACONS.getFirst(), gameWorld()).add(8, 0, 0));
        server.getScheduler().performTicks(60);
        assertTrue(redLeftBeacon().isInDanger(), "an enemy 8 blocks away isn't noticed");
        assertContains(messages(red), "There are enemies near your");
    }

    @Test
    void enemiesFarAwayDontWarn() {
        startGame();
        blue.setLocation(at(RED_BEACONS.getFirst(), gameWorld()).add(12, 0, 0));
        server.getScheduler().performTicks(60);
        assertFalse(redLeftBeacon().isInDanger());
    }

    @Test
    void warningRadiusCanBeConfigured() {
        plugin.getFileManager().getConfigFile().set("game.beacon-warning-radius", 20);
        startGame();
        blue.setLocation(at(RED_BEACONS.getFirst(), gameWorld()).add(15, 0, 0));
        server.getScheduler().performTicks(60);
        assertTrue(redLeftBeacon().isInDanger());
    }

    @Test
    void protectedAfterRespawn() {
        startGame();
        killAndRespawn(blue);
        assertFalse(attack(red, blue), "got damage right after the respawn");

        server.getScheduler().performTicks(3 * 20 + 1);
        assertTrue(attack(red, blue), "still protected after 3 seconds");
    }

    @Test
    void attackingEndsTheProtection() {
        startGame();
        killAndRespawn(blue);
        attack(blue, red);
        assertTrue(attack(red, blue), "still protected after attacking");
    }

    @Test
    void noProtectionWithoutRespawn() {
        startGame();
        assertTrue(attack(red, blue));
    }
    @Test
    void lobbyCountdownFromConfig() {
        plugin.getFileManager().getConfigFile().set("lobby.countdown", 20);
        // the countdown is read when the lobby starts, so start a new round
        game.quit(plugin.getUser(red));
        game.quit(plugin.getUser(blue));
        execute(red, "ctb join");
        execute(blue, "ctb join");
        server.getScheduler().performTicks(20 * 15);
        assertEquals(GameState.LOBBY, game.getGameState(), "started too early");
        server.getScheduler().performTicks(20 * 7);
        assertEquals(GameState.INGAME, game.getGameState(), "didn't start after the countdown");
    }

    @Test
    void lobbyWaitsForEnoughPlayers() {
        plugin.getFileManager().getConfigFile().set("lobby.countdown", 10);
        game.quit(plugin.getUser(blue));
        game.quit(plugin.getUser(red));
        execute(red, "ctb join");
        server.getScheduler().performTicks(20 * 30);
        assertEquals(GameState.LOBBY, game.getGameState());
        assertContains(messages(red), "Waiting for players");
    }

    @Test
    void friendlyFireIsOffByDefault() {
        PlayerMock teammate = addPlayer("Red2");
        execute(teammate, "ctb join");
        plugin.getUser(teammate).setTeam(Team.RED);
        startGame();
        assertFalse(attack(teammate, red), "teammates can hurt each other");
        assertTrue(attack(blue, red), "enemies can't hurt each other");
    }

    @Test
    void friendlyFireCanBeEnabled() {
        plugin.getFileManager().getConfigFile().set("game.friendly-fire", true);
        PlayerMock teammate = addPlayer("Red2");
        execute(teammate, "ctb join");
        plugin.getUser(teammate).setTeam(Team.RED);
        startGame();
        assertTrue(attack(teammate, red));
    }

    @Test
    void deathMessagesCanBeDisabled() {
        plugin.getFileManager().getConfigFile().set("game.show-death-messages", false);
        startGame();
        messages(red);
        blue.setKiller(red);
        blue.damage(100, red);
        assertNotContains(messages(red), "was killed by");
        assertEquals(1, plugin.getUser(red).getKills(), "kills must still be counted");
    }

    @Test
    void deathMessagesAreShown() {
        startGame();
        messages(red);
        blue.setKiller(red);
        blue.damage(100, red);
        assertContains(messages(red), "was killed by");
    }

    @Test
    void endingDurationCanBeConfigured() {
        plugin.getFileManager().getConfigFile().set("game.ending-seconds", 5);
        startGame();
        execute(blue, "ctb quit");
        assertEquals(GameState.ENDING, game.getGameState());
        server.getScheduler().performTicks(20 * 3);
        assertEquals(gameWorld(), red.getWorld(), "sent back too early");
        server.getScheduler().performTicks(20 * 4);
        assertEquals("world", red.getWorld().getName());
    }

    @Test
    void endingTakesAtLeastAFewSeconds() {
        plugin.getFileManager().getConfigFile().set("game.ending-seconds", 0);
        assertEquals(EndingState.MIN_DURATION, EndingState.getDuration());
    }

    @RepeatedTest(10)
    void randomPlayersAreSplitIntoBothTeams() {
        plugin.getUser(red).setTeam(Team.RANDOM);
        plugin.getUser(blue).setTeam(Team.RANDOM);
        startGame();
        assertNotEquals(plugin.getUser(red).getTeam(), plugin.getUser(blue).getTeam());
    }

    @RepeatedTest(10)
    void playersWithoutTeamFillTheSmallerTeam() {
        plugin.getUser(blue).setTeam(Team.RED);
        PlayerMock third = addPlayer("Third");
        PlayerMock fourth = addPlayer("Fourth");
        joinWithTeam(third, Team.RANDOM);
        joinWithTeam(fourth, null);
        startGame();
        assertEquals(Team.BLUE, plugin.getUser(third).getTeam());
        assertEquals(Team.BLUE, plugin.getUser(fourth).getTeam());
    }

    @Test
    void everybodyInOneTeamIsSplit() {
        plugin.getUser(blue).setTeam(Team.RED);
        startGame();
        assertEquals(1, game.getGamePlayers(Team.RED).size());
        assertEquals(1, game.getGamePlayers(Team.BLUE).size());
    }

    @Test
    void fullTeamCantBeJoined() {
        for (int i = 2; i <= 4; i++) joinWithTeam(addPlayer("Red" + i), Team.RED);
        PlayerMock fifth = addPlayer("Red5");
        execute(fifth, "ctb join");
        assertFalse(plugin.getUser(fifth).setTeam(Team.RED), "a team can only have half of the max players");
        assertTrue(plugin.getUser(red).setTeam(Team.RED), "choosing the own team again must work");
        assertTrue(plugin.getUser(fifth).setTeam(Team.RANDOM));
    }

    @Test
    void serverStopDuringTheGame() {
        startGame();
        red.getInventory().clear();
        assertDoesNotThrow(() -> server.getPluginManager().disablePlugin(plugin));
        assertEquals("world", red.getWorld().getName(), "not sent back");
        assertEquals("world", blue.getWorld().getName());
        assertNotEquals(GameState.ENDING, game.getGameState(), "the game ended while the plugin was disabled");
    }

    @Test
    void serverStopDuringTheEnd() {
        startGame();
        execute(blue, "ctb quit");
        assertEquals(GameState.ENDING, game.getGameState());
        assertDoesNotThrow(() -> server.getPluginManager().disablePlugin(plugin));
        assertEquals("world", red.getWorld().getName());
    }
    @Test
    void killsAreCounted() {
        startGame();
        GamePlayer redPlayer = plugin.getUser(red);
        // MockBukkit doesn't remember who dealt the damage
        blue.setKiller(red);
        blue.damage(100, red);
        assertEquals(1, redPlayer.getKills());
    }

    // --- games after a restart

    private void restart() {
        server.getPluginManager().disablePlugin(plugin);
        server.getPluginManager().enablePlugin(plugin);
    }

    private List<Game> castleGames() {
        return plugin.getGameManager().getGames(plugin.getMapHandler().getArena("castle"));
    }

    @Test
    void createdGamesComeBackAfterARestart() {
        assertTrue(plugin.getMapHandler().getArena("castle").hasAutoGame());
        restart();
        assertEquals(1, castleGames().size());
    }

    @Test
    void removedGamesDontComeBack() {
        var console = server.getConsoleSender();
        execute(console, "ctb arena castle removegame");
        assertContains(messages(console), "Removed 1 game(s) of arena 'castle'");
        assertTrue(castleGames().isEmpty());
        assertFalse(plugin.getMapHandler().getArena("castle").isInUse(), "the arena can't be changed");
        assertNull(plugin.getUser(red).getGame());
        assertEquals("world", red.getWorld().getName());
        restart();
        assertTrue(castleGames().isEmpty());
        execute(console, "ctb arena castle removegame");
        assertContains(messages(console), "Arena 'castle' has no game");
    }

    @Test
    void runningGamesAreRemovedWithoutANewRound() {
        startGame();
        execute(server.getConsoleSender(), "ctb arena castle removegame");
        assertTrue(castleGames().isEmpty());
        assertEquals("world", blue.getWorld().getName());
    }

    @Test
    void gamesFileOfOlderVersionsIsImported() throws java.io.IOException {
        execute(server.getConsoleSender(), "ctb arena castle removegame");
        java.io.File file = new java.io.File(plugin.getDataFolder(), "games.yml");
        java.nio.file.Files.writeString(file.toPath(), "games:\n  a1b2c3d4:\n    arena: castle\n");
        restart();
        assertFalse(file.exists(), "games.yml is still there");
        assertTrue(plugin.getMapHandler().getArena("castle").hasAutoGame());
        assertEquals(1, castleGames().size());
    }

}
