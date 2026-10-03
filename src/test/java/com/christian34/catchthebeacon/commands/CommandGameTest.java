package com.christian34.catchthebeacon.commands;

import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.GameTestBase;
import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.lib.Menu;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * /ctb game ...: managing the running games
 *
 * @author Christian34
 */
class CommandGameTest extends GameTestBase {
    private PlayerMock admin;

    @BeforeEach
    void setUpAdmin() {
        admin = addAdmin("Admin2");
        messages(admin);
    }

    private Game onlyGame() {
        assertEquals(1, plugin.getGameManager().getGames().size());
        return plugin.getGameManager().getGames().iterator().next();
    }

    @Test
    void listShowsTheGames() {
        execute(admin, "ctb game list");
        List<String> messages = messages(admin);
        assertContains(messages, game.getUniqueId() + " castle | LOBBY | 2/");
        assertContains(messages, "[Start]");
    }

    @Test
    void playersCantManageGames() {
        execute(blue, "ctb game list");
        assertContains(messages(blue), "You do not have permission");
    }

    @Test
    void unknownGame() {
        execute(admin, "ctb game abc stop");
        assertContains(messages(admin), "Couldn't find the game 'abc'");
    }

    @Test
    void startsTheGame() {
        execute(admin, "ctb game " + game.getUniqueId() + " start");
        assertContains(messages(admin), "The game was started");
        server.getScheduler().performTicks(20 * 7);
        assertEquals(GameState.INGAME, game.getGameState());
    }

    @Test
    void stopsARunningGameAndCreatesTheNextRound() {
        startGame();
        execute(admin, "ctb game " + game.getUniqueId() + " stop");
        assertContains(messages(red), "The game was stopped by an admin.");
        assertNull(plugin.getUser(red).getGame());
        assertNull(plugin.getUser(blue).getGame());
        Game next = onlyGame();
        assertNotSame(game, next);
        assertEquals(GameState.PENDING, next.getGameState());
        assertNull(game.getGameWorld().getWorld(), "the game world is still loaded");
        assertContains(messages(admin), "the next round is " + next.getUniqueId());

        // nothing of the stopped round runs any more
        server.getScheduler().performTicks(20 * 30);
        assertTrue(messages(red).isEmpty());
    }

    @Test
    void stopWithRemoveCreatesNoNextRound() {
        execute(admin, "ctb game " + game.getUniqueId() + " stop --remove");
        assertTrue(plugin.getGameManager().getGames().isEmpty());
        assertNull(plugin.getUser(red).getGame());
        assertContains(messages(admin), "has no game until the next restart");
    }

    @Test
    void stoppedLobbyCountdownDoesntStartAnything() {
        execute(red, "ctb start");
        execute(admin, "ctb game " + game.getUniqueId() + " stop --remove");
        server.getScheduler().performTicks(20 * 10);
        assertNotEquals(GameState.INGAME, game.getGameState());
    }

    // --- menu (captured as Bedrock form, InventoryGui doesn't run in MockBukkit)

    private List<Menu> captureMenus() {
        List<Menu> menus = new ArrayList<>();
        plugin.setBedrockForms((player, menu) -> player == admin && menus.add(menu));
        return menus;
    }

    private static List<String> names(Menu menu) {
        return menu.getButtons().stream().map(button -> PlainTextComponentSerializer.plainText().serialize(button.name())).toList();
    }

    private Menu.Button button(Menu menu, String name) {
        return menu.getButtons().stream()
                .filter(button -> PlainTextComponentSerializer.plainText().serialize(button.name()).equals(name))
                .findFirst().orElseThrow(() -> new AssertionError(name + " not in " + names(menu)));
    }

    @Test
    void menuShowsTheGames() {
        List<Menu> menus = captureMenus();
        execute(admin, "ctb game");
        assertEquals(1, menus.size());
        assertEquals(List.of(game.getUniqueId() + " castle"), names(menus.getFirst()));
        Menu.Button entry = menus.getFirst().getButtons().getFirst();
        assertEquals("LOBBY - 2/" + game.getArena().getMaxPlayers(),
                PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(entry.detail())));

        entry.action().accept(admin);
        assertEquals(List.of("Start the game", "Stop the game", "Back"), names(menus.getLast()));
    }

    @Test
    void menuStartsTheGame() {
        List<Menu> menus = captureMenus();
        GameMenu.openGame(admin, game);
        button(menus.getLast(), "Start the game").action().accept(admin);
        server.getScheduler().performTicks(20 * 7);
        assertEquals(GameState.INGAME, game.getGameState());

        GameMenu.openGame(admin, game);
        assertEquals(List.of("Watch the game", "Stop the game", "Back"), names(menus.getLast()));
    }

    @Test
    void menuStopsTheGame() {
        List<Menu> menus = captureMenus();
        GameMenu.openGame(admin, game);
        button(menus.getLast(), "Stop the game").action().accept(admin);
        assertEquals(List.of("Stop, new round", "Stop and remove", "Back"), names(menus.getLast()));
        button(menus.getLast(), "Stop, new round").action().accept(admin);

        Game next = onlyGame();
        assertNotSame(game, next);
        assertNull(plugin.getUser(red).getGame());
        // back to the overview with the new round
        assertEquals(List.of(next.getUniqueId() + " castle"), names(menus.getLast()));
    }

    @Test
    void oldMenuOfAStoppedGameDoesNothing() {
        List<Menu> menus = captureMenus();
        GameMenu.openGame(admin, game);
        Menu old = menus.getLast();
        execute(admin, "ctb game " + game.getUniqueId() + " stop --remove");
        messages(admin);
        button(old, "Start the game").action().accept(admin);
        assertContains(messages(admin), "Couldn't find the game '" + game.getUniqueId() + "'");
        assertNotEquals(GameState.INGAME, game.getGameState());
    }

    @Test
    void spectatesARunningGame() {
        startGame();
        execute(admin, "ctb game " + game.getUniqueId() + " spectate");
        assertTrue(game.getSpectators().contains(plugin.getUser(admin)));
    }

}
