package com.christian34.catchthebeacon.listeners;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Beacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.Variant;
import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.lib.InteractionItems;
import com.christian34.catchthebeacon.user.GamePlayer;
import io.papermc.paper.event.block.BlockBreakProgressUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Things of a running round: the boss bar while a beacon is being mined, the rules of the variants and the compass.
 *
 * @author Christian34
 */
public class RoundListener implements Listener {
    /**
     * damage that kills in the variant one-hit (armor only reduces it)
     */
    private static final double ONE_HIT_DAMAGE = 1000;
    private final CatchTheBeacon plugin;

    public RoundListener(CatchTheBeacon plugin) {
        this.plugin = plugin;
    }

    @Nullable
    private Game runningGame(Player player) {
        Game game = plugin.getUser(player).getGame();
        return game != null && game.getGameState() == GameState.INGAME ? game : null;
    }

    /**
     * an enemy mines a beacon: everybody sees the progress (base.mining-bossbar), with a sound when it starts
     */
    @EventHandler(ignoreCancelled = true)
    public void onMiningProgress(BlockBreakProgressUpdateEvent e) {
        if (e.getBlock().getType() != Material.BEACON || !(e.getEntity() instanceof Player player)) return;
        Game game = runningGame(player);
        GamePlayer gamePlayer = plugin.getUser(player);
        if (game == null || gamePlayer.isSpectator()) return;
        Beacon beacon = game.getBeacon(e.getBlock().getLocation());
        if (beacon == null || !beacon.isAlive() || beacon.getTeam() == gamePlayer.getTeam()) return;
        boolean bossBar = plugin.getFileManager().getConfigFile().getBoolean("base.mining-bossbar");
        List<Player> viewers = new ArrayList<>(game.getPlayers());
        game.getSpectators().forEach(spectator -> viewers.add(spectator.getPlayer()));
        if (beacon.updateMining(player, e.getProgress(), Bukkit.getCurrentTick(), bossBar, viewers) && bossBar) {
            for (Player viewer : viewers) {
                viewer.playSound(viewer.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
            }
        }
    }

    /**
     * bow-only: melee attacks don't hurt; one-hit: every hit of an enemy kills
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVariantDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Game game = runningGame(victim);
        if (game == null || game.getVariant() == Variant.NORMAL) return;
        Player attacker = e.getDamager() instanceof Player player ? player
                : e.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter ? shooter : null;
        if (attacker == null || attacker.equals(victim) || plugin.getUser(attacker).getGame() != game
                || plugin.getUser(attacker).getTeam() == plugin.getUser(victim).getTeam()) return;
        switch (game.getVariant()) {
            case BOW_ONLY -> {
                if (!(e.getDamager() instanceof Projectile)) e.setCancelled(true);
            }
            case ONE_HIT -> e.setDamage(ONE_HIT_DAMAGE);
            default -> {
            }
        }
    }

    /**
     * Compasses of the game have no use of their own: other plugins must not react to them either (the navigation
     * wand of WorldEdit is a compass, clicking would teleport). WorldEdit ignores interactions with a denied item.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onCompass(PlayerInteractEvent e) {
        if (e.getItem() == null || e.getItem().getType() != Material.COMPASS) return;
        if (plugin.getUser(e.getPlayer()).getGame() != null) e.setUseItemInHand(Event.Result.DENY);
    }

    /**
     * the beacon compass belongs to the kit, nobody can pick it up
     */
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (plugin.getUser(e.getEntity()).getGame() == null) return;
        e.getDrops().removeIf(item -> item != null && item.isSimilar(InteractionItems.getBeaconCompass()));
    }

}
