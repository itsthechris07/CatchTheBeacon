package com.christian34.catchthebeacon.lib.lang;

import com.christian34.catchthebeacon.PluginTestBase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentIteratorType;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the translations (language in config.yml) and the clickable commands in the messages.
 *
 * @author Christian34
 */
class LanguageTest extends PluginTestBase {
    private static final Pattern ARGUMENT = Pattern.compile("\\{\\d+}");
    private static final Pattern COMMAND_TAG = Pattern.compile("<(run|suggest):[^>]*>");
    private static final Pattern CLICKABLE = Pattern.compile("<(run|suggest):[^>]*>.*?</\\1>");
    // texts that can't be clicked: a hover and the action bar
    private static final Set<String> NOT_CLICKABLE = Set.of("first_steps_import_hover", "setup_actionbar_lobby");

    @AfterEach
    void resetLanguage() {
        setLanguage("en");
    }

    private void setLanguage(String language) {
        plugin.getFileManager().getConfigFile().set("language", language);
        I.load();
    }

    private YamlConfiguration bundled(String name) {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(plugin.getResource(name), name), StandardCharsets.UTF_8));
    }

    private static Set<String> matches(Pattern pattern, String text) {
        Set<String> found = new TreeSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) found.add(matcher.group());
        return found;
    }

    @Test
    void germanHasTheSameMessages() {
        YamlConfiguration english = bundled("messages.yml");
        YamlConfiguration german = bundled("messages_de.yml");
        assertEquals(english.getKeys(false), german.getKeys(false));
        for (String key : english.getKeys(false)) {
            String en = english.getString(key, "");
            String de = german.getString(key, "");
            assertEquals(matches(ARGUMENT, en), matches(ARGUMENT, de), "placeholders of " + key);
            assertEquals(matches(COMMAND_TAG, en), matches(COMMAND_TAG, de), "clickable commands of " + key);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"messages.yml", "messages_de.yml"})
    void commandsInChatAreClickable(String file) {
        YamlConfiguration messages = bundled(file);
        for (String key : messages.getKeys(false)) {
            if (NOT_CLICKABLE.contains(key)) continue;
            String rest = CLICKABLE.matcher(messages.getString(key, "")).replaceAll("");
            assertFalse(rest.contains("/ctb"), file + ": the command in " + key + " isn't clickable");
        }
    }

    @Nullable
    private static ClickEvent click(Component component) {
        for (Component child : component.iterable(ComponentIteratorType.DEPTH_FIRST)) {
            if (child.clickEvent() != null) return child.clickEvent();
        }
        return null;
    }

    @Test
    void runCommand() {
        Component text = I.i18n(LangText.SPECTATE_STARTED);
        ClickEvent click = click(text);
        assertNotNull(click);
        assertEquals(ClickEvent.runCommand("/ctb quit"), click);
        assertEquals("You are watching the game. Leave it with /ctb quit.", I.plain(text));
    }

    @Test
    void argumentsAreInsertedIntoTheCommand() {
        Component text = I.i18n(LangText.WORLD_READY, "castle");
        assertEquals(ClickEvent.runCommand("/ctb arena castle save"), click(text));
        assertTrue(I.plain(text).endsWith("/ctb arena castle save"));
        // no formatting or tags of names in the command
        assertEquals(ClickEvent.suggestCommand("/ctb arena xy removegame"),
                click(I.i18n(LangText.AUTO_GAME_ENABLED, "x'y>")));
    }

    @Test
    void suggestedCommandsAreTypedIntoTheChat() {
        assertEquals(ClickEvent.suggestCommand("/ctb arena create "), click(I.i18n(LangText.ARENA_LIST_EMPTY)));
    }

    @Test
    void invalidSyntaxSuggestsTheCommand() {
        var admin = addAdmin("Admin");
        execute(admin, "ctb arena create");
        assertTrue(messages(admin).stream().anyMatch(message -> message.contains("/ctb arena create")),
                messages(admin).toString());
    }

    @Test
    void germanMessages() {
        setLanguage("de");
        assertEquals("Das Spiel ist vorbei - niemand hat gewonnen.", I.plain(LangText.GAME_DRAW));
        assertTrue(new File(plugin.getDataFolder(), "messages_de.yml").exists());
    }

    @Test
    void ownTranslationFallsBackToEnglish() throws IOException {
        YamlConfiguration french = new YamlConfiguration();
        french.set("game_draw", "La partie est terminée.");
        french.save(new File(plugin.getDataFolder(), "messages_fr.yml"));

        setLanguage("fr");

        assertEquals("La partie est terminée.", I.plain(LangText.GAME_DRAW));
        assertEquals("Waiting for players...", I.plain(LangText.WAITING_FOR_PLAYERS));
    }

    @Test
    void unknownLanguageIsEnglish() {
        setLanguage("xx");
        assertEquals("The game is over - nobody has won.", I.plain(LangText.GAME_DRAW));
    }

    @Test
    void fileNames() {
        assertEquals("messages.yml", TextContainer.fileName(null));
        assertEquals("messages.yml", TextContainer.fileName(" EN "));
        assertEquals("messages_de.yml", TextContainer.fileName("DE"));
        assertEquals("messages_pt_br.yml", TextContainer.fileName("pt_BR"));
        assertEquals("messages.yml", TextContainer.fileName("../config"), "no paths");
    }

}
