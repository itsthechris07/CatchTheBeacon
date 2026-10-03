package com.christian34.catchthebeacon.listeners;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import com.christian34.catchthebeacon.user.UserStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;


public class QuitListener implements @NotNull Listener {
    private final CatchTheBeacon plugin;

    public QuitListener(CatchTheBeacon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        GamePlayer gamePlayer = plugin.getUser(e.getPlayer());
        Game game = gamePlayer.getGame();
        if (game == null) return;
        // he may come back into his team (game.rejoin-seconds)
        game.disconnect(gamePlayer);
    }

    /**
     * the server crashed while the player was in a game or the setup: he gets his inventory etc. back (first, before
     * he can join a game again)
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoinAfterCrash(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        // after the join is done, otherwise the teleport doesn't work
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || plugin.getUser(player).getUserStorage().isStored()) return;
            if (UserStorage.restoreBackupFile(player)) plugin.getUser(player).sendMessage(LangText.BACKUP_RESTORED);
        });
    }

    /**
     * players who left a running game come back into their team
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        for (Game game : plugin.getGameManager().getGames()) {
            if (game.canRejoin(player)) {
                // after the join is done, otherwise the teleport doesn't work
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline() && game.canRejoin(player)) game.rejoin(plugin.getUser(player));
                });
                return;
            }
        }
    }

}
