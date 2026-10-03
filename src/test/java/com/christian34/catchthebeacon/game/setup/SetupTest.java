package com.christian34.catchthebeacon.game.setup;

import com.christian34.catchthebeacon.PluginTestBase;
import com.christian34.catchthebeacon.game.Beacon;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.game.map.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the guided setup with the hotbar tools.
 *
 * @author Christian34
 */
class SetupTest extends PluginTestBase {
    private PlayerMock admin;

    @BeforeEach
    void createArena() {
        admin = addAdmin("Admin");
        addMapFolder("castle");
        addMapFolder("lobby");
        execute(admin, "ctb arena create castle");
        messages(admin);
    }

    private Arena arena() {
        return plugin.getMapHandler().getArena("castle");
    }

    /**
     * right-clicks with the tool and lets a tick pass (the same tool is only used once per tick)
     */
    private void use(SetupItem item, @Nullable Block clickedBlock) {
        click(item, clickedBlock);
        server.getScheduler().performOneTick();
    }

    private void click(SetupItem item, @Nullable Block clickedBlock) {
        ItemStack stack = admin.getInventory().getItem(item.getSlot());
        assertEquals(item, SetupItem.of(stack), item + " is not in its slot");
        admin.getInventory().setHeldItemSlot(item.getSlot());
        plugin.getServer().getPluginManager().callEvent(new PlayerInteractEvent(admin,
                clickedBlock == null ? Action.RIGHT_CLICK_AIR : Action.RIGHT_CLICK_BLOCK, stack, clickedBlock,
                BlockFace.UP, EquipmentSlot.HAND));
    }

    @Test
    void doubleClickSwitchesOnlyOnce() {
        execute(admin, "ctb arena castle lobby setworld lobby");
        execute(admin, "ctb arena castle setup");
        use(SetupItem.LOBBY_SPAWN, null);
        // right-click on a block: "use item on block" and "use item" in the same tick
        click(SetupItem.SWITCH_WORLD, admin.getLocation().getBlock());
        click(SetupItem.SWITCH_WORLD, null);
        server.getScheduler().performTicks(2);
        assertEquals("ctb_castle_temp", admin.getWorld().getName());
        assertNotNull(plugin.getSetupManager().getSession(admin), "the setup has been stopped");
        assertNotNull(server.getWorld("ctb_lobby_temp"), "the lobby world has been unloaded");
    }

    @Test
    void switchingKeepsTheSetupRunning() {
        execute(admin, "ctb arena castle lobby setworld lobby");
        execute(admin, "ctb arena castle setup");
        use(SetupItem.SWITCH_WORLD, null);
        use(SetupItem.SWITCH_WORLD, null);
        use(SetupItem.SWITCH_WORLD, null);
        server.getScheduler().performTicks(2);
        assertEquals("ctb_castle_temp", admin.getWorld().getName());
        assertNotNull(plugin.getSetupManager().getSession(admin));
    }

    @Test
    void teleportingAwayEndsTheSetup() {
        execute(admin, "ctb arena castle setup");
        admin.teleport(server.getWorld("world").getSpawnLocation());
        server.getScheduler().performTicks(2);
        assertNull(plugin.getSetupManager().getSession(admin));
        assertNull(server.getWorld("ctb_castle_temp"), "the setup world is still loaded");
    }

    @Test
    void startsInTheArenaWithoutLobby() {
        execute(admin, "ctb arena castle setup");
        assertNotNull(plugin.getSetupManager().getSession(admin));
        assertEquals("ctb_castle_temp", admin.getWorld().getName());
        assertEquals(SetupItem.RED_SPAWN, SetupItem.of(admin.getInventory().getItemInMainHand()), "next step not selected");
        assertEquals(SetupItem.SAVE, SetupItem.of(admin.getInventory().getItem(8)));
    }

    private static String plain(net.kyori.adventure.text.Component component) {
        return com.christian34.catchthebeacon.lib.lang.I.plain(component);
    }

    @Test
    void itemTextsComeFromMessages() {
        execute(admin, "ctb arena castle setup");
        var meta = admin.getInventory().getItem(SetupItem.BLUE_BEACON_RIGHT.getSlot()).getItemMeta();
        assertEquals("Set right beacon of team " + Team.BLUE.getName(), plain(meta.displayName()));
        assertEquals(java.util.List.of("Right-click the beacon"), meta.lore().stream().map(SetupTest::plain).toList());

        meta = admin.getInventory().getItem(SetupItem.RED_SPAWN.getSlot()).getItemMeta();
        assertEquals("Set spawn of team " + Team.RED.getName(), plain(meta.displayName()));
        assertEquals(java.util.List.of("Right-click to set the spawn of team " + Team.RED.getName(), "to your position"),
                meta.lore().stream().map(SetupTest::plain).toList());
        assertEquals(net.kyori.adventure.text.format.NamedTextColor.RED, meta.displayName().color());
        assertEquals(net.kyori.adventure.text.format.TextDecoration.State.FALSE,
                meta.displayName().decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC));

        meta = admin.getInventory().getItem(SetupItem.SAVE.getSlot()).getItemMeta();
        assertEquals("Save & exit", plain(meta.displayName()));
        assertEquals(2, meta.lore().size());
    }

    @Test
    void startsInTheLobbyIfItIsTheNextStep() {
        execute(admin, "ctb arena castle lobby setworld lobby");
        execute(admin, "ctb arena castle setup");
        assertEquals("ctb_lobby_temp", admin.getWorld().getName());
        assertEquals(SetupItem.LOBBY_SPAWN, SetupItem.of(admin.getInventory().getItemInMainHand()));
    }

    @Test
    void setsSpawnsAndBeaconsWithTheTools() {
        execute(admin, "ctb arena castle setup");
        admin.setLocation(new Location(admin.getWorld(), 5, 70, 5));
        use(SetupItem.RED_SPAWN, null);
        assertTrue(arena().hasSpawnLocation(Team.RED));
        assertEquals(SetupItem.BLUE_SPAWN, SetupItem.of(admin.getInventory().getItemInMainHand()), "didn't go on");

        Block beacon = admin.getWorld().getBlockAt(20, 64, 20);
        beacon.setType(Material.BEACON);
        use(SetupItem.RED_BEACON_LEFT, beacon);
        assertEquals(20, arena().getBeaconLocations(Team.RED).get(Beacon.Position.LEFT).getBlockX());
        assertEquals(Material.BEACON, beacon.getType(), "the beacon has been changed");
    }

    @Test
    void beaconToolNeedsABeacon() {
        execute(admin, "ctb arena castle setup");
        messages(admin);
        use(SetupItem.RED_BEACON_LEFT, admin.getWorld().getBlockAt(0, 0, 0));
        assertContains(messages(admin), "Right-click a beacon");
        assertFalse(arena().getBeaconLocations(Team.RED).containsKey(Beacon.Position.LEFT));
    }

    @Test
    void switchesToTheLobby() {
        execute(admin, "ctb arena castle lobby setworld lobby");
        execute(admin, "ctb arena castle setup");
        use(SetupItem.LOBBY_SPAWN, null);
        assertNotNull(arena().getLobbyMap().getSpawnLocation());
        assertEquals(SetupItem.SWITCH_WORLD, SetupItem.of(admin.getInventory().getItemInMainHand()),
                "the next step is in the arena");

        use(SetupItem.SWITCH_WORLD, null);
        assertEquals("ctb_castle_temp", admin.getWorld().getName());
        assertEquals(SetupItem.RED_SPAWN, SetupItem.of(admin.getInventory().getItemInMainHand()));
    }

    @Test
    void switchWithoutLobbyExplainsIt() {
        execute(admin, "ctb arena castle setup");
        messages(admin);
        use(SetupItem.SWITCH_WORLD, null);
        assertContains(messages(admin), "no lobby world yet");
        assertEquals("ctb_castle_temp", admin.getWorld().getName());
    }

    @Test
    void saveRestoresThePlayerAndSavesTheWorld() {
        admin.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 3));
        Location start = admin.getLocation();
        execute(admin, "ctb arena castle setup");
        admin.getWorld().getBlockAt(1, 100, 1).setType(Material.GOLD_BLOCK);

        use(SetupItem.SAVE, null);
        assertNull(plugin.getSetupManager().getSession(admin));
        assertEquals(start.getWorld(), admin.getWorld());
        assertEquals(Material.DIAMOND, admin.getInventory().getItem(0).getType(), "inventory not restored");
        for (ItemStack item : admin.getInventory().getContents()) {
            assertNull(SetupItem.of(item), "setup item left in the inventory");
        }
        assertTrue(new File(plugin.getDataFolder(), "maps/castle").isDirectory());
    }

    @Test
    void exitCommand() {
        execute(admin, "ctb arena castle setup");
        execute(admin, "ctb arena castle setup exit");
        assertNull(plugin.getSetupManager().getSession(admin));
    }

    @Test
    void quitEndsTheSetup() {
        execute(admin, "ctb arena castle setup");
        admin.disconnect();
        assertNull(plugin.getSetupManager().getSession(admin));
    }

    @Test
    void toolsCantBeDropped() {
        execute(admin, "ctb arena castle setup");
        ItemStack tool = admin.getInventory().getItem(0);
        PlayerDropItemEvent event = new PlayerDropItemEvent(admin, admin.getWorld().dropItem(admin.getLocation(), tool));
        plugin.getServer().getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
    }

    @Test
    void onlyOneSetupPerArena() {
        execute(admin, "ctb arena castle setup");
        PlayerMock other = addAdmin("Other");
        execute(other, "ctb arena castle setup");
        assertContains(messages(other), "Admin is already setting up this arena!");
        assertNull(plugin.getSetupManager().getSession(other));
    }

    @Test
    void completedSetupMakesTheArenaPlayable() {
        execute(admin, "ctb arena castle lobby setworld lobby");
        execute(admin, "ctb arena castle setup");
        use(SetupItem.LOBBY_SPAWN, null);
        use(SetupItem.SWITCH_WORLD, null);
        use(SetupItem.RED_SPAWN, null);
        use(SetupItem.BLUE_SPAWN, null);
        int x = 0;
        for (SetupItem item : new SetupItem[]{SetupItem.RED_BEACON_LEFT, SetupItem.RED_BEACON_RIGHT,
                SetupItem.BLUE_BEACON_LEFT, SetupItem.BLUE_BEACON_RIGHT}) {
            Block beacon = admin.getWorld().getBlockAt(x += 10, 64, 0);
            beacon.setType(Material.BEACON);
            use(item, beacon);
        }
        assertNull(SetupStep.next(arena()));
        assertTrue(arena().getMissingSetup().isEmpty(), "missing: " + arena().getMissingSetup());
        assertEquals(SetupItem.SAVE, SetupItem.of(admin.getInventory().getItemInMainHand()));
    }

}
