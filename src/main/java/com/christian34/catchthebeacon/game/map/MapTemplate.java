package com.christian34.catchthebeacon.game.map;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.files.FileManager;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.Set;


public class MapTemplate {
    private static final Set<MapTemplate> templates = new HashSet<>();
    private final GameMap map;
    private final GameWorld gameWorld;
    private boolean loading;

    private MapTemplate(GameMap map) {
        this.map = map;
        File sourceFile = new File(FileManager.getPluginFolder() + "/maps/" + map.getName());
        this.gameWorld = new GameWorld(sourceFile, sourceFile.getName() + "_temp");
    }

    public static Set<MapTemplate> getTemplates() {
        return templates;
    }

    @NotNull
    public static MapTemplate get(GameMap map) {
        for (MapTemplate template : templates) {
            if (template.getMap().equals(map)) {
                return template;
            }
        }
        Debug.info("Creating template for map '" + map.getName() + "'...");
        MapTemplate template = new MapTemplate(map);
        templates.add(template);
        return template;
    }

    public static boolean hasTemplate(GameMap map) {
        for (MapTemplate template : templates) {
            if (template.getMap().equals(map)) {
                return true;
            }
        }
        return false;
    }

    public World getWorld() {
        return gameWorld.getWorld();
    }

    public GameMap getMap() {
        return map;
    }

    /**
     * loads the editable copy of the map (if it isn't loaded yet)
     *
     * @throws RuntimeException if the world couldn't be loaded (the template is removed then)
     */
    @NotNull
    public World load() {
        if (loading) {
            // loading a world handles waiting packets, which may try to load it again
            throw new IllegalStateException("The world is still loading");
        }
        if (this.gameWorld.getWorld() == null) {
            loading = true;
            try {
                this.gameWorld.loadWorld();
            } catch (RuntimeException ex) {
                templates.remove(this);
                throw ex;
            } finally {
                loading = false;
            }
        }
        return getWorld();
    }

    /**
     * the lobby spawn (if it is a lobby and the spawn is set), otherwise the spawn of the world
     */
    @NotNull
    public Location getSpawn() {
        World world = load();
        if (map instanceof LobbyMap lobbyMap && lobbyMap.getSpawnLocation() != null) {
            Location spawn = lobbyMap.getSpawnLocation();
            spawn.setWorld(world);
            return spawn;
        }
        return world.getSpawnLocation();
    }

    public void join(GamePlayer gamePlayer) {
        gamePlayer.sendMessage(LangText.WORLD_LOADING);
        try {
            load();
        } catch (RuntimeException ex) {
            gamePlayer.sendMessage(LangText.WORLD_LOAD_FAILED, ex.getMessage());
            throw ex;
        }

        gamePlayer.getUserStorage().store();
        gamePlayer.getUserStorage().cleanPlayer();
        gamePlayer.getPlayer().setGameMode(GameMode.CREATIVE);
        gamePlayer.getPlayer().setAllowFlight(true);
        gamePlayer.getPlayer().teleport(getSpawn());
    }

    public void quit(GamePlayer gamePlayer) {
        gamePlayer.getUserStorage().restore();
        World playersWorld = gamePlayer.getPlayer().getLocation().getWorld();
        if (playersWorld == null) return;
        if (playersWorld.getName().startsWith("ctb_")) {
            gamePlayer.getPlayer().teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
    }

    public void close() {
        for (GamePlayer gamePlayer : getPlayers()) {
            quit(gamePlayer);
        }
        try {
            gameWorld.close(true);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        MapTemplate.getTemplates().remove(this);
    }

    public Set<GamePlayer> getPlayers() {
        Set<GamePlayer> players = new HashSet<>();
        for (Player player : getWorld().getPlayers()) {
            GamePlayer gamePlayer = CatchTheBeacon.getInstance().getUser(player);
            players.add(gamePlayer);
        }
        return players;
    }

}
