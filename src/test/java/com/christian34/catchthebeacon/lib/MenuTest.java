package com.christian34.catchthebeacon.lib;

import com.christian34.catchthebeacon.PluginTestBase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The chest menu of Java players: layout, buttons, clicks
 *
 * @author Christian34
 */
class MenuTest extends PluginTestBase {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void fewButtonsAreCenteredWithGaps() {
        assertArrayEquals(new String[]{"    g    "}, Menu.layout(1));
        assertArrayEquals(new String[]{"  g g g  "}, Menu.layout(3));
        assertArrayEquals(new String[]{"g g g g g"}, Menu.layout(5));
    }

    @Test
    void manyButtonsFillRows() {
        assertArrayEquals(new String[]{"ggggggggg"}, Menu.layout(6));
        assertArrayEquals(new String[]{"ggggggggg", "ggggggggg"}, Menu.layout(10));
        assertEquals(6, Menu.layout(100).length);
    }

    @Test
    void cornerButtonGetsItsOwnRowAtTheRight() {
        assertArrayEquals(new String[]{"    g    ", "        c"}, Menu.layout(1, true));
        assertArrayEquals(new String[]{"        c"}, Menu.layout(0, true));
        assertEquals(6, Menu.layout(100, true).length);
    }

    @Test
    void cornerButtonIsAtTheBottomRight() {
        PlayerMock player = addPlayer("Player");
        List<Player> clicked = new ArrayList<>();
        menu(new ArrayList<>()).corner(new ItemStack(Material.NETHER_STAR), Component.text("New"), null, List.of(),
                clicked::add).show(player);
        Inventory top = player.getOpenInventory().getTopInventory();
        assertEquals(18, top.getSize());
        assertEquals(Material.NETHER_STAR, Objects.requireNonNull(top.getItem(17)).getType());
        click(player, 17);
        server.getScheduler().performOneTick();
        assertEquals(List.of(player), clicked);
    }

    @Test
    void emptyMenuHasOneRow() {
        assertEquals(1, Menu.layout(0).length);
        assertEquals(9, Menu.layout(0)[0].length());
    }

    private Menu menu(List<Player> clicked) {
        return new Menu(Component.text("Choose"))
                .button(new ItemStack(Material.RED_WOOL), Component.text("Red"), null,
                        List.of(Component.text("1/4")), clicked::add)
                .button(new ItemStack(Material.BLUE_WOOL), Component.text("Blue"), null, List.of(), player -> {
                });
    }

    private InventoryClickEvent click(PlayerMock player, int slot) {
        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                slot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    void chestMenuShowsTheButtons() {
        PlayerMock player = addPlayer("Player");
        menu(new ArrayList<>()).show(player);
        Inventory top = player.getOpenInventory().getTopInventory();
        assertEquals(9, top.getSize());
        assertEquals("Choose", plain(player.getOpenInventory().title()));
        // "   g g   "
        ItemStack red = Objects.requireNonNull(top.getItem(3));
        assertEquals(Material.RED_WOOL, red.getType());
        assertEquals("Red", plain(Objects.requireNonNull(red.getItemMeta().displayName())));
        assertEquals(List.of("1/4"), Objects.requireNonNull(red.getItemMeta().lore()).stream().map(MenuTest::plain).toList());
        assertEquals(Material.BLUE_WOOL, Objects.requireNonNull(top.getItem(5)).getType());
        assertNull(top.getItem(4));
    }

    @Test
    void clickingAButtonRunsItsAction() {
        PlayerMock player = addPlayer("Player");
        List<Player> clicked = new ArrayList<>();
        menu(clicked).show(player);
        InventoryClickEvent event = click(player, 3);
        assertTrue(event.isCancelled(), "the item can't be taken");
        server.getScheduler().performOneTick();
        assertEquals(List.of(player), clicked);
        Inventory top = player.getOpenInventory().getTopInventory();
        assertTrue(top == null || top.getType() != InventoryType.CHEST, "the menu is closed");
    }

    @Test
    void clicksBesideTheButtonsDoNothing() {
        PlayerMock player = addPlayer("Player");
        List<Player> clicked = new ArrayList<>();
        menu(clicked).show(player);
        assertTrue(click(player, 4).isCancelled());
        server.getScheduler().performOneTick();
        assertTrue(clicked.isEmpty());
        assertEquals(InventoryType.CHEST, player.getOpenInventory().getTopInventory().getType());
    }

}
