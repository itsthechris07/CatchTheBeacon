package com.christian34.catchthebeacon.listeners;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Beacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.GameItems;
import com.christian34.catchthebeacon.game.ResourceBlocks;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.game.Variant;
import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.integrations.EconomySupport;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.stats.Achievement;
import com.christian34.catchthebeacon.stats.AchievementManager;
import com.christian34.catchthebeacon.stats.StatsManager;
import com.christian34.catchthebeacon.user.GamePlayer;
import com.christian34.catchthebeacon.utils.WorldUtils;
import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

@SuppressWarnings("unused")
public class PlayerEventListener implements Listener {
    private final EventListener eventListener;
    private final CatchTheBeacon instance;

    public PlayerEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
        this.instance = CatchTheBeacon.getInstance();
    }

    @EventHandler
    public void onPlayerDropItemEvent(PlayerDropItemEvent e) {
        if (eventListener.cancel(e.getPlayer(), e)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerBedEnterEvent(PlayerBedEnterEvent e) {
        if (eventListener.cancel(e.getPlayer(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerPickupItemEvent(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (eventListener.cancel((Player) e.getEntity(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (eventListener.cancel((Player) e.getEntity(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player) && !(e.getDamager() instanceof Player)) return;
        Player player = (Player) (e.getEntity() instanceof Player ? e.getEntity() : e.getDamager());
        if (eventListener.cancel(player, e)) {
            e.setDamage(0);
        }
    }

    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent e) {
        if (!WorldUtils.isGameWorld(e.getPlayer().getWorld())) {
            return;
        }
        if (eventListener.cancel(e.getPlayer(), e)) {
            e.setCancelled(true);
            return;
        }
        GamePlayer gamePlayer = instance.getUser(e.getPlayer());
        Game game = gamePlayer.getGame();
        if (game == null || !game.getGameState().equals(GameState.INGAME)) return;

        if (e.getBlock().getType().equals(Material.BEACON)) {
            Beacon beacon = game.getBeacon(e.getBlock().getLocation());
            if (beacon == null) return;
            e.setCancelled(true);

            if (beacon.getTeam().equals(gamePlayer.getTeam())) {
                gamePlayer.sendMessage(i18n(LangText.WRONG_BEACON));
                return;
            }

            e.getBlock().setType(Material.AIR);
            beacon.setAlive(false);
            beacon.stopMining();
            game.addDestroyedBeacon(gamePlayer, beacon);
            instance.getStatsManager().add(e.getPlayer(), StatsManager.Stat.BEACONS);
            instance.getEconomySupport().reward(e.getPlayer(), EconomySupport.Reward.BEACON);
            game.broadcast(i18n(LangText.BEACON_DESTROYED,
                    beacon.getPosition().getName(),
                    beacon.getTeam().getDisplayName()));
            playDestroyEffects(game, beacon, gamePlayer, e.getBlock());

            boolean beaconsLeft = false;
            for (Beacon b : game.getBeacons(beacon.getTeam())) {
                if (b.isAlive()) {
                    beaconsLeft = true;
                    break;
                }
            }
            if (!beaconsLeft) {
                // all beacons of the team alone
                boolean solo = game.getDestroyedBeacons().stream()
                        .filter(destroyed -> destroyed.beacon().getTeam() == beacon.getTeam())
                        .allMatch(destroyed -> destroyed.player().equals(e.getPlayer().getUniqueId()));
                if (solo) instance.getAchievementManager().unlock(e.getPlayer(), Achievement.SOLO);
                game.setWinner(beacon.getTeam().getOpponent());
                game.getGameStateManager().setGameState(GameState.ENDING);
            }
        } else if (isUnbreakable(e.getBlock())) {
            gamePlayer.sendMessage(i18n(LangText.BLOCK_UNBREAKABLE));
            e.setCancelled(true);
        } else if (isProtected(game, gamePlayer, e.getBlock().getLocation())) {
            e.setCancelled(true);
        } else if (onlyPlacedBlocksBreakable() && !game.isPlacedBlock(e.getBlock())
                && !ResourceBlocks.isResource(e.getBlock().getType())) {
            gamePlayer.sendMessage(i18n(LangText.ONLY_PLACED_BLOCKS_BREAKABLE));
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBroken(BlockBreakEvent e) {
        Game game = instance.getGameManager().getGame(e.getBlock().getWorld());
        if (game == null || game.getGameState() != GameState.INGAME) return;
        // resources of the map give rewards instead of their drop (placed ones are normal blocks)
        GamePlayer gamePlayer = instance.getUser(e.getPlayer());
        Material material = e.getBlock().getType();
        if (!game.isPlacedBlock(e.getBlock()) && gamePlayer.getGame() == game && !gamePlayer.isSpectator()
                && ResourceBlocks.isResource(material)) {
            e.setDropItems(false);
            // variant no-iron: nothing
            if (game.getVariant() != Variant.NO_IRON && ResourceBlocks.reward(gamePlayer, material)) {
                game.addMinedResource(gamePlayer.getTeam(), material);
            }
        }
        game.removePlacedBlock(e.getBlock());
    }

    /**
     * the beacon was destroyed: block particles and a firework in the color of its team (explodes high enough to
     * hurt nobody), a title for everybody in the game and a growl for the team that lost it
     */
    private void playDestroyEffects(Game game, Beacon beacon, GamePlayer destroyer, Block block) {
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().spawnParticle(Particle.BLOCK, center, 80, 0.4, 0.4, 0.4, Material.BEACON.createBlockData());
        block.getWorld().spawnParticle(Particle.EXPLOSION, center, 3, 0.3, 0.3, 0.3);
        block.getWorld().spawn(center, Firework.class, firework -> {
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(beacon.getTeam().getLeatherColor())
                    .withFade(Color.WHITE)
                    .trail(true)
                    .build());
            meta.setPower(1);
            firework.setFireworkMeta(meta);
        });
        Title title = Title.title(i18n(LangText.BEACON_DESTROYED_TITLE),
                i18n(LangText.BEACON_DESTROYED_SUBTITLE, destroyer.getDisplayName(), beacon.getPosition().getName(),
                        beacon.getTeam().getDisplayName()));
        List<GamePlayer> viewers = new ArrayList<>(game.getGamePlayers());
        viewers.addAll(game.getSpectators());
        for (GamePlayer viewer : viewers) {
            Player player = viewer.getPlayer();
            player.showTitle(title);
            if (viewer.getTeam() == beacon.getTeam() && !viewer.isSpectator()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
            }
        }
    }

    private boolean onlyPlacedBlocksBreakable() {
        return instance.getFileManager().getConfigFile().getBoolean("game.only-placed-blocks-breakable");
    }

    /**
     * game.unbreakable-blocks, e.g. enchanting tables
     */
    private boolean isUnbreakable(Block block) {
        for (String name : instance.getFileManager().getConfigFile().getList("game.unbreakable-blocks")) {
            if (block.getType().name().equalsIgnoreCase(name.trim())) return true;
        }
        return false;
    }

    /**
     * blocks near beacons and team spawns can't be changed (tells the player why)
     */
    private boolean isProtected(Game game, GamePlayer gamePlayer, Location location) {
        LangText reason = getProtection(game, location);
        if (reason != null) gamePlayer.sendMessage(i18n(reason));
        return reason != null;
    }

    /**
     * @return why the block can't be changed, null if it can
     */
    @Nullable
    private LangText getProtection(Game game, Location location) {
        for (Team team : Team.getTeams()) {
            for (Beacon beacon : game.getBeacons(team)) {
                if (WorldUtils.isSameBlock(beacon.getLocation(), location)
                        || (game.hasBeaconProtection() && WorldUtils.distance(beacon.getLocation(), location) <= 3)) {
                    return LangText.NO_BLOCK_MODIFICATION_NEAR_BEACON;
                }
            }
        }
        int spawnProtection = instance.getFileManager().getConfigFile().getInt("game.spawn-protection");
        if (spawnProtection <= 0) return null;
        for (Team team : Team.getTeams()) {
            Location spawn = game.getTeamSpawn(team);
            if (spawn != null && WorldUtils.distance(spawn, location) <= spawnProtection) {
                return LangText.NO_BLOCK_MODIFICATION_NEAR_SPAWN;
            }
        }
        return null;
    }

    /**
     * TNT & co. are allowed, but don't destroy beacons, protected blocks or (with
     * game.only-placed-blocks-breakable) the map
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        filterExplosion(e.getLocation().getWorld(), e.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        filterExplosion(e.getBlock().getWorld(), e.blockList());
    }

    private void filterExplosion(World world, List<Block> blocks) {
        if (!WorldUtils.isPlayWorld(world)) return;
        Game game = instance.getGameManager().getGame(world);
        if (game == null) return;
        boolean onlyPlaced = onlyPlacedBlocksBreakable();
        blocks.removeIf(block -> block.getType() == Material.BEACON || isUnbreakable(block)
                || getProtection(game, block.getLocation()) != null
                || (onlyPlaced && !game.isPlacedBlock(block))
                // resources are only for players who mine them
                || (ResourceBlocks.isResource(block.getType()) && !game.isPlacedBlock(block)));
        for (Block block : blocks) {
            game.removePlacedBlock(block);
        }
    }

    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent e) {
        if (!WorldUtils.isGameWorld(e.getPlayer().getWorld())) {
            return;
        }
        if (eventListener.cancel(e.getPlayer(), e)) {
            e.setCancelled(true);
            return;
        }
        GamePlayer gamePlayer = instance.getUser(e.getPlayer());
        Game game = gamePlayer.getGame();
        if (game == null || !game.getGameState().equals(GameState.INGAME)) return;
        if (isProtected(game, gamePlayer, e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlaced(BlockPlaceEvent e) {
        Game game = instance.getGameManager().getGame(e.getBlock().getWorld());
        if (game != null && game.getGameState() == GameState.INGAME) game.addPlacedBlock(e.getBlock());
    }

    @EventHandler
    public void onFoodLevelChangeEvent(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (eventListener.cancel((Player) e.getEntity(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onBlockMultiPlaceEvent(BlockMultiPlaceEvent e) {
        if (eventListener.cancel(e.getPlayer(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onCraftItemEvent(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (eventListener.cancel((Player) e.getWhoClicked(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent e) {
        if (eventListener.cancel(e.getPlayer(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onProjectileLaunchEvent(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (eventListener.cancel((Player) e.getEntity(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent e) {
        if (eventListener.cancel(e.getPlayer(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerBucketEmptyEvent(PlayerBucketEmptyEvent e) {
        if (eventListener.cancel(e.getPlayer(), e)) e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryOpenEvent(InventoryOpenEvent e) {
        // the ender chest holds the items of the real game
        if (e.getInventory().getType() == InventoryType.ENDER_CHEST && WorldUtils.isGameWorld(e.getPlayer().getWorld())) {
            e.setCancelled(true);
            return;
        }
        if (!e.getInventory().getType().equals(InventoryType.BEACON)
                || !WorldUtils.isGameWorld(e.getPlayer().getWorld())) return;
        Player player = (Player) e.getPlayer();
        GamePlayer gamePlayer = instance.getUser(player);
        Game game = gamePlayer.getGame();
        if (game == null) return;
        if (game.getGameState().equals(GameState.INGAME)) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent e) {
        if (!WorldUtils.isGameWorld(e.getEntity().getWorld())) return;
        GamePlayer gamePlayer = instance.getUser(e.getEntity());
        Game game = gamePlayer.getGame();
        if (game == null) return;
        e.deathMessage(null);
        if (game.getGameState().equals(GameState.LOBBY)) {
            e.setKeepInventory(true);
        } else if (game.getGameState().equals(GameState.INGAME)) {
            GamePlayer killerPlayer = getKiller(game, gamePlayer);
            gamePlayer.setLastAttacker(null);
            gamePlayer.resetKillStreak();
            instance.getStatsManager().add(e.getEntity(), StatsManager.Stat.DEATHS);
            // the beacons he was mining
            List<Beacon> mined = new ArrayList<>();
            for (Team team : Team.getTeams()) {
                for (Beacon beacon : game.getBeacons(team)) {
                    if (e.getEntity().getUniqueId().equals(beacon.getMiner())) mined.add(beacon);
                }
            }
            if (killerPlayer != null) {
                killerPlayer.addKill();
                instance.getStatsManager().add(killerPlayer.getPlayer(), StatsManager.Stat.KILLS);
                instance.getEconomySupport().reward(killerPlayer.getPlayer(), EconomySupport.Reward.KILL);
                unlockKillAchievements(game, killerPlayer, mined);
                if (showDeathMessages()) {
                    game.broadcast(i18n(LangText.PLAYER_WAS_KILLED,
                            gamePlayer.getDisplayName(), killerPlayer.getDisplayName()));
                }
            } else if (showDeathMessages()) {
                game.broadcast(i18n(LangText.PLAYER_DIED, gamePlayer.getDisplayName()));
            }
            mined.forEach(Beacon::stopMining);
            e.getEntity().getInventory().clear();
        }
    }

    /**
     * @param mined the beacons the victim was mining
     */
    private void unlockKillAchievements(Game game, GamePlayer killer, List<Beacon> mined) {
        AchievementManager achievements = instance.getAchievementManager();
        Player player = killer.getPlayer();
        if (game.isFirstKill()) achievements.unlock(player, Achievement.FIRST_BLOOD);
        if (mined.stream().anyMatch(beacon -> beacon.getTeam() == killer.getTeam())) {
            achievements.unlock(player, Achievement.LAST_SECOND);
        }
        if (killer.getKillStreak() >= Achievement.KILLING_SPREE_KILLS) {
            achievements.unlock(player, Achievement.KILLING_SPREE);
        }
    }

    /**
     * @return the player who killed him - or who hit him last (game.last-hit-seconds), e.g. into the void
     */
    @Nullable
    private GamePlayer getKiller(Game game, GamePlayer victim) {
        Player killer = victim.getPlayer().getKiller();
        if (killer == null) {
            UUID lastAttacker = victim.getLastAttacker(instance.getFileManager().getConfigFile().getInt("game.last-hit-seconds"));
            killer = lastAttacker == null ? null : Bukkit.getPlayer(lastAttacker);
        }
        if (killer == null || killer.equals(victim.getPlayer())) return null;
        GamePlayer killerPlayer = instance.getUser(killer);
        return killerPlayer.getGame() == game ? killerPlayer : null;
    }

    /**
     * remembers who hit the player last (see {@link #getKiller(Game, GamePlayer)})
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim) || !WorldUtils.isPlayWorld(victim.getWorld())) return;
        Player attacker = getAttacker(e);
        if (attacker == null || attacker.equals(victim)) return;
        GamePlayer victimPlayer = instance.getUser(victim);
        GamePlayer attackerPlayer = instance.getUser(attacker);
        if (victimPlayer.getGame() != null && victimPlayer.getGame() == attackerPlayer.getGame()
                && victimPlayer.getTeam() != attackerPlayer.getTeam()) {
            victimPlayer.setLastAttacker(attacker.getUniqueId());
        }
    }

    /**
     * no advancements (achievements) in the worlds of the plugin - neither in games nor in the setup
     */
    @EventHandler(ignoreCancelled = true)
    public void onAdvancementProgress(PlayerAdvancementCriterionGrantEvent e) {
        if (WorldUtils.isGameWorld(e.getPlayer().getWorld())) {
            e.setCancelled(true);
        }
    }

    /**
     * the advancements of the real game (announced in the chat) are not shown to the players of a game
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAdvancementDone(PlayerAdvancementDoneEvent e) {
        Component message = e.message();
        if (message == null) return;
        e.message(null);
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (instance.getUser(player).getGame() == null) player.sendMessage(message);
        }
    }

    /**
     * the game has no effect on the statistics of the real game (kills, deaths, mined blocks, play time, ...)
     */
    @EventHandler(ignoreCancelled = true)
    public void onStatistic(PlayerStatisticIncrementEvent e) {
        if (WorldUtils.isGameWorld(e.getPlayer().getWorld())) {
            e.setCancelled(true);
        }
    }

    /**
     * no recipes are unlocked in the worlds of the plugin
     */
    @EventHandler(ignoreCancelled = true)
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent e) {
        if (WorldUtils.isGameWorld(e.getPlayer().getWorld())) {
            e.setCancelled(true);
        }
    }
    @EventHandler
    public void onPlayerRespawnEvent(PlayerRespawnEvent e) {
        if (!WorldUtils.isGameWorld(e.getPlayer().getWorld())) return;
        GamePlayer gamePlayer = instance.getUser(e.getPlayer());
        Game game = gamePlayer.getGame();
        if (game == null) return;
        if (game.getGameState().equals(GameState.LOBBY)) {
            e.setRespawnLocation(game.getLobbySpawn());
        } else if (game.getGameState().equals(GameState.INGAME)) {
            Team team = gamePlayer.getTeam();
            e.setRespawnLocation(game.getTeamSpawn(team));
            GameItems.setItems(gamePlayer);
            // effects can only be added after the respawn
            Bukkit.getScheduler().runTask(instance, () -> game.applyEventEffects(e.getPlayer()));
            int seconds = instance.getFileManager().getConfigFile().getInt("game.respawn-protection");
            if (seconds > 0) {
                gamePlayer.protectFor(seconds);
                e.getPlayer().sendActionBar(i18n(LangText.SPAWN_PROTECTION, seconds));
            }
        }
    }

    private boolean showDeathMessages() {
        return instance.getFileManager().getConfigFile().getData().getBoolean("game.show-death-messages", true);
    }

    /**
     * the player behind an attack (also if shot with a projectile)
     */
    @Nullable
    private static Player getAttacker(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player player) return player;
        if (e.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    /**
     * game.friendly-fire in config.yml: players of the same team can't hurt each other
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFriendlyFire(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim) || !WorldUtils.isPlayWorld(victim.getWorld())) return;
        Player attacker = getAttacker(e);
        if (attacker == null || attacker.equals(victim)
                || instance.getFileManager().getConfigFile().getData().getBoolean("game.friendly-fire")) return;
        GamePlayer victimPlayer = instance.getUser(victim);
        GamePlayer attackerPlayer = instance.getUser(attacker);
        if (victimPlayer.getGame() != null && victimPlayer.getGame() == attackerPlayer.getGame()
                && victimPlayer.getTeam() != null && victimPlayer.getTeam() == attackerPlayer.getTeam()) {
            e.setCancelled(true);
        }
    }

    /**
     * no damage right after a respawn - attacking somebody ends the protection
     */
    @EventHandler(ignoreCancelled = true)
    public void onDamageWhileProtected(EntityDamageEvent e) {
        if (e instanceof EntityDamageByEntityEvent byEntity && getAttacker(byEntity) instanceof Player attacker
                && WorldUtils.isPlayWorld(attacker.getWorld())) {
            instance.getUser(attacker).endProtection();
        }
        if (e.getEntity() instanceof Player player && WorldUtils.isPlayWorld(player.getWorld())
                && instance.getUser(player).isProtected()) {
            e.setCancelled(true);
        }
    }
}
