package com.christian34.catchthebeacon.lib;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Beacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.GameEvent;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.game.Variant;
import com.christian34.catchthebeacon.game.states.IngameState;
import com.christian34.catchthebeacon.game.states.LobbyState;
import com.christian34.catchthebeacon.game.states.State;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;


/**
 * The sidebar of the players in a game: lobby info while waiting, the beacons of both teams, kills and time while
 * playing. Every player gets an own scoreboard (with the teams, so name tags have the team color).
 *
 * @author Christian34
 */
public class GameScoreboard {
    private static final int MAX_LINES = 15;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private final CatchTheBeacon plugin;
    private final Component title = i18n(LangText.SCOREBOARD_TITLE);
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    /**
     * false if the server can't show custom names of scores (MockBukkit): the lines are entries then
     */
    private boolean customNames = true;

    public GameScoreboard(CatchTheBeacon plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 10L);
    }

    private void updateAll() {
        for (Game game : new ArrayList<>(plugin.getGameManager().getGames())) {
            List<GamePlayer> players = new ArrayList<>(game.getGamePlayers());
            players.addAll(game.getSpectators());
            for (GamePlayer gamePlayer : players) {
                update(game, gamePlayer);
            }
        }
    }

    /**
     * gives the player the main scoreboard back (after leaving a game)
     */
    public void remove(Player player) {
        if (boards.remove(player.getUniqueId()) != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private void update(Game game, GamePlayer gamePlayer) {
        Player player = gamePlayer.getPlayer();
        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(), uuid -> createBoard());
        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }
        setLines(board.getObjective("ctb"), board, getLines(game, gamePlayer));
        updateTeams(board, game);
    }

    private Scoreboard createBoard() {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("ctb", Criteria.DUMMY, title, RenderType.INTEGER);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        try {
            objective.numberFormat(NumberFormat.blank());
        } catch (RuntimeException ex) {
            // not implemented by MockBukkit - only hides the red numbers
            customNames = false;
        }
        for (Team team : Team.getTeams()) {
            org.bukkit.scoreboard.Team boardTeam = board.registerNewTeam("ctb_" + team.name().toLowerCase());
            boardTeam.color(NamedTextColor.nearestTo(team.getColor()));
        }
        return board;
    }

    /**
     * the lines are fixed entries with a custom name, so changing a line doesn't flicker
     */
    private void setLines(Objective objective, Scoreboard board, List<Component> lines) {
        if (!customNames) {
            for (String entry : board.getEntries()) {
                if (objective.getScore(entry).isScoreSet()) board.resetScores(entry);
            }
            for (int i = 0; i < lines.size(); i++) {
                // entries must be unique - empty lines differ by the number of spaces
                String text = LEGACY.serialize(lines.get(i));
                // entries may only have 40 characters
                text = text.substring(0, Math.min(text.length(), 40 - i)) + " ".repeat(i);
                objective.getScore(text).setScore(lines.size() - i);
            }
            return;
        }
        for (int i = 0; i < MAX_LINES; i++) {
            String entry = "line" + i;
            if (i < lines.size()) {
                Score score = objective.getScore(entry);
                score.setScore(lines.size() - i);
                if (!lines.get(i).equals(score.customName())) {
                    score.customName(lines.get(i));
                }
            } else if (objective.getScore(entry).isScoreSet()) {
                board.resetScores(entry);
            }
        }
    }

    private static void updateTeams(Scoreboard board, Game game) {
        for (GamePlayer gamePlayer : game.getGamePlayers()) {
            String name = gamePlayer.getPlayer().getName();
            Team team = gamePlayer.getTeam();
            if (team == null || !Team.getTeams().contains(team)) {
                org.bukkit.scoreboard.Team current = board.getEntryTeam(name);
                if (current != null) current.removeEntry(name);
                continue;
            }
            org.bukkit.scoreboard.Team boardTeam = board.getTeam("ctb_" + team.name().toLowerCase());
            if (boardTeam != null && !boardTeam.hasEntry(name)) {
                boardTeam.addEntry(name);
            }
        }
    }

    private List<Component> getLines(Game game, GamePlayer gamePlayer) {
        List<Component> lines = new ArrayList<>();
        State state = game.getGameStateManager().getCurrentGameState();
        lines.add(Component.empty());
        switch (game.getGameState()) {
            case PENDING, LOBBY -> {
                lines.add(i18n(LangText.SCOREBOARD_ARENA, game.getArena().getDisplayName()));
                lines.add(i18n(LangText.SCOREBOARD_PLAYERS, game.getGamePlayers().size(), game.getArena().getMaxPlayers()));
                Team team = gamePlayer.getTeam();
                lines.add(i18n(LangText.SCOREBOARD_TEAM,
                        team == null ? i18n(LangText.SCOREBOARD_TEAM_RANDOM) : team.getDisplayName()));
                lines.add(Component.empty());
                int remaining = state instanceof LobbyState lobby ? lobby.getTimeRemaining() : -1;
                if (game.getGamePlayers().size() < game.getArena().getMinPlayers() || remaining < 0) {
                    lines.add(i18n(LangText.SCOREBOARD_WAITING));
                } else {
                    lines.add(i18n(LangText.SCOREBOARD_START_IN, remaining));
                }
            }
            case INGAME -> {
                for (Team team : List.of(Team.RED, Team.BLUE)) {
                    lines.add(i18n(team.equals(gamePlayer.getTeam()) ? LangText.SCOREBOARD_TEAM_OWN
                            : LangText.SCOREBOARD_TEAM_OTHER, team.getDisplayName()));
                    for (Beacon.Position position : Beacon.Position.values()) {
                        lines.add(i18n(LangText.SCOREBOARD_BEACON, beaconStatus(game.getBeacon(team, position)),
                                position.getName()));
                    }
                    lines.add(Component.empty());
                }
                if (!gamePlayer.isSpectator()) lines.add(i18n(LangText.SCOREBOARD_KILLS, gamePlayer.getKills()));
                if (state instanceof IngameState ingame) {
                    long seconds = ingame.getSecondsPlayed();
                    lines.add(i18n(LangText.SCOREBOARD_TIME, time(seconds)));
                    if (game.getVariant() != Variant.NORMAL) {
                        lines.add(i18n(LangText.SCOREBOARD_VARIANT, game.getVariant().getName()));
                    }
                    GameEvent next = ingame.getNextEvent();
                    if (next != null) {
                        lines.add(Component.empty());
                        lines.add(i18n(LangText.SCOREBOARD_NEXT_EVENT, next.getName()));
                        lines.add(i18n(LangText.SCOREBOARD_NEXT_EVENT_TIME, time(ingame.getSecondsUntil(next))));
                    }
                }
            }
            case ENDING, RESTART -> {
                Team winner = game.getWinner();
                lines.add(i18n(LangText.SCOREBOARD_WINNER,
                        winner == null ? i18n(LangText.SCOREBOARD_WINNER_NONE) : winner.getDisplayName()));
                lines.add(i18n(LangText.SCOREBOARD_KILLS, gamePlayer.getKills()));
            }
        }
        return lines;
    }

    private static String time(long seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private static Component beaconStatus(Beacon beacon) {
        if (beacon == null) return i18n(LangText.SCOREBOARD_BEACON_NONE);
        if (!beacon.isAlive()) return i18n(LangText.SCOREBOARD_BEACON_DESTROYED);
        if (beacon.isInDanger()) return i18n(LangText.SCOREBOARD_BEACON_DANGER);
        return i18n(LangText.SCOREBOARD_BEACON_ALIVE);
    }

}
