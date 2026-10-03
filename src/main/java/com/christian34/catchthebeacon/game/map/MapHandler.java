package com.christian34.catchthebeacon.game.map;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.files.ArenasFile;
import com.christian34.catchthebeacon.files.FileManager;
import com.christian34.catchthebeacon.game.IncompleteConfigurationException;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MapHandler {
    private final ArenasFile arenasFile;
    private final Set<Arena> arenas;
    private final CatchTheBeacon instance;
    private final File mapDirectory;

    public MapHandler(CatchTheBeacon instance) {
        this.arenasFile = instance.getFileManager().getArenasFile();
        this.arenas = Collections.synchronizedSet(new HashSet<>());
        this.instance = instance;
        this.mapDirectory = new File(FileManager.getPluginFolder(), "maps/");
        MapPreset.installDownloaded(instance, mapDirectory);
        loadMaps();
        if (arenas.isEmpty()) {
            for (MapPreset preset : MapPreset.notInstalled(instance)) {
                Debug.info("No arenas yet? Download '" + preset.name() + "' by " + preset.author() + " (" + preset.url()
                        + "), put the zip into " + mapDirectory + " and restart - its setup comes with CatchTheBeacon");
            }
        }
    }

    public ArenasFile getArenasFile() {
        return arenasFile;
    }

    public Set<Arena> getGameMaps() {
        return arenas;
    }

    private void loadMaps() {
        for (File file : mapDirectory.listFiles()) {
            if (file.isDirectory()) {
                String name = file.getName();
                name = name.replace(" ", "_").replaceAll("[^A-Za-z0-9_]", "");
                //noinspection ResultOfMethodCallIgnored
                file.renameTo(new File(mapDirectory, name));
            }
        }
        ConfigurationSection section = arenasFile.getSection("");
        if (section == null) {
            Debug.warn("no arenas found");
            return;
        }

        for (String mapName : section.getKeys(false)) {
            try {
                arenas.add(new Arena(mapName));
            } catch (IncompleteConfigurationException ignored) {
                Debug.warn("The configuration for arena '" + mapName + "' is not completed yet!");
            }
        }
    }

    public boolean createMap(String name) {
        File file = new File(FileManager.getPluginFolder(), "maps/" + name);
        if (!file.exists() || !file.isDirectory()) {
            return false;
        }
        String prefix = name + ".";
        arenasFile.set(prefix + "min-players", 2);
        arenasFile.set(prefix + "max-players", 8);
        arenasFile.save();
        try {
            Arena arena = new Arena(name);
            arenas.add(arena);
        } catch (IncompleteConfigurationException ex) {
            Debug.warn(ex.getMessage());
            return false;
        }
        return true;
    }

    /**
     * folder names in plugins/CatchTheBeacon/maps may only contain letters, digits and _
     */
    public static String toMapName(String name) {
        return name.replace(" ", "_").replaceAll("[^A-Za-z0-9_]", "");
    }

    /**
     * sets up the downloaded map of the preset (see {@link MapPreset#install})
     *
     * @return the name of the arena, null if it couldn't be set up
     */
    @Nullable
    public String installPreset(MapPreset preset) {
        return preset.install(instance, mapDirectory);
    }

    @Nullable
    public File findPresetDownload(MapPreset preset) {
        return preset.findDownload(mapDirectory);
    }

    public boolean isMap(String name) {
        return new File(mapDirectory, name).isDirectory();
    }

    public List<String> getMapNames() {
        File[] folders = mapDirectory.listFiles(File::isDirectory);
        if (folders == null) return List.of();
        return Arrays.stream(folders).map(File::getName).sorted().toList();
    }

    /**
     * worlds of the server that can be imported as a map (not the copies made by this plugin)
     */
    public static List<World> getImportableWorlds() {
        return Bukkit.getWorlds().stream().filter(world -> !world.getName().startsWith(GameWorld.PREFIX)).toList();
    }

    @Nullable
    public static World getImportableWorld(String name) {
        return getImportableWorlds().stream().filter(world -> world.getName().equalsIgnoreCase(name)).findFirst()
                .orElse(null);
    }

    /**
     * copies a (loaded) world of the server into plugins/CatchTheBeacon/maps
     *
     * @return false if the map already exists or the world couldn't be copied
     */
    public boolean importWorld(World world, String mapName) {
        File target = new File(mapDirectory, mapName);
        if (target.exists()) return false;
        try {
            world.save();
        } catch (RuntimeException ex) {
            // not implemented by MockBukkit - on a server the copy would miss the latest changes
            Debug.warn("Couldn't save world '" + world.getName() + "' before copying it: " + ex);
        }
        File source = GameWorld.getFolder(world);
        if (!FileManager.copyFolder(source, target)) {
            Debug.warn("Couldn't copy world '" + world.getName() + "' from " + source + " to " + target);
            FileManager.deleteDirectory(target);
            return false;
        }
        Debug.info("Imported world '" + world.getName() + "' as map '" + mapName + "'");
        return true;
    }

    @Nullable
    public Arena getArena(String name) {
        for (Arena map : arenas) {
            if (map.getName().equalsIgnoreCase(name)) {
                return map;
            }
        }
        return null;
    }

}
