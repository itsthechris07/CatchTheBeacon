package com.christian34.catchthebeacon.integrations;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Multiverse: the worlds of games (ctb_*) are left alone.
 * <ul>
 *     <li>Multiverse-Core imports worlds of other plugins (auto-import-3rd-party-worlds) and would then enforce its
 *     game mode etc. there and load them again after a restart - they are removed from it again.</li>
 *     <li>Multiverse-Inventories would swap the inventory when players enter or leave the worlds of games (the game
 *     saves and restores it itself, see UserStorage) - it doesn't handle them while they are in a game or setup.</li>
 * </ul>
 *
 * @author Christian34
 */
public final class MultiverseSupport {
    static final String CORE = "Multiverse-Core";
    static final String INVENTORIES = "Multiverse-Inventories";

    private MultiverseSupport() {
    }

    /**
     * before the leftover worlds of games are deleted (Multiverse may have loaded them again)
     */
    public static void hook(CatchTheBeacon plugin) {
        if (Bukkit.getPluginManager().isPluginEnabled(CORE)) {
            try {
                MultiverseCoreHook hook = new MultiverseCoreHook(plugin);
                Bukkit.getPluginManager().registerEvents(hook, plugin);
                hook.removeGameWorlds();
                Debug.info("Hooked into Multiverse-Core (the worlds of games aren't managed by it)");
            } catch (Exception | LinkageError ex) {
                Debug.warn("Couldn't hook into Multiverse-Core: " + ex);
            }
        }
        if (Bukkit.getPluginManager().isPluginEnabled(INVENTORIES)) {
            try {
                Bukkit.getPluginManager().registerEvents(new MultiverseInventoriesHook(), plugin);
                Debug.info("Hooked into Multiverse-Inventories (no inventory groups in the worlds of games)");
            } catch (Exception | LinkageError ex) {
                Debug.warn("Couldn't hook into Multiverse-Inventories, it may swap inventories in games: " + ex);
            }
        }
    }

    /**
     * @return true for the worlds of games and setups
     */
    static boolean isGameWorld(@Nullable String world) {
        return world != null && world.startsWith("ctb_");
    }

    /**
     * @param from the world the player leaves, null if he doesn't change the world
     * @param to   the world the player enters, null if he doesn't change the world
     * @return true if Multiverse-Inventories mustn't save or load the inventory of the player: he is in a game or setup
     * (his real things are saved by the game) or he enters or leaves the world of a game
     */
    public static boolean ignoresInventory(@NotNull Player player, @Nullable String from, @Nullable String to) {
        return isGameWorld(from) || isGameWorld(to) || isGameWorld(player.getWorld().getName())
                || CatchTheBeacon.getInstance().getUser(player).getUserStorage().isStored();
    }

}
