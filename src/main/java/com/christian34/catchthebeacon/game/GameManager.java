package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.files.FileManager;
import com.christian34.catchthebeacon.game.map.Arena;
import com.christian34.catchthebeacon.game.map.GameWorld;
import com.christian34.catchthebeacon.integrations.VanishSupport;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;


public class GameManager {
    private final CatchTheBeacon plugin;
    private final Set<Game> games;

    public GameManager(CatchTheBeacon plugin) {
        this.plugin = plugin;
        this.games = Collections.synchronizedSet(new HashSet<>());
        importGamesFile();
    }

    /**
     * games.yml of older versions listed the games to create on start: their arenas get auto-game instead
     */
    private void importGamesFile() {
        File file = new File(FileManager.getPluginFolder(), "games.yml");
        if (!file.isFile()) return;
        ConfigurationSection section = YamlConfiguration.loadConfiguration(file).getConfigurationSection("games");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                Arena arena = plugin.getMapHandler().getArena(section.getString(id + ".arena"));
                if (arena != null) arena.setAutoGame(true);
            }
        }
        if (!file.delete()) Debug.warn("Couldn't delete games.yml, it isn't used any more");
    }

    /**
     * creates the games of the arenas with auto-game (on a lobby server of a network there are none)
     */
    public void createAutoGames() {
        if (plugin.getNetworkManager().isLobby()) return;
        for (Arena arena : plugin.getMapHandler().getGameMaps()) {
            if (!arena.hasAutoGame() || !getGames(arena).isEmpty()) continue;
            if (!arena.isPlayable()) {
                Debug.warn("Couldn't create a game with arena '" + arena.getName() + "', it isn't set up completely: "
                        + arena.getMissingSetup().stream().map(I::plain).toList());
                continue;
            }
            try {
                createGame(arena);
            } catch (RuntimeException ex) {
                Debug.warn("Couldn't create a game with arena '" + arena.getName() + "': " + ex.getMessage());
            }
        }
    }

    /**
     * stops the games of the arena (players are sent back) and removes them
     *
     * @return the number of removed games
     */
    public int removeGames(@NotNull Arena arena) {
        List<Game> arenaGames = getGames(arena);
        for (Game game : arenaGames) {
            game.stop();
            games.remove(game);
        }
        return arenaGames.size();
    }

    /**
     * An admin stops the game: everybody is sent back (on a game server of a network: to the lobby server).
     *
     * @param nextRound true: the arena gets a new round right away, false: none until the next restart
     * @return the new round, null if there is none
     */
    @Nullable
    public Game stopGame(@NotNull Game game, boolean nextRound) {
        game.broadcast(i18n(LangText.GAME_STOPPED));
        List<Player> players = new ArrayList<>(game.getPlayers());
        game.getSpectators().forEach(spectator -> players.add(spectator.getPlayer()));
        game.stop();
        games.remove(game);
        if (plugin.getNetworkManager().isGameServer()) {
            players.forEach(player -> plugin.getNetworkManager().sendToLobby(player));
        }
        return nextRound ? createGame(game.getArena()) : null;
    }

    /**
     * @param id the unique id of the game (as in /ctb game list)
     */
    @Nullable
    public Game getGame(@NotNull String id) {
        for (Game game : new ArrayList<>(getGames())) {
            if (game.getUniqueId().equalsIgnoreCase(id)) return game;
        }
        return null;
    }

    @Nullable
    public Game getGame(World world) {
        if (world == null) return null;
        for (Game game : getGames()) {
            GameWorld gameWorld = game.getGameWorld();
            if (gameWorld.getWorld() != null && gameWorld.getWorld().equals(world)) return game;
            GameWorld lobbyWorld = game.getLobbyWorld();
            if (lobbyWorld.getWorld() != null && lobbyWorld.getWorld().equals(world)) return game;
        }
        return null;
    }

    /**
     * @param arena defines the game world
     * @return new game instance
     */
    public Game createGame(@NotNull Arena arena) {
        Game game = new Game(arena, generateUniqueId());
        games.add(game);
        return game;
    }

    public Set<Game> getGames() {
        return games;
    }

    /**
     * joins a waiting game, otherwise watches a running one - a party leader takes the members of his party along
     * (into a game with room for all of them, if there is one)
     *
     * @param arena only games of this arena, null: all
     */
    public void join(@NotNull GamePlayer gamePlayer, @Nullable Arena arena) {
        // vanish plugins would hide him from the other players (EssentialsX makes him visible)
        if (!VanishSupport.canPlay(gamePlayer.getPlayer())) {
            gamePlayer.sendMessage(LangText.JOIN_VANISHED);
            return;
        }
        List<GamePlayer> followers = new ArrayList<>();
        for (Player player : plugin.getPartySupport().getFollowers(gamePlayer.getPlayer())) {
            GamePlayer follower = plugin.getUser(player);
            if (follower.getGame() == null && plugin.getSetupManager().getSession(player) == null
                    && !VanishSupport.isVanished(player)) {
                followers.add(follower);
            }
        }
        Game game = findJoinableGame(arena, 1 + followers.size());
        if (game == null) game = findGameFor(arena, gamePlayer.getPlayer());
        if (game != null) {
            game.join(gamePlayer);
            for (GamePlayer follower : followers) {
                if (!game.isJoinable()) break;
                game.join(follower);
                follower.sendMessage(LangText.JOINED_WITH_PARTY, gamePlayer.getPlayer().getName());
            }
            return;
        }
        game = findSpectatableGame(arena);
        if (game != null) {
            game.spectate(gamePlayer);
            for (GamePlayer follower : followers) {
                game.spectate(follower);
            }
            return;
        }
        gamePlayer.sendMessage(i18n(LangText.NO_GAME_TO_JOIN));
    }

    /**
     * @param arena only games of this arena, null: all
     * @return the game a player can join, the one with the most players (so rounds fill up)
     */
    @Nullable
    public Game findJoinableGame(@Nullable Arena arena) {
        return findJoinableGame(arena, 1);
    }

    /**
     * @param arena only games of this arena, null: all
     * @param slots the number of players joining together (party)
     * @return the game with room for the players, the one with the most players (so rounds fill up)
     */
    @Nullable
    public Game findJoinableGame(@Nullable Arena arena, int slots) {
        return (arena == null ? new ArrayList<>(getGames()) : getGames(arena)).stream()
                .filter(Game::isJoinable)
                .filter(game -> game.getArena().getMaxPlayers() - game.getGamePlayers().size() >= slots)
                .max(Comparator.comparingInt(game -> game.getGamePlayers().size()))
                .orElse(null);
    }

    /**
     * @param arena only games of this arena, null: all
     * @return the game the player can join - a VIP also gets into a full lobby (see {@link Game#isJoinable(Player)})
     */
    @Nullable
    public Game findGameFor(@Nullable Arena arena, @NotNull Player player) {
        Game game = findJoinableGame(arena);
        if (game != null) return game;
        return (arena == null ? new ArrayList<>(getGames()) : getGames(arena)).stream()
                .filter(g -> g.isJoinable(player))
                .findFirst()
                .orElse(null);
    }

    /**
     * @param arena only games of this arena, null: all
     * @return a running game players can watch, null if there is none or spectators are disabled
     */
    @Nullable
    public Game findSpectatableGame(@Nullable Arena arena) {
        if (!plugin.getFileManager().getConfigFile().getBoolean("spectators.enabled")) return null;
        return (arena == null ? new ArrayList<>(getGames()) : getGames(arena)).stream()
                .filter(Game::isSpectatable)
                .max(Comparator.comparingInt(game -> game.getGamePlayers().size()))
                .orElse(null);
    }

    /**
     * @return the games played in the arena
     */
    public List<Game> getGames(@NotNull Arena arena) {
        List<Game> arenaGames = new ArrayList<>();
        for (Game game : getGames()) {
            if (game.getArena().getName().equals(arena.getName())) arenaGames.add(game);
        }
        return arenaGames;
    }

    private String generateUniqueId() {
        return UUID.randomUUID().toString().split("-")[0];
    }

}
