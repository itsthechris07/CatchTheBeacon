package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.integrations.EssentialsSupport;
import com.christian34.catchthebeacon.integrations.MultiverseSupport;
import com.christian34.catchthebeacon.integrations.NpcStatusDisplay;
import com.christian34.catchthebeacon.integrations.NpcSupport;
import com.christian34.catchthebeacon.integrations.TabSupport;
import com.christian34.catchthebeacon.integrations.VanishSupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Other plugins changing players or worlds: TAB, EssentialsX, vanish plugins, Multiverse and the state of the game on
 * join NPCs - with fakes of the plugins
 *
 * @author Christian34
 */
class CompatibilityTest extends GameTestBase {

    private static String plain(@Nullable Component component) {
        return component == null ? null : PlainTextComponentSerializer.plainText().serialize(component);
    }

    @SuppressWarnings("deprecation")
    private void vanish(Player player) {
        player.setMetadata(VanishSupport.METADATA, new FixedMetadataValue(plugin, true));
    }

    // TAB

    private static class FakeTab implements TabSupport {
        final Set<Player> takenOver = new HashSet<>();
        final List<Player> released = new ArrayList<>();

        @Override
        public void takeOver(@NotNull Player player, @NotNull Component header, @NotNull Component footer) {
            takenOver.add(player);
        }

        @Override
        public void release(@NotNull Player player) {
            takenOver.remove(player);
            released.add(player);
        }
    }

    @Test
    void tabDoesNotOverwriteTheTabListInGames() {
        FakeTab tab = new FakeTab();
        plugin.getTabListManager().setTabSupport(tab);
        PlayerMock player = addPlayer("Player");
        execute(player, "ctb join");
        assertTrue(tab.takenOver.contains(player));

        execute(player, "ctb quit");
        assertFalse(tab.takenOver.contains(player));
        assertEquals(List.of(player), tab.released);
    }

    @Test
    void tabIsReleasedForSpectatorsLeaving() {
        FakeTab tab = new FakeTab();
        plugin.getTabListManager().setTabSupport(tab);
        startGame();
        PlayerMock spectator = addPlayer("Spectator");
        execute(spectator, "ctb spectate");
        assertTrue(tab.takenOver.contains(spectator));
        execute(spectator, "ctb quit");
        assertFalse(tab.takenOver.contains(spectator));
    }

    // EssentialsX

    private static class FakeEssentials implements EssentialsSupport {
        final Map<Player, Boolean> joined = new HashMap<>();
        final List<Player> left = new ArrayList<>();
        final Set<Player> vanished = new HashSet<>();

        @Override
        public void joinGame(@NotNull Player player, boolean spectator) {
            joined.put(player, spectator);
            vanished.remove(player);
        }

        @Override
        public void leaveGame(@NotNull Player player) {
            left.add(player);
        }

        @Override
        public boolean isVanished(@NotNull Player player) {
            return vanished.contains(player);
        }
    }

    @Test
    void essentialsStatesAreTurnedOffInGames() {
        FakeEssentials essentials = new FakeEssentials();
        plugin.setEssentials(essentials);
        PlayerMock player = addPlayer("Player");
        execute(player, "ctb join");
        assertEquals(Boolean.FALSE, essentials.joined.get(player));
        execute(player, "ctb quit");
        assertEquals(List.of(player), essentials.left);

        startGame();
        PlayerMock spectator = addPlayer("Spectator");
        execute(spectator, "ctb spectate");
        assertEquals(Boolean.TRUE, essentials.joined.get(spectator), "spectators keep god mode and vanish");
    }

    @Test
    void essentialsTeleportsDontLeadIntoGames() {
        Location lobby = game.getLobbySpawn();
        PlayerMock outside = addPlayer("Outside");
        assertTrue(EssentialsSupport.blocksTeleport(outside, lobby), "/back into the world of a game");
        assertFalse(EssentialsSupport.blocksTeleport(outside, outside.getLocation()));
        assertTrue(EssentialsSupport.blocksTeleport(red, lobby.clone().add(5, 0, 0)), "players can't teleport within the game");
        assertFalse(EssentialsSupport.blocksTeleport(red, outside.getLocation()), "leaving the game is handled by the game");
        assertTrue(EssentialsSupport.isPlaying(red));
        assertFalse(EssentialsSupport.isPlaying(outside));
    }

    // vanish plugins

    @Test
    void vanishedPlayersCantPlay() {
        PlayerMock player = addPlayer("Player");
        vanish(player);
        assertTrue(VanishSupport.isVanished(player));
        execute(player, "ctb join");
        assertNull(plugin.getUser(player).getGame());
        assertContains(messages(player), "You're vanished");
    }

    @Test
    void playersVanishedByEssentialsBecomeVisible() {
        FakeEssentials essentials = new FakeEssentials();
        plugin.setEssentials(essentials);
        PlayerMock player = addPlayer("Player");
        vanish(player);
        essentials.vanished.add(player);
        execute(player, "ctb join");
        assertEquals(game, plugin.getUser(player).getGame());
    }

    @Test
    void vanishedPlayersDontJoinGameServers() {
        plugin.getFileManager().getConfigFile().set("network.mode", "game");
        PlayerMock admin = addPlayer("Admin2");
        vanish(admin);
        server.getScheduler().performOneTick();
        assertNull(plugin.getUser(admin).getGame());
        assertEquals(2, game.getGamePlayers().size());
    }

    // Multiverse

    @Test
    void multiverseInventoriesIgnoresGames() {
        PlayerMock outside = addPlayer("Outside");
        assertFalse(MultiverseSupport.ignoresInventory(outside, "world", "world_nether"));
        assertTrue(MultiverseSupport.ignoresInventory(outside, "world", "ctb_1234_lobby"));
        assertTrue(MultiverseSupport.ignoresInventory(outside, "ctb_1234", "world"));
        // the inventory of players in a game is saved by the game (e.g. while changing the game mode)
        assertTrue(MultiverseSupport.ignoresInventory(red, null, null));
        assertFalse(MultiverseSupport.ignoresInventory(outside, null, null));
    }

    // state of the game on join NPCs

    private static class FakeNpcs implements NpcStatusDisplay {
        final Map<String, String> names = new HashMap<>();
        final List<String> calls = new ArrayList<>();
        boolean loaded = true;

        @Override
        public boolean exists(@NotNull String id) {
            return loaded;
        }

        @Override
        public @Nullable String showStatus(@NotNull String id, @Nullable String shown, @Nullable Component status) {
            calls.add(id + "=" + plain(status));
            if (status == null) {
                names.remove(id);
                return null;
            }
            names.put(id, plain(status));
            return "shown:" + plain(status);
        }
    }

    private FakeNpcs linkNpc(String id) {
        FakeNpcs npcs = new FakeNpcs();
        plugin.getNpcSupport().addPlugin("Citizens", "citizens", npcs);
        PlayerMock admin = addAdmin("Admin2");
        execute(admin, "ctb npc link castle");
        plugin.getNpcSupport().click(admin, "citizens:" + id, "Guide");
        return npcs;
    }

    @Test
    void npcsShowTheStateOfTheGame() {
        FakeNpcs npcs = linkNpc("1");
        plugin.getNpcSupport().updateStatus();
        int max = game.getArena().getMaxPlayers();
        assertEquals("Lobby • 2/" + max, npcs.names.get("1"));

        // only changes are sent to the NPC plugin
        plugin.getNpcSupport().updateStatus();
        assertEquals(1, npcs.calls.size());
        execute(addPlayer("Player"), "ctb join");
        plugin.getNpcSupport().updateStatus();
        assertEquals("Lobby • 3/" + max, npcs.names.get("1"));

        startGame();
        plugin.getNpcSupport().updateStatus();
        assertEquals("Running • 3/" + max, npcs.names.get("1"));
    }

    @Test
    void npcStateIsRemoved() {
        FakeNpcs npcs = linkNpc("1");
        NpcSupport support = plugin.getNpcSupport();
        support.updateStatus();

        // what the NPC plugin shows is saved: after a restart it knows what to replace
        NpcSupport loaded = new NpcSupport(plugin);
        FakeNpcs reloaded = new FakeNpcs();
        List<String> shownBefore = new ArrayList<>();
        loaded.addPlugin("Citizens", "citizens", (NpcStatusDisplayAdapter) (id, shown, status) -> {
            shownBefore.add(shown);
            return reloaded.showStatus(id, shown, status);
        });
        loaded.updateStatus();
        assertEquals("shown:Lobby • 2/" + game.getArena().getMaxPlayers(), shownBefore.getFirst());

        plugin.getFileManager().getConfigFile().set("npcs.show-status", false);
        support.updateStatus();
        assertNull(npcs.names.get("1"));

        plugin.getFileManager().getConfigFile().set("npcs.show-status", true);
        support.updateStatus();
        assertNotNull(npcs.names.get("1"));
        PlayerMock admin = addAdmin("Admin3");
        execute(admin, "ctb npc unlink");
        support.click(admin, "citizens:1", "Guide");
        assertNull(npcs.names.get("1"), "unlinked NPCs don't show the state any more");
    }

    @Test
    void npcsLoadedLaterGetTheState() {
        FakeNpcs npcs = linkNpc("1");
        npcs.loaded = false;
        plugin.getNpcSupport().updateStatus();
        assertTrue(npcs.calls.isEmpty());
        npcs.loaded = true;
        plugin.getNpcSupport().updateStatus();
        assertNotNull(npcs.names.get("1"));
    }

    /**
     * an NPC plugin whose NPCs always exist
     */
    @FunctionalInterface
    private interface NpcStatusDisplayAdapter extends NpcStatusDisplay {
        @Override
        default boolean exists(@NotNull String id) {
            return true;
        }
    }

}
