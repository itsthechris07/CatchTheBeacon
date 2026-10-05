package com.christian34.catchthebeacon.listeners;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.files.ConfigFile;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The chat in games: only the players of the game read it. While playing (chat.team-chat), messages only go to the
 * own team, unless they start with chat.shout-prefix. Spectators chat among themselves. Players in games don't read
 * the chat of the rest of the server (chat.isolate-games).
 */
public class ChatListener implements Listener {
    private final CatchTheBeacon instance;

    public ChatListener(CatchTheBeacon instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        GamePlayer gamePlayer = findUser(e.getPlayer());
        if (gamePlayer == null || gamePlayer.getGame() == null) {
            if (instance.getFileManager().getConfigFile().getBoolean("chat.isolate-games")) {
                e.viewers().removeIf(viewer -> viewer instanceof Player player && isInGame(player));
            }
            return;
        }
        e.setCancelled(true);
        Component message = e.message();
        // the players of the game may only be read on the main thread
        if (Bukkit.isPrimaryThread()) {
            send(gamePlayer, message);
        } else {
            Bukkit.getScheduler().runTask(instance, () -> send(gamePlayer, message));
        }
    }

    /**
     * the user of a player in a ctb_ world, without creating one (the event runs async)
     */
    private GamePlayer findUser(Player player) {
        if (!player.getWorld().getName().startsWith("ctb_")) return null;
        Set<GamePlayer> users = instance.getUsers();
        synchronized (users) {
            for (GamePlayer user : users) {
                if (user.getPlayer().equals(player)) return user;
            }
        }
        return null;
    }

    private boolean isInGame(Player player) {
        GamePlayer user = findUser(player);
        return user != null && user.getGame() != null;
    }

    private void send(GamePlayer sender, Component message) {
        Game game = sender.getGame();
        if (game == null) return;
        ConfigFile config = instance.getFileManager().getConfigFile();
        List<GamePlayer> recipients = new ArrayList<>();
        LangText format;
        Team team = sender.getTeam();
        String shoutPrefix = config.getString("chat.shout-prefix");
        if (sender.isSpectator()) {
            format = LangText.CHAT_SPECTATOR;
            recipients.addAll(game.getSpectators());
        } else if (game.getGameState() == GameState.INGAME && config.getBoolean("chat.team-chat")
                && team != null && Team.getTeams().contains(team)) {
            String text = PlainTextComponentSerializer.plainText().serialize(message);
            if (shoutPrefix != null && !shoutPrefix.isEmpty() && text.startsWith(shoutPrefix)
                    && text.length() > shoutPrefix.length()) {
                message = Component.text(text.substring(shoutPrefix.length()).trim());
                format = LangText.CHAT_ALL;
                recipients.addAll(game.getGamePlayers());
                recipients.addAll(game.getSpectators());
            } else {
                format = LangText.CHAT_TEAM;
                recipients.addAll(game.getGamePlayers(team));
                // spectators read everything
                recipients.addAll(game.getSpectators());
            }
        } else {
            format = LangText.CHAT_TEAM;
            recipients.addAll(game.getGamePlayers());
            recipients.addAll(game.getSpectators());
        }
        Component name = sender.getDisplayName();
        if (team == null || !Team.getTeams().contains(team)) name = name.colorIfAbsent(NamedTextColor.GREEN);
        Component text = I.i18n(format, name, message);
        for (GamePlayer recipient : recipients) {
            recipient.getPlayer().sendMessage(text);
        }
    }

}
