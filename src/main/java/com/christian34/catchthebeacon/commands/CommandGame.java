package com.christian34.catchthebeacon.commands;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.GameManager;
import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.game.states.LobbyState;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.christian34.catchthebeacon.commands.CommandCatchTheBeacon.ADMIN;
import static com.christian34.catchthebeacon.commands.CommandCatchTheBeacon.ROOT;

/**
 * Managing the running games (/ctb game ...): list, start, stop and watch them.
 *
 * @author Christian34
 */
@Permission(ADMIN)
public class CommandGame {
    private static final String GAME = ROOT + " game";
    private static final String ONE_GAME = GAME + " <game>";

    private GameManager games() {
        return CatchTheBeacon.getInstance().getGameManager();
    }

    @Command(GAME + " list")
    @CommandDescription("command_game_list")
    public void listCmd(CommandSender sender) {
        List<Game> list = new ArrayList<>(games().getGames());
        if (list.isEmpty()) {
            sender.sendMessage(I.prefixed(LangText.GAME_LIST_EMPTY));
            return;
        }
        list.sort(Comparator.comparing((Game game) -> game.getArena().getName()).thenComparing(Game::getUniqueId));
        sender.sendMessage(I.i18n(LangText.GAME_LIST_TITLE));
        for (Game game : list) {
            String id = game.getUniqueId();
            Component line = I.i18n(LangText.GAME_LIST_ENTRY, id, game.getArena().getName(), game.getGameState().name(),
                    game.getGamePlayers().size(), game.getArena().getMaxPlayers(), game.getSpectators().size(),
                    formatTime(game.getRoundSeconds()));
            if (game.getGameState() == GameState.LOBBY) {
                line = line.appendSpace().append(I.button(LangText.BUTTON_START_GAME, ClickEvent.runCommand("/ctb game " + id + " start")));
            }
            if (game.isSpectatable() && sender instanceof Player) {
                line = line.appendSpace().append(I.button(LangText.BUTTON_SPECTATE_GAME, ClickEvent.runCommand("/ctb game " + id + " spectate")));
            }
            line = line.appendSpace().append(I.button(LangText.BUTTON_STOP_GAME, ClickEvent.suggestCommand("/ctb game " + id + " stop")));
            sender.sendMessage(line);
        }
    }

    static String formatTime(long seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * players: the menu of the games ({@link GameMenu}), the console: the list
     */
    @Command(GAME)
    @CommandDescription("command_game")
    public void menuCmd(CommandSender sender) {
        if (sender instanceof Player player) {
            GameMenu.openGames(player);
        } else {
            listCmd(sender);
        }
    }

    @Command(ONE_GAME + " start")
    @CommandDescription("command_game_start")
    public void startCmd(CommandSender sender, @Argument("game") Game game) {
        start(sender, game);
    }

    /**
     * ends the game without a winner and without stats - the arena gets a new round (--remove: none until the restart)
     */
    @Command(ONE_GAME + " stop")
    @CommandDescription("command_game_stop")
    public void stopCmd(CommandSender sender, @Argument("game") Game game, @Flag("remove") boolean remove) {
        stop(sender, game, remove);
    }

    @Command(ONE_GAME + " spectate")
    @CommandDescription("command_game_spectate")
    public void spectateCmd(Player player, @Argument("game") Game game) {
        spectate(player, game);
    }

    /**
     * @return false if the game can't be started (the sender got a message why)
     */
    static boolean start(CommandSender sender, Game game) {
        if (game.getGameState() != GameState.LOBBY) {
            sender.sendMessage(I.prefixed(LangText.GAME_WAS_ALREADY_STARTED));
            return false;
        }
        if (!((LobbyState) game.getGameStateManager().getCurrentGameState()).forceStart()) {
            sender.sendMessage(I.prefixed(LangText.GAME_NOT_ENOUGH_PLAYERS, game.getArena().getMinPlayers()));
            return false;
        }
        sender.sendMessage(I.prefixed(LangText.FORCE_START));
        return true;
    }

    static void stop(CommandSender sender, Game game, boolean remove) {
        Game next = CatchTheBeacon.getInstance().getGameManager().stopGame(game, !remove);
        if (next == null) {
            sender.sendMessage(I.prefixed(LangText.GAME_STOPPED_REMOVED, game.getUniqueId(), game.getArena().getName()));
        } else {
            sender.sendMessage(I.prefixed(LangText.GAME_STOPPED_NEXT_ROUND, game.getUniqueId(), next.getUniqueId()));
        }
    }

    static void spectate(Player player, Game game) {
        GamePlayer gamePlayer = CatchTheBeacon.getInstance().getUser(player);
        if (gamePlayer.getGame() != null) {
            gamePlayer.sendMessage(LangText.ALREADY_IN_GAME);
            return;
        }
        if (!game.isSpectatable()) {
            gamePlayer.sendMessage(LangText.NO_GAME_TO_SPECTATE);
            return;
        }
        game.spectate(gamePlayer);
    }

}
