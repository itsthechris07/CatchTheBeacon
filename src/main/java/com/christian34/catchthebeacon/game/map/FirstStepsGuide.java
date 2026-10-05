package com.christian34.catchthebeacon.game.map;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

/**
 * Tells admins what to do while there is no arena yet (like the hint in the console at the start): download a map
 * with a prepared setup ({@link MapPreset}) or set up an own map - with clickable commands.
 *
 * @author Christian34
 */
public class FirstStepsGuide implements Listener {
    private static final long JOIN_DELAY_TICKS = 3 * 20L;
    private final CatchTheBeacon plugin;

    public FirstStepsGuide(CatchTheBeacon plugin) {
        this.plugin = plugin;
    }

    /**
     * @return true if the admins have to set up an arena first (lobby servers of a network don't have arenas)
     */
    boolean isNeeded() {
        return plugin.getMapHandler().getGameMaps().isEmpty() && !plugin.getNetworkManager().isLobby();
    }

    void send(Player player) {
        player.sendMessage(I.prefixed(LangText.FIRST_STEPS_TITLE));
        List<MapPreset> presets = MapPreset.notInstalled(plugin);
        for (MapPreset preset : presets) {
            player.sendMessage(I.i18n(LangText.FIRST_STEPS_PRESET, preset.displayName(), preset.author())
                    .append(I.button(LangText.BUTTON_MAP_DOWNLOAD, ClickEvent.openUrl(preset.url()))
                            .hoverEvent(HoverEvent.showText(Component.text(preset.url())))));
        }
        if (!presets.isEmpty()) {
            Component install = I.i18n(LangText.FIRST_STEPS_PRESET_INSTALL);
            for (MapPreset preset : presets) {
                install = install.append(command(I.i18n(LangText.BUTTON_SET_UP_MAP, preset.displayName()),
                        "/ctb arena create " + preset.name(), false));
            }
            player.sendMessage(install);
        }
        player.sendMessage(I.i18n(LangText.FIRST_STEPS_OWN_MAP)
                .append(command(I.i18n(LangText.BUTTON_IMPORT_WORLD), "/ctb arena create ", true)
                        .hoverEvent(HoverEvent.showText(I.i18n(LangText.FIRST_STEPS_IMPORT_HOVER)))));
        player.sendMessage(I.i18n(LangText.FIRST_STEPS_HELP)
                .append(command(I.i18n(LangText.BUTTON_HELP), "/ctb help arena", false))
                .appendSpace()
                .append(command(I.i18n(LangText.BUTTON_ARENA_LIST), "/ctb arena list", false)));
    }

    private static Component command(Component button, String command, boolean suggest) {
        return button.clickEvent(suggest ? ClickEvent.suggestCommand(command) : ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(command)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (!player.hasPermission("ctb.admin") || !isNeeded()) return;
        // after the join messages of the server and other plugins, so it doesn't get lost
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && isNeeded()) send(player);
        }, JOIN_DELAY_TICKS);
    }

}
