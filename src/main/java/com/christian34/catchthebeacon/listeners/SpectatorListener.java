package com.christian34.catchthebeacon.listeners;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.lib.InteractionItems;
import com.christian34.catchthebeacon.lib.Menu;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

/**
 * Spectators can't interact with the game: no damage (in both directions), no items, no blocks. The compass
 * teleports them to a player, the red dye leaves the game.
 *
 * @author Christian34
 */
@SuppressWarnings("unused")
public class SpectatorListener implements Listener {
    private final CatchTheBeacon plugin;

    public SpectatorListener(CatchTheBeacon plugin) {
        this.plugin = plugin;
    }

    @Nullable
    private GamePlayer spectator(@Nullable Object entity) {
        if (!(entity instanceof Player player)) return null;
        GamePlayer gamePlayer = plugin.getUser(player);
        return gamePlayer.isSpectator() && gamePlayer.getGame() != null ? gamePlayer : null;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        GamePlayer spectator = spectator(e.getPlayer());
        if (spectator == null) return;
        e.setCancelled(true);
        if (!e.getAction().isRightClick() || e.getItem() == null) return;
        if (e.getItem().isSimilar(InteractionItems.getSpectatorCompass())) {
            openTeleportMenu(spectator);
        } else if (e.getItem().isSimilar(InteractionItems.getLeaveItem())) {
            spectator.getGame().quit(spectator);
            plugin.getNetworkManager().sendToLobby(e.getPlayer());
        }
    }

    private void openTeleportMenu(GamePlayer spectator) {
        Game game = spectator.getGame();
        if (game == null) return;
        Menu menu = new Menu(i18n(LangText.GUI_SPECTATE));
        for (GamePlayer target : new ArrayList<>(game.getGamePlayers())) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(target.getPlayer());
                head.setItemMeta(meta);
            }
            menu.button(head, target.getDisplayName(), null, List.of(), player -> {
                if (target.getGame() == game) spectator.teleport(target.getPlayer().getLocation());
            });
        }
        menu.show(spectator.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent e) {
        if (spectator(e.getEntity()) != null
                || (e instanceof EntityDamageByEntityEvent byEntity && spectator(byEntity.getDamager()) != null)) {
            e.setCancelled(true);
        }
    }

    /**
     * arrows fly through spectators
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileHit(ProjectileHitEvent e) {
        if (spectator(e.getHitEntity()) != null) e.setCancelled(true);
    }

    /**
     * the items of spectators can't be moved (menus of the plugin handle their clicks themselves)
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent e) {
        if (spectator(e.getWhoClicked()) != null && e.getView().getTopInventory().getType() == InventoryType.CRAFTING) {
            e.setCancelled(true);
        }
    }

}
