package com.christian34.catchthebeacon.integrations;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.SignManager;
import com.christian34.catchthebeacon.network.ServerStatus;
import com.christian34.catchthebeacon.user.GamePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The values of the placeholders that aren't stats, shared by PlaceholderAPI (stats/StatsExpansion) and
 * MiniPlaceholders ({@link CtbMiniPlaceholders}): the game of an arena (or game server) for NPC names, holograms
 * or menus, and the team and arena of a player.
 *
 * @author Christian34
 */
public final class Placeholders {

    private Placeholders() {
    }

    /**
     * @param kind   players, maxplayers or state
     * @param target the arena, on a lobby server the game server
     * @return the value, null if the placeholder doesn't exist
     */
    @Nullable
    public static Component game(String kind, String target) {
        ServerStatus status = CatchTheBeacon.getInstance().getSignManager().getStatus(target);
        return switch (kind.toLowerCase()) {
            case "players" -> Component.text(status.players());
            case "maxplayers" -> Component.text(status.maxPlayers());
            case "state" -> SignManager.stateText(status);
            default -> null;
        };
    }

    /**
     * @return the name of the player's team, empty if he isn't playing or has none yet
     */
    public static Component team(Player player) {
        GamePlayer gamePlayer = CatchTheBeacon.getInstance().getUser(player);
        if (gamePlayer.getGame() == null || gamePlayer.getTeam() == null) return Component.empty();
        return gamePlayer.getTeam().getDisplayName();
    }

    /**
     * @return the arena the player is playing or watching, empty if none
     */
    public static Component arena(Player player) {
        Game game = CatchTheBeacon.getInstance().getUser(player).getGame();
        return game == null ? Component.empty() : game.getArena().getDisplayName();
    }

}
