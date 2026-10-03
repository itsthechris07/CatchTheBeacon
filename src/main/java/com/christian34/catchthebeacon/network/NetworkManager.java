package com.christian34.catchthebeacon.network;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.GameManager;
import com.christian34.catchthebeacon.game.map.Arena;
import com.christian34.catchthebeacon.integrations.VanishSupport;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

/**
 * Network mode (network.* in config.yml) for servers behind a Velocity (or BungeeCord) proxy:
 * <ul>
 *     <li>standalone: games on this server, as without a network</li>
 *     <li>game: this server hosts one game. Players joining the server join it (or watch it), after the game or with
 *     /ctb quit they are sent to the lobby server. The state of the game is sent as MOTD ({@link ServerStatus}).</li>
 *     <li>lobby: join signs and /ctb join send players to the game servers, whose MOTD is read every 2 seconds.</li>
 * </ul>
 * Players are sent with the "Connect" message of the BungeeCord channel, which Velocity understands as well.
 *
 * @author Christian34
 */
public class NetworkManager implements Listener {
    public static final String CHANNEL = "BungeeCord";
    /**
     * players with this permission aren't put into the game when they join a game server (e.g. to set up the arena)
     */
    public static final String BYPASS_PERMISSION = "ctb.network.bypass";
    private static final int PING_TIMEOUT_MILLIS = 1000;
    private final CatchTheBeacon plugin;
    private final Map<String, ServerStatus> statuses = new ConcurrentHashMap<>();

    public enum Mode {
        STANDALONE, GAME, LOBBY
    }

    /**
     * a game server of the network
     *
     * @param name    the name in the proxy config
     * @param host    the address the lobby server pings
     * @param port    the port of the server
     */
    public record Server(String name, String host, int port) {
    }

    public NetworkManager(CatchTheBeacon plugin) {
        this.plugin = plugin;
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::pingAll, 20L, 40L);
        ensureGame();
    }

    @NotNull
    public Mode getMode() {
        String mode = plugin.getFileManager().getConfigFile().getString("network.mode");
        try {
            return mode == null ? Mode.STANDALONE : Mode.valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Mode.STANDALONE;
        }
    }

    public boolean isGameServer() {
        return getMode() == Mode.GAME;
    }

    public boolean isLobby() {
        return getMode() == Mode.LOBBY;
    }

    /**
     * sends the player to another server of the network
     */
    public void connect(@NotNull Player player, @NotNull String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendMessage(I.prefixed(LangText.NETWORK_CONNECTING, server));
        player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
    }

    /**
     * on a game server: sends the player to the lobby server (after the game or when he quits it)
     */
    public void sendToLobby(@NotNull Player player) {
        String lobby = plugin.getFileManager().getConfigFile().getString("network.lobby-server");
        if (!isGameServer() || !player.isOnline() || lobby == null || lobby.isBlank()) return;
        connect(player, lobby);
    }

    // ---- game server ----

    /**
     * @return the arena of this game server: network.arena, otherwise the first playable arena
     */
    @Nullable
    public Arena getArena() {
        String name = plugin.getFileManager().getConfigFile().getString("network.arena");
        if (name != null && !name.isBlank()) return plugin.getMapHandler().getArena(name);
        return plugin.getMapHandler().getGameMaps().stream()
                .filter(Arena::isPlayable)
                .min(Comparator.comparing(Arena::getName))
                .orElse(null);
    }

    /**
     * on a game server: creates the game of the arena if there is none
     */
    public void ensureGame() {
        if (!isGameServer()) return;
        Arena arena = getArena();
        if (arena == null || !arena.isPlayable()) {
            Debug.warn("network.mode is 'game', but there is no playable arena (network.arena)!");
            return;
        }
        if (!plugin.getGameManager().getGames(arena).isEmpty()) return;
        try {
            plugin.getGameManager().createGame(arena);
        } catch (RuntimeException ex) {
            Debug.warn("Couldn't create the game of arena '" + arena.getName() + "': " + ex.getMessage());
        }
    }

    /**
     * players joining a game server join the game (not vanished ones, e.g. admins checking the server)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (!isGameServer() || player.hasPermission(BYPASS_PERMISSION)) return;
        // after the join is done, otherwise the teleport doesn't work (and vanish plugins have hidden him)
        Bukkit.getScheduler().runTask(plugin, () -> joinGame(player));
    }

    private void joinGame(Player player) {
        if (!player.isOnline() || VanishSupport.isVanished(player)) return;
        GamePlayer gamePlayer = plugin.getUser(player);
        if (gamePlayer.getGame() != null) return;
        GameManager gameManager = plugin.getGameManager();
        for (Game game : gameManager.getGames()) {
            // comes back into his team (QuitListener)
            if (game.canRejoin(player)) return;
        }
        Arena arena = getArena();
        if (arena == null) return;
        Game game = gameManager.findJoinableGame(arena);
        if (game != null) {
            game.join(gamePlayer);
            return;
        }
        game = gameManager.findSpectatableGame(arena);
        if (game != null) {
            game.spectate(gamePlayer);
            return;
        }
        // without a game (arena not set up yet) the player stays
        if (gameManager.getGames(arena).isEmpty()) return;
        gamePlayer.sendMessage(i18n(LangText.NETWORK_GAME_UNAVAILABLE));
        sendToLobby(player);
    }

    /**
     * @return the status of the game on this server
     */
    public ServerStatus getStatus() {
        Arena arena = getArena();
        if (arena == null) return ServerStatus.OFFLINE;
        Game game = plugin.getGameManager().findJoinableGame(arena);
        if (game == null) {
            List<Game> games = plugin.getGameManager().getGames(arena);
            game = games.isEmpty() ? null : games.getFirst();
        }
        return ServerStatus.of(game);
    }

    @EventHandler
    public void onPing(ServerListPingEvent e) {
        if (!isGameServer()) return;
        ServerStatus status = getStatus();
        e.motd(Component.text(status.toMotd()));
        if (status.maxPlayers() > 0) e.setMaxPlayers(status.maxPlayers());
    }

    // ---- lobby server ----

    /**
     * @return the game servers of network.servers
     */
    public List<Server> getServers() {
        List<Server> servers = new ArrayList<>();
        for (Map<?, ?> entry : plugin.getFileManager().getConfigFile().getData().getMapList("network.servers")) {
            Object name = entry.get("name");
            Object address = entry.get("address");
            if (name == null || address == null) continue;
            String[] parts = address.toString().trim().split(":");
            try {
                servers.add(new Server(name.toString(), parts[0], parts.length > 1 ? Integer.parseInt(parts[1]) : 25565));
            } catch (NumberFormatException ex) {
                Debug.warn("Invalid address of server '" + name + "' in network.servers: " + address);
            }
        }
        return servers;
    }

    @Nullable
    public Server getServer(@NotNull String name) {
        for (Server server : getServers()) {
            if (server.name().equalsIgnoreCase(name)) return server;
        }
        return null;
    }

    /**
     * on the lobby server: reads the status of all game servers (not in the main thread, it waits for the answers)
     */
    public void pingAll() {
        if (!isLobby()) return;
        for (Server server : getServers()) {
            statuses.put(server.name().toLowerCase(),
                    ServerStatus.parse(ServerPinger.ping(server.host(), server.port(), PING_TIMEOUT_MILLIS)));
        }
    }

    /**
     * @return the last known status of the game server
     */
    @NotNull
    public ServerStatus getStatus(@NotNull String server) {
        return statuses.getOrDefault(server.toLowerCase(), ServerStatus.OFFLINE);
    }

    /**
     * on the lobby server: sends the player to the game server with the most players waiting, otherwise to a running
     * game to watch it. A party leader is sent to a server with room for his whole party, if there is one (the proxy
     * plugins of Parties or Party and Friends take the members along).
     *
     * @param spectate only running games
     */
    public void joinAnyServer(@NotNull Player player, boolean spectate) {
        Comparator<Server> mostPlayers = Comparator.comparingInt(server -> getStatus(server.name()).players());
        int partySize = spectate ? 1 : plugin.getPartySupport().getPartySize(player);
        Server server = spectate ? null : getServers().stream()
                .filter(s -> getStatus(s.name()).isJoinable())
                .filter(s -> getStatus(s.name()).maxPlayers() - getStatus(s.name()).players() >= partySize)
                .max(mostPlayers)
                .orElse(null);
        if (server == null && !spectate) {
            server = getServers().stream()
                    .filter(s -> getStatus(s.name()).isJoinable())
                    .max(mostPlayers)
                    .orElse(null);
        }
        if (server == null) {
            server = getServers().stream()
                    .filter(s -> getStatus(s.name()).isSpectatable())
                    .max(mostPlayers)
                    .orElse(null);
        }
        if (server == null) {
            player.sendMessage(I.prefixed(spectate ? LangText.NO_GAME_TO_SPECTATE : LangText.NO_GAME_TO_JOIN));
            return;
        }
        connect(player, server.name());
    }

    /**
     * on the lobby server: sends the player to the game server if he can join or watch the game there
     */
    public void joinServer(@NotNull Player player, @NotNull String name) {
        Server server = getServer(name);
        ServerStatus status = getStatus(name);
        if (server == null || !(status.isJoinable() || status.isSpectatable())) {
            player.sendMessage(I.prefixed(LangText.NO_GAME_TO_JOIN));
            return;
        }
        connect(player, server.name());
    }

}
