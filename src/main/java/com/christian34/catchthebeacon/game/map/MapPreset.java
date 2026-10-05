package com.christian34.catchthebeacon.game.map;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.Debug;
import com.christian34.catchthebeacon.files.ArenasFile;
import com.christian34.catchthebeacon.files.FileManager;
import com.christian34.catchthebeacon.files.LobbyFile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A map of somebody else that can't be shipped, but whose setup is prepared (maps/presets/&lt;name&gt;.yml): the admin
 * downloads it and puts the zip (or the world folder) into plugins/CatchTheBeacon/maps - it is set up on the next
 * start or with /ctb arena create &lt;name&gt;, with the lobby that comes with the plugin ({@link #LOBBY}).
 */
public record MapPreset(String name, String author, String url, ConfigurationSection arena) {
    /**
     * the lobby that comes with the plugin (maps/lobby.zip), installed with the first preset
     */
    public static final String LOBBY = "default_lobby";
    private static final List<String> NAMES = List.of("sakura");
    private static final String LOBBY_ZIP = "maps/lobby.zip";
    private static final String WORLD_GEN_SETTINGS = "data/minecraft/world_gen_settings.dat";

    @NotNull
    public static List<MapPreset> all(CatchTheBeacon plugin) {
        List<MapPreset> presets = new ArrayList<>();
        for (String name : NAMES) {
            try {
                YamlConfiguration config = loadResource(plugin, "maps/presets/" + name + ".yml");
                presets.add(new MapPreset(name, config.getString("author"), config.getString("url"),
                        config.getConfigurationSection("arena")));
            } catch (IOException ex) {
                Debug.warn("Couldn't load the map preset '" + name + "': " + ex);
            }
        }
        return presets;
    }

    @Nullable
    public static MapPreset get(CatchTheBeacon plugin, String name) {
        return all(plugin).stream().filter(preset -> preset.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    /**
     * the presets without an arena yet (to tell the admins about them)
     */
    @NotNull
    public static List<MapPreset> notInstalled(CatchTheBeacon plugin) {
        return all(plugin).stream().filter(preset -> !hasArena(plugin, preset.name())).toList();
    }

    /**
     * sets up the downloaded presets in the map folder (on the start, not on lobby servers of a network)
     */
    static void installDownloaded(CatchTheBeacon plugin, File mapDirectory) {
        if ("lobby".equalsIgnoreCase(plugin.getFileManager().getConfigFile().getString("network.mode"))) return;
        for (MapPreset preset : notInstalled(plugin)) {
            if (preset.findDownload(mapDirectory) == null) continue;
            preset.install(plugin, mapDirectory);
        }
    }

    /**
     * the name for messages (Sakura), {@link #name()} is the name of the arena (sakura)
     */
    public String displayName() {
        return name.isEmpty() ? name : name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    private static boolean hasArena(CatchTheBeacon plugin, String name) {
        ConfigurationSection arenas = plugin.getFileManager().getArenasFile().getSection("");
        return arenas != null && arenas.getKeys(false).stream().anyMatch(name::equalsIgnoreCase);
    }

    /**
     * the downloaded map: a world folder with the name of the preset or a zip with the name in its file name
     */
    @Nullable
    public File findDownload(File mapDirectory) {
        File[] files = mapDirectory.listFiles();
        if (files == null) return null;
        for (File file : files) {
            String fileName = file.getName().toLowerCase(Locale.ROOT);
            if (file.isDirectory() && fileName.equals(name)) return file;
            if (file.isFile() && fileName.endsWith(".zip") && fileName.contains(name)) return file;
        }
        return null;
    }

    /**
     * sets up the downloaded map as arena (a zip is extracted to maps/&lt;name&gt; and deleted)
     *
     * @return the name of the arena, null if it couldn't be installed (see the log)
     */
    @Nullable
    public String install(CatchTheBeacon plugin, File mapDirectory) {
        File download = findDownload(mapDirectory);
        if (download == null) return null;
        try {
            File map = download.isDirectory() ? download : extract(download, mapDirectory);
            addVoidGenerator(plugin, map);
            installLobby(plugin, mapDirectory);
            ArenasFile arenasFile = plugin.getFileManager().getArenasFile();
            copy(arena, arenasFile.getData(), "arenas." + map.getName());
            arenasFile.getData().set("arenas." + map.getName() + ".lobby", LOBBY);
            arenasFile.save();
            if (download.isFile() && !download.delete()) {
                Debug.warn("Couldn't delete " + download + " after extracting it");
            }
            Debug.info("Set up the map '" + map.getName() + "' by " + author + " - it can be played right away");
            return map.getName();
        } catch (IOException | RuntimeException ex) {
            Debug.warn("Couldn't set up the map '" + name + "' (" + download + "): " + ex);
            return null;
        }
    }

    /**
     * extracts the world of the zip to maps/&lt;name&gt; (the world may be in a folder of the zip)
     */
    private File extract(File zip, File mapDirectory) throws IOException {
        File target = new File(mapDirectory, name);
        if (target.exists()) throw new IOException("the folder " + target + " exists already");
        File temp = new File(mapDirectory, "." + name + "_download");
        FileManager.deleteDirectory(temp);
        try {
            try (InputStream in = new FileInputStream(zip)) {
                unzip(in, temp, entry -> entry);
            }
            File world = findWorld(temp, 3);
            if (world == null) throw new IOException("there is no world (region folder) in " + zip.getName());
            if (!FileManager.copyFolder(world, target)) throw new IOException("couldn't copy the world to " + target);
            return target;
        } catch (IOException ex) {
            FileManager.deleteDirectory(target);
            throw ex;
        } finally {
            FileManager.deleteDirectory(temp);
        }
    }

    @Nullable
    private static File findWorld(File folder, int depth) {
        if (new File(folder, "region").isDirectory()) return folder;
        File[] folders = folder.listFiles(File::isDirectory);
        if (folders == null || depth == 0) return null;
        for (File child : folders) {
            File world = findWorld(child, depth - 1);
            if (world != null) return world;
        }
        return null;
    }

    /**
     * makes sure the lobby that comes with the plugin exists with its spawn
     */
    private static void installLobby(CatchTheBeacon plugin, File mapDirectory) throws IOException {
        if (!new File(mapDirectory, LOBBY).isDirectory()) {
            try (InputStream in = plugin.getResource(LOBBY_ZIP)) {
                if (in == null) throw new IOException(LOBBY_ZIP + " is missing in the jar");
                unzip(in, mapDirectory, entry -> entry.startsWith(LOBBY + "/") ? entry : null);
            }
        }
        LobbyFile lobbyFile = plugin.getFileManager().getLobbyFile();
        if (!lobbyFile.getData().contains(LOBBY + ".spawn.x")) {
            copy(loadResource(plugin, "maps/lobby.yml"), lobbyFile.getData(), LOBBY);
            lobbyFile.save();
        }
    }

    /**
     * the void generator of the lobby (world_gen_settings.dat) for a map without one, so it doesn't get normal
     * terrain around it
     */
    private static void addVoidGenerator(CatchTheBeacon plugin, File map) throws IOException {
        File settings = new File(map, WORLD_GEN_SETTINGS);
        if (settings.exists()) return;
        String source = LOBBY + "/" + WORLD_GEN_SETTINGS;
        try (InputStream in = plugin.getResource(LOBBY_ZIP)) {
            if (in == null) throw new IOException(LOBBY_ZIP + " is missing in the jar");
            unzip(in, map, entry -> entry.equals(source) ? WORLD_GEN_SETTINGS : null);
        }
    }

    private static YamlConfiguration loadResource(CatchTheBeacon plugin, String path) throws IOException {
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) throw new IOException(path + " is missing in the jar");
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    private static void copy(@Nullable ConfigurationSection from, FileConfiguration to, String path) {
        if (from == null) return;
        for (String key : from.getKeys(true)) {
            if (!from.isConfigurationSection(key)) {
                to.set(path + "." + key, from.get(key));
            }
        }
    }

    /**
     * @param entries the path of an entry in the target folder, null to skip it
     */
    private static void unzip(InputStream in, File target, Function<String, String> entries) throws IOException {
        File root = target.getCanonicalFile();
        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entries.apply(entry.getName().replace('\\', '/'));
                if (name == null) continue;
                File file = new File(root, name).getCanonicalFile();
                if (!file.toPath().startsWith(root.toPath())) throw new IOException("invalid entry " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(file.toPath());
                } else {
                    Files.createDirectories(file.getParentFile().toPath());
                    Files.copy(zip, file.toPath());
                }
            }
        }
    }

}
