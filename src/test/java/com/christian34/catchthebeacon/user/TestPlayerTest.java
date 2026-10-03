package com.christian34.catchthebeacon.user;

import com.christian34.catchthebeacon.PluginTestBase;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the fake player used for debugging.
 *
 * @author Christian34
 */
class TestPlayerTest extends PluginTestBase {

    @Test
    void needsARealPlayerOnline() {
        assertThrows(IllegalStateException.class, () -> TestPlayer.create("Bot"));
    }

    @Test
    void emulatesBasicPlayerMethods() {
        PlayerMock admin = addPlayer("Christian34");
        Player bot = TestPlayer.create("Bot");

        assertEquals("Bot", bot.getName());
        assertNotNull(bot.getUniqueId());
        assertEquals(admin.getLocation(), bot.getLocation());
        assertSame(admin.getInventory(), bot.getInventory());
        assertTrue(bot.isOnline());

        Location target = admin.getLocation().add(5, 0, 5);
        assertTrue(bot.teleport(target));
        assertEquals(target, bot.getLocation());

        bot.setGameMode(GameMode.ADVENTURE);
        assertEquals(GameMode.ADVENTURE, bot.getGameMode());
    }

    @Test
    void otherMethodsReturnDefaults() {
        addPlayer("Christian34");
        Player bot = TestPlayer.create("Bot");
        assertFalse(bot.isOp());
        assertEquals(0, bot.getLevel());
        assertNull(bot.getAddress());
    }

    @Test
    void equalsOnlyItself() {
        addPlayer("Christian34");
        Player bot = TestPlayer.create("Bot");
        assertEquals(bot, bot);
        assertNotEquals(bot, TestPlayer.create("Bot"));
        assertEquals(bot.getUniqueId().hashCode(), bot.hashCode());
    }

}
