package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.integrations.CtbMiniPlaceholders;
import com.christian34.catchthebeacon.integrations.NpcSupport;
import com.christian34.catchthebeacon.lib.InteractionItems;
import com.christian34.catchthebeacon.lib.Menu;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.stats.StatsExpansion;
import io.github.miniplaceholders.api.Expansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Join NPCs (Citizens, FancyNpcs), menus as Bedrock forms (Floodgate) and the placeholders (PlaceholderAPI,
 * MiniPlaceholders) - with fake plugins
 *
 * @author Christian34
 */
class IntegrationsTest extends GameTestBase {

    private NpcSupport npcs() {
        return plugin.getNpcSupport();
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private void rightClick(PlayerMock player, ItemStack item) {
        server.getPluginManager().callEvent(new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, item, null, BlockFace.SELF));
    }

    // join NPCs

    @Test
    void npcCommandsNeedAnNpcPlugin() {
        PlayerMock admin = addAdmin("Admin2");
        execute(admin, "ctb npc link castle");
        assertContains(messages(admin), "Install Citizens or FancyNpcs");
    }

    @Test
    void linkedNpcJoinsTheGame() {
        npcs().addPlugin("Citizens");
        PlayerMock admin = addAdmin("Admin2");
        execute(admin, "ctb npc link castle");
        assertContains(messages(admin), "Right-click the NPC that should join castle");
        assertTrue(npcs().click(admin, "citizens:1234", "Guide"));
        assertContains(messages(admin), "Clicking the NPC Guide now joins castle");
        // the admin isn't put into the game by linking
        assertNull(plugin.getUser(admin).getGame());

        PlayerMock player = addPlayer("Player");
        assertTrue(npcs().click(player, "citizens:1234", "Guide"));
        assertEquals(game, plugin.getUser(player).getGame());
        // other NPCs are left to the NPC plugin
        assertFalse(npcs().click(addPlayer("Other"), "citizens:999", "Shop"));

        execute(admin, "ctb npc list");
        assertContains(messages(admin), "Guide (citizens) -> castle");
    }

    @Test
    void linksAreSaved() {
        npcs().addPlugin("FancyNpcs");
        PlayerMock admin = addAdmin("Admin2");
        execute(admin, "ctb npc link castle");
        npcs().click(admin, "fancynpcs:abc", "Guide");

        NpcSupport loaded = new NpcSupport(plugin);
        assertEquals(List.of(new NpcSupport.Link("fancynpcs:abc", "Guide", "castle")), loaded.getLinks());
    }

    @Test
    void unlinkNpc() {
        npcs().addPlugin("Citizens");
        PlayerMock admin = addAdmin("Admin2");
        execute(admin, "ctb npc link castle");
        npcs().click(admin, "citizens:1", "Guide");
        messages(admin);

        execute(admin, "ctb npc unlink");
        assertTrue(npcs().click(admin, "citizens:1", "Guide"));
        assertContains(messages(admin), "The NPC Guide doesn't join a game any more");
        assertFalse(npcs().click(addPlayer("Player"), "citizens:1", "Guide"));

        execute(admin, "ctb npc unlink");
        npcs().click(admin, "citizens:1", "Guide");
        assertContains(messages(admin), "isn't linked");
        execute(admin, "ctb npc list");
        assertContains(messages(admin), "No NPC is linked yet");
    }

    @Test
    void linkNeedsAnExistingArena() {
        npcs().addPlugin("Citizens");
        PlayerMock admin = addAdmin("Admin2");
        execute(admin, "ctb npc link nowhere");
        assertContains(messages(admin), "Couldn't find the arena 'nowhere'");
        // the next click isn't taken for linking
        assertFalse(npcs().click(admin, "citizens:1", "Guide"));
    }

    @Test
    void npcCommandsNeedAdmin() {
        npcs().addPlugin("Citizens");
        PlayerMock player = addPlayer("Player");
        execute(player, "ctb npc link castle");
        assertFalse(npcs().click(player, "citizens:1", "Guide"));
    }

    // Bedrock forms

    @Test
    void bedrockPlayersGetTheTeamMenuAsForm() {
        List<Menu> forms = new ArrayList<>();
        plugin.setBedrockForms((player, menu) -> player == red && forms.add(menu));
        rightClick(red, InteractionItems.getSelectTeamItem());

        assertEquals(1, forms.size());
        List<Menu.Button> buttons = forms.getFirst().getButtons();
        assertEquals(3, buttons.size());
        assertEquals(plain(Team.RED.getDisplayName()), plain(buttons.getFirst().name()));
        assertTrue(plain(Objects.requireNonNull(buttons.getFirst().detail())).startsWith("1/"));
        // random has no size
        assertNull(buttons.get(1).detail());

        buttons.get(2).action().accept(red);
        assertEquals(Team.BLUE, plugin.getUser(red).getTeam());
    }

    @Test
    void bedrockPlayersVoteWithAForm() {
        List<Menu> forms = new ArrayList<>();
        plugin.setBedrockForms((player, menu) -> forms.add(menu));
        rightClick(red, InteractionItems.getVoteItem());

        assertEquals(Variant.getVotable().size(), forms.getFirst().getButtons().size());
        forms.getFirst().getButtons().getFirst().action().accept(red);
        assertEquals(Variant.getVotable().getFirst(), game.getVote(plugin.getUser(red)));
    }

    // placeholders

    @Test
    void placeholderApiShowsTheGameOfAnArena() {
        StatsExpansion expansion = new StatsExpansion(plugin);
        assertEquals("2", expansion.onRequest(null, "players_castle"));
        assertEquals(String.valueOf(game.getArena().getMaxPlayers()), expansion.onRequest(null, "maxplayers_castle"));
        assertEquals(I.legacy(SignManager.stateText(plugin.getSignManager().getStatus("castle"))),
                expansion.onRequest(null, "state_castle"));
        assertEquals(I.legacy(Team.RED.getDisplayName()), expansion.onRequest(red, "team"));
        assertEquals(I.legacy(game.getArena().getDisplayName()), expansion.onRequest(red, "arena"));
        assertEquals("", expansion.onRequest(addPlayer("Outside"), "team"));
    }

    @Test
    void miniPlaceholders() {
        Expansion expansion = new CtbMiniPlaceholders(plugin).register();
        try {
            TagResolver resolvers = TagResolver.resolver(expansion.globalPlaceholders(), expansion.audiencePlaceholders());
            MiniMessage miniMessage = MiniMessage.miniMessage();
            assertEquals("2/" + game.getArena().getMaxPlayers(),
                    plain(miniMessage.deserialize("<ctb_players:castle>/<ctb_maxplayers:castle>", resolvers)));
            assertEquals(plain(Team.RED.getDisplayName()), plain(miniMessage.deserialize("<ctb_team>", red, resolvers)));
            assertEquals("0", plain(miniMessage.deserialize("<ctb_kills>", red, resolvers)));
            assertEquals("", plain(miniMessage.deserialize("<ctb_top:wins:1:name>", resolvers)));
        } finally {
            expansion.unregister();
        }
    }

}
