package com.christian34.catchthebeacon.files;

import com.christian34.catchthebeacon.PluginTestBase;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the bundled yml files, the messages and the {@link ConfigUpdater}.
 *
 * @author Christian34
 */
class ResourceFilesTest extends PluginTestBase {

    private YamlConfiguration bundled(String name) {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(plugin.getResource(name)), StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @EnumSource(LangText.class)
    void everyMessageExists(LangText text) {
        assertNotNull(bundled("messages.yml").getString(text.getKey()), "missing in messages.yml: " + text.getKey());
    }

    @Test
    void messagesReplacePlaceholdersAndColors() {
        assertEquals("§eSteve §7joined the game! (§e1§7/§e8§7)", I.legacy(LangText.PLAYER_JOINED_GAME, "Steve", 1, 8));
    }

    @Test
    void oldColorCodesStillWork() {
        assertEquals("§cRed §lbold§r §7gray", I.legacy(I.text("&cRed &lbold&r §7gray")));
        assertEquals("<reset><red>Red <bold>bold<reset> <reset><gray>gray",
                I.legacyToMiniMessage("&cRed &lbold&r §7gray"));
        assertEquals("<reset><#ff8800>x", I.legacyToMiniMessage("&#FF8800x"));
    }

    @Test
    void argumentsAreNotParsed() {
        // player names and chat messages may not format the text
        assertEquals("§e<red>&cSteve §7joined the game! (§e1§7/§e8§7)",
                I.legacy(LangText.PLAYER_JOINED_GAME, "<red>&cSteve", 1, 8));
    }

    @Test
    void componentArgumentsKeepTheirStyle() {
        assertEquals("§7You joined team §cRed§7!", I.legacy(LangText.JOINED_TEAM, Team.RED.getDisplayName()));
    }

    @Test
    void missingArgumentsStayVisible() {
        assertEquals("§eSteve §7joined the game! (§e{1}§7/§e{2}§7)", I.legacy(LangText.PLAYER_JOINED_GAME, "Steve"));
    }

    @Test
    void everyCommandHasADescription() {
        YamlConfiguration messages = bundled("messages.yml");
        for (var command : plugin.getCommandManager().getManager().commands()) {
            String key = command.commandDescription().description().textDescription();
            assertTrue(key.startsWith("command_"), "no key: " + key);
            assertNotNull(messages.getString(key), "missing in messages.yml: " + key);
        }
    }

    @Test
    void teamsAreLoadedFromTeamsYml() {
        assertEquals("Red", Team.RED.getName());
        assertEquals(NamedTextColor.RED, Team.RED.getColor());
        assertEquals(Material.RED_WOOL, Team.RED.getItem().getType());
        assertEquals(Material.BLUE_WOOL, Team.BLUE.getItem().getType());
        assertEquals(176, Team.RED.getLeatherColor().getRed());
        assertEquals(2, Team.getTeams().size(), "random is not a playable team");
    }

    @Test
    void teamByName() {
        assertEquals(Team.RED, Team.get("RED"));
        assertEquals(Team.BLUE, Team.get("blue"));
        assertNull(Team.get("random"));
    }

    @Test
    void updaterAddsMissingKeysAndKeepsValues() throws IOException {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfiguration messages = YamlConfiguration.loadConfiguration(file);
        messages.set("no_permissions", "custom text");
        messages.set("player_not_found", null);
        messages.save(file);

        ConfigUpdater.update(plugin, "messages.yml", file);

        YamlConfiguration updated = YamlConfiguration.loadConfiguration(file);
        assertEquals("custom text", updated.getString("no_permissions"));
        assertEquals(bundled("messages.yml").getString("player_not_found"), updated.getString("player_not_found"));
    }

    @Test
    void updaterKeepsComments() throws IOException {
        File file = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("lobby.countdown", null);
        config.save(file);

        ConfigUpdater.update(plugin, "config.yml", file);

        YamlConfiguration updated = YamlConfiguration.loadConfiguration(file);
        assertEquals(180, updated.getInt("lobby.countdown"));
        assertFalse(updated.getComments("game.spawn-protection").isEmpty(), "comments got lost");
    }

}
