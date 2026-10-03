package com.christian34.catchthebeacon.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Join signs: "[ctb]" + arena name creates one, it shows the state and joins players.
 *
 * @author Christian34
 */
class JoinSignTest extends GameTestBase {

    private Block signBlock() {
        Block block = new Location(server.getWorld("world"), 5, 70, 5).getBlock();
        block.setType(Material.OAK_SIGN);
        // MockBukkit doesn't load chunks by itself (signs are only updated in loaded chunks)
        block.getChunk().load();
        return block;
    }

    private SignChangeEvent write(PlayerMock player, Block block, String... lines) {
        List<Component> components = java.util.Arrays.stream(lines).map(line -> (Component) Component.text(line)).toList();
        SignChangeEvent event = new SignChangeEvent(block, player, new java.util.ArrayList<>(components), Side.FRONT);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private static String plain(Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    private Block createSign() {
        PlayerMock admin = addAdmin("SignAdmin");
        Block block = signBlock();
        SignChangeEvent event = write(admin, block, "[CTB]", "castle", "", "");
        assertEquals("[CatchTheBeacon]", plain(event.line(0)));
        assertContains(messages(admin), "The join sign for arena castle has been created.");
        return block;
    }

    @Test
    void adminsCreateSigns() {
        Block block = createSign();
        assertNotNull(plugin.getSignManager().getSign(block));
        server.getScheduler().performTicks(20);
        Sign sign = (Sign) block.getState();
        assertEquals("castle", plain(sign.getSide(Side.FRONT).line(1)));
        assertEquals("Lobby", plain(sign.getSide(Side.FRONT).line(2)));
        assertEquals("2/8", plain(sign.getSide(Side.FRONT).line(3)));
    }

    @Test
    void signShowsRunningGames() {
        Block block = createSign();
        startGame();
        server.getScheduler().performTicks(20);
        assertEquals("Running", plain(((Sign) block.getState()).getSide(Side.FRONT).line(2)));
    }

    @Test
    void playersCantCreateSigns() {
        SignChangeEvent event = write(addPlayer("Steve"), signBlock(), "[ctb]", "castle", "", "");
        assertEquals("[ctb]", plain(event.line(0)));
        assertTrue(plugin.getSignManager().getSigns().isEmpty());
    }

    @Test
    void unknownArena() {
        PlayerMock admin = addAdmin("SignAdmin");
        write(admin, signBlock(), "[ctb]", "nothing", "", "");
        assertTrue(plugin.getSignManager().getSigns().isEmpty());
        assertContains(messages(admin), "Couldn't find the arena 'nothing'!");
    }

    private void click(PlayerMock player, Block block) {
        server.getPluginManager().callEvent(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP));
    }

    @Test
    void clickingJoins() {
        Block block = createSign();
        PlayerMock steve = addPlayer("Steve");
        click(steve, block);
        assertEquals(game, plugin.getUser(steve).getGame());
    }

    @Test
    void clickingDuringTheGameWatches() {
        Block block = createSign();
        startGame();
        PlayerMock steve = addPlayer("Steve");
        click(steve, block);
        assertTrue(plugin.getUser(steve).isSpectator());
    }

    @Test
    void signsAreOnlyRemovedWhileSneaking() {
        Block block = createSign();
        PlayerMock admin = addAdmin("Remover");
        BlockBreakEvent event = admin.simulateBlockBreak(block);
        assertTrue(event == null || event.isCancelled(), "removed by accident");
        assertNotNull(plugin.getSignManager().getSign(block));

        admin.setSneaking(true);
        admin.simulateBlockBreak(block);
        assertNull(plugin.getSignManager().getSign(block));
        assertContains(messages(admin), "The join sign has been removed.");
    }

    @Test
    void arenasWithSignsGetAGameOnStart() {
        createSign();
        server.getPluginManager().disablePlugin(plugin);
        server.getPluginManager().enablePlugin(plugin);
        assertEquals(1, plugin.getSignManager().getSigns().size());
        assertEquals(1, plugin.getGameManager().getGames().size());
        assertEquals("castle", plugin.getGameManager().getGames().iterator().next().getArena().getName());
    }

}
