package com.christian34.catchthebeacon.network;

import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.GameTestBase;
import com.christian34.catchthebeacon.game.states.EndingState;
import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.user.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Network mode: game servers put joining players into their game and send them to the lobby afterwards, the lobby
 * server reads their state (MOTD) and sends players there.
 *
 * @author Christian34
 */
class NetworkTest extends GameTestBase {
    private final List<FakeServer> fakeServers = new ArrayList<>();

    @AfterEach
    void closeFakeServers() throws IOException {
        for (FakeServer fakeServer : fakeServers) {
            fakeServer.close();
        }
    }

    private void mode(String mode) {
        plugin.getFileManager().getConfigFile().set("network.mode", mode);
    }

    private NetworkManager network() {
        return plugin.getNetworkManager();
    }

    // ---- game server ----

    @Test
    void standaloneByDefault() {
        assertEquals(NetworkManager.Mode.STANDALONE, network().getMode());
        PlayerMock newcomer = addPlayer("Newcomer");
        server.getScheduler().performOneTick();
        assertNull(plugin.getUser(newcomer).getGame(), "players only join with /ctb join");
        execute(red, "ctb quit");
        assertTrue(connects(red).isEmpty());
    }

    @Test
    void invalidModeIsStandalone() {
        mode("bungee");
        assertEquals(NetworkManager.Mode.STANDALONE, network().getMode());
    }

    @Test
    void gameServerPlayersJoinTheGame() {
        mode("game");
        PlayerMock newcomer = addPlayer("Newcomer");
        server.getScheduler().performOneTick();
        assertEquals(game, plugin.getUser(newcomer).getGame());
        assertEquals(3, game.getGamePlayers().size());
    }

    @Test
    void gameServerRunningGameIsWatched() {
        mode("game");
        startGame();
        PlayerMock late = addPlayer("Late");
        server.getScheduler().performOneTick();
        GamePlayer gamePlayer = plugin.getUser(late);
        assertEquals(game, gamePlayer.getGame());
        assertTrue(gamePlayer.isSpectator());
        assertTrue(connects(late).isEmpty());
    }

    @Test
    void gameServerSendsToLobbyWithoutSpectators() {
        mode("game");
        plugin.getFileManager().getConfigFile().set("spectators.enabled", false);
        startGame();
        PlayerMock late = addPlayer("Late");
        server.getScheduler().performOneTick();
        assertNull(plugin.getUser(late).getGame());
        assertEquals(List.of("lobby"), connects(late));
        assertContains(messages(late), "You can't join or watch the game right now.");
    }

    @Test
    void gameServerWithoutGameKeepsPlayers() {
        mode("game");
        game.stop();
        plugin.getGameManager().getGames().clear();
        PlayerMock builder = addPlayer("Builder");
        server.getScheduler().performOneTick();
        assertNull(plugin.getUser(builder).getGame());
        assertTrue(connects(builder).isEmpty(), "the arena may still be set up");
    }

    @Test
    void bypassPermission() {
        mode("game");
        PlayerMock builder = addPlayer("Builder", NetworkManager.BYPASS_PERMISSION);
        server.getScheduler().performOneTick();
        assertNull(plugin.getUser(builder).getGame());
    }

    @Test
    void rejoinWinsOverAutoJoin() {
        mode("game");
        startGame();
        GamePlayer before = plugin.getUser(blue);
        blue.disconnect();
        blue.reconnect();
        server.getScheduler().performOneTick();
        GamePlayer after = plugin.getUser(blue);
        assertEquals(game, after.getGame());
        assertFalse(after.isSpectator(), "back in his team, not watching");
        assertEquals(before.getTeam(), after.getTeam());
    }

    @Test
    void gameServerQuitSendsToLobby() {
        mode("game");
        execute(red, "ctb quit");
        assertNull(plugin.getUser(red).getGame());
        assertEquals(List.of("lobby"), connects(red));
        assertContains(messages(red), "Sending you to lobby");
    }

    @Test
    void lobbyServerName() {
        mode("game");
        plugin.getFileManager().getConfigFile().set("network.lobby-server", "hub");
        execute(red, "ctb quit");
        assertEquals(List.of("hub"), connects(red));
    }

    @Test
    void gameServerEndSendsEverybodyToLobby() {
        mode("game");
        startGame();
        PlayerMock spectator = addPlayer("Spectator");
        server.getScheduler().performOneTick();
        for (Location location : blueBeacons()) {
            Block beacon = location.getBlock();
            beacon.setType(Material.BEACON);
            red.simulateBlockBreak(beacon);
        }
        assertEquals(GameState.ENDING, game.getGameState());
        messages(red);
        server.getScheduler().performTicks(20L * (EndingState.getDuration() + 2));
        assertEquals(List.of("lobby"), connects(red));
        assertEquals(List.of("lobby"), connects(blue));
        assertEquals(List.of("lobby"), connects(spectator));
        assertNotContains(messages(red), "Play again");
        // the next round waits on the server
        Game next = plugin.getGameManager().findJoinableGame(game.getArena());
        assertNotNull(next);
        assertNotEquals(game, next);
    }

    @Test
    void standaloneEndOffersPlayAgain() {
        startGame();
        for (Location location : blueBeacons()) {
            Block beacon = location.getBlock();
            beacon.setType(Material.BEACON);
            red.simulateBlockBreak(beacon);
        }
        messages(red);
        server.getScheduler().performTicks(20L * (EndingState.getDuration() + 2));
        assertTrue(connects(red).isEmpty());
        assertContains(messages(red), "Play again");
    }

    @Test
    void gameServerCreatesTheGameOfItsArena() {
        game.stop();
        plugin.getGameManager().getGames().clear();
        mode("game");
        plugin.getFileManager().getConfigFile().set("network.arena", "castle");
        network().ensureGame();
        assertEquals(1, plugin.getGameManager().getGames().size());
        network().ensureGame();
        assertEquals(1, plugin.getGameManager().getGames().size(), "only one game per server");
    }

    @Test
    void firstPlayableArenaByDefault() {
        assertEquals("castle", network().getArena().getName());
        plugin.getFileManager().getConfigFile().set("network.arena", "unknown");
        assertNull(network().getArena());
    }

    @Test
    void gameServerStatus() {
        mode("game");
        assertEquals(new ServerStatus(ServerStatus.State.LOBBY, 2, 8, "castle"), network().getStatus());
        startGame();
        assertEquals(ServerStatus.State.INGAME, network().getStatus().state());
    }

    @Test
    void gameServerSendsStatusAsMotd() {
        mode("game");
        ServerListPingEvent event = new ServerListPingEvent("localhost", InetAddress.getLoopbackAddress(),
                Component.text("A Minecraft Server"), 2, 20);
        network().onPing(event);
        assertEquals("CTB;LOBBY;2;8;castle", PlainTextComponentSerializer.plainText().serialize(event.motd()));
        assertEquals(8, event.getMaxPlayers());
    }

    @Test
    void standaloneKeepsMotd() {
        ServerListPingEvent event = new ServerListPingEvent("localhost", InetAddress.getLoopbackAddress(),
                Component.text("A Minecraft Server"), 2, 20);
        network().onPing(event);
        assertEquals("A Minecraft Server", PlainTextComponentSerializer.plainText().serialize(event.motd()));
    }

    // ---- status ----

    @Test
    void statusMotdRoundTrip() {
        ServerStatus status = new ServerStatus(ServerStatus.State.INGAME, 5, 8, "castle");
        assertEquals(status, ServerStatus.parse(status.toMotd()));
        assertEquals(ServerStatus.OFFLINE, ServerStatus.parse("A Minecraft Server"));
        assertEquals(ServerStatus.OFFLINE, ServerStatus.parse("CTB;FLYING;1;2;castle"));
        assertEquals(ServerStatus.OFFLINE, ServerStatus.parse(null));
        assertTrue(new ServerStatus(ServerStatus.State.LOBBY, 7, 8, "castle").isJoinable());
        assertFalse(new ServerStatus(ServerStatus.State.LOBBY, 8, 8, "castle").isJoinable(), "full");
        assertTrue(status.isSpectatable());
    }

    // ---- pinging ----

    /**
     * answers Server List Pings with the JSON
     */
    private static final class FakeServer implements AutoCloseable {
        private final ServerSocket socket;
        private volatile String json;
        private volatile int nextState = -1;

        FakeServer(String json) throws IOException {
            this.json = json;
            this.socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            Thread thread = new Thread(this::run, "FakeServer");
            thread.setDaemon(true);
            thread.start();
        }

        FakeServer motd(String motd) {
            this.json = "{\"version\":{\"name\":\"26.2\",\"protocol\":1},\"description\":{\"text\":\"" + motd + "\"}}";
            return this;
        }

        private void run() {
            while (!socket.isClosed()) {
                try (Socket client = socket.accept()) {
                    DataInputStream in = new DataInputStream(client.getInputStream());
                    // handshake: length, id, protocol, host, port, next state
                    ServerPinger.readVarInt(in);
                    ServerPinger.readVarInt(in);
                    ServerPinger.readVarInt(in);
                    in.readNBytes(ServerPinger.readVarInt(in));
                    in.readUnsignedShort();
                    nextState = ServerPinger.readVarInt(in);
                    // status request
                    in.readNBytes(ServerPinger.readVarInt(in));
                    ByteArrayOutputStream packet = new ByteArrayOutputStream();
                    ServerPinger.writeVarInt(packet, 0x00);
                    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                    ServerPinger.writeVarInt(packet, bytes.length);
                    packet.write(bytes);
                    OutputStream out = client.getOutputStream();
                    ServerPinger.writeVarInt(out, packet.size());
                    packet.writeTo(out);
                    out.flush();
                } catch (IOException ex) {
                    // closed
                }
            }
        }

        int port() {
            return socket.getLocalPort();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    private FakeServer fakeServer(String motd) throws IOException {
        FakeServer fakeServer = new FakeServer("").motd(motd);
        fakeServers.add(fakeServer);
        return fakeServer;
    }

    @Test
    void pingReadsMotd() throws IOException {
        FakeServer fakeServer = fakeServer("CTB;LOBBY;3;8;castle");
        assertEquals("CTB;LOBBY;3;8;castle", ServerPinger.ping("127.0.0.1", fakeServer.port(), 2000));
        assertEquals(1, fakeServer.nextState, "asks for the status");
    }

    @Test
    void pingReadsPlainMotdAndColors() throws IOException {
        FakeServer fakeServer = fakeServer("");
        fakeServer.json = "{\"description\":\"CTB;INGAME;4;8;castle\"}";
        assertEquals("CTB;INGAME;4;8;castle", ServerPinger.ping("127.0.0.1", fakeServer.port(), 2000));
        fakeServer.json = "{\"description\":{\"text\":\"CTB;\",\"extra\":[{\"text\":\"ENDING;0;8;castle\",\"color\":\"red\"}]}}";
        assertEquals("CTB;ENDING;0;8;castle", ServerPinger.ping("127.0.0.1", fakeServer.port(), 2000));
    }

    @Test
    void pingOfflineServer() throws IOException {
        FakeServer fakeServer = fakeServer("");
        int port = fakeServer.port();
        fakeServer.close();
        assertNull(ServerPinger.ping("127.0.0.1", port, 500));
    }

    @Test
    void varInts() throws IOException {
        for (int value : new int[]{0, 1, 127, 128, 255, 25565, 2097151, Integer.MAX_VALUE, -1}) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ServerPinger.writeVarInt(out, value);
            assertEquals(value, ServerPinger.readVarInt(new java.io.ByteArrayInputStream(out.toByteArray())));
        }
    }

    // ---- lobby server ----

    /**
     * lobby mode with the game servers ctb-1 and ctb-2 (fake servers with the MOTDs)
     */
    private void lobby(String motd1, String motd2) throws IOException {
        mode("lobby");
        // no local games on a lobby server
        game.stop();
        plugin.getGameManager().getGames().clear();
        plugin.getFileManager().getConfigFile().set("network.servers", List.of(
                Map.of("name", "ctb-1", "address", "127.0.0.1:" + fakeServer(motd1).port()),
                Map.of("name", "ctb-2", "address", "127.0.0.1:" + fakeServer(motd2).port())));
        network().pingAll();
    }

    @Test
    void serversFromConfig() {
        plugin.getFileManager().getConfigFile().set("network.servers", List.of(
                Map.of("name", "ctb-1", "address", "10.0.0.5:25570"),
                Map.of("name", "ctb-2", "address", "game2"),
                Map.of("name", "broken", "address", "host:port"),
                Map.of("address", "noname:1")));
        assertEquals(List.of(new NetworkManager.Server("ctb-1", "10.0.0.5", 25570),
                new NetworkManager.Server("ctb-2", "game2", 25565)), network().getServers());
        assertNotNull(network().getServer("CTB-1"));
    }

    @Test
    void lobbyReadsStatusOfGameServers() throws IOException {
        lobby("CTB;LOBBY;3;8;castle", "A Minecraft Server");
        assertEquals(new ServerStatus(ServerStatus.State.LOBBY, 3, 8, "castle"), network().getStatus("ctb-1"));
        assertEquals(ServerStatus.OFFLINE, network().getStatus("ctb-2"), "not a game server");
        assertEquals(ServerStatus.OFFLINE, network().getStatus("unknown"));
    }

    @Test
    void lobbyJoinSendsToFullestWaitingServer() throws IOException {
        lobby("CTB;LOBBY;3;8;castle", "CTB;LOBBY;5;8;castle");
        PlayerMock player = addPlayer("Player");
        execute(player, "ctb join");
        assertEquals(List.of("ctb-2"), connects(player));
        assertNull(plugin.getUser(player).getGame());
    }

    @Test
    void lobbyJoinSkipsFullAndRunningServers() throws IOException {
        lobby("CTB;LOBBY;8;8;castle", "CTB;INGAME;6;8;castle");
        PlayerMock player = addPlayer("Player");
        execute(player, "ctb join");
        // watches the running game
        assertEquals(List.of("ctb-2"), connects(player));
    }

    @Test
    void lobbyJoinWithoutGames() throws IOException {
        lobby("CTB;ENDING;0;8;castle", "A Minecraft Server");
        PlayerMock player = addPlayer("Player");
        messages(player);
        execute(player, "ctb join");
        assertTrue(connects(player).isEmpty());
        assertContains(messages(player), "There is no game you could join right now!");
        execute(player, "ctb spectate");
        assertContains(messages(player), "There is no running game you could watch!");
    }

    @Test
    void lobbySpectate() throws IOException {
        lobby("CTB;LOBBY;3;8;castle", "CTB;INGAME;6;8;castle");
        PlayerMock player = addPlayer("Player");
        execute(player, "ctb spectate");
        assertEquals(List.of("ctb-2"), connects(player));
    }

    private static String plain(Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    private Block writeSign(PlayerMock player, String... lines) {
        Block block = new Location(server.getWorld("world"), 5, 70, 5).getBlock();
        block.setType(Material.OAK_SIGN);
        block.getChunk().load();
        List<Component> components = new ArrayList<>(Arrays.stream(lines).map(line -> (Component) Component.text(line)).toList());
        server.getPluginManager().callEvent(new SignChangeEvent(block, player, components, Side.FRONT));
        return block;
    }

    @Test
    void lobbySignsShowGameServers() throws IOException {
        lobby("CTB;LOBBY;3;8;castle", "CTB;INGAME;6;8;castle");
        PlayerMock admin = addAdmin("SignAdmin");
        Block block = writeSign(admin, "[ctb]", "ctb-1", "", "");
        assertContains(messages(admin), "The join sign for server ctb-1 has been created.");
        server.getScheduler().performTicks(20);
        Sign sign = (Sign) block.getState();
        assertEquals("ctb-1", plain(sign.getSide(Side.FRONT).line(1)));
        assertEquals("Lobby", plain(sign.getSide(Side.FRONT).line(2)));
        assertEquals("3/8", plain(sign.getSide(Side.FRONT).line(3)));

        PlayerMock player = addPlayer("Player");
        server.getPluginManager().callEvent(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP));
        assertEquals(List.of("ctb-1"), connects(player));
    }

    @Test
    void lobbySignOfOfflineServer() throws IOException {
        lobby("A Minecraft Server", "CTB;INGAME;6;8;castle");
        Block block = writeSign(addAdmin("SignAdmin"), "[ctb]", "ctb-1", "", "");
        server.getScheduler().performTicks(20);
        assertEquals("No game", plain(((Sign) block.getState()).getSide(Side.FRONT).line(2)));
        PlayerMock player = addPlayer("Player");
        messages(player);
        server.getPluginManager().callEvent(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP));
        assertTrue(connects(player).isEmpty());
        assertContains(messages(player), "There is no game you could join right now!");
    }

    @Test
    void lobbySignNeedsKnownServer() throws IOException {
        lobby("CTB;LOBBY;3;8;castle", "CTB;LOBBY;3;8;castle");
        PlayerMock admin = addAdmin("SignAdmin");
        writeSign(admin, "[ctb]", "castle", "", "");
        assertContains(messages(admin), "There is no server called 'castle' in network.servers!");
        assertTrue(plugin.getSignManager().getSigns().isEmpty());
    }

}
