package com.christian34.catchthebeacon.game.map;

import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.files.FileManager;
import com.christian34.catchthebeacon.utils.WorldUtils;
import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Set;

/**
 * A temporary copy of a map in plugins/CatchTheBeacon/maps, loaded as its own world.
 * <p>
 * Since 26.1 Paper stores every world as a dimension of the main world ({@code world/dimensions/minecraft/<key>}),
 * a folder next to the main world is ignored (without level.dat) or moved there once (with level.dat). So the map
 * is copied directly into the dimension folder and loaded by its key.
 */
public class GameWorld {
    public static final String PREFIX = "ctb_";
    /**
     * files and folders of a (pre 26.1) world save that don't belong into a dimension folder
     */
    private static final Set<String> SKIPPED_FILES = Set.of("level.dat", "level.dat_old", "uid.dat", "session.lock",
            "DIM-1", "DIM1", "dimensions", "playerdata", "players", "advancements", "stats", "datapacks");
    private static File dimensionsFolder;
    private static final String METADATA_FILE = "data/paper/metadata.dat";
    private final File sourceFile;
    private final String worldName;
    private WeakReference<World> world;

    public GameWorld(File sourceFile, String name) {
        this.sourceFile = sourceFile;
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IllegalArgumentException("Map folder " + sourceFile + " does not exist!");
        }
        this.worldName = (PREFIX + name).toLowerCase(Locale.ROOT);
    }

    /**
     * the folder the worlds of the server are stored in (world/dimensions/minecraft)
     */
    public static File getDimensionsFolder() {
        if (dimensionsFolder != null) return dimensionsFolder;
        return Bukkit.getWorlds().getFirst().getWorldFolder().getParentFile();
    }

    /**
     * the folder of a loaded world: dimensions/namespace/key (like {@link World#getWorldFolder()}, which MockBukkit lacks)
     */
    public static File getFolder(World world) {
        NamespacedKey key = world.getKey();
        return new File(getDimensionsFolder().getParentFile(), key.getNamespace() + "/" + key.getKey());
    }

    /**
     * only for the tests: MockBukkit doesn't know where worlds are stored
     */
    public static void setDimensionsFolder(@Nullable File folder) {
        dimensionsFolder = folder;
    }

    /**
     * deletes all copies of maps that are left over (e.g. after a crash)
     */
    public static void deleteLeftovers() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().startsWith(PREFIX)) {
                Bukkit.unloadWorld(world, false);
            }
        }
        File[] folders = getDimensionsFolder().listFiles(file -> file.isDirectory() && file.getName().startsWith(PREFIX));
        if (folders == null) return;
        for (File folder : folders) {
            Debug.info("Deleting leftover world " + folder.getName());
            FileManager.deleteDirectory(folder);
        }
    }

    /**
     * the folder with the world data of the map: the map itself, or the overworld of a world saved with 26.1+
     */
    private File getMapData() {
        File overworld = new File(sourceFile, "dimensions/minecraft/overworld");
        return overworld.isDirectory() ? overworld : sourceFile;
    }

    private File getWorldFolder() {
        return new File(getDimensionsFolder(), worldName);
    }

    public void loadWorld() {
        if (getWorld() != null) return;

        File worldFolder = getWorldFolder();
        FileManager.deleteDirectory(worldFolder);
        if (!copyMap(getMapData(), worldFolder)) {
            throw new IllegalStateException("Couldn't copy map " + sourceFile + " to " + worldFolder);
        }

        World world = new WorldCreator(NamespacedKey.minecraft(worldName)).createWorld();
        if (world == null) {
            FileManager.deleteDirectory(worldFolder);
            throw new IllegalStateException("Couldn't load world " + worldName + " (see the log above)");
        }

        this.world = new WeakReference<>(world);
        world.setAutoSave(false);
        world.setTime(2000);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.MOB_DROPS, false);
        world.setGameRule(GameRules.KEEP_INVENTORY, true);
        world.setGameRule(GameRules.SHOW_DEATH_MESSAGES, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.SPAWN_MONSTERS, false);
        world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        if (WorldUtils.isPlayWorld(world)) {
            // mobs of chunks loaded later are removed by the WorldEventListener
            world.getEntitiesByClass(Mob.class).forEach(Entity::remove);
        }
    }

    private static boolean copyMap(File from, File to) {
        if (!to.mkdirs() && !to.isDirectory()) return false;
        File[] files = from.listFiles();
        if (files == null) return false;
        for (File file : files) {
            if (SKIPPED_FILES.contains(file.getName())) continue;
            if (!FileManager.copyFolder(file, new File(to, file.getName()))) return false;
        }
        // holds the uuid of the world (like uid.dat before 26.1): Paper refuses to load a world with the uuid of an
        // already loaded one, e.g. if the map is a copy of a world of the server - without it Paper creates a new one
        new File(to, METADATA_FILE).delete();
        return true;
    }

    /**
     * unloads the world and deletes its folder
     *
     * @param save true to copy the world back into the map folder (after editing the map)
     */
    public void close(boolean save) throws Exception {
        World world = getWorld();
        if (world == null) return;

        if (!world.getPlayers().isEmpty()) {
            throw new Exception("Can't close world '" + worldName + "'! There are still players in the world.");
        }

        if (!Bukkit.unloadWorld(world, save)) {
            throw new Exception("Couldn't unload world '" + worldName + "'!");
        }
        this.world = null;
        File worldFolder = getWorldFolder();
        if (save && !copyMap(worldFolder, getMapData())) {
            Debug.warn("Couldn't save world '" + worldName + "' to " + getMapData() + " - the copy is kept in " + worldFolder);
            return;
        }
        FileManager.deleteDirectory(worldFolder);
    }

    public World getWorld() {
        if (this.world == null) {
            return null;
        }

        World world = this.world.get();
        Preconditions.checkArgument(world != null, "World unloaded");
        return world;
    }

    public String getWorldName() {
        return worldName;
    }

}
