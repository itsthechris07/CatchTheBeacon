package com.christian34.catchthebeacon.integrations;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * TAB (NEZNAMY): while a player is in a game, TAB doesn't overwrite the header and footer of the tab list, the name
 * tags (team colors) and the sidebar of the game. Everything of TAB comes back when he leaves the game.
 *
 * @author Christian34
 */
public interface TabSupport {
    TabSupport NONE = new TabSupport() {
        @Override
        public void takeOver(@NotNull Player player, @NotNull Component header, @NotNull Component footer) {
        }

        @Override
        public void release(@NotNull Player player) {
        }
    };

    /**
     * the player is in a game (called again on every update, only the first call counts)
     */
    void takeOver(@NotNull Player player, @NotNull Component header, @NotNull Component footer);

    /**
     * the player has left the game: TAB shows its things again
     */
    void release(@NotNull Player player);

    static TabSupport create(CatchTheBeacon plugin) {
        if (!Bukkit.getPluginManager().isPluginEnabled(TabHook.PLUGIN)) return NONE;
        try {
            TabSupport hook = new TabHook(plugin);
            Debug.info("Hooked into TAB (tab list, name tags and sidebar in games)");
            return hook;
        } catch (Exception | LinkageError ex) {
            Debug.warn("Couldn't hook into TAB, it may overwrite the tab list and name tags in games: " + ex);
            return NONE;
        }
    }

}
