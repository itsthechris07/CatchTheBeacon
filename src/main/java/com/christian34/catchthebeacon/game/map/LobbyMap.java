package com.christian34.catchthebeacon.game.map;


import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.files.ArenasFile;
import com.christian34.catchthebeacon.files.FileManager;
import com.christian34.catchthebeacon.files.LobbyFile;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class LobbyMap implements GameMap {
    private final Arena arena;
    private final ArenasFile arenasFile;
    private final String mapName;
    private Location spawnLocation;

    private File directory;

    protected LobbyMap(Arena arena) {
        this.arena = arena;
        FileManager fileManager = CatchTheBeacon.getInstance().getFileManager();
        this.arenasFile = fileManager.getArenasFile();
        this.mapName = arenasFile.getString(arena.getName() + ".lobby");
        if (mapName == null) {
            Debug.warn("Arena '" + arena.getName() + "' does not have a lobby!");
            return;
        }
        this.directory = new File(FileManager.getPluginFolder() + "/maps", mapName);
        if (!directory.exists()) {
            Debug.warn("No lobby world has been saved for arena '" + arena.getName() + "'!");
        }

        LobbyFile config = getConfig();
        String path = getName() + ".spawn.";
        if (config.getData().contains(path + "x")) {
            this.spawnLocation = new Location(null, config.getDouble(path + "x"), config.getDouble(path + "y"),
                    config.getDouble(path + "z"), (float) config.getDouble(path + "yaw"),
                    (float) config.getDouble(path + "pitch"));
        } else {
            this.spawnLocation = null;
            Debug.warn("You haven't set the spawn for lobby '" + mapName + "'!");
        }
    }

    public File getDirectory() {
        return directory;
    }

    private LobbyFile getConfig() {
        return CatchTheBeacon.getInstance().getFileManager().getLobbyFile();
    }

    @Nullable
    public Location getSpawnLocation() {
        if (spawnLocation == null) {
            return null;
        }
        return spawnLocation.clone();
    }

    public void setSpawnLocation(@NotNull Location location) {
        LobbyFile config = getConfig();
        String path = getName() + ".spawn.";
        config.getData().set(path + "x", location.getX());
        config.getData().set(path + "y", location.getY());
        config.getData().set(path + "z", location.getZ());
        config.getData().set(path + "yaw", location.getYaw());
        config.getData().set(path + "pitch", location.getPitch());
        config.save();
        this.spawnLocation = location;
        spawnLocation.setWorld(null);
    }

    @Override
    public String getName() {
        return mapName;
    }

    @Override
    public boolean isPlayable() {
        return directory != null && directory.isDirectory() && spawnLocation != null;
    }

}
