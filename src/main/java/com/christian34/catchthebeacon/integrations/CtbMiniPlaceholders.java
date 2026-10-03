package com.christian34.catchthebeacon.integrations;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.stats.StatsExpansion;
import io.github.miniplaceholders.api.Expansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The placeholders for MiniPlaceholders (https://github.com/MiniPlaceholders/MiniPlaceholders), the same as those of
 * PlaceholderAPI: &lt;ctb_kills&gt;, &lt;ctb_wins&gt;, ... &lt;ctb_team&gt;, &lt;ctb_arena&gt; (of the viewer),
 * &lt;ctb_top:wins:1:name&gt;, &lt;ctb_players:castle&gt;, &lt;ctb_maxplayers:castle&gt;, &lt;ctb_state:castle&gt;.
 *
 * @author Christian34
 */
public class CtbMiniPlaceholders {
    static final List<String> STATS = List.of("kills", "deaths", "kd", "wins", "games", "beacons", "achievements");
    private final CatchTheBeacon plugin;

    public CtbMiniPlaceholders(CatchTheBeacon plugin) {
        this.plugin = plugin;
    }

    public Expansion register() {
        Expansion.Builder builder = Expansion.builder("ctb")
                .author("Christian34")
                .version(plugin.getPluginMeta().getVersion());
        for (String stat : STATS) {
            builder.audiencePlaceholder(Player.class, stat, (player, queue, context) ->
                    text(StatsExpansion.playerValue(plugin, player.getUniqueId(), stat)));
        }
        builder.audiencePlaceholder(Player.class, "team", (player, queue, context) ->
                Tag.selfClosingInserting(Placeholders.team(player)));
        builder.audiencePlaceholder(Player.class, "arena", (player, queue, context) ->
                Tag.selfClosingInserting(Placeholders.arena(player)));
        builder.globalPlaceholder("top", (queue, context) -> {
            // <ctb_top:wins:1:name>
            String params = "top_" + String.join("_", arguments(queue, 3));
            return text(StatsExpansion.top(plugin.getStatsManager(), params));
        });
        for (String kind : List.of("players", "maxplayers", "state")) {
            builder.globalPlaceholder(kind, (queue, context) -> {
                if (!queue.hasNext()) return null;
                Component value = Placeholders.game(kind, queue.pop().value());
                return value == null ? null : Tag.selfClosingInserting(value);
            });
        }
        Expansion expansion = builder.build();
        expansion.register();
        return expansion;
    }

    private static List<String> arguments(ArgumentQueue queue, int count) {
        List<String> arguments = new ArrayList<>();
        while (queue.hasNext() && arguments.size() < count) {
            arguments.add(queue.pop().value());
        }
        return arguments;
    }

    private static Tag text(String value) {
        return value == null ? null : Tag.selfClosingInserting(Component.text(value));
    }

}
