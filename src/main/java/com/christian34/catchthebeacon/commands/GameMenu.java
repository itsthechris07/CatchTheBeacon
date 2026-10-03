package com.christian34.catchthebeacon.commands;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.lib.Menu;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

/**
 * The game manager for admins (/ctb game): all games, clicking one shows what can be done with it (start, watch,
 * stop). Every action checks again that the game still exists - the menu may be old.
 *
 * @author Christian34
 */
public final class GameMenu {
    /**
     * the players of a game listed in the description of its item
     */
    private static final int MAX_LISTED_PLAYERS = 8;

    private GameMenu() {
    }

    public static void openGames(Player player) {
        List<Game> games = new ArrayList<>(CatchTheBeacon.getInstance().getGameManager().getGames());
        if (games.isEmpty()) {
            player.sendMessage(I.prefixed(LangText.GAME_LIST_EMPTY));
            return;
        }
        games.sort(Comparator.comparing((Game game) -> game.getArena().getName()).thenComparing(Game::getUniqueId));
        Menu menu = new Menu(i18n(LangText.GUI_GAMES));
        for (Game game : games) {
            List<Component> lore = new ArrayList<>(details(game));
            List<GamePlayer> players = new ArrayList<>(game.getGamePlayers());
            for (int i = 0; i < Math.min(MAX_LISTED_PLAYERS, players.size()); i++) {
                lore.add(Component.text("- ").append(players.get(i).getDisplayName()));
            }
            if (players.size() > MAX_LISTED_PLAYERS) {
                lore.add(i18n(LangText.GAME_MENU_MORE_PLAYERS, players.size() - MAX_LISTED_PLAYERS));
            }
            lore.add(Component.empty());
            lore.add(i18n(LangText.GAME_MENU_CLICK));
            menu.button(new ItemStack(icon(game.getGameState())), name(game), summary(game), lore,
                    clicker -> ifRunning(clicker, game, () -> openGame(clicker, game)));
        }
        menu.show(player);
    }

    public static void openGame(Player player, Game game) {
        Menu menu = new Menu(i18n(LangText.GUI_GAME, game.getUniqueId(), game.getArena().getName()));
        if (game.getGameState() == GameState.LOBBY) {
            menu.button(new ItemStack(Material.EMERALD), i18n(LangText.ITEM_GAME_START), null,
                    List.of(i18n(LangText.ITEM_GAME_START_LORE, game.getArena().getMinPlayers())),
                    clicker -> ifRunning(clicker, game, () -> CommandGame.start(clicker, game)));
        }
        if (game.isSpectatable()) {
            menu.button(new ItemStack(Material.ENDER_EYE), i18n(LangText.ITEM_GAME_WATCH), null, List.of(),
                    clicker -> ifRunning(clicker, game, () -> CommandGame.spectate(clicker, game)));
        }
        menu.button(new ItemStack(Material.BARRIER), i18n(LangText.ITEM_GAME_STOP), null, details(game),
                clicker -> ifRunning(clicker, game, () -> openStop(clicker, game)));
        menu.button(new ItemStack(Material.ARROW), i18n(LangText.ITEM_BACK), null, List.of(), GameMenu::openGames);
        menu.show(player);
    }

    /**
     * asks how to stop the game
     */
    public static void openStop(Player player, Game game) {
        Menu menu = new Menu(i18n(LangText.GUI_GAME_STOP, game.getUniqueId()));
        menu.button(new ItemStack(Material.ORANGE_WOOL), i18n(LangText.ITEM_STOP_NEXT_ROUND), null,
                List.of(i18n(LangText.ITEM_STOP_NEXT_ROUND_LORE)), clicker -> ifRunning(clicker, game, () -> {
                    CommandGame.stop(clicker, game, false);
                    openGames(clicker);
                }));
        menu.button(new ItemStack(Material.RED_WOOL), i18n(LangText.ITEM_STOP_REMOVE), null,
                List.of(i18n(LangText.ITEM_STOP_REMOVE_LORE)), clicker -> ifRunning(clicker, game, () -> {
                    CommandGame.stop(clicker, game, true);
                    openGames(clicker);
                }));
        menu.button(new ItemStack(Material.ARROW), i18n(LangText.ITEM_BACK), null, List.of(),
                clicker -> ifRunning(clicker, game, () -> openGame(clicker, game)));
        menu.show(player);
    }

    /**
     * runs the action if the game still exists (the menu was opened a while ago), otherwise tells the player
     */
    private static void ifRunning(Player player, Game game, Runnable action) {
        if (!CatchTheBeacon.getInstance().getGameManager().getGames().contains(game)) {
            player.sendMessage(I.prefixed(LangText.GAME_NOT_FOUND, game.getUniqueId()));
            return;
        }
        action.run();
    }

    private static Component name(Game game) {
        return i18n(LangText.GAME_MENU_NAME, game.getUniqueId(), game.getArena().getName());
    }

    /**
     * the short second line on Bedrock
     */
    private static Component summary(Game game) {
        return i18n(LangText.GAME_MENU_SUMMARY, game.getGameState().name(), game.getGamePlayers().size(),
                game.getArena().getMaxPlayers());
    }

    private static List<Component> details(Game game) {
        return List.of(
                i18n(LangText.GAME_MENU_STATE, game.getGameState().name()),
                i18n(LangText.GAME_MENU_PLAYERS, game.getGamePlayers().size(), game.getArena().getMaxPlayers()),
                i18n(LangText.GAME_MENU_SPECTATORS, game.getSpectators().size()),
                i18n(LangText.GAME_MENU_TIME, CommandGame.formatTime(game.getRoundSeconds())));
    }

    static Material icon(GameState state) {
        return switch (state) {
            case PENDING -> Material.LIGHT_GRAY_WOOL;
            case LOBBY -> Material.LIME_WOOL;
            case INGAME -> Material.BEACON;
            case ENDING -> Material.FIREWORK_ROCKET;
            case RESTART -> Material.CLOCK;
        };
    }

}
