package com.christian34.catchthebeacon.commands;

import com.christian34.catchthebeacon.PluginTestBase;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests the general commands of /ctb.
 *
 * @author Christian34
 */
class CommandCatchTheBeaconTest extends PluginTestBase {

    @Test
    void rootCommandShowsVersion() {
        PlayerMock player = addPlayer("Steve");
        assertNull(execute(player, "ctb"));
        assertContains(messages(player), "version " + plugin.getPluginMeta().getVersion());
    }

    @Test
    void aliasesWork() {
        PlayerMock player = addPlayer("Steve");
        execute(player, "catchthebeacon");
        execute(player, "ctbeacon");
        execute(player, "catchbeacon");
        assertContains(messages(player), "This server uses CatchTheBeacon");
    }

    @Test
    void helpListsCommands() {
        PlayerMock player = addPlayer("Steve");
        assertNull(execute(player, "ctb help"));
        assertContains(messages(player), "ctb quit");
    }

    @Test
    void helpShowsTheDescriptionOfMessagesYml() {
        PlayerMock player = addPlayer("Steve");
        assertNull(execute(player, "ctb help ctb join"));
        List<String> messages = messages(player);
        assertContains(messages, "joins a running game");
        assertNotContains(messages, "command_join");
    }

    @Test
    void quitWithoutGame() {
        PlayerMock player = addPlayer("Steve");
        execute(player, "ctb quit");
        assertContains(messages(player), "You're not playing a game!");
    }

    @Test
    void joinWithoutGame() {
        PlayerMock player = addPlayer("Steve");
        execute(player, "ctb join");
        assertContains(messages(player), "There is no game you could join right now!");
    }

    @Test
    void startNeedsPermission() {
        PlayerMock player = addPlayer("Steve");
        execute(player, "ctb start");
        assertContains(messages(player), "You do not have permission");
    }

    @Test
    void startOnlyInGame() {
        PlayerMock player = addPlayer("Steve", "ctb.vip");
        execute(player, "ctb start");
        assertContains(messages(player), "This command only works in the game.");
    }

    @Test
    void playerCommandsDontWorkInConsole() {
        ConsoleCommandSenderMock console = server.getConsoleSender();
        execute(console, "ctb quit");
        assertContains(messages(console), "Only players can execute this command!");
    }

    @Test
    void debugWorksInConsole() {
        ConsoleCommandSenderMock console = server.getConsoleSender();
        assertNull(execute(console, "ctb debug"));
        assertContains(messages(console), "Arenas: 0");
    }

    @Test
    void debugNeedsAdmin() {
        PlayerMock player = addPlayer("Steve");
        execute(player, "ctb debug");
        assertContains(messages(player), "You do not have permission");
    }

    @Test
    void debugInfoOfPlayerWithoutGame() {
        PlayerMock admin = addAdmin("Admin");
        execute(admin, "ctb debug info");
        assertContains(messages(admin), "Admin is not playing a game!");
    }

    @Test
    void invalidSyntaxShowsCorrectSyntax() {
        PlayerMock admin = addAdmin("Admin");
        execute(admin, "ctb debug setstate");
        assertContains(messages(admin), "Invalid syntax");
    }

}
