package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.map.Arena;
import com.christian34.catchthebeacon.game.map.GameWorld;
import com.christian34.catchthebeacon.game.map.LobbyMap;
import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.game.states.GameStateManager;
import com.christian34.catchthebeacon.lib.InteractionItems;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import com.christian34.catchthebeacon.utils.WorldUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;


public class Game {
    /**
     * /ctb start and the start item in the lobby
     */
    public static final String START_PERMISSION = "ctb.vip";
    private final CatchTheBeacon instance;
    private final Arena arena;
    private final GameStateManager gameStateManager;
    private final Set<GamePlayer> gamePlayers;
    private final GameWorld gameWorld;
    private final Location lobbySpawn;
    private final String uniqueId;
    private final GameWorld lobbyWorld;
    private final Map<Team, Location> teamSpawns;
    private final Map<Team, Set<Beacon>> beacons;
    private final Set<GamePlayer> spectators = new HashSet<>();
    /**
     * players who have left the running game and can come back (see {@link #disconnect(GamePlayer)})
     */
    private final Map<UUID, Rejoin> rejoins = new HashMap<>();
    /**
     * blocks placed by players in the game world (game.only-placed-blocks-breakable)
     */
    private final Set<BlockPos> placedBlocks = new HashSet<>();
    /**
     * the effects of timed events everybody has (again after a respawn)
     */
    private final List<PotionEffect> eventEffects = new ArrayList<>();
    private boolean beaconProtection = true;
    /**
     * the variant players voted for in the lobby (see {@link Variant})
     */
    private final Map<UUID, Variant> votes = new HashMap<>();
    private Variant variant = Variant.NORMAL;
    /**
     * for the round summary
     */
    private final List<DestroyedBeacon> destroyedBeacons = new ArrayList<>();
    private final Map<Team, Map<Material, Integer>> minedResources = new HashMap<>();
    private int startTick = -1;
    private int endTick = -1;
    private boolean anyKill;
    private Team winner;
    /**
     * true while the game is stopped (plugin disabled): players leaving must not change the state any more
     */
    private boolean stopping;

    private record BlockPos(int x, int y, int z) {
        BlockPos(Block block) {
            this(block.getX(), block.getY(), block.getZ());
        }
    }

    private record Rejoin(Team team, int kills, BukkitTask expiry) {
    }

    /**
     * @param second seconds after the start of the round
     */
    public record DestroyedBeacon(UUID player, String playerName, Team playerTeam, Beacon beacon, long second) {
    }

    protected Game(@NotNull Arena arena, String uniqueId) {
        this.instance = CatchTheBeacon.getInstance();
        this.arena = arena;
        this.uniqueId = uniqueId;
        this.gamePlayers = new HashSet<>();
        this.gameStateManager = new GameStateManager(this);
        this.teamSpawns = new HashMap<>();
        this.gameWorld = new GameWorld(arena.getDirectory(), uniqueId);
        LobbyMap lobby = arena.getLobbyMap();
        this.lobbyWorld = new GameWorld(arena.getLobbyMap().getDirectory(), uniqueId + "_lobby");
        this.lobbyWorld.loadWorld();
        this.lobbySpawn = lobby.getSpawnLocation();
        if (lobbySpawn == null) {
            throw new Error("Location is null!");
        }
        this.lobbySpawn.setWorld(lobbyWorld.getWorld());

        this.beacons = new HashMap<>();
        for (Team team : Team.getTeams()) {
            Set<Beacon> beaconSet = new HashSet<>();
            Map<Beacon.Position, Location> beacons = arena.getBeaconLocations(team);
            if (beacons == null) continue;
            for (Beacon.Position position : beacons.keySet()) {
                Beacon beacon = new Beacon(position, team, this);
                beacon.setLocation(beacons.get(position));
                beaconSet.add(beacon);
            }
            this.beacons.put(team, beaconSet);
        }

        log("A new game with arena '" + arena.getName() + "' has been created!");
    }

    public void log(String message) {
        File logsDir = instance.getFileManager().getLogsDir();
        File logFile = new File(logsDir, getUniqueId() + ".txt");
        try {
            if (logFile.exists() || logFile.createNewFile()) {
                FileWriter fw = new FileWriter(logFile, true);
                PrintWriter pw = new PrintWriter(fw);
                String time = DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now());
                pw.println("[" + time + "] " + message);
                pw.flush();
                pw.close();
            }
        } catch (IOException ignored) {
        }
    }

    @Nullable
    public Beacon getBeacon(Location location) {
        for (Team team : beacons.keySet()) {
            Set<Beacon> allBeacons = beacons.get(team);
            for (Beacon beacon : allBeacons) {
                if (WorldUtils.isSameBlock(location, beacon.getLocation())) {
                    return beacon;
                }
            }
        }
        return null;
    }

    @NotNull
    public Set<Beacon> getBeacons(Team team) {
        return new HashSet<>(this.beacons.getOrDefault(team, new HashSet<>()));
    }

    @Nullable
    public Beacon getBeacon(Team team, Beacon.Position position) {
        if (!this.beacons.containsKey(team)) return null;
        for (Beacon beacon : this.beacons.get(team)) {
            if (beacon.getPosition().equals(position)) return beacon;
        }
        return null;
    }

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public GameWorld getGameWorld() {
        return gameWorld;
    }

    public GameWorld getLobbyWorld() {
        return lobbyWorld;
    }

    public void stop() {
        log("stopping game...");
        this.stopping = true;
        gameStateManager.cancel();
        for (GamePlayer gamePlayer : new ArrayList<>(gamePlayers)) {
            quit(gamePlayer);
        }
        for (GamePlayer spectator : new ArrayList<>(spectators)) {
            quit(spectator);
        }
        clearRejoins();
        try {
            this.lobbyWorld.close(false);
            this.gameWorld.close(false);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void join(GamePlayer gamePlayer) {
        log("Player '" + gamePlayer.getPlayer().getName() + "' joined the game.");
        if (gamePlayers.size() == 0) {
            getGameStateManager().setGameState(GameState.LOBBY);
        }
        this.gamePlayers.add(gamePlayer);
        gamePlayer.getUserStorage().store();
        gamePlayer.getUserStorage().cleanPlayer();
        instance.getEssentials().joinGame(gamePlayer.getPlayer(), false);
        gamePlayer.setGame(this);
        gamePlayer.getPlayer().setGameMode(GameMode.ADVENTURE);
        gamePlayer.getPlayer().getInventory().setItem(4, InteractionItems.getSelectTeamItem());
        if (!Variant.getVotable().isEmpty()) {
            gamePlayer.getPlayer().getInventory().setItem(6, InteractionItems.getVoteItem());
        }
        if (gamePlayer.getPlayer().hasPermission(START_PERMISSION)) {
            gamePlayer.getPlayer().getInventory().setItem(2, InteractionItems.getStartItem());
        }
        gamePlayer.teleport(lobbySpawn);

        broadcast(i18n(LangText.PLAYER_JOINED_GAME, gamePlayer.getPlayer().displayName(), gamePlayers.size(),
                arena.getMaxPlayers()));
        updateTabList();

        if (gamePlayers.size() >= arena.getMinPlayers() && this.gameWorld.getWorld() == null) {
            prepareGameWorld();
        }
    }

    public void quit(GamePlayer gamePlayer) {
        leave(gamePlayer, i18n(LangText.PLAYER_LEFT_GAME, gamePlayer.getPlayer().displayName(),
                gamePlayers.size() - (gamePlayers.contains(gamePlayer) ? 1 : 0), arena.getMaxPlayers()));
    }

    /**
     * The player has left the server during the game: with game.rejoin-seconds, he can come back into his team
     * within this time (the team doesn't lose in the meantime).
     */
    public void disconnect(GamePlayer gamePlayer) {
        int seconds = instance.getFileManager().getConfigFile().getInt("game.rejoin-seconds");
        Team team = gamePlayer.getTeam();
        if (seconds <= 0 || stopping || getGameState() != GameState.INGAME || !gamePlayers.contains(gamePlayer)
                || team == null || !Team.getTeams().contains(team)) {
            quit(gamePlayer);
            return;
        }
        UUID uuid = gamePlayer.getPlayer().getUniqueId();
        BukkitTask expiry = Bukkit.getScheduler().runTaskLater(instance, () -> {
            rejoins.remove(uuid);
            checkForWinner();
        }, seconds * 20L);
        rejoins.put(uuid, new Rejoin(team, gamePlayer.getKills(), expiry));
        leave(gamePlayer, i18n(LangText.PLAYER_LEFT_CAN_REJOIN, gamePlayer.getDisplayName(), seconds));
    }

    /**
     * @return true if the player has left the running game and can still come back
     */
    public boolean canRejoin(Player player) {
        return rejoins.containsKey(player.getUniqueId()) && getGameState() == GameState.INGAME;
    }

    /**
     * brings a player back into his team (see {@link #disconnect(GamePlayer)})
     */
    public void rejoin(GamePlayer gamePlayer) {
        Rejoin rejoin = rejoins.remove(gamePlayer.getPlayer().getUniqueId());
        if (rejoin == null) return;
        rejoin.expiry().cancel();
        log("Player '" + gamePlayer.getPlayer().getName() + "' rejoined the game.");
        this.gamePlayers.add(gamePlayer);
        gamePlayer.getUserStorage().store();
        gamePlayer.getUserStorage().cleanPlayer();
        instance.getEssentials().joinGame(gamePlayer.getPlayer(), false);
        gamePlayer.setGame(this);
        gamePlayer.assignTeam(rejoin.team());
        gamePlayer.setKills(rejoin.kills());
        Player player = gamePlayer.getPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        gamePlayer.teleport(getTeamSpawn(rejoin.team()));
        GameItems.setItems(gamePlayer);
        applyEventEffects(player);
        broadcast(i18n(LangText.PLAYER_REJOINED, gamePlayer.getDisplayName()));
        updateTabList();
    }

    private void clearRejoins() {
        for (Rejoin rejoin : rejoins.values()) {
            rejoin.expiry().cancel();
        }
        rejoins.clear();
    }

    private void leave(GamePlayer gamePlayer, Component message) {
        log("Player '" + gamePlayer.getPlayer().getName() + "' quit the game.");
        boolean spectator = spectators.remove(gamePlayer);
        gamePlayers.remove(gamePlayer);
        votes.remove(gamePlayer.getPlayer().getUniqueId());
        for (Set<Beacon> teamBeacons : beacons.values()) {
            for (Beacon beacon : teamBeacons) {
                beacon.hideBossBar(gamePlayer.getPlayer());
            }
        }
        gamePlayer.getUserStorage().restore();
        gamePlayer.setGame(null);
        instance.getEssentials().leaveGame(gamePlayer.getPlayer());
        gamePlayer.setSpectator(false);
        Player player = gamePlayer.getPlayer();
        player.setInvulnerable(false);
        player.setCollidable(true);
        instance.getUsers().remove(gamePlayer);
        if (!spectator) broadcast(message);
        instance.getTabListManager().update(gamePlayer);
        instance.getGameScoreboard().remove(player);
        Set<Player> playersInGame = getPlayers();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!playersInGame.contains(p)) {
                p.showPlayer(instance, player);
                player.showPlayer(instance, p);
            }
        }

        if (stopping) return;
        if (gamePlayers.isEmpty() && getGameState().equals(GameState.LOBBY)) {
            getGameStateManager().setGameState(GameState.PENDING);
        } else {
            checkForWinner();
        }
    }

    /**
     * the last team with players wins (players who can still rejoin count)
     */
    private void checkForWinner() {
        if (stopping || getGameState() != GameState.INGAME) return;
        List<Team> teamsLeft = new ArrayList<>();
        for (Team team : Team.getTeams()) {
            boolean rejoining = rejoins.values().stream().anyMatch(rejoin -> rejoin.team() == team);
            if (!getGamePlayers(team).isEmpty() || rejoining) teamsLeft.add(team);
        }
        if (teamsLeft.size() < 2) {
            this.winner = teamsLeft.isEmpty() ? null : teamsLeft.getFirst();
            getGameStateManager().setGameState(GameState.ENDING);
        }
    }

    /**
     * the player votes for the variant of the round (again: changes his vote)
     */
    public void vote(GamePlayer gamePlayer, Variant variant) {
        votes.put(gamePlayer.getPlayer().getUniqueId(), variant);
    }

    @Nullable
    public Variant getVote(GamePlayer gamePlayer) {
        return votes.get(gamePlayer.getPlayer().getUniqueId());
    }

    public int getVotes(Variant variant) {
        return (int) votes.values().stream().filter(variant::equals).count();
    }

    /**
     * the variant with the most votes is played (a random one of them on a tie, normal without votes)
     */
    public Variant chooseVariant() {
        List<Variant> votable = Variant.getVotable();
        int most = votable.stream().mapToInt(this::getVotes).max().orElse(0);
        List<Variant> best = votable.stream().filter(v -> most > 0 && getVotes(v) == most).toList();
        this.variant = best.isEmpty() ? Variant.NORMAL : best.get(ThreadLocalRandom.current().nextInt(best.size()));
        return variant;
    }

    public Variant getVariant() {
        return variant;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
    }

    /**
     * the round has started (IngameState)
     */
    public void startRound() {
        this.startTick = Bukkit.getCurrentTick();
        this.endTick = -1;
    }

    /**
     * the round is over (IngameState): the time stops
     */
    public void endRound() {
        if (startTick >= 0 && endTick < 0) this.endTick = Bukkit.getCurrentTick();
    }

    /**
     * @return seconds since the start of the round (until its end), 0 before
     */
    public long getRoundSeconds() {
        if (startTick < 0) return 0;
        return ((endTick < 0 ? Bukkit.getCurrentTick() : endTick) - startTick) / 20;
    }

    public void addDestroyedBeacon(GamePlayer gamePlayer, Beacon beacon) {
        destroyedBeacons.add(new DestroyedBeacon(gamePlayer.getPlayer().getUniqueId(), gamePlayer.getPlayer().getName(),
                gamePlayer.getTeam(), beacon, getRoundSeconds()));
    }

    public List<DestroyedBeacon> getDestroyedBeacons() {
        return destroyedBeacons;
    }

    /**
     * a player of the team got a reward for a resource block of the map
     */
    public void addMinedResource(Team team, Material material) {
        minedResources.computeIfAbsent(team, key -> new EnumMap<>(Material.class)).merge(material, 1, Integer::sum);
    }

    /**
     * @return the resource blocks each team has mined
     */
    public Map<Team, Map<Material, Integer>> getMinedResources() {
        return minedResources;
    }

    /**
     * @return true for the first kill of the round only
     */
    public boolean isFirstKill() {
        if (anyKill) return false;
        anyKill = true;
        return true;
    }

    /**
     * nobody mines the beacons any more (end of the round): hides the boss bars
     */
    public void stopMining() {
        for (Set<Beacon> teamBeacons : beacons.values()) {
            teamBeacons.forEach(Beacon::stopMining);
        }
    }

    /**
     * lets the player watch the running game (flying, invisible for the players, with a compass to teleport)
     */
    public void spectate(GamePlayer gamePlayer) {
        log("Player '" + gamePlayer.getPlayer().getName() + "' is watching the game.");
        spectators.add(gamePlayer);
        gamePlayer.getUserStorage().store();
        gamePlayer.getUserStorage().cleanPlayer();
        instance.getEssentials().joinGame(gamePlayer.getPlayer(), true);
        gamePlayer.setGame(this);
        gamePlayer.setSpectator(true);
        Player player = gamePlayer.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        player.setCollidable(false);
        player.getInventory().setItem(0, InteractionItems.getSpectatorCompass());
        player.getInventory().setItem(8, InteractionItems.getLeaveItem());
        Location spawn = getTeamSpawn(Team.RED);
        gamePlayer.teleport(spawn != null ? spawn : lobbySpawn);
        gamePlayer.sendMessage(i18n(LangText.SPECTATE_STARTED));
        updateTabList();
    }

    public Set<GamePlayer> getSpectators() {
        return spectators;
    }

    /**
     * @return true if players can watch the game
     */
    public boolean isSpectatable() {
        return getGameState() == GameState.INGAME;
    }

    /**
     * a player placed the block (it may be broken again with game.only-placed-blocks-breakable)
     */
    public void addPlacedBlock(Block block) {
        placedBlocks.add(new BlockPos(block));
    }

    public boolean isPlacedBlock(Block block) {
        return placedBlocks.contains(new BlockPos(block));
    }

    public void removePlacedBlock(Block block) {
        placedBlocks.remove(new BlockPos(block));
    }

    /**
     * @return false once the protection zone around the beacons has been removed by a timed event
     */
    public boolean hasBeaconProtection() {
        return beaconProtection;
    }

    public void setBeaconProtection(boolean beaconProtection) {
        this.beaconProtection = beaconProtection;
    }

    /**
     * everybody gets the effects until the end of the game (also after a respawn)
     */
    public void addEventEffects(List<PotionEffect> effects) {
        eventEffects.addAll(effects);
        for (GamePlayer gamePlayer : gamePlayers) {
            applyEventEffects(gamePlayer.getPlayer());
        }
    }

    public void applyEventEffects(Player player) {
        player.addPotionEffects(eventEffects);
    }

    private void prepareGameWorld() {
        log("preparing world");
        this.gameWorld.loadWorld();
        for (Team team : Team.getTeams()) {
            Location loc = this.arena.getSpawnLocation(team);
            loc.setWorld(this.gameWorld.getWorld());
            this.teamSpawns.put(team, loc);
            for (Beacon beacon : getBeacons(team)) {
                beacon.getLocation().setWorld(this.gameWorld.getWorld());
            }
        }
    }

    @NotNull
    public GameState getGameState() {
        return this.gameStateManager.getCurrentGameState().getGameState();
    }

    public void updateTabList() {
        for (GamePlayer gamePlayer : getGamePlayers()) {
            instance.getTabListManager().update(gamePlayer);
        }

        Set<Player> allPlayers = new HashSet<>(Bukkit.getOnlinePlayers());
        Set<Player> playersInGame = getPlayers();

        for (Player player : allPlayers) {
            for (Player gamePlayer : playersInGame) {
                if (player.equals(gamePlayer)) continue;
                try {
                    if (!playersInGame.contains(player)) {
                        gamePlayer.hidePlayer(instance, player);
                        player.hidePlayer(instance, gamePlayer);
                    } else {
                        player.showPlayer(instance, gamePlayer);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        // spectators see everybody in the game, but nobody sees them (except other spectators)
        for (GamePlayer spectator : spectators) {
            Player player = spectator.getPlayer();
            instance.getTabListManager().update(spectator);
            for (Player gamePlayer : playersInGame) {
                gamePlayer.hidePlayer(instance, player);
                player.showPlayer(instance, gamePlayer);
            }
            for (GamePlayer other : spectators) {
                if (other != spectator) player.showPlayer(instance, other.getPlayer());
            }
        }
    }

    public void broadcast(Component message) {
        this.gamePlayers.forEach((player) -> player.sendMessage(message));
        this.spectators.forEach((player) -> player.sendMessage(message));
    }

    public void broadcast(Team team, Component message) {
        getGamePlayers(team).forEach((player) -> player.sendMessage(message));
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public Arena getArena() {
        return arena;
    }

    public GameStateManager getGameStateManager() {
        return gameStateManager;
    }

    public Set<GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public Set<Player> getPlayers() {
        return getGamePlayers().stream().map(GamePlayer::getPlayer).collect(Collectors.toSet());
    }

    @NotNull
    public Set<GamePlayer> getGamePlayers(@NotNull Team team) {
        Set<GamePlayer> players = new HashSet<>();
        for (GamePlayer gamePlayer : getGamePlayers()) {
            if (gamePlayer.getTeam() != null && gamePlayer.getTeam().equals(team)) {
                players.add(gamePlayer);
            }
        }
        return players;
    }

    /**
     * @return the team that has won (null while the game is running or if nobody has won)
     */
    @Nullable
    public Team getWinner() {
        return winner;
    }

    public void setWinner(@Nullable Team winner) {
        this.winner = winner;
    }

    /**
     * @return true if players can join (the game hasn't started yet)
     */
    public boolean isJoinable() {
        GameState state = getGameState();
        return (state == GameState.PENDING || state == GameState.LOBBY)
                && gamePlayers.size() < arena.getMaxPlayers();
    }

    public Location getTeamSpawn(Team team) {
        return this.teamSpawns.get(team);
    }

}
