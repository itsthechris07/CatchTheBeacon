package com.christian34.catchthebeacon.listeners;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.states.GameState;
import com.christian34.catchthebeacon.lib.Menu;
import com.christian34.catchthebeacon.user.GamePlayer;
import com.christian34.catchthebeacon.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.plugin.PluginManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EventListener {
    private final CatchTheBeacon instance;
    private final Map<GameState, List<String>> blockedPlayerEvents;
    private final Map<GameState, List<String>> blockedWorldEvents;

    public EventListener() {
        this.instance = CatchTheBeacon.getInstance();
        this.blockedPlayerEvents = new HashMap<>();
        this.blockedPlayerEvents.put(GameState.LOBBY, Arrays.asList("PlayerBedEnterEvent",
                "PlayerDropItemEvent", "EntityPickupItemEvent", "EntityDamageEvent", "EntityDamageByEntityEvent",
                "BlockBreakEvent", "BlockPlaceEvent", "FoodLevelChangeEvent", "BlockMultiPlaceEvent", "CraftItemEvent",
                "PlayerArmorStandManipulateEvent", "ProjectileLaunchEvent", "PlayerInteractEntityEvent",
                "PlayerBucketEmptyEvent", ""));
        // the winner is celebrated with fireworks, which would hurt the players
        this.blockedPlayerEvents.put(GameState.ENDING, Arrays.asList("PlayerDropItemEvent", "EntityPickupItemEvent",
                "EntityDamageEvent", "EntityDamageByEntityEvent", "BlockBreakEvent", "BlockPlaceEvent",
                "FoodLevelChangeEvent", "BlockMultiPlaceEvent", "ProjectileLaunchEvent", "PlayerBucketEmptyEvent",
                "PlayerInteractEntityEvent", "PlayerArmorStandManipulateEvent"));
        this.blockedWorldEvents = new HashMap<>();
        this.blockedWorldEvents.put(GameState.LOBBY, Arrays.asList("BlockBurnEvent", "EntityExplodeEvent",
                "BlockFadeEvent", "BlockFormEvent", "StructureGrowEvent", "CreatureSpawnEvent", "EntityChangeBlockEvent"));
    }

    public boolean cancel(Player player, Cancellable event) {
        if (!WorldUtils.isGameWorld(player.getWorld())) return false;
        GamePlayer gamePlayer = instance.getUser(player);
        Game game = gamePlayer.getGame();
        if (game == null) return false;
        // spectators can't change anything
        if (gamePlayer.isSpectator()) return !event.isCancelled();
        return check(this.blockedPlayerEvents.get(game.getGameState()), event);
    }

    public boolean cancel(Location location, Cancellable event) {
        if (!WorldUtils.isGameWorld(location.getWorld())) return false;
        Game game = instance.getGameManager().getGame(location.getWorld());
        if (game == null) return false;

        return check(this.blockedWorldEvents.get(game.getGameState()), event);
    }

    private boolean check(List<String> blocked, Cancellable event) {
        if (event.isCancelled() || blocked == null) return false;
        String eventName = event.getClass().getSimpleName();
        return blocked.contains(eventName);
    }

    public void registerEvents() {
        PluginManager pm = Bukkit.getServer().getPluginManager();
        pm.registerEvents(new QuitListener(this.instance), this.instance);
        pm.registerEvents(new TeleportListener(this.instance), this.instance);
        pm.registerEvents(new ChatListener(instance), this.instance);
        pm.registerEvents(new LobbyInteractionListener(instance), this.instance);
        pm.registerEvents(new PlayerEventListener(this), this.instance);
        pm.registerEvents(new WorldEventListener(this), this.instance);
        pm.registerEvents(new SpectatorListener(this.instance), this.instance);
        pm.registerEvents(new RoundListener(this.instance), this.instance);
        pm.registerEvents(new Menu.ChestListener(), this.instance);
    }

}
