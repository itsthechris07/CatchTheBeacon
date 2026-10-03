package com.christian34.catchthebeacon.game.states;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.files.ConfigFile;
import com.christian34.catchthebeacon.game.Beacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.GameEvent;
import com.christian34.catchthebeacon.game.GameItems;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.lib.InteractionItems;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.stats.StatsManager;
import com.christian34.catchthebeacon.user.GamePlayer;
import com.christian34.catchthebeacon.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;


public class IngameState implements State {
    /**
     * the effects near the beacons last a bit longer than the check interval, so they don't flicker
     */
    private static final int EFFECT_TICKS = 40;
    /**
     * the boss bar of a beacon disappears if nobody has mined it for this time (the progress comes in steps)
     */
    private static final int MINING_TIMEOUT_TICKS = 60;
    /**
     * every tip is shown this long (in seconds)
     */
    private static final int TIP_SECONDS = 8;
    private final Game game;
    private BukkitTask checker;
    private int startTick;
    private List<GameEvent> events = new ArrayList<>();
    private int nextEvent;

    public IngameState(Game game) {
        this.game = game;
    }

    @Override
    public void start() {
        this.startTick = Bukkit.getCurrentTick();
        game.startRound();
        this.events = GameEvent.load();
        this.nextEvent = 0;
        for (GamePlayer gamePlayer : game.getGamePlayers()) {
            Player player = gamePlayer.getPlayer();
            gamePlayer.getUserStorage().cleanPlayer();
            Team team = gamePlayer.getTeam();
            player.teleport(game.getTeamSpawn(team));
            player.setGameMode(GameMode.SURVIVAL);
            GameItems.setItems(gamePlayer);
        }
        this.checker = Bukkit.getScheduler().runTaskTimer(CatchTheBeacon.getInstance(), this::tick, 20L, 20L);
    }

    @Override
    public void stop() {
        if (this.checker != null) this.checker.cancel();
        game.endRound();
        game.stopMining();
    }

    @Override
    public void cancel() {
        stop();
    }

    /**
     * every second, on the main thread (player locations must not be read async)
     */
    private void tick() {
        checkBeacons();
        checkMining();
        updateCompasses();
        showTips();
        runEvents();
    }

    private void checkBeacons() {
        ConfigFile config = CatchTheBeacon.getInstance().getFileManager().getConfigFile();
        // enemies within this radius around a beacon warn its team
        double warningRadius = config.getDouble("game.beacon-warning-radius");
        double fatigueRadius = config.getDouble("base.mining-fatigue-radius");
        int fatigueLevel = Math.max(1, config.getInt("base.mining-fatigue-level"));
        double regenerationRadius = config.getDouble("base.regeneration-radius");
        double trapRadius = config.getDouble("base.trap-radius");
        int trapTicks = Math.max(1, config.getInt("base.trap-seconds")) * 20;
        double campingRadius = config.getDouble("base.camping-radius");
        Set<GamePlayer> camping = new HashSet<>();

        for (Team team : Team.getTeams()) {
            for (Beacon.Position position : Beacon.Position.values()) {
                Beacon beacon = this.game.getBeacon(team, position);
                if (beacon == null || !beacon.isAlive()) continue;
                boolean isInDanger = false;
                for (GamePlayer enemy : game.getGamePlayers(team.getOpponent())) {
                    Player player = enemy.getPlayer();
                    double distance = distance(player, beacon);
                    if (distance <= warningRadius) isInDanger = true;
                    if (distance <= fatigueRadius) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, EFFECT_TICKS,
                                fatigueLevel - 1, false, false, true));
                    }
                    if (distance <= trapRadius && beacon.isTrapArmed()) {
                        beacon.disarmTrap();
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, trapTicks, 0));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, trapTicks, 1));
                        enemy.sendMessage(i18n(LangText.TRAP_TRIGGERED_YOU));
                        game.broadcast(team, i18n(LangText.TRAP_TRIGGERED, enemy.getDisplayName(), position.getName()));
                    }
                }
                if (isInDanger && !beacon.isInDanger()) {
                    game.broadcast(team, i18n(LangText.ENEMIES_NEAR_BEACON, position.getName()));
                }
                beacon.setInDanger(isInDanger);

                for (GamePlayer defender : game.getGamePlayers(team)) {
                    double distance = distance(defender.getPlayer(), beacon);
                    if (distance <= regenerationRadius) {
                        defender.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,
                                EFFECT_TICKS + 20, 0, false, false, true));
                    }
                    // defending against enemies near the beacon is no camping
                    if (distance <= campingRadius && !isInDanger) camping.add(defender);
                }
            }
        }
        punishCamping(camping, config.getInt("base.camping-seconds"));
    }

    /**
     * players standing near their own beacon for too long get poison
     */
    private void punishCamping(Set<GamePlayer> camping, int maxSeconds) {
        for (GamePlayer gamePlayer : game.getGamePlayers()) {
            if (!camping.contains(gamePlayer) || maxSeconds <= 0) {
                gamePlayer.setCampingSeconds(0);
                continue;
            }
            int seconds = gamePlayer.getCampingSeconds() + 1;
            gamePlayer.setCampingSeconds(seconds);
            if (seconds > maxSeconds) {
                if (seconds == maxSeconds + 1) gamePlayer.sendMessage(i18n(LangText.CAMPING));
                gamePlayer.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.POISON, EFFECT_TICKS, 0));
            }
        }
    }

    /**
     * the boss bar disappears if nobody has mined the beacon for a while (stopped mining, left, ...)
     */
    private void checkMining() {
        int tick = Bukkit.getCurrentTick();
        for (Team team : Team.getTeams()) {
            for (Beacon beacon : game.getBeacons(team)) {
                if (beacon.getMiner() == null) continue;
                Player miner = Bukkit.getPlayer(beacon.getMiner());
                if (tick - beacon.getLastMiningTick() > MINING_TIMEOUT_TICKS || miner == null
                        || !game.getPlayers().contains(miner)) {
                    beacon.stopMining();
                }
            }
        }
    }

    /**
     * game.beacon-compass: the compass points to an own beacon while it is being mined, otherwise to the nearest
     * enemy beacon; holding it shows the distance
     */
    private void updateCompasses() {
        if (!CatchTheBeacon.getInstance().getFileManager().getConfigFile().getBoolean("game.beacon-compass")) return;
        for (GamePlayer gamePlayer : game.getGamePlayers()) {
            Beacon target = getCompassTarget(gamePlayer);
            if (target == null) continue;
            Player player = gamePlayer.getPlayer();
            player.setCompassTarget(target.getLocation().clone().add(0.5, 0, 0.5));
            if (InteractionItems.getBeaconCompass().isSimilar(player.getInventory().getItemInMainHand())) {
                boolean defend = target.getTeam() == gamePlayer.getTeam();
                player.sendActionBar(i18n(defend ? LangText.COMPASS_DEFEND : LangText.COMPASS_ENEMY,
                        target.getPosition().getName(), Math.round(distance(player, target))));
            }
        }
    }

    /**
     * @return an own beacon being mined, otherwise the nearest enemy beacon; null if there is none
     */
    @Nullable
    public Beacon getCompassTarget(GamePlayer gamePlayer) {
        Team team = gamePlayer.getTeam();
        if (team == null || !Team.getTeams().contains(team)) return null;
        Player player = gamePlayer.getPlayer();
        Comparator<Beacon> nearest = Comparator.comparingDouble(beacon -> distance(player, beacon));
        return game.getBeacons(team).stream()
                .filter(beacon -> beacon.isAlive() && beacon.getMiner() != null)
                .min(nearest)
                .orElseGet(() -> game.getBeacons(team.getOpponent()).stream()
                        .filter(Beacon::isAlive)
                        .min(nearest)
                        .orElse(null));
    }

    /**
     * tips.rounds: players in their first rounds get tips in the action bar (the stats know their rounds)
     */
    private void showTips() {
        ConfigFile config = CatchTheBeacon.getInstance().getFileManager().getConfigFile();
        int rounds = config.getInt("tips.rounds");
        long seconds = getSecondsPlayed();
        // the action bar stays for about 3 seconds
        if (rounds <= 0 || seconds % 2 != 0) return;
        List<LangText> tips = new ArrayList<>(List.of(LangText.TIP_RESOURCES, LangText.TIP_BEACON, LangText.TIP_DEFEND,
                LangText.TIP_ARROWS));
        if (config.getBoolean("game.beacon-compass")) tips.add(2, LangText.TIP_COMPASS);
        double fatigueRadius = config.getDouble("base.mining-fatigue-radius");
        for (GamePlayer gamePlayer : game.getGamePlayers()) {
            Player player = gamePlayer.getPlayer();
            StatsManager.PlayerStats stats = CatchTheBeacon.getInstance().getStatsManager().getCached(player.getUniqueId());
            if (stats == null || stats.games() >= rounds || gamePlayer.getTeam() == null
                    || InteractionItems.getBeaconCompass().isSimilar(player.getInventory().getItemInMainHand())) continue;
            boolean nearEnemyBeacon = game.getBeacons(gamePlayer.getTeam().getOpponent()).stream()
                    .anyMatch(beacon -> beacon.isAlive() && distance(player, beacon) <= fatigueRadius);
            LangText tip = nearEnemyBeacon ? LangText.TIP_BEACON : tips.get((int) (seconds / TIP_SECONDS % tips.size()));
            player.sendActionBar(i18n(tip));
        }
    }

    /**
     * @return the second of the game the event happens (earlier in a rush)
     */
    private long eventSecond(GameEvent event) {
        return Math.round(event.second() * game.getVariant().getTimeFactor());
    }

    /**
     * @return seconds until the event happens
     */
    public long getSecondsUntil(GameEvent event) {
        return Math.max(0, eventSecond(event) - getSecondsPlayed());
    }

    private static double distance(Player player, Beacon beacon) {
        return WorldUtils.distance(player.getLocation(), beacon.getLocation().clone().add(0.5, 0, 0.5));
    }

    private void runEvents() {
        while (nextEvent < events.size() && getSecondsPlayed() >= eventSecond(events.get(nextEvent))) {
            GameEvent event = events.get(nextEvent++);
            game.log("event: " + event.action());
            game.broadcast(i18n(event.action().getMessage()));
            switch (event.action()) {
                case BEACON_PROTECTION_OFF -> game.setBeaconProtection(false);
                case EFFECTS -> game.addEventEffects(event.effects());
                case END -> {
                    // the team with more beacons left wins
                    int red = aliveBeacons(Team.RED);
                    int blue = aliveBeacons(Team.BLUE);
                    game.setWinner(red == blue ? null : (red > blue ? Team.RED : Team.BLUE));
                    game.getGameStateManager().setGameState(GameState.ENDING);
                    return;
                }
            }
        }
    }

    private int aliveBeacons(Team team) {
        return (int) game.getBeacons(team).stream().filter(Beacon::isAlive).count();
    }

    /**
     * @return the next timed event, null if there is none
     */
    @Nullable
    public GameEvent getNextEvent() {
        return nextEvent < events.size() ? events.get(nextEvent) : null;
    }

    /**
     * @return seconds since the game has started
     */
    public long getSecondsPlayed() {
        return (Bukkit.getCurrentTick() - startTick) / 20;
    }

    @Override
    public GameState getGameState() {
        return GameState.INGAME;
    }

}
