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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * The chat in games: only the players of the game read it. While playing (chat.team-chat), messages only go to the
 * own team, unless they start with chat.shout-prefix. Spectators chat among themselves.
 */
public class ChatListener implements Listener {
    private final CatchTheBeacon instance;

    public ChatListener(CatchTheBeacon instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        if (!e.getPlayer().getWorld().getName().startsWith("ctb_")) {
            return;
        }
        GamePlayer gamePlayer = instance.getUser(e.getPlayer());
        if (gamePlayer.getGame() == null) {
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
