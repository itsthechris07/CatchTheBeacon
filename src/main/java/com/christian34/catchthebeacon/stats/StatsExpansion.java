package com.christian34.catchthebeacon.stats;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.integrations.Placeholders;
import com.christian34.catchthebeacon.lib.lang.I;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * The placeholders of PlaceholderAPI: %ctb_kills%, %ctb_deaths%, %ctb_kd%, %ctb_wins%, %ctb_games%, %ctb_beacons%,
 * %ctb_achievements% (number) and the ranking: %ctb_top_<stat>_<place>_name% and %ctb_top_<stat>_<place>_value%
 * (e.g. %ctb_top_wins_1_name%, places 1 to 10, empty if nobody is there; refreshed every minute), %ctb_team% and
 * %ctb_arena% of the player and the game of an arena (lobby server: game server) for NPCs and holograms:
 * %ctb_players_<arena>%, %ctb_maxplayers_<arena>%, %ctb_state_<arena>%.
 * Only loaded if PlaceholderAPI is installed.
 *
 * @author Christian34
 */
public class StatsExpansion extends PlaceholderExpansion {
    private final CatchTheBeacon plugin;

    public StatsExpansion(CatchTheBeacon plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ctb";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Christian34";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        String lower = params.toLowerCase();
        if (lower.startsWith("top_")) return top(plugin.getStatsManager(), params);
        for (String kind : new String[]{"players", "maxplayers", "state"}) {
            if (lower.startsWith(kind + "_")) {
                Component value = Placeholders.game(kind, params.substring(kind.length() + 1));
                return value == null ? null : I.legacy(value);
            }
        }
        if (player == null) return "";
        if (lower.equals("team") || lower.equals("arena")) {
            Player online = player.getPlayer();
            if (online == null) return "";
            return I.legacy(lower.equals("team") ? Placeholders.team(online) : Placeholders.arena(online));
        }
        return playerValue(plugin, player.getUniqueId(), params);
    }

    /**
     * @param name kills, deaths, kd, wins, games, beacons or achievements
     * @return the value of the player, null if the placeholder doesn't exist
     */
    @Nullable
    public static String playerValue(CatchTheBeacon plugin, UUID player, String name) {
        if (name.equalsIgnoreCase("achievements")) {
            return String.valueOf(plugin.getAchievementManager().get(player).size());
        }
        return value(plugin.getStatsManager().get(player), name);
    }

    /**
     * @return the value of the placeholder, null if it doesn't exist
     */
    @Nullable
    static String value(StatsManager.PlayerStats stats, String params) {
        return switch (params.toLowerCase()) {
            case "kills" -> String.valueOf(stats.kills());
            case "deaths" -> String.valueOf(stats.deaths());
            case "kd" -> String.valueOf(stats.killDeathRatio());
            case "wins" -> String.valueOf(stats.wins());
            case "games" -> String.valueOf(stats.games());
            case "beacons" -> String.valueOf(stats.beacons());
            default -> null;
        };
    }

    /**
     * @param params top_<stat>_<place>_<name|value>
     * @return the name or value of the player at that place, null if the placeholder doesn't exist
     */
    @Nullable
    public static String top(StatsManager stats, String params) {
        String[] parts = params.toLowerCase().split("_");
        if (parts.length != 4 || !(parts[3].equals("name") || parts[3].equals("value"))) return null;
        StatsManager.Stat stat;
        int place;
        try {
            stat = StatsManager.Stat.valueOf(parts[1].toUpperCase());
            place = Integer.parseInt(parts[2]);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        if (place < 1 || place > StatsManager.TOP_CACHE_SIZE) return null;
        List<StatsManager.RankedStat> top = stats.getTop(stat);
        if (place > top.size()) return "";
        StatsManager.RankedStat entry = top.get(place - 1);
        return parts[3].equals("name") ? entry.name() : String.valueOf(entry.value());
    }

}
