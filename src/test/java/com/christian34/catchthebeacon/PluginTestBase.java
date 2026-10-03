package com.christian34.catchthebeacon;

import com.christian34.catchthebeacon.commands.CommandManager;
import com.christian34.catchthebeacon.game.map.GameWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.MessageTarget;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts a mocked Paper server with CatchTheBeacon enabled for every test. Each test gets a fresh plugin data folder.
 *
 * @author Christian34
 */
public abstract class PluginTestBase {
    protected ServerMock server;
    protected CatchTheBeacon plugin;
    protected TestCommandManager commands;
    @TempDir
    protected File worldsFolder;

    @BeforeEach
    void setUpServer() {
        this.server = MockBukkit.mock();
        this.server.addSimpleWorld("world");
        GameWorld.setDimensionsFolder(new File(worldsFolder, "dimensions/minecraft"));
        this.plugin = MockBukkit.load(CatchTheBeacon.class);
        this.commands = new TestCommandManager();
        this.plugin.setCommandManager(new CommandManager(commands));
    }

    @AfterEach
    void tearDownServer() {
        MockBukkit.unmock();
    }

    protected PlayerMock addPlayer(String name, String... permissions) {
        PlayerMock player = new ProxiedPlayerMock(server, name);
        for (String permission : permissions) {
            player.addAttachment(plugin, permission, true);
        }
        server.addPlayer(player);
        return player;
    }

    /**
     * @return the servers of the network the player was sent to (players of {@link #addPlayer})
     */
    protected static List<String> connects(PlayerMock player) {
        return ((ProxiedPlayerMock) player).getConnects();
    }

    protected PlayerMock addAdmin(String name) {
        return addPlayer(name, "ctb.admin");
    }

    /**
     * creates an (empty) world folder in plugins/CatchTheBeacon/maps, as an admin would save it there
     */
    protected File addMapFolder(String name) {
        File folder = new File(plugin.getDataFolder(), "maps/" + name);
        assertTrue(folder.mkdirs() || folder.isDirectory(), "couldn't create " + folder);
        return folder;
    }

    /**
     * runs the command like a player would type it (without the leading slash) and the tasks it schedules
     *
     * @return the exception the command failed with (the error message has been sent to the sender already)
     */
    @Nullable
    protected Throwable execute(@NotNull CommandSender sender, @NotNull String input) {
        Throwable failure = null;
        try {
            commands.commandExecutor().executeCommand(sender, input).join();
        } catch (CompletionException ex) {
            failure = ex.getCause();
        }
        server.getScheduler().performOneTick();
        return failure;
    }

    /**
     * all messages the sender received since the last call, without colors
     */
    protected List<String> messages(@NotNull MessageTarget target) {
        List<String> messages = new ArrayList<>();
        Component message;
        while ((message = target.nextComponentMessage()) != null) {
            // legacy messages keep their color codes in the text
            messages.add(ChatColor.stripColor(PlainTextComponentSerializer.plainText().serialize(message)));
        }
        return messages;
    }

    protected static void assertContains(List<String> messages, String expected) {
        assertTrue(messages.stream().anyMatch(message -> message.contains(expected)),
                () -> "expected a message containing '" + expected + "' but got " + messages);
    }

    protected static void assertNotContains(List<String> messages, String unexpected) {
        assertTrue(messages.stream().noneMatch(message -> message.contains(unexpected)),
                () -> "expected no message containing '" + unexpected + "' but got " + messages);
    }

}
