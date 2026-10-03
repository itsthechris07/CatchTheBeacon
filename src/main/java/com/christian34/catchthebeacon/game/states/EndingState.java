package com.christian34.catchthebeacon.game.states;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Beacon;
import com.christian34.catchthebeacon.game.Countdown;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.RoundSummary;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.network.NetworkManager;
import com.christian34.catchthebeacon.stats.Achievement;
import com.christian34.catchthebeacon.stats.AchievementManager;
import com.christian34.catchthebeacon.stats.StatsManager;
import com.christian34.catchthebeacon.user.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

/**
 * The end of a game: shows the winner with a title and fireworks, then sends everybody back (with a button to join
 * the next round) and restarts the game.
 *
 * @author Christian34
 */
public class EndingState implements State {
    /**
     * the shortest time until the players are sent back (game.ending-seconds)
     */
    public static final int MIN_DURATION = 3;
    private static final int FIREWORK_SECONDS = 10;
    private final Game game;
    private Countdown countdown;

    public EndingState(Game game) {
        this.game = game;
    }

    @Override
    public void start() {
        if (this.game.getGamePlayers().isEmpty()) {
            for (GamePlayer spectator : new ArrayList<>(game.getSpectators())) {
                game.quit(spectator);
            }
            this.game.getGameStateManager().setGameState(GameState.RESTART);
            return;
        }
        Team winner = game.getWinner();
        game.log(winner == null ? "The game ended without a winner" : "Team " + winner.getName() + " has won");
        game.broadcast(winner == null ? i18n(LangText.GAME_DRAW) : i18n(LangText.GAME_WON, winner.getDisplayName()));
        StatsManager stats = CatchTheBeacon.getInstance().getStatsManager();
        for (GamePlayer gamePlayer : game.getGamePlayers()) {
            stats.add(gamePlayer.getPlayer(), StatsManager.Stat.GAMES);
            if (winner != null && winner.equals(gamePlayer.getTeam())) {
                stats.add(gamePlayer.getPlayer(), StatsManager.Stat.WINS);
                unlockWinAchievements(gamePlayer, winner);
            }
        }
        CatchTheBeacon.getInstance().getTelemetry().roundPlayed(game);
        if (CatchTheBeacon.getInstance().getFileManager().getConfigFile().getBoolean("game.round-summary")) {
            for (Component line : RoundSummary.getLines(game)) {
                game.broadcast(line);
            }
        }

        Title title = Title.title(
                winner == null ? Component.empty() : i18n(LangText.TITLE_WINNER, winner.getDisplayName()),
                i18n(LangText.TITLE_WINNER_SUBTITLE),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1)));
        for (GamePlayer gamePlayer : game.getGamePlayers()) {
            Player player = gamePlayer.getPlayer();
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
            player.setFoodLevel(20);
            player.showTitle(title);
            player.playSound(player.getLocation(), winner != null && winner.equals(gamePlayer.getTeam())
                    ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_WITHER_DEATH, 1f, 1f);
        }

        int duration = getDuration();
        this.countdown = new Countdown(duration + 1).onTick(time -> {
            if (time > duration - FIREWORK_SECONDS) {
                launchFireworks(winner);
            }
            if (time <= 5 || time == 10) {
                game.broadcast(i18n(LangText.BACK_TO_LOBBY_COUNTDOWN, time));
            }
            if (time <= 1) {
                finish();
                return 0;
            }
            return -1;
        });
        this.countdown.run();
    }

    /**
     * @return seconds until the players are sent back (game.ending-seconds)
     */
    public static int getDuration() {
        return Math.max(MIN_DURATION, CatchTheBeacon.getInstance().getFileManager().getConfigFile().getInt("game.ending-seconds"));
    }

    private void unlockWinAchievements(GamePlayer gamePlayer, Team winner) {
        AchievementManager achievements = CatchTheBeacon.getInstance().getAchievementManager();
        Player player = gamePlayer.getPlayer();
        achievements.unlock(player, Achievement.FIRST_WIN);
        // not if the enemies have just left
        boolean destroyedAll = game.getBeacons(winner.getOpponent()).stream().noneMatch(Beacon::isAlive);
        if (destroyedAll && game.getDestroyedBeacons().stream().noneMatch(destroyed -> destroyed.beacon().getTeam() == winner)) {
            achievements.unlock(player, Achievement.FLAWLESS);
        }
        if (destroyedAll && game.getRoundSeconds() <= Achievement.getQuickWinMinutes() * 60L) {
            achievements.unlock(player, Achievement.QUICK_WIN);
        }
        // the win of this round isn't in the cache yet
        StatsManager.PlayerStats stats = CatchTheBeacon.getInstance().getStatsManager().getCached(player.getUniqueId());
        if (stats != null && stats.wins() + 1 >= Achievement.VETERAN_WINS) {
            achievements.unlock(player, Achievement.VETERAN);
        }
    }

    private void launchFireworks(Team winner) {
        List<GamePlayer> players = new ArrayList<>(winner == null ? game.getGamePlayers() : game.getGamePlayers(winner));
        Color color = winner == null ? Color.WHITE : winner.getLeatherColor();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (GamePlayer gamePlayer : players) {
            Location location = gamePlayer.getPlayer().getLocation().add(random.nextDouble(-4, 4), 1, random.nextDouble(-4, 4));
            location.getWorld().spawn(location, Firework.class, firework -> {
                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(random.nextBoolean() ? FireworkEffect.Type.BALL_LARGE : FireworkEffect.Type.STAR)
                        .withColor(color)
                        .withFade(Color.WHITE)
                        .flicker(random.nextBoolean())
                        .trail(true)
                        .build());
                meta.setPower(1);
                firework.setFireworkMeta(meta);
            });
        }
    }

    /**
     * sends everybody back and offers to join the next round (on a game server of a network: to the lobby server)
     */
    private void finish() {
        NetworkManager network = CatchTheBeacon.getInstance().getNetworkManager();
        Component playAgain = I.prefixed(i18n(LangText.PLAY_AGAIN)
                .clickEvent(ClickEvent.runCommand("/ctb join"))
                .hoverEvent(HoverEvent.showText(i18n(LangText.PLAY_AGAIN_HOVER))));
        List<GamePlayer> everybody = new ArrayList<>(game.getGamePlayers());
        everybody.addAll(game.getSpectators());
        for (GamePlayer gamePlayer : everybody) {
            game.quit(gamePlayer);
            if (network.isGameServer()) {
                network.sendToLobby(gamePlayer.getPlayer());
            } else {
                gamePlayer.getPlayer().sendMessage(playAgain);
            }
        }
        game.getGameStateManager().setGameState(GameState.RESTART);
    }

    @Override
    public void stop() {
        if (countdown != null) {
            countdown.stop();
        }
        try {
            this.game.getLobbyWorld().close(false);
            this.game.getGameWorld().close(false);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void cancel() {
        if (countdown != null) countdown.stop();
    }

    @Override
    public GameState getGameState() {
        return GameState.ENDING;
    }

}
