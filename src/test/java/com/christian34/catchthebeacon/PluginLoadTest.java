package com.christian34.catchthebeacon;

import com.christian34.catchthebeacon.lib.lang.I;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the plugin starts with all files and commands.
 *
 * @author Christian34
 */
class PluginLoadTest extends PluginTestBase {

    @Test
    void pluginIsEnabled() {
        assertTrue(plugin.isEnabled());
        assertSame(plugin, CatchTheBeacon.getInstance());
    }

    @ParameterizedTest
    @ValueSource(strings = {"config.yml", "messages.yml", "teams.yml", "arenas.yml", "lobby.yml", "items.yml"})
    void createsFilesInDataFolder(String file) {
        assertTrue(new File(plugin.getDataFolder(), file).isFile(), file + " has not been created");
    }

    @ParameterizedTest
    @ValueSource(strings = {"maps", "logs"})
    void createsDirectoriesInDataFolder(String directory) {
        assertTrue(new File(plugin.getDataFolder(), directory).isDirectory(), directory + " has not been created");
    }

    @Test
    void doesNotWriteIntoWorkingDirectory() {
        // the plugin folder used to be the relative path plugins/CatchTheBeacon
        assertFalse(new File("plugins/CatchTheBeacon").exists());
    }

    @Test
    void startsWithoutArenasAndGames() {
        assertTrue(plugin.getMapHandler().getGameMaps().isEmpty());
        assertTrue(plugin.getGameManager().getGames().isEmpty());
    }

    @Test
    void keepsConfigChangesOnRestart() {
        plugin.getFileManager().getConfigFile().set("game.spawn-protection", 42);
        server.getPluginManager().disablePlugin(plugin);
        server.getPluginManager().enablePlugin(plugin);
        assertEquals(42, plugin.getFileManager().getConfigFile().getInt("game.spawn-protection"));
    }

    @Test
    void prefixFromConfig() {
        plugin.getFileManager().getConfigFile().set("prefix", "&6[CTB]");
        server.getPluginManager().disablePlugin(plugin);
        server.getPluginManager().enablePlugin(plugin);
        assertEquals("§6[CTB] ", I.legacy(CatchTheBeacon.PREFIX), "old color codes still work");
        plugin.getFileManager().getConfigFile().set("prefix", "<gold>[CTB]");
        server.getPluginManager().disablePlugin(plugin);
        server.getPluginManager().enablePlugin(plugin);
        assertEquals("§6[CTB] ", I.legacy(CatchTheBeacon.PREFIX));
    }

    @Test
    void emptyPrefix() {
        plugin.getFileManager().getConfigFile().set("prefix", "");
        server.getPluginManager().disablePlugin(plugin);
        server.getPluginManager().enablePlugin(plugin);
        assertEquals(Component.empty(), CatchTheBeacon.PREFIX);

    }

    @Test
    void commandsAreRegistered() {
        assertNotNull(plugin.getCommandManager());
        assertFalse(commands.commands().isEmpty());
    }

    @Test
    void playerIsNotInGameAfterJoin() {
        assertFalse(plugin.isInGame(addPlayer("Steve")));
    }

}
