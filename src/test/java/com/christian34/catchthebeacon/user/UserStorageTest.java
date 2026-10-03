package com.christian34.catchthebeacon.user;

import com.christian34.catchthebeacon.game.GameTestBase;
import com.christian34.catchthebeacon.game.Team;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The game has no effect on the real game: the state of the player is restored when he leaves - and after a crash
 * of the server from the backup file.
 *
 * @author Christian34
 */
class UserStorageTest extends GameTestBase {
    private PlayerMock steve;
    private Location home;

    /**
     * Steve with items, experience, effects, ... outside of the game
     */
    private PlayerMock steve() {
        steve = addPlayer("Steve");
        home = new Location(steve.getWorld(), 100.5, 70, -20.5, 90, 10);
        steve.teleport(home);
        steve.getInventory().setItem(3, new ItemStack(Material.DIAMOND, 12));
        steve.getInventory().setItem(39, new ItemStack(Material.NETHERITE_HELMET));
        steve.getInventory().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
        steve.getEnderChest().setItem(0, new ItemStack(Material.ELYTRA));
        steve.setLevel(30);
        steve.setExp(0.5f);
        steve.setFoodLevel(7);
        steve.setHealth(9);
        steve.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 600, 0));
        steve.setGameMode(GameMode.CREATIVE);
        steve.setRespawnLocation(home, true);
        return steve;
    }

    private File backupFile(PlayerMock player) {
        return new File(plugin.getDataFolder(), "backups/" + player.getUniqueId() + ".yml");
    }

    private void assertRestored() {
        assertEquals(new ItemStack(Material.DIAMOND, 12), steve.getInventory().getItem(3));
        assertEquals(Material.NETHERITE_HELMET, steve.getInventory().getHelmet().getType());
        assertEquals(Material.TOTEM_OF_UNDYING, steve.getInventory().getItemInOffHand().getType());
        assertEquals(Material.ELYTRA, steve.getEnderChest().getItem(0).getType());
        assertFalse(steve.getInventory().contains(Material.ARROW), "items of the game are still there");
        assertEquals(30, steve.getLevel());
        assertEquals(0.5f, steve.getExp(), 0.001);
        assertEquals(7, steve.getFoodLevel());
        assertEquals(9, steve.getHealth(), 0.001);
        assertEquals(20, steve.getAttribute(Attribute.MAX_HEALTH).getBaseValue(), "the extra hearts stay");
        assertNotNull(steve.getPotionEffect(PotionEffectType.NIGHT_VISION));
        assertNull(steve.getPotionEffect(PotionEffectType.REGENERATION));
        assertEquals(GameMode.CREATIVE, steve.getGameMode());
        assertEquals(home, steve.getLocation());
        assertEquals(home, steve.getRespawnLocation());
    }

    /**
     * Steve plays: he gets the kit, loses health, gets effects, dies
     */
    private void play() {
        joinWithTeam(steve, Team.RED);
        startGame();
        assertFalse(steve.getInventory().contains(Material.DIAMOND));
        steve.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 0));
        steve.setLevel(2);
        killAndRespawn(steve);
    }

    @Test
    void everythingIsRestoredAfterTheGame() {
        steve();
        play();
        execute(steve, "ctb quit");
        assertRestored();
    }

    @Test
    void backupFileOnlyWhileInTheGame() {
        steve();
        assertFalse(backupFile(steve).exists());
        joinWithTeam(steve, Team.RED);
        assertTrue(backupFile(steve).isFile(), "no backup file");
        execute(steve, "ctb quit");
        assertFalse(backupFile(steve).exists(), "the backup file is still there");
    }

    @Test
    void everythingIsRestoredAfterACrash() throws IOException {
        steve();
        play();
        // the server crashes: the plugin can't restore anything, the backup file stays
        byte[] backup = Files.readAllBytes(backupFile(steve).toPath());
        execute(steve, "ctb quit");
        steve.getInventory().clear();
        steve.getInventory().addItem(new ItemStack(Material.ARROW, 16));
        steve.setLevel(0);
        steve.teleport(new Location(steve.getWorld(), 0, 70, 0));
        steve.setGameMode(GameMode.ADVENTURE);
        Files.write(backupFile(steve).toPath(), backup);

        steve.disconnect();
        steve.reconnect();
        server.getScheduler().performTicks(1);
        assertRestored();
        assertFalse(backupFile(steve).exists());
        assertContains(messages(steve), "you got back your inventory");
    }

    @Test
    void oldBackupsAreNeverOverwritten() throws IOException {
        steve();
        File file = backupFile(steve);
        // noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        Files.writeString(file.toPath(), "name: Steve\ninventory: broken\n");
        joinWithTeam(steve, Team.RED);
        File[] backups = file.getParentFile().listFiles((dir, name) -> name.startsWith(steve.getUniqueId().toString()));
        assertNotNull(backups);
        assertEquals(2, backups.length, "the old backup has been overwritten");
    }

    @Test
    void secondStoreKeepsTheRealState() {
        steve();
        joinWithTeam(steve, Team.RED);
        // e.g. teleported into the setup while in the game
        plugin.getUser(steve).getUserStorage().store();
        execute(steve, "ctb quit");
        assertEquals(new ItemStack(Material.DIAMOND, 12), steve.getInventory().getItem(3));
    }

    @Test
    void noBackupFilesOnAGameServer() {
        plugin.getFileManager().getConfigFile().set("network.mode", "game");
        steve();
        joinWithTeam(steve, Team.RED);
        assertFalse(backupFile(steve).exists());
        execute(steve, "ctb quit");
        assertEquals(new ItemStack(Material.DIAMOND, 12), steve.getInventory().getItem(3), "not restored");
    }

    @Test
    void noStatisticsInTheGame() {
        startGame();
        var event = new PlayerStatisticIncrementEvent(red, Statistic.PLAYER_KILLS, 0, 1);
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
        execute(red, "ctb quit");
        event = new PlayerStatisticIncrementEvent(red, Statistic.PLAYER_KILLS, 0, 1);
        server.getPluginManager().callEvent(event);
        assertFalse(event.isCancelled(), "statistics outside of games are blocked");
    }

    private static boolean opensEnderChest(PlayerMock player) {
        player.openInventory(player.getEnderChest());
        var top = player.getOpenInventory().getTopInventory();
        return top != null && top.getType() == InventoryType.ENDER_CHEST;
    }

    @Test
    void enderChestCantBeOpenedInTheGame() {
        assertTrue(opensEnderChest(addPlayer("Outside")), "the ender chest can't be opened at all");
        startGame();
        assertFalse(opensEnderChest(red), "the ender chest of the real game is open in the game");
    }

}
