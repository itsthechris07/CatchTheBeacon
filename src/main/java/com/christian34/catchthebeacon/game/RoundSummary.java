package com.christian34.catchthebeacon.game;

import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

/**
 * The summary at the end of a round (game.round-summary): duration, variant, MVP, most kills, destroyed beacons
 * and mined resources.
 *
 * @author Christian34
 */
public final class RoundSummary {
    /**
     * a destroyed beacon counts like this many kills for the MVP
     */
    public static final int BEACON_POINTS = 3;

    private RoundSummary() {
    }

    public static List<Component> getLines(Game game) {
        List<Component> lines = new ArrayList<>();
        lines.add(i18n(LangText.SUMMARY_TITLE));
        lines.add(i18n(LangText.SUMMARY_DURATION, time(game.getRoundSeconds())));
        if (game.getVariant() != Variant.NORMAL) lines.add(i18n(LangText.SUMMARY_VARIANT, game.getVariant().getName()));

        GamePlayer mvp = getMvp(game);
        if (mvp != null) {
            lines.add(i18n(LangText.SUMMARY_MVP, mvp.getDisplayName(), mvp.getKills(), beacons(game, mvp)));
        }
        game.getGamePlayers().stream()
                .filter(gamePlayer -> gamePlayer.getKills() > 0)
                .max(Comparator.comparingInt(GamePlayer::getKills))
                .ifPresent(killer -> lines.add(i18n(LangText.SUMMARY_MOST_KILLS, killer.getDisplayName(), killer.getKills())));

        for (Game.DestroyedBeacon destroyed : game.getDestroyedBeacons()) {
            Component player = Component.text(destroyed.playerName());
            if (destroyed.playerTeam() != null) player = player.color(destroyed.playerTeam().getColor());
            lines.add(i18n(LangText.SUMMARY_BEACON, player, destroyed.beacon().getPosition().getName(),
                    destroyed.beacon().getTeam().getDisplayName(), time(destroyed.second())));
        }
        for (Team team : Team.getTeams()) {
            Map<Material, Integer> mined = game.getMinedResources().get(team);
            if (mined == null || mined.isEmpty()) continue;
            String resources = mined.entrySet().stream()
                    .map(entry -> entry.getValue() + "x " + name(entry.getKey()))
                    .collect(Collectors.joining(", "));
            lines.add(i18n(LangText.SUMMARY_MINED, team.getDisplayName(), resources));
        }
        return lines;
    }

    /**
     * @return the player with the most points (kills + {@link #BEACON_POINTS} per beacon), null if nobody has any
     */
    public static GamePlayer getMvp(Game game) {
        return game.getGamePlayers().stream()
                .filter(gamePlayer -> points(game, gamePlayer) > 0)
                .max(Comparator.comparingInt(gamePlayer -> points(game, gamePlayer)))
                .orElse(null);
    }

    private static int points(Game game, GamePlayer gamePlayer) {
        return gamePlayer.getKills() + BEACON_POINTS * beacons(game, gamePlayer);
    }

    private static int beacons(Game game, GamePlayer gamePlayer) {
        UUID uuid = gamePlayer.getPlayer().getUniqueId();
        return (int) game.getDestroyedBeacons().stream().filter(destroyed -> destroyed.player().equals(uuid)).count();
    }

    /**
     * @return e.g. "iron block"
     */
    private static String name(Material material) {
        return material.name().toLowerCase().replace('_', ' ');
    }

    private static String time(long seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

}
