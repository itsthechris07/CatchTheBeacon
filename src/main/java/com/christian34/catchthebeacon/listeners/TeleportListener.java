package com.christian34.catchthebeacon.listeners;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.user.GamePlayer;
import com.christian34.catchthebeacon.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;


/**
 * Players teleported out of the worlds of the plugin (e.g. by another plugin) leave the game and get their things
 * back.
 */
public class TeleportListener implements @NotNull Listener {
    private final CatchTheBeacon plugin;

    public TeleportListener(CatchTheBeacon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (WorldUtils.isGameWorld(e.getTo().getWorld())) return;
        Player player = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            // where he is now: he may have joined a game in the meantime
            if (!player.isOnline() || WorldUtils.isGameWorld(player.getWorld())) return;
            GamePlayer gamePlayer = plugin.getUser(player);
            Game game = gamePlayer.getGame();
            if (game != null) {
                game.quit(gamePlayer);
            } else {
                gamePlayer.getUserStorage().restore();
            }
        }, 20);
    }

}
