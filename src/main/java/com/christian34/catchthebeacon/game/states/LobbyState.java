package com.christian34.catchthebeacon.game.states;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Countdown;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.game.Variant;
import com.christian34.catchthebeacon.game.map.Arena;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.stats.StatsManager;
import com.christian34.catchthebeacon.user.GamePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;


public class LobbyState implements State {
    /**
     * the "waiting for players" message is repeated this often (in seconds)
     */
    private static final int WAITING_MESSAGE_INTERVAL = 30;
    private final Game game;
    private Countdown startTimer;
    private int secondsWaiting;

    public LobbyState(Game game) {
        this.game = game;
    }

    /**
     * lobby.countdown in config.yml (at least 10 seconds)
     */
    private static int getCountdown() {
        return Math.max(10, CatchTheBeacon.getInstance().getFileManager().getConfigFile().getInt("lobby.countdown"));
    }

    @Override
    public void start() {
        int countdown = getCountdown();
        this.secondsWaiting = 0;
        this.startTimer = new Countdown(countdown)
                .onTick(time -> {
                    Arena arena = game.getArena();
                    int players = game.getGamePlayers().size();
                    if (players < arena.getMinPlayers()) {
                        // the countdown only runs with enough players
                        if (secondsWaiting++ % WAITING_MESSAGE_INTERVAL == 0) {
                            game.broadcast(i18n(LangText.WAITING_FOR_PLAYERS));
                        }
                        return countdown;
                    }
                    secondsWaiting = 0;
                    if (time > 10 && players >= arena.getMaxPlayers()) {
                        // full - no need to wait any longer
                        time = 10;
                    }
                    if (time == 1) {
                        game.broadcast(i18n(LangText.LOBBY_COUNTDOWN_ONE, time + ""));
                        game.getGameStateManager().setGameState(GameState.INGAME);
                        return 0;
                    }
                    if (time == 60 || time == 30 || time == 15 || time == 10 || time <= 5) {
                        game.broadcast(i18n(LangText.LOBBY_COUNTDOWN, time + ""));
                    }
                    return time;
                });
        this.startTimer.run();
    }
    @Override
    public void stop() {
        // otherwise it keeps running (e.g. everybody left: pending) and starts the game later on
        cancel();
        assignTeams();
        CatchTheBeacon.getInstance().getTelemetry().votesCast(game);
        Variant variant = game.chooseVariant();
        if (variant != Variant.NORMAL) {
            game.broadcast(i18n(LangText.VARIANT_CHOSEN, variant.getName(), variant.getDescription()));
        }
    }

    /**
     * Puts the players without a team (or "random") into the smaller team - with game.balance-teams the strongest
     * first, into the team with the lower win rates on equal sizes. If a team is still empty afterwards (everybody
     * chose the same team), half of the other team is moved into it.
     * Parties (parties.same-team) are placed first and together: into the team a member chose, otherwise into the
     * smaller team, as long as it doesn't get more than half of the players.
     */
    void assignTeams() {
        List<GamePlayer> withoutTeam = new ArrayList<>();
        for (GamePlayer player : this.game.getGamePlayers()) {
            if (player.getTeam() == null || player.getTeam().equals(Team.RANDOM)) withoutTeam.add(player);
        }
        Collections.shuffle(withoutTeam);
        boolean balance = CatchTheBeacon.getInstance().getFileManager().getConfigFile().getBoolean("game.balance-teams");
        // stable sort: equal players stay shuffled
        if (balance) withoutTeam.sort(Comparator.comparingDouble(LobbyState::strength).reversed());

        Map<GamePlayer, UUID> parties = new HashMap<>();
        for (GamePlayer player : this.game.getGamePlayers()) {
            UUID party = CatchTheBeacon.getInstance().getPartySupport().getTeamParty(player.getPlayer());
            if (party != null) parties.put(player, party);
        }
        // players without a party form a group of their own
        Map<Object, List<GamePlayer>> groups = new LinkedHashMap<>();
        for (GamePlayer player : withoutTeam) {
            groups.computeIfAbsent(parties.containsKey(player) ? parties.get(player) : player,
                    key -> new ArrayList<>()).add(player);
        }
        List<List<GamePlayer>> ordered = new ArrayList<>(groups.values());
        // stable sort: the biggest parties first, the others keep their order
        ordered.sort(Comparator.comparingInt(List<GamePlayer>::size).reversed());

        int maxTeamSize = (this.game.getGamePlayers().size() + 1) / 2;
        for (List<GamePlayer> group : ordered) {
            Team team = chosenTeam(group.getFirst(), parties);
            if (team == null) team = smallerTeam(balance);
            for (GamePlayer player : group) {
                Team playerTeam = team;
                if (this.game.getGamePlayers(team).size() >= maxTeamSize) {
                    playerTeam = team == Team.RED ? Team.BLUE : Team.RED;
                }
                player.assignTeam(playerTeam);
            }
        }

        List<GamePlayer> red = new ArrayList<>(this.game.getGamePlayers(Team.RED));
        List<GamePlayer> blue = new ArrayList<>(this.game.getGamePlayers(Team.BLUE));
        if (red.isEmpty() || blue.isEmpty()) {
            List<GamePlayer> full = red.isEmpty() ? blue : red;
            Team empty = red.isEmpty() ? Team.RED : Team.BLUE;
            Collections.shuffle(full);
            for (int i = 0; i < full.size() / 2; i++) {
                full.get(i).assignTeam(empty);
            }
        }
    }

    /**
     * @return the team a member of the player's party chose, null if none
     */
    @Nullable
    private Team chosenTeam(GamePlayer player, Map<GamePlayer, UUID> parties) {
        UUID party = parties.get(player);
        if (party == null) return null;
        for (GamePlayer member : this.game.getGamePlayers()) {
            Team team = member.getTeam();
            if (party.equals(parties.get(member)) && (team == Team.RED || team == Team.BLUE)) return team;
        }
        return null;
    }

    /**
     * @return the team with fewer players - with game.balance-teams the weaker one on equal sizes
     */
    private Team smallerTeam(boolean balance) {
        int red = this.game.getGamePlayers(Team.RED).size();
        int blue = this.game.getGamePlayers(Team.BLUE).size();
        if (red != blue) return red < blue ? Team.RED : Team.BLUE;
        if (balance && teamStrength(Team.RED) != teamStrength(Team.BLUE)) {
            return teamStrength(Team.RED) < teamStrength(Team.BLUE) ? Team.RED : Team.BLUE;
        }
        return ThreadLocalRandom.current().nextBoolean() ? Team.RED : Team.BLUE;
    }

    /**
     * @return the win rate of the player, with one win and one loss added, so new players count as average (0.5)
     */
    static double strength(GamePlayer player) {
        StatsManager.PlayerStats stats = CatchTheBeacon.getInstance().getStatsManager().getCached(player.getPlayer().getUniqueId());
        if (stats == null) return 0.5;
        return (stats.wins() + 1.0) / (stats.games() + 2.0);
    }

    private double teamStrength(Team team) {
        return game.getGamePlayers(team).stream().mapToDouble(LobbyState::strength).sum();
    }

    /**
     * @return seconds until the game starts, -1 if the countdown isn't running
     */
    public int getTimeRemaining() {
        return startTimer == null ? -1 : startTimer.getTimeRemaining();
    }

    @Override
    public void cancel() {
        if (startTimer != null) startTimer.stop();
        this.startTimer = null;
    }

    public boolean forceStart() {
        if (this.game.getGamePlayers().size() < this.game.getArena().getMinPlayers()) {
            return false;
        }
        //todo set to 15
        if (!(this.startTimer.getTimeRemaining() <= 15)) {
            this.startTimer.setTimeRemaining(5);
        }
        return true;
    }

    @Override
    public GameState getGameState() {
        return GameState.LOBBY;
    }

}
