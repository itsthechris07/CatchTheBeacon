package com.christian34.catchthebeacon.game.setup;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.map.Arena;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Starts and stops the guided setups and handles the setup tools.
 *
 * @author Christian34
 */
public class SetupManager implements Listener {
    private final CatchTheBeacon plugin;
    private final Map<UUID, SetupSession> sessions = new HashMap<>();

    public SetupManager(CatchTheBeacon plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Nullable
    public SetupSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void start(GamePlayer gamePlayer, Arena arena) {
        Player player = gamePlayer.getPlayer();
        SetupSession current = getSession(player);
        if (current != null) {
            if (current.getArena().equals(arena)) {
                current.sendChecklist();
            } else {
                gamePlayer.sendMessage(LangText.SETUP_OTHER_ARENA, current.getArena().getName());
            }
            return;
        }
        if (gamePlayer.getGame() != null) {
            gamePlayer.sendMessage(LangText.SETUP_LEAVE_GAME);
            return;
        }
        if (arena.isInUse()) {
            gamePlayer.sendMessage(LangText.SETUP_ARENA_IN_USE);
            return;
        }
        for (SetupSession session : sessions.values()) {
            if (session.getArena().equals(arena)) {
                gamePlayer.sendMessage(LangText.SETUP_TAKEN, session.getPlayer().getName());
                return;
            }
        }
        SetupSession session = new SetupSession(plugin, gamePlayer, arena);
        sessions.put(player.getUniqueId(), session);
        if (!session.start()) {
            sessions.remove(player.getUniqueId());
        }
    }

    /**
     * saves the worlds and restores the player
     */
    public void stop(SetupSession session) {
        if (sessions.remove(session.getPlayer().getUniqueId()) == null) return;
        session.close();
        Arena arena = session.getArena();
        session.getPlayer().sendMessage(I.prefixed(I.i18n(LangText.SETUP_SAVED)
                .append(arena.isPlayable()
                        ? I.button(LangText.BUTTON_CREATE_GAME, ClickEvent.runCommand("/ctb arena " + arena.getName() + " creategame"))
                        : I.button(LangText.BUTTON_CONTINUE_SETUP, ClickEvent.runCommand("/ctb arena " + arena.getName() + " setup")))));
    }

    public void stopAll() {
        for (SetupSession session : new ArrayList<>(sessions.values())) {
            stop(session);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        SetupItem item = SetupItem.of(e.getItem());
        if (item == null) return;
        e.setCancelled(true);
        SetupSession session = getSession(e.getPlayer());
        if (session == null) {
            // left over after a crash
            e.getPlayer().getInventory().setItemInMainHand(null);
            return;
        }
        session.use(item, e.getClickedBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player) || getSession(player) == null) return;
        boolean hotbarSwap = e.getHotbarButton() >= 0
                && SetupItem.of(player.getInventory().getItem(e.getHotbarButton())) != null;
        if (SetupItem.of(e.getCurrentItem()) != null || SetupItem.of(e.getCursor()) != null || hotbarSwap) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (SetupItem.of(e.getItemDrop().getItemStack()) != null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        if (SetupItem.of(e.getMainHandItem()) != null || SetupItem.of(e.getOffHandItem()) != null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (SetupItem.of(e.getItemInHand()) != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        SetupSession session = getSession(e.getPlayer());
        if (session != null && !session.isSetupWorld(e.getTo().getWorld())) {
            // left the setup worlds (e.g. /spawn) - end the setup like the emerald does, but only if the player is
            // still outside of them a tick later (not while switching between the lobby and the arena)
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (sessions.get(e.getPlayer().getUniqueId()) == session && !session.isSetupWorld(e.getPlayer().getWorld())) {
                    stop(session);
                }
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        SetupSession session = getSession(e.getPlayer());
        if (session != null) stop(session);
    }

}
