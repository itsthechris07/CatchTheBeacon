package com.christian34.catchthebeacon.integrations;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.scoreboard.ScoreboardManager;
import me.neznamy.tab.api.tablist.HeaderFooterManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TAB (https://github.com/NEZNAMY/TAB), see {@link TabSupport}. Its features may be disabled (manager null).
 */
class TabHook implements TabSupport {
    static final String PLUGIN = "TAB";
    /**
     * TAB understands &-codes with &#rrggbb
     */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&').hexCharacter('#').hexColors().build();
    /**
     * the players in a game, with header and footer of the tab list
     */
    private final Map<UUID, String[]> players = new ConcurrentHashMap<>();
    /**
     * the players whose sidebar of TAB was hidden for the game
     */
    private final Set<UUID> hiddenSidebars = new HashSet<>();
    private boolean failed;

    TabHook(CatchTheBeacon plugin) {
        // TAB loads players a moment after they joined (or after /tab reload): the game takes over again
        TabAPI.getInstance().getEventBus().register(PlayerLoadEvent.class, event -> {
            UUID uuid = event.getPlayer().getUniqueId();
            if (players.containsKey(uuid)) Bukkit.getScheduler().runTask(plugin, () -> apply(uuid));
        });
    }

    @Override
    public void takeOver(@NotNull Player player, @NotNull Component header, @NotNull Component footer) {
        if (players.putIfAbsent(player.getUniqueId(), new String[]{LEGACY.serialize(header), LEGACY.serialize(footer)}) != null) {
            return;
        }
        apply(player.getUniqueId());
    }

    private void apply(UUID uuid) {
        String[] headerFooter = players.get(uuid);
        TabPlayer tabPlayer = getTabPlayer(uuid);
        if (headerFooter == null || tabPlayer == null) return;
        try {
            TabAPI api = TabAPI.getInstance();
            HeaderFooterManager headerFooterManager = api.getHeaderFooterManager();
            if (headerFooterManager != null) {
                headerFooterManager.setHeaderAndFooter(tabPlayer, headerFooter[0], headerFooter[1]);
            }
            // the teams of the game color the name tags
            NameTagManager nameTags = api.getNameTagManager();
            if (nameTags != null && !nameTags.hasTeamHandlingPaused(tabPlayer)) nameTags.pauseTeamHandling(tabPlayer);
            ScoreboardManager sidebar = api.getScoreboardManager();
            if (sidebar != null && sidebar.hasScoreboardVisible(tabPlayer)) {
                sidebar.setScoreboardVisible(tabPlayer, false, false);
                hiddenSidebars.add(uuid);
            }
        } catch (Exception | LinkageError ex) {
            warn(ex);
        }
    }

    @Override
    public void release(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        if (players.remove(uuid) == null) return;
        boolean hiddenSidebar = hiddenSidebars.remove(uuid);
        TabPlayer tabPlayer = getTabPlayer(uuid);
        if (tabPlayer == null) return;
        try {
            TabAPI api = TabAPI.getInstance();
            HeaderFooterManager headerFooterManager = api.getHeaderFooterManager();
            // null: back to the header and footer of TAB's config
            if (headerFooterManager != null) headerFooterManager.setHeaderAndFooter(tabPlayer, null, null);
            NameTagManager nameTags = api.getNameTagManager();
            if (nameTags != null && nameTags.hasTeamHandlingPaused(tabPlayer)) nameTags.resumeTeamHandling(tabPlayer);
            ScoreboardManager sidebar = api.getScoreboardManager();
            if (sidebar != null && hiddenSidebar) sidebar.setScoreboardVisible(tabPlayer, true, false);
        } catch (Exception | LinkageError ex) {
            warn(ex);
        }
    }

    /**
     * @return the player, null if TAB hasn't loaded him yet
     */
    @Nullable
    private static TabPlayer getTabPlayer(UUID uuid) {
        TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(uuid);
        return tabPlayer == null || !tabPlayer.isLoaded() ? null : tabPlayer;
    }

    private void warn(Throwable ex) {
        if (failed) return;
        failed = true;
        Debug.warn("TAB may overwrite the tab list or name tags of games, the API call failed: " + ex);
    }

}
